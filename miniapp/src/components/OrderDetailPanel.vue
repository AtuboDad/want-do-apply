<template>
  <view class="panel">
    <!-- 滚动区 -->
    <scroll-view class="panel-scroll" scroll-y :show-scrollbar="false">
      <view v-if="order" class="content">
        <!-- 主卡片 -->
        <view class="main-card">
          <view class="author">
            <image class="avatar" :src="fullUrl(order.userAvatar) || defaultAvatar" mode="aspectFill" />
            <view class="grow">
              <text class="name">{{ order.userNickName || '匿名审批员' }}</text>
              <text class="time">{{ fromNow(order.createTime) }}</text>
            </view>
            <view
              class="status"
              :style="{ color: statusColor(order.status), background: statusColor(order.status) + '1A' }"
            >
              {{ statusText(order.status) }}
            </view>
          </view>

          <text class="title">{{ order.title }}</text>
          <view class="price-row">
            <text class="currency">¥</text>
            <text class="price">{{ money(order.price) }}</text>
          </view>

          <view v-if="order.status === '0'" class="cd-row">
            <text class="cd-label">距截止还有</text>
            <text class="cd-value">{{ countdown(order.remainSeconds) }}</text>
          </view>

          <!-- 票数 -->
          <view class="tally">
            <view class="tally-bar">
              <view class="bar-pass" :style="{ width: passPercent + '%' }" />
              <view class="bar-reject" :style="{ width: 100 - passPercent + '%' }" />
            </view>
            <view class="tally-text">
              <text class="t-pass">通过 {{ order.passCount || 0 }}</text>
              <text class="t-mid">已批 {{ order.voteCount || 0 }} / {{ order.targetVotes || 10 }}</text>
              <text class="t-reject">驳回 {{ order.rejectCount || 0 }}</text>
            </view>
          </view>
        </view>

        <!-- 理由 -->
        <view class="block">
          <text class="block-title">购买理由</text>
          <text class="block-text">{{ order.reason }}</text>
        </view>

        <view v-if="order.substitute" class="block">
          <text class="block-title">有没有替代品</text>
          <text class="block-text">{{ order.substitute }}</text>
        </view>

        <view v-if="order.imageList && order.imageList.length" class="block">
          <text class="block-title">商品图</text>
          <view class="img-grid">
            <image
              v-for="(img, i) in order.imageList"
              :key="i"
              class="img"
              :src="fullUrl(img)"
              mode="aspectFill"
              @tap="preview(i)"
            />
          </view>
        </view>

        <!-- 批条 -->
        <view class="block">
          <view class="block-head">
            <text class="block-title">网友批条</text>
            <view class="sorter">
              <text :class="{ on: voteSort === 'hot' }" @tap="switchVoteSort('hot')">热门</text>
              <text class="sep">|</text>
              <text :class="{ on: voteSort === 'new' }" @tap="switchVoteSort('new')">最新</text>
            </view>
          </view>
          <VoteCard v-for="v in votes" :key="v.id" :item="v" @like="onLike" />
          <Empty v-if="votes.length === 0" text="还没有人批，快去摇人" emoji="✍️" />
        </view>

        <!-- 评论 -->
        <view class="block">
          <view class="block-head">
            <text class="block-title">讨论区 ({{ order.commentCount || 0 }})</text>
            <view class="c-add" @tap="openComment()">
              <text>写评论</text>
            </view>
          </view>
          <view v-for="c in comments" :key="c.id" class="comment">
            <image class="c-avatar" :src="fullUrl(c.userAvatar) || defaultAvatar" mode="aspectFill" />
            <view class="grow">
              <view class="c-head">
                <text class="c-name">{{ c.userNickName || '匿名' }}</text>
                <text class="c-time">{{ fromNow(c.createTime) }}</text>
              </view>
              <text class="c-text">{{ c.content }}</text>
              <view class="c-reply" @tap="replyTo(c)">
                <text>回复</text>
              </view>
              <view v-for="ch in c.children" :key="ch.id" class="sub">
                <text class="sub-name">{{ ch.userNickName }}</text>
                <text v-if="ch.replyNickName" class="sub-to"> 回复 {{ ch.replyNickName }}</text>
                <text class="sub-text">：{{ ch.content }}</text>
              </view>
            </view>
          </view>
          <Empty v-if="comments.length === 0" text="还没有讨论" emoji="💬" />
        </view>

        <view class="bottom-space" />
      </view>

      <view v-else class="loading">
        <text>加载中…</text>
      </view>
    </scroll-view>

    <!-- 底部操作 -->
    <view v-if="order" class="action-bar">
      <view v-if="canVote" class="vote-actions">
        <button class="btn reject" @tap="openVote('2')">驳回</button>
        <button class="btn pass" @tap="openVote('1')">通过</button>
      </view>
      <view v-else-if="order.mine" class="mine-actions">
        <text class="hint">{{ mineHint }}</text>
        <button v-if="order.status === '0'" class="btn ghost" @tap="cancelMine">撤销</button>
      </view>
      <view v-else class="voted-tip">
        <text>{{ order.myVoted ? '你已经批过这单了' : '这单已经结案' }}</text>
      </view>
    </view>

    <!-- 批阅弹窗 -->
    <view v-if="voteVisible" class="mask" @tap="closeVote">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">{{ voteResult === '1' ? '通过这单' : '驳回这单' }}</text>
          <text class="sheet-close" @tap="closeVote">✕</text>
        </view>
        <text class="sheet-tip">写点理由，这是灵魂</text>
        <textarea
          v-model="voteReason"
          class="sheet-input"
          :placeholder="voteResult === '1' ? '说说为什么该买…' : '说说为什么不该买…'"
          maxlength="200"
          :focus="true"
          auto-height
        />
        <view class="quick">
          <text v-for="q in quickWords" :key="q" class="quick-item" @tap="voteReason = q">{{ q }}</text>
        </view>
        <button class="submit" :class="voteResult === '1' ? 'pass' : 'reject'" @tap="doVote">提交批条</button>
      </view>
    </view>

    <!-- 评论输入 -->
    <view v-if="commentVisible" class="mask" @tap="closeComment">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">{{ replyTarget ? '回复 ' + (replyTarget.userNickName || '') : '说点什么' }}</text>
          <text class="sheet-close" @tap="closeComment">✕</text>
        </view>
        <textarea
          v-model="commentText"
          class="sheet-input"
          placeholder="友善发言，别把天聊死…"
          maxlength="500"
          :focus="true"
          auto-height
        />
        <button class="submit pass" @tap="doComment">发布</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import VoteCard from './VoteCard.vue'
