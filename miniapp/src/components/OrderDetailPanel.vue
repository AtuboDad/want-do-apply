<template>
  <view class="panel">
    <!-- 滚动区 -->
    <scroll-view class="panel-scroll" scroll-y :show-scrollbar="false">
      <view v-if="order" class="content">
        <!-- 商品图 -->
        <view v-if="order.imageList && order.imageList.length" class="hero" @tap="preview(0)">
          <image class="hero-img" :src="fullUrl(order.imageList[0])" mode="aspectFill" />
          <view v-if="order.imageList.length > 1" class="hero-badge">共 {{ order.imageList.length }} 张</view>
        </view>

        <!-- 商品主信息 -->
        <view class="card hero-card" :class="{ 'has-stamp': !!stampSrc }">
          <!-- 结论印章：通过 / 驳回 / 已结束 -->
          <image v-if="stampSrc" class="stamp" :src="stampSrc" mode="aspectFit" />
          <text class="title">{{ order.title }}</text>
          <view class="price-row">
            <text class="currency">¥</text>
            <text class="price">{{ money(order.price) }}</text>
            <text v-if="order.isTop === '1'" class="tag-top">置顶</text>
          </view>
          <view class="author">
            <view class="avatar-wrap">
              <image class="avatar" :src="avatarUrl(order.userAvatar)" mode="aspectFill" />
              <VipBadge v-if="showAuthorVip" size="mini" />
            </view>
            <text class="a-name">{{ order.userNickName || '匿名审批员' }}</text>
            <text class="a-time">{{ fromNow(order.createTime) }}</text>
            <text v-if="order.groupName" class="a-group">{{ order.groupName }}</text>
          </view>
        </view>

        <!-- 审批规则 -->
        <view class="card rule-card">
          <text class="rule-label">审批规则</text>
          <text class="rule-main">{{ modeText(order.approveMode) }} · {{ ruleSuffix }}</text>
          <view class="rule-meta">
            <text class="rule-count">已表态 {{ order.voteCount || 0 }} 人</text>
            <text class="rule-status" :style="{ color: statusColor(order.status) }">
              {{ statusText(order.status) }}
            </text>
          </view>
          <!-- 0 票时不画双色条：否则 100% 红条会让人误以为全票驳回 -->
          <view class="rule-bar">
            <template v-if="(order.voteCount || 0) > 0">
              <view class="rb-pass" :style="{ width: passPercent + '%' }" />
              <view class="rb-reject" :style="{ width: 100 - passPercent + '%' }" />
            </template>
          </view>
          <view class="rule-foot">
            <text class="rf-left">{{ order.approveMode === '0' ? '一人表态即出结果' : '少数服从多数' }}</text>
            <text v-if="order.status === '0'" class="rf-cd">还剩 {{ countdown(order.remainSeconds) }}</text>
          </view>

          <view v-if="order.extraNote" class="note">
            <text class="note-label">发起人补充</text>
            <text class="note-text">{{ order.extraNote }}</text>
          </view>
        </view>

        <!-- 商品详情：理由要排在批条前面，让审批人先看理由再表态，别被别人的意见带节奏 -->
        <view class="card">
          <text class="card-title">想买的理由</text>
          <text class="para">{{ order.reason }}</text>
          <view v-if="order.substitute" class="sub-block">
            <text class="para-label">有没有替代品</text>
            <text class="para">{{ order.substitute }}</text>
          </view>
        </view>

        <!-- 回应 -->
        <view class="card">
          <view class="card-head">
            <text class="card-title">回应</text>
            <view class="sorter">
              <text :class="{ on: voteSort === 'hot' }" @tap="switchVoteSort('hot')">热门</text>
              <text class="sep">|</text>
              <text :class="{ on: voteSort === 'new' }" @tap="switchVoteSort('new')">最新</text>
            </view>
          </view>
          <view class="tally-row">
            <text class="t-pass">通过 {{ order.passCount || 0 }} 票</text>
            <text class="t-reject">驳回 {{ order.rejectCount || 0 }} 票</text>
          </view>

          <view v-for="v in votes" :key="v.id" class="resp">
            <view class="resp-head">
              <text class="resp-result" :class="v.result === '1' ? 'pass' : 'reject'">
                {{ v.result === '1' ? '通过' : '驳回' }}
              </text>
              <text class="resp-sep">·</text>
              <text class="resp-nick">{{ v.userNickName || '匿名审批员' }}</text>
              <view class="resp-more" @tap.stop="voteMore(v)">
                <text class="dot" />
                <text class="dot" />
                <text class="dot" />
              </view>
            </view>
            <text v-if="v.reason" class="resp-text">{{ v.reason }}</text>
            <text v-else class="resp-text quiet">没写理由，投完就跑</text>
            <view class="resp-foot">
              <text class="resp-time">{{ dateTime(v.createTime) }} · 北京时间</text>
              <view class="flower" :class="{ on: v.liked }" @tap.stop="onLike(v.id)">
                <text class="flower-ico">🌷</text>
                <text>{{ v.liked ? '已送' : '送花' }}</text>
                <text v-if="v.likeCount" class="flower-num">{{ v.likeCount }}</text>
              </view>
            </view>
          </view>

          <Empty v-if="votes.length === 0" text="还没人表态，快去摇人" emoji="🌿" />
        </view>

        <!-- 讨论 -->
        <view class="card">
          <view class="card-head">
            <text class="card-title">讨论 ({{ order.commentCount || 0 }})</text>
            <view class="c-add" @tap="openComment()">
              <text>写评论</text>
            </view>
          </view>
          <view v-for="c in comments" :key="c.id" class="comment">
            <image class="c-avatar" :src="avatarUrl(c.userAvatar)" mode="aspectFill" />
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
      <!-- 围观：从列表卡片挪到详情里，别人的单才显示 -->
      <view v-if="!order.mine" class="fav-row">
        <view class="fav-btn" :class="{ on: order.favorited }" @tap="toggleFav">
          <view class="eye">
            <view class="pupil" />
          </view>
          <text class="fav-label">{{ order.favorited ? '已围观' : '围观这单' }}</text>
          <text class="fav-tip">{{ order.favorited ? '出结果会通知你' : '不表态，只看结果' }}</text>
        </view>
      </view>
      <!-- 发起人：撤回 / 补充说明 / 提前结束 -->
      <view v-if="order.mine && order.status === '0'" class="row-3">
        <button class="btn ghost" @tap="cancelMine">撤回审批</button>
        <button class="btn dark" @tap="openNote">补充说明</button>
        <button class="btn warm" @tap="finishEarly">提前结束</button>
      </view>
      <view v-else-if="order.mine" class="row-mine">
        <text class="hint">{{ mineHint }}</text>
        <button class="btn ghost small" @tap="onMore">管理</button>
      </view>
      <!-- 其他人：批一下 -->
      <view v-else-if="canVote" class="row-2">
        <button class="btn reject" @tap="openVote('2')">驳回</button>
        <button class="btn pass" @tap="openVote('1')">通过</button>
      </view>
      <view v-else class="voted-tip">
        <text>{{ order.myVoted ? '你已经批过这单了，等结果吧' : '审批已结束' }}</text>
      </view>
    </view>

    <!-- 批阅弹窗 -->
    <view v-if="voteVisible" class="mask" @tap="closeVote">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">{{ voteResult === '1' ? '通过这单' : '驳回这单' }}</text>
          <text class="sheet-close" @tap="closeVote">✕</text>
        </view>
        <text class="sheet-tip">想说说就写两句，不写也行</text>
        <textarea
          v-model="voteReason"
          class="sheet-input"
          :placeholder="voteResult === '1' ? '说说为什么该买…（可不填）' : '说说为什么不该买…（可不填）'"
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

    <!-- 补充说明 -->
    <view v-if="noteVisible" class="mask" @tap="noteVisible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">补充说明</text>
          <text class="sheet-close" @tap="noteVisible = false">✕</text>
        </view>
        <text class="sheet-tip">写清背景，帮大家更好判断（会显示在审批规则下面）</text>
        <textarea
          v-model="noteText"
          class="sheet-input"
          placeholder="例：旧的那把是薄膜键盘，手感真的不行…"
          maxlength="500"
          :focus="true"
          auto-height
        />
        <button class="submit pass" @tap="saveNote">保存说明</button>
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
import Empty from './Empty.vue'
import VipBadge from './VipBadge.vue'
import {
  getDetail,
  cancelOrder,
  toggleFavorite,
  removeOrder,
  updateNote,
  finishEarly as finishEarlyApi
} from '../api/order'
import { submitVote, likeVote, addComment, getComments } from '../api/vote'
import { report } from '../api/group'
import { fullUrl } from '../utils/request'
import { avatarUrl, shouldShowVip } from '../utils/avatar'
import {
  fromNow,
  money,
  countdown,
  statusText,
  statusColor,
  dateTime,
  durationLabel,
  modeText
} from '../utils/format'
import { ensureLogin } from '../utils/auth'
import type { OrderItem, VoteItem, CommentItem } from '../api/types'

