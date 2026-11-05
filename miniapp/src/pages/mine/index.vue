<template>
  <view class="page">
    <view class="hero">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="profile">
        <image class="avatar" :src="avatarUrl(user?.avatar)" mode="aspectFill" @tap="editProfile" />
        <view class="grow">
          <text class="nick">{{ user?.nickName || '审批员' }}</text>
          <view class="credit">
            <text class="credit-label">信用分</text>
            <text class="credit-val">{{ user?.creditScore ?? 100 }}</text>
          </view>
        </view>
        <view class="bell" @tap="goNotice">
          <text class="bell-icon">🔔</text>
          <view v-if="unread > 0" class="dot" />
        </view>
      </view>

      <view class="stats">
        <view class="stat">
          <text class="num">{{ profile?.applyTotal || 0 }}</text>
          <text class="label">提交</text>
        </view>
        <view class="stat">
          <text class="num">{{ profile?.voteTotal || 0 }}</text>
          <text class="label">处理</text>
        </view>
        <view class="stat">
          <text class="num">{{ profile?.adoptedTotal || 0 }}</text>
          <text class="label">被采纳</text>
        </view>
        <view class="stat">
          <text class="num">¥{{ money(profile?.savedAmount) }}</text>
          <text class="label">帮人省下</text>
        </view>
      </view>
    </view>

    <view class="tabs">
      <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: type === t.key }" @tap="switchTab(t.key)">
        {{ t.label }}
      </view>
    </view>

    <view class="list">
      <OrderCard v-for="o in orders" :key="o.id" :item="o" @open="openSheet" />
      <Empty v-if="orders.length === 0" :text="emptyText" emoji="📭" />
      <view v-if="orders.length > 0" class="footer-tip">
        {{ hasMore ? '上拉加载更多' : '— 到底了 —' }}
      </view>
    </view>

    <!-- 编辑资料 -->
    <view v-if="editVisible" class="mask" @tap="closeEdit">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">修改资料</text>
          <text class="sheet-close" @tap="closeEdit">✕</text>
        </view>

        <view class="cur-avatar">
          <image class="avatar-preview" :src="avatarUrl(editAvatar)" mode="aspectFill" />
          <text class="avatar-tip">挑一个顺眼的形象</text>
        </view>

        <text class="block-label">我们画的</text>
        <view class="avatar-grid">
          <view
            v-for="id in PRESET_AVATARS"
            :key="id"
            class="av-cell"
            :class="{ on: editAvatar === presetAvatarValue(id) }"
            @tap="pickPreset(id)"
          >
            <image class="av-img" :src="presetAvatarPath(id)" mode="aspectFill" />
          </view>
        </view>

        <button class="wx-avatar" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          或者用微信头像
        </button>

        <input
          class="sheet-input"
          type="nickname"
          :value="editNick"
          placeholder="输入昵称"
          maxlength="20"
          @input="onNickInput"
        />
        <button class="submit" @tap="saveProfile">保存</button>
      </view>
    </view>

    <OrderSheet :visible="sheetVisible" :order-id="sheetOrderId" @close="sheetVisible = false" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import OrderCard from '../../components/OrderCard.vue'
import OrderSheet from '../../components/OrderSheet.vue'
import Empty from '../../components/Empty.vue'
import { getMine, getProfile } from '../../api/order'
import { getUnread } from '../../api/group'
import { refreshMe, updateProfile } from '../../api/auth'
import { uploadFile } from '../../utils/request'
import { avatarUrl, PRESET_AVATARS, presetAvatarPath, presetAvatarValue } from '../../utils/avatar'
import { money } from '../../utils/format'
import { userStore } from '../../store/user'
import { ensureLogin } from '../../utils/auth'
import type { OrderItem } from '../../api/types'
import type { ProfileData } from '../../api/order'

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

type MineTab = 'apply' | 'vote' | 'favorite'

const tabs: { key: MineTab; label: string }[] = [
  { key: 'apply', label: '我提交的' },
  { key: 'vote', label: '我处理的' },
  { key: 'favorite', label: '我围观的' }
]

const type = ref<MineTab>('apply')
const orders = ref<OrderItem[]>([])
const profile = ref<ProfileData | null>(null)
const unread = ref(0)
const pageNum = ref(1)
const hasMore = ref(true)
const loading = ref(false)

const user = computed(() => userStore.user)

const emptyText = computed(() => {
  if (type.value === 'apply') return '还没提交过审批'
  if (type.value === 'vote') return '还没处理过别人的单'
  return '还没围观过别人的单'
})

const editVisible = ref(false)
const editNick = ref('')
const editAvatar = ref('')

const sheetVisible = ref(false)
const sheetOrderId = ref(0)

function openSheet(id: number) {
  sheetOrderId.value = id
  sheetVisible.value = true
}

async function loadProfile() {
  await ensureLogin()
  profile.value = await getProfile()
  unread.value = await getUnread().catch(() => 0)
}