import Empty from './Empty.vue'
import { getDetail, cancelOrder, toggleFavorite, removeOrder } from '../api/order'
import { submitVote, likeVote, addComment, getComments } from '../api/vote'
import { report } from '../api/group'
import { fullUrl } from '../utils/request'
import { fromNow, money, countdown, statusText, statusColor } from '../utils/format'
import { ensureLogin } from '../utils/auth'
import type { OrderItem, VoteItem, CommentItem } from '../api/types'

const props = defineProps<{ orderId: number }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'more'): void
}>()

const defaultAvatar =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcng9IjUwIiBmaWxsPSIjRkVFRkU2Ii8+PGNpcmNsZSBjeD0iNTAiIGN5PSIzOCIgcj0iMTYiIGZpbGw9IiNGMEM4QTQiLz48cGF0aCBkPSJNMjIgODJjMC0xNiAxMi0yOCAyOC0yOHMyOCAxMiAyOCAyOCIgZmlsbD0iI0YwQzhBNCIvPjwvc3ZnPg=='

const order = ref<OrderItem | null>(null)
const votes = ref<VoteItem[]>([])
const comments = ref<CommentItem[]>([])
const voteSort = ref<'hot' | 'new'>('hot')

const voteVisible = ref(false)
const voteResult = ref('1')
const voteReason = ref('')
const commentVisible = ref(false)
const commentText = ref('')
const replyTarget = ref<CommentItem | null>(null)

const quickWords = ['性价比不高', '已有类似款', '真的需要吗', '等打折再说', '买！我支持', '生活需要仪式感']

const passPercent = computed(() => {
  const p = order.value?.passCount || 0
  const r = order.value?.rejectCount || 0
  if (p + r === 0) return 50
  return Math.round((p / (p + r)) * 100)
})

const canVote = computed(() => {
  const o = order.value
  return !!o && o.status === '0' && !o.mine && !o.myVoted && (o.remainSeconds || 0) > 0
})

const mineHint = computed(() => {
  const o = order.value
  if (!o) return ''
  if (o.status === '0') return '等待网友审判中…'
  return ''
})

async function load() {
  if (!props.orderId) return
  await ensureLogin()
  const res = await getDetail(props.orderId)
  order.value = res.order
  votes.value = res.votes || []
  comments.value = res.comments || []
}