const props = defineProps<{ orderId: number | string }>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

const order = ref<OrderItem | null>(null)
/** 发起人挂着会员头像且没关角标 */
const showAuthorVip = computed(() => shouldShowVip(order.value?.userAvatar, order.value?.userShowVip))
const votes = ref<VoteItem[]>([])
const comments = ref<CommentItem[]>([])
const voteSort = ref<'hot' | 'new'>('hot')

/**
 * 这次打开弹层有没有真的改动过这单（投票 / 撤回 / 提前结束 / 补说明 / 评论）。
 *
 * 列表页据此决定关掉弹层后要不要重拉：什么都没干就重拉，
 * 会把翻了三四页的列表打回第一页，比状态不刷新更烦人。
 */
const dirty = ref(false)

const voteVisible = ref(false)
const voteResult = ref('1')
const voteReason = ref('')
const noteVisible = ref(false)
const noteText = ref('')
const commentVisible = ref(false)
const commentText = ref('')
const replyTarget = ref<CommentItem | null>(null)

const quickWords = ['性价比不高', '已有类似款', '真的需要吗', '等打折再说', '买！我支持', '生活需要仪式感']

const passPercent = computed(() => {
  const p = order.value?.passCount || 0
  const r = order.value?.rejectCount || 0
  if (p + r === 0) return 0
  return Math.round((p / (p + r)) * 100)
})