async function loadOrders(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const res = await getMine(type.value, pageNum.value, 10)
    const rows = res.rows || []
    orders.value = reset ? rows : orders.value.concat(rows)
    hasMore.value = rows.length >= 10
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function switchTab(k: MineTab) {
  if (type.value === k) return
  type.value = k
  orders.value = []
  loadOrders(true)
}

function goNotice() {
  uni.navigateTo({ url: '/pages/notice/index' })
}

function editProfile() {
  editNick.value = user.value?.nickName || ''
  editAvatar.value = user.value?.avatar || ''
  editVisible.value = true
  // tabBar 是原生层，不藏起来会压住弹层底部的保存按钮
  toggleTabBar(false)
}

function closeEdit() {
  editVisible.value = false
  toggleTabBar(true)
}

function toggleTabBar(show: boolean) {
  const opts = { animation: false, fail: () => {} }
  if (show) {
    uni.showTabBar(opts)
  } else {
    uni.hideTabBar(opts)
  }
}

/** 选预设头像：存 preset:av03 这种标记，前端渲染走本地图片 */
function pickPreset(id: string) {
  editAvatar.value = presetAvatarValue(id)
}

function onNickInput(e: any) {
  editNick.value = e.detail.value
}

async function onChooseAvatar(e: any) {
  const temp = e.detail.avatarUrl as string
  if (!temp) return
  uni.showLoading({ title: '上传中' })
  try {
    const up = await uploadFile(temp)
    editAvatar.value = up.url
  } catch (err) {
    // ignore
  } finally {
    uni.hideLoading()
  }
}

async function saveProfile() {
  if (!editNick.value.trim()) {
    uni.showToast({ title: '填个昵称', icon: 'none' })
    return
  }
  await updateProfile(editNick.value.trim(), editAvatar.value)
  editVisible.value = false
  toggleTabBar(true)
  uni.showToast({ title: '已保存', icon: 'none' })
  await loadProfile()
  // 列表里的作者头像也要跟着换，否则得手动下拉才看得到
  await loadOrders(true)
}

onShow(() => {
  loadProfile()
  loadOrders(true)
})

onReachBottom(() => {
  if (hasMore.value) loadOrders()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f5f7;
}

.hero {
  background: linear-gradient(135deg, #ff8a3d 0%, #ff6b35 55%, #f2542d 100%);
  padding-bottom: 40rpx;
  border-bottom-left-radius: 36rpx;
  border-bottom-right-radius: 36rpx;
}

.profile {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 36rpx 0;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  border: 3rpx solid rgba(255, 255, 255, 0.6);
}

.nick {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}

.credit {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 10rpx;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 999rpx;
  padding: 4rpx 18rpx;
}

.credit-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.85);
}

.credit-val {
  font-size: 22rpx;
  font-weight: 700;
  color: #fff;
}

.bell {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.bell-icon {
  font-size: 32rpx;
}

.dot {
  position: absolute;
  top: 12rpx;
  right: 14rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #fff;
}

.stats {
  display: flex;
  margin: 32rpx 30rpx 0;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 24rpx;
  padding: 22rpx 0;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-size: 34rpx;
  font-weight: 800;
  color: #fff;
}

.label {
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 4rpx;
}

.tabs {
  display: flex;
  gap: 12rpx;
  padding: 24rpx 24rpx 8rpx;
}

.tab {
  font-size: 26rpx;
  color: #6b7280;
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
  background: #fff;
}

.tab.active {
  color: #fff;
  background: #1a1a1a;
  font-weight: 600;
}

.list {
  padding: 16rpx 24rpx 40rpx;
}

.footer-tip {
  text-align: center;
  font-size: 22rpx;
  color: #b8bcc2;
  padding: 24rpx 0;
}

.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fff;
  border-top-left-radius: 32rpx;
  border-top-right-radius: 32rpx;
  padding: 34rpx 32rpx calc(34rpx + env(safe-area-inset-bottom));
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sheet-close {
  font-size: 30rpx;
  color: #9aa0a6;
  padding: 0 8rpx;
}

.cur-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  margin-top: 20rpx;
}

.avatar-preview {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: #f5f5f7;
  border: 4rpx solid #fff;
  box-shadow: 0 6rpx 24rpx rgba(255, 107, 53, 0.18);
}

.avatar-tip {
  font-size: 22rpx;
  color: #9aa0a6;
}

.block-label {
  display: block;
  margin-top: 24rpx;
  font-size: 23rpx;
  color: #9aa0a6;
}

/* 4 列：4×140 + 3×16 = 608rpx，排在 686rpx 的可用宽度里居中 */
.avatar-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16rpx;
  margin-top: 14rpx;
}

.av-cell {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  overflow: hidden;
  box-sizing: border-box;
  background: #f7f7f9;
  border: 4rpx solid transparent;
}

.av-cell.on {
  border-color: #ff6b35;
  box-shadow: 0 0 0 4rpx rgba(255, 107, 53, 0.16);
}

.av-img {
  width: 100%;
  height: 100%;
  display: block;
}

.wx-avatar {
  margin-top: 24rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 999rpx;
  background: #f5f5f7;
  color: #5a5a5a;
  font-size: 26rpx;
  font-weight: 500;
  padding: 0;
}

.wx-avatar::after {
  border: none;
}

.sheet-input {
  margin-top: 22rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: #f7f7f9;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  text-align: center;
}

.submit {
  margin-top: 26rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff8a3d, #ff6b35);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