watch(
  () => props.orderId,
  (id) => {
    if (!id) return
    order.value = null
    votes.value = []
    comments.value = []
    voteSort.value = 'hot'
    load()
  },
  { immediate: true }
)

async function doReport() {
  try {
    await report({ targetType: '1', targetId: props.orderId, reasonType: 'other', content: '用户举报' })
    uni.showToast({ title: '已提交举报', icon: 'none' })
  } catch (e) {
    // ignore
  }
}

function openVote(result: string) {
  voteResult.value = result
  voteReason.value = ''
  voteVisible.value = true
}

function closeVote() {
  voteVisible.value = false
}

async function doVote() {
  if (voteReason.value.trim().length < 2) {
    uni.showToast({ title: '理由至少写 2 个字', icon: 'none' })
    return
  }
  const settled = await submitVote({
    orderId: props.orderId,
    result: voteResult.value,
    reason: voteReason.value.trim()
  })
  voteVisible.value = false
  uni.showToast({ title: settled ? '这单结案了' : '批完啦', icon: 'none' })
  await load()
}

async function onLike(vid: number) {
  const liked = await likeVote(vid)
  const v = votes.value.find((x) => x.id === vid)
  if (v) {
    v.liked = liked
    v.likeCount = Math.max((v.likeCount || 0) + (liked ? 1 : -1), 0)
  }
}

async function switchVoteSort(s: 'hot' | 'new') {
  if (voteSort.value === s) return
  voteSort.value = s
  const res = await getDetail(props.orderId)
  votes.value = res.votes || []
}

function openComment(target?: CommentItem) {
  replyTarget.value = target || null
  commentText.value = ''
  commentVisible.value = true
}

function replyTo(c: CommentItem) {
  openComment(c)
}

function closeComment() {
  commentVisible.value = false
  replyTarget.value = null
}

async function doComment() {
  if (!commentText.value.trim()) {
    uni.showToast({ title: '说点什么吧', icon: 'none' })
    return
  }
  await addComment({
    orderId: props.orderId,
    content: commentText.value.trim(),
    parentId: replyTarget.value?.parentId ? replyTarget.value.parentId : replyTarget.value?.id,
    replyUserId: replyTarget.value?.userId
  })
  commentVisible.value = false
  commentText.value = ''
  replyTarget.value = null
  comments.value = await getComments(props.orderId)
  uni.showToast({ title: '发布成功', icon: 'none' })
}

function cancelMine() {
  uni.showModal({
    title: '撤销这单？',
    content: '撤销后不再接受审批',
    success: async (res) => {
      if (res.confirm) {
        await cancelOrder(props.orderId)
        uni.showToast({ title: '已撤销', icon: 'none' })
        await load()
      }
    }
  })
}

function preview(i: number) {
  const urls = (order.value?.imageList || []).map((u) => fullUrl(u))
  uni.previewImage({ urls, current: i })
}

/** 供弹层头部「···」调用 */
async function onMore() {
  const items = ['收藏 / 取消收藏', '举报这个审批']
  if (order.value?.mine) items.push('删除这单')
  uni.showActionSheet({
    itemList: items,
    success: async (res) => {
      if (res.tapIndex === 0) {
        const fav = await toggleFavorite(props.orderId)
        uni.showToast({ title: fav ? '已收藏' : '已取消收藏', icon: 'none' })
      } else if (res.tapIndex === 1) {
        doReport()
      } else if (res.tapIndex === 2) {
        await removeOrder(props.orderId)
        uni.showToast({ title: '已删除', icon: 'none' })
        setTimeout(() => emit('close'), 600)
      }
    }
  })
}

defineExpose({ onMore, reload: load })
</script>

<style lang="scss" scoped>
.panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-scroll {
  flex: 1;
  min-height: 0;
}

.content {
  padding: 0 24rpx;
}

.loading {
  padding: 120rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #9aa0a6;
}

.main-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
}

.author {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #f5f5f7;
}