/** 结论印章：已出结果的单子才盖，图是 scripts/gen_stamps.py 生成的 */
const stampSrc = computed(() => {
  switch (order.value?.status) {
    case '1': return '/static/stamps/stamp-pass.png'
    case '2': return '/static/stamps/stamp-reject.png'
    case '3': return '/static/stamps/stamp-cancel.png'
    case '4': return '/static/stamps/stamp-closed.png'
    default: return ''
  }
})

/** 规则那句尾巴：单人审批说「一人处理后结束」，多人审批说时长 */
const ruleSuffix = computed(() => {
  const o = order.value
  if (!o) return ''
  if (o.approveMode === '0') return '一人处理后结束'
  return `${durationLabel(o.durationHours)}后自动结束`
})

const canVote = computed(() => {
  const o = order.value
  return !!o && o.status === '0' && !o.mine && !o.myVoted && (o.remainSeconds || 0) > 0
})

const mineHint = computed(() => {
  const o = order.value
  if (!o) return ''
  switch (o.status) {
    case '0': return '大家还在批，到点自动出结果'
    case '1': return '通过了，这钱花得理直气壮'
    case '2': return '被驳回了，省下一笔'
    case '3': return '这单已撤回'
    case '4': return '流拍了，没人来得及表态'
    default: return ''
  }
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

async function doReport(targetType = '1', targetId?: number | string) {
  try {
    await report({ targetType, targetId: targetId || props.orderId, reasonType: 'other', content: '用户举报' })
    uni.showToast({ title: '已提交举报', icon: 'none' })
  } catch (e) {
    // ignore
  }
}

/** 批条右侧「···」 */
function voteMore(v: VoteItem) {
  uni.showActionSheet({
    itemList: ['举报这条批条'],
    success: () => doReport('2', v.id)
  })
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
  // 理由选填：不写也能批，写了更好玩
  const settled = await submitVote({
    orderId: props.orderId,
    result: voteResult.value,
    reason: voteReason.value.trim()
  })
  voteVisible.value = false
  dirty.value = true
  uni.showToast({ title: settled ? '审批已结束' : '批完啦，等大家投票', icon: 'none' })
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

// ---------------- 发起人操作 ----------------

function cancelMine() {
  uni.showModal({
    title: '撤回这单？',
    content: '撤回后不再接受审批，也不会出结果',
    success: async (res) => {
      if (res.confirm) {
        await cancelOrder(props.orderId)
        dirty.value = true
        uni.showToast({ title: '已撤回', icon: 'none' })
        await load()
      }
    }
  })
}

function openNote() {
  noteText.value = order.value?.extraNote || ''
  noteVisible.value = true
}

async function saveNote() {
  const text = noteText.value.trim()
  if (!text) {
    uni.showToast({ title: '写点什么吧', icon: 'none' })
    return
  }
  await updateNote(props.orderId, text)
  noteVisible.value = false
  dirty.value = true
  uni.showToast({ title: '补充好了', icon: 'none' })
  await load()
}

function finishEarly() {
  const o = order.value
  if (!o) return
  const p = o.passCount || 0
  const r = o.rejectCount || 0
  uni.showModal({
    title: '提前结束？',
    content:
      p + r === 0
        ? '还没有人表态，现在结束会算流拍'
        : `当前通过 ${p} 票、驳回 ${r} 票，按少数服从多数直接出结果，结束后不能再改`,
    confirmText: '结束',
    confirmColor: '#C0503F',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const status = await finishEarlyApi(props.orderId)
        dirty.value = true
        const tip = status === '1' ? '通过了' : status === '2' ? '被驳回了' : '流拍了'
        uni.showToast({ title: tip, icon: 'none' })
      } catch (e) {
        // 错误已提示
      }
      await load()
    }
  })
}

// ---------------- 评论 ----------------

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
  dirty.value = true
  comments.value = await getComments(props.orderId)
  uni.showToast({ title: '发布成功', icon: 'none' })
}

function preview(i: number) {
  const urls = (order.value?.imageList || []).map((u) => fullUrl(u))
  uni.previewImage({ urls, current: i })
}

/**
 * 围观 / 取消围观。
 *
 * 这个入口原来挂在列表卡片上，现在收进详情底部 —— 卡片太重，
 * 而且围观本来就是「看完再决定」的动作。
 */
async function toggleFav() {
  if (!order.value) return
  try {
    const fav = await toggleFavorite(props.orderId)
    order.value.favorited = fav
    // 列表里的「我围观的」也要跟着变
    dirty.value = true
    uni.showToast({ title: fav ? '开始围观，出结果会通知你' : '已取消围观', icon: 'none' })
  } catch (e) {
    // 提示已在 request 里统一弹过
  }
}

/** 供弹层头部「···」调用 */
async function onMore() {
  const items = ['举报这个审批']
  if (order.value?.mine) items.push('删除这单')
  uni.showActionSheet({
    itemList: items,
    success: async (res) => {
      if (res.tapIndex === 0) {
        doReport('1')
      } else if (res.tapIndex === 1) {
        await removeOrder(props.orderId)
        uni.showToast({ title: '已删除', icon: 'none' })
        setTimeout(() => emit('close'), 600)
      }
    }
  })
}

defineExpose({ onMore, reload: load, wasChanged: () => dirty.value })
</script>

<style lang="scss" scoped>
/* 弹层内部：头部固定、正文滚动、底部操作栏固定。
   小程序里 scroll-view 是原生组件，只写 flex:1 它不会跟着父级收缩，
   而是按内容撑到很高，把下面的 action-bar 顶出 .os-sheet（overflow:hidden 一裁就没了）
   —— 表现就是「详情里看不到通过/驳回按钮」。
   必须同时给 flex-basis:0 和 height:0，它才会按剩余空间算高度。 */
.panel {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-scroll {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
}

.content {
  padding: 0 24rpx;
}

.loading {
  padding: 120rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #9a9488;
}

/* ---------- 商品图 ---------- */
.hero {
  position: relative;
  margin-bottom: 18rpx;
}

.hero-img {
  width: 100%;
  height: 340rpx;
  border-radius: 24rpx;
  background: #efe8d9;
}

.hero-badge {
  position: absolute;
  right: 18rpx;
  bottom: 18rpx;
  font-size: 20rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 999rpx;
  padding: 4rpx 16rpx;
}

/* ---------- 通用卡片 ---------- */
.card {
  background: #fff;
  border-radius: 28rpx;
  padding: 30rpx 28rpx;
  margin-bottom: 18rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1d2320;
}

.title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.45;
  color: #1d2320;
}

/* 有印章时标题让出右上角，避免压字 */
.hero-card {
  position: relative;
}

.hero-card.has-stamp .title {
  padding-right: 156rpx;
}

.stamp {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  width: 168rpx;
  height: 168rpx;
  transform: rotate(-13deg);
  opacity: 0.95;
  pointer-events: none;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  margin-top: 10rpx;
}