.name {
  font-size: 27rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.time {
  display: block;
  font-size: 22rpx;
  color: #9aa0a6;
  margin-top: 2rpx;
}

.status {
  font-size: 22rpx;
  padding: 4rpx 18rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 26rpx;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.4;
  color: #1a1a1a;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  margin-top: 10rpx;
}

.currency {
  font-size: 28rpx;
  color: #ff6b35;
  font-weight: 700;
}

.price {
  font-size: 60rpx;
  color: #ff6b35;
  font-weight: 800;
  line-height: 1.1;
}

.cd-row {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.cd-label {
  font-size: 23rpx;
  color: #9aa0a6;
}

.cd-value {
  font-size: 23rpx;
  color: #ff6b35;
  font-weight: 600;
}

.tally {
  margin-top: 28rpx;
}

.tally-bar {
  height: 12rpx;
  border-radius: 999rpx;
  background: #f0f0f2;
  overflow: hidden;
  display: flex;
}

.bar-pass {
  background: #22c55e;
  height: 100%;
}

.bar-reject {
  background: #ef4444;
  height: 100%;
}

.tally-text {
  margin-top: 12rpx;
  display: flex;
  justify-content: space-between;
  font-size: 22rpx;
}

.t-pass {
  color: #22c55e;
  font-weight: 600;
}

.t-mid {
  color: #9aa0a6;
}

.t-reject {
  color: #ef4444;
  font-weight: 600;
}

.block {
  margin-top: 20rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 30rpx;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.block-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.block-head .block-title {
  margin-bottom: 0;
}

.block-text {
  display: block;
  margin-top: 14rpx;
  font-size: 28rpx;
  color: #333;
  line-height: 1.75;
}

.sorter {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 23rpx;
  color: #9aa0a6;
}

.sorter .on {
  color: #ff6b35;
  font-weight: 600;
}

.sep {
  color: #e5e5e8;
}

.c-add {
  font-size: 23rpx;
  color: #ff6b35;
  background: #fff1ea;
  border-radius: 999rpx;
  padding: 6rpx 20rpx;
}

.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  background: #f5f5f7;
}

.comment {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f7;
}

.comment:last-child {
  border-bottom: none;
}

.c-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #f5f5f7;
  flex-shrink: 0;
}

.c-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.c-name {
  font-size: 24rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.c-time {
  font-size: 21rpx;
  color: #b8bcc2;
}

.c-text {
  display: block;
  margin-top: 8rpx;
  font-size: 27rpx;
  color: #333;
  line-height: 1.6;
}

.c-reply {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9aa0a6;
}

.sub {
  margin-top: 12rpx;
  background: #fafafa;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
  font-size: 25rpx;
  line-height: 1.6;
}

.sub-name {
  color: #6b7280;
  font-weight: 600;
}

.sub-to {
  color: #9aa0a6;
}

.sub-text {
  color: #333;
}

.bottom-space {
  height: 40rpx;
}

.action-bar {
  background: #fff;
  /* 弹层已整体悬空，底部安全区由外层负责，这里只留视觉间距 */
  padding: 20rpx 28rpx 24rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.vote-actions {
  display: flex;
  gap: 20rpx;
}

.btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #fff;
  text-align: center;
  padding: 0;
  margin: 0;
}

.btn.reject {
  background: #fef2f2;
  color: #ef4444;
  border: 1rpx solid #fecaca;
}

.btn.pass {
  background: linear-gradient(135deg, #34d399, #22c55e);
  color: #fff;
}

.btn.ghost {
  flex: none;
  width: 200rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  background: #f5f5f7;
  color: #6b7280;
}

.mine-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hint {
  font-size: 25rpx;
  color: #9aa0a6;
}

.voted-tip {
  text-align: center;
  font-size: 26rpx;
  color: #9aa0a6;
  padding: 16rpx 0;
}

.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fff;
  border-top-left-radius: 32rpx;
  border-top-right-radius: 32rpx;
  padding: 36rpx 32rpx 44rpx;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
}

.sheet-close {
  font-size: 32rpx;
  color: #9aa0a6;
  padding: 8rpx;
}

.sheet-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #9aa0a6;
}

.sheet-input {
  margin-top: 20rpx;
  width: 100%;
  min-height: 180rpx;
  background: #f7f7f9;
  border-radius: 18rpx;
  padding: 24rpx;
  font-size: 28rpx;
  line-height: 1.6;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.quick-item {
  font-size: 23rpx;
  color: #6b7280;
  background: #f5f5f7;
  border-radius: 999rpx;
  padding: 8rpx 20rpx;
}

.submit {
  margin-top: 28rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #fff;
  padding: 0;
}

.submit.pass {
  background: linear-gradient(135deg, #34d399, #22c55e);
}

.submit.reject {
  background: linear-gradient(135deg, #f87171, #ef4444);
}
</style>