.currency {
  font-size: 26rpx;
  color: #b04b3c;
  font-weight: 700;
}

.price {
  font-size: 54rpx;
  color: #b04b3c;
  font-weight: 800;
  line-height: 1.1;
}

.tag-top {
  font-size: 19rpx;
  color: #fff;
  background: #c08a2e;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  margin-left: 12rpx;
  align-self: center;
}

.author {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f1ebde;
}

.avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: #f4efe4;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 52rpx;
  height: 52rpx;
}

.a-name {
  font-size: 24rpx;
  color: #4b5350;
  font-weight: 600;
}

.a-time {
  font-size: 22rpx;
  color: #a8a294;
}

.a-group {
  margin-left: auto;
  font-size: 20rpx;
  color: #7a7466;
  background: #f4efe4;
  border-radius: 999rpx;
  padding: 4rpx 16rpx;
}

/* ---------- 审批规则 ---------- */
.rule-card {
  background: #faf3e3;
}

.rule-label {
  display: block;
  font-size: 22rpx;
  color: #a58c5c;
  letter-spacing: 1rpx;
}

.rule-main {
  display: block;
  margin-top: 10rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #234439;
}

.rule-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.rule-count {
  font-size: 26rpx;
  color: #8a7442;
}

.rule-status {
  font-size: 26rpx;
  font-weight: 600;
}

.rule-bar {
  display: flex;
  height: 14rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #ece0c8;
  margin-top: 16rpx;
}

.rb-pass {
  background: #2f7d5b;
  height: 100%;
}

.rb-reject {
  background: #b04b3c;
  height: 100%;
}

.rule-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: #b0a184;
}

.rf-cd {
  color: #a8762a;
  font-weight: 600;
}

.note {
  margin-top: 22rpx;
  padding-top: 22rpx;
  border-top: 1rpx dashed #e7d4b2;
}

.note-label {
  display: block;
  font-size: 22rpx;
  color: #a58c5c;
}

.note-text {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #4a5250;
}

/* ---------- 回应 ---------- */
.sorter {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 23rpx;
  color: #9a9488;
}

.sorter .on {
  color: #2f5a4c;
  font-weight: 600;
}

.sep {
  color: #e5dfd1;
}

.tally-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f1ebde;
}

.t-pass {
  font-size: 26rpx;
  color: #2f7d5b;
  font-weight: 600;
}

.t-reject {
  font-size: 26rpx;
  color: #b04b3c;
  font-weight: 600;
}

.resp {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f4efe6;
}

.resp:last-child {
  border-bottom: none;
  padding-bottom: 4rpx;
}

.resp-head {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.resp-result {
  font-size: 26rpx;
  font-weight: 700;
}

.resp-result.pass {
  color: #2f7d5b;
}

.resp-result.reject {
  color: #b04b3c;
}

.resp-sep {
  color: #cfc8ba;
  font-size: 24rpx;
}

.resp-nick {
  font-size: 25rpx;
  color: #5f6b63;
  max-width: 380rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 三个点用实体圆点画，比字符「···」整齐 */
.resp-more {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 8rpx;
}

.resp-more .dot {
  width: 7rpx;
  height: 7rpx;
  border-radius: 50%;
  background: #c2bcae;
}

.resp-text {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  line-height: 1.65;
  color: #2b3230;
}

.resp-text.quiet {
  color: #b3ada1;
  font-size: 26rpx;
  font-style: italic;
}

.resp-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
}

.resp-time {
  font-size: 21rpx;
  color: #b3ada1;
}

.flower {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: #9a9488;
  background: #f8f4ec;
  border-radius: 999rpx;
  padding: 6rpx 18rpx;
}

.flower.on {
  color: #b04b3c;
  background: #fbece9;
}

.flower-ico {
  font-size: 22rpx;
}

.flower-num {
  font-weight: 600;
}

/* ---------- 商品详情 ---------- */
.para {
  display: block;
  margin-top: 12rpx;
  font-size: 27rpx;
  line-height: 1.75;
  color: #3d4542;
}

.para-label {
  display: block;
  margin-top: 22rpx;
  font-size: 24rpx;
  color: #8b8578;
}

.sub-block {
  margin-top: 4rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #f1ebde;
}

/* ---------- 评论 ---------- */
.c-add {
  font-size: 23rpx;
  color: #2f5a4c;
  background: #e9efe9;
  border-radius: 999rpx;
  padding: 6rpx 20rpx;
}

.comment {
  display: flex;
  gap: 16rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f4efe6;
}

.comment:last-child {
  border-bottom: none;
}

.c-avatar {
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  background: #f4efe4;
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
  color: #1d2320;
}

.c-time {
  font-size: 21rpx;
  color: #b3ada1;
}

.c-text {
  display: block;
  margin-top: 8rpx;
  font-size: 27rpx;
  color: #2b3230;
  line-height: 1.6;
}

.c-reply {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9a9488;
}

.sub {
  margin-top: 12rpx;
  background: #f8f4ec;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
  font-size: 25rpx;
  line-height: 1.6;
}

.sub-name {
  color: #5f6b63;
  font-weight: 600;
}

.sub-to {
  color: #9a9488;
}

.sub-text {
  color: #2b3230;
}

.bottom-space {
  height: 40rpx;
}

/* ---------- 底部操作 ---------- */
.action-bar {
  background: #fff;
  padding: 18rpx 24rpx 22rpx;
  box-shadow: 0 -4rpx 18rpx rgba(45, 58, 51, 0.05);
  flex-shrink: 0;
}

/* 围观：整条是个浅绿胶囊，点一下切换；已围观反白成墨绿 */
.fav-row {
  margin-bottom: 16rpx;
}

.fav-btn {
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #edf3ee;
}

.fav-btn .eye {
  width: 28rpx;
  height: 20rpx;
  border: 3rpx solid #2f5a4c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  flex-shrink: 0;
}

.fav-btn .pupil {
  width: 9rpx;
  height: 9rpx;
  border-radius: 50%;
  background: #2f5a4c;
}

.fav-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #2f5a4c;
  line-height: 1;
}

.fav-tip {
  font-size: 22rpx;
  color: #7d8f84;
  line-height: 1;
}

.fav-btn.on {
  background: #2f5a4c;
}

.fav-btn.on .eye {
  border-color: #fff;
}

.fav-btn.on .pupil {
  background: #fff;
}

.fav-btn.on .fav-label {
  color: #fff;
}

.fav-btn.on .fav-tip {
  color: rgba(255, 255, 255, 0.72);
}

.row-3 {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.row-2 {
  display: flex;
  gap: 20rpx;
}

.row-mine {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
  text-align: center;
  padding: 0;
  margin: 0;
  border: none;
}

.btn::after {
  border: none;
}

/* 撤回：浅米底，最弱 */
.btn.ghost {
  background: #f1efe6;
  color: #3a3a38;
  font-weight: 500;
}

.btn.ghost.small {
  flex: none;
  width: 150rpx;
  height: 66rpx;
  line-height: 66rpx;
  font-size: 25rpx;
}

/* 补充说明：深墨绿 */
.btn.dark {
  background: #2f5a4c;
  color: #fbf7ee;
}

/* 提前结束：砖红 */
.btn.warm {
  background: #b4503e;
  color: #fbf7ee;
}

.btn.reject {
  background: #fbece9;
  color: #b04b3c;
  border: 1rpx solid #f0d8d2;
}

.btn.pass {
  background: #2f5a4c;
  color: #fbf7ee;
}

.hint {
  font-size: 25rpx;
  color: #8b8578;
}

.voted-tip {
  text-align: center;
  font-size: 26rpx;
  color: #9a9488;
  padding: 18rpx 0;
}

/* ---------- 内层弹窗 ---------- */
.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(20, 24, 22, 0.45);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  padding: 0 20rpx calc(20rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 40rpx;
  padding: 36rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1d2320;
}

.sheet-close {
  font-size: 32rpx;
  color: #9a9488;
  padding: 8rpx;
}

.sheet-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #9a9488;
}

.sheet-input {
  margin-top: 20rpx;
  width: 100%;
  min-height: 180rpx;
  background: #f8f4ec;
  border-radius: 18rpx;
  padding: 24rpx;
  font-size: 28rpx;
  line-height: 1.6;
  color: #1d2320;
  box-sizing: border-box;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.quick-item {
  font-size: 23rpx;
  color: #5f6b63;
  background: #f4efe4;
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
  color: #fbf7ee;
  padding: 0;
}

.submit.pass {
  background: #2f5a4c;
}

.submit.reject {
  background: #b04b3c;
}
</style>
