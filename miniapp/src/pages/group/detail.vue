<template>
  <!-- #ifdef MP-WEIXIN -->
  <!-- 弹层打开时禁掉页面原生滚动，否则手指在弹层上划，底下的页面会跟着动 -->
  <page-meta :page-style="pageStyle" />
  <!-- #endif -->
  <view class="page">
    <NavBar :title="group?.name || '群内审批'" back bg="#F6F2E9" />

    <view v-if="group" class="head-card">
      <view class="top">
        <view class="badge">{{ group.name.slice(0, 1) }}</view>
        <view class="grow">
          <text class="name">{{ group.name }}</text>
          <text class="meta">{{ group.memberCount }} 人 · 邀请码 {{ group.inviteCode }}</text>
        </view>
        <view class="copy" @tap="copyCode">复制</view>
      </view>
      <text v-if="group.notice" class="notice">{{ group.notice }}</text>
    </view>

    <view class="list">
      <view class="section-head">
        <text class="section-title">群内审批</text>
        <view class="sorter">
          <text :class="{ on: sort === 'new' }" @tap="changeSort('new')">最新</text>
          <text class="sep">|</text>
          <text :class="{ on: sort === 'hot' }" @tap="changeSort('hot')">最热</text>
        </view>
      </view>

      <OrderCard v-for="o in orders" :key="o.id" :item="o" @open="openSheet" />
      <Empty v-if="orders.length === 0" text="群里还没人发单" emoji="🧾" />

      <view v-if="orders.length > 0" class="footer-tip">
        {{ hasMore ? '上拉加载更多' : '— 到底了 —' }}
      </view>
    </view>

    <view class="bottom">
      <button class="btn-main" @tap="publishHere">在这个群发一单</button>
      <button class="btn-quit" @tap="onQuit">{{ group?.isOwner ? '解散群' : '退出群' }}</button>
      <view class="safe" />
    </view>

    <OrderSheet :visible="sheetVisible" :order-id="sheetOrderId" @close="sheetVisible = false" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { pageStyleWhen } from '../../utils/scroll-lock'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import NavBar from '../../components/NavBar.vue'
import OrderCard from '../../components/OrderCard.vue'
import OrderSheet from '../../components/OrderSheet.vue'
import Empty from '../../components/Empty.vue'
import { getGroup, getGroupOrders, quitGroup, dissolveGroup } from '../../api/group'
import { ensureLogin } from '../../utils/auth'
import type { GroupItem, OrderItem } from '../../api/types'

const groupId = ref(0)
const group = ref<GroupItem | null>(null)
const orders = ref<OrderItem[]>([])
const sort = ref('new')
const pageNum = ref(1)
const hasMore = ref(true)
const loading = ref(false)

const sheetVisible = ref(false)
/** 弹层打开时锁页面滚动（仅小程序生效） */
const pageStyle = pageStyleWhen(() => sheetVisible.value)
const sheetOrderId = ref(0)

function openSheet(id: number) {
  sheetOrderId.value = id
  sheetVisible.value = true
}

async function loadGroup() {
  await ensureLogin()
  group.value = await getGroup(groupId.value)
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
    const res = await getGroupOrders(groupId.value, { sort: sort.value, pageNum: pageNum.value, pageSize: 10 })
    const rows = res.rows || []
    orders.value = reset ? rows : orders.value.concat(rows)
    hasMore.value = rows.length >= 10
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function changeSort(s: string) {
  if (sort.value === s) return
  sort.value = s
  loadOrders(true)
}

function copyCode() {
  if (!group.value) return
  uni.setClipboardData({
    data: group.value.inviteCode,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'none' })
  })
}

function publishHere() {
  uni.switchTab({ url: '/pages/publish/index' })
}

function onQuit() {
  const isOwner = group.value?.isOwner
  uni.showModal({
    title: isOwner ? '解散这个群？' : '退出这个群？',
    content: isOwner ? '解散后成员将无法再看到群内审批' : '退出后群内审批不再展示',
    confirmColor: '#EF4444',
    success: async (res) => {
      if (!res.confirm || !group.value) return
      if (isOwner) {
        await dissolveGroup(group.value.id)
      } else {
        await quitGroup(group.value.id)
      }
      uni.showToast({ title: '已处理', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
    }
  })
}

onLoad((options) => {
  groupId.value = Number(options?.id || 0)
  if (groupId.value) {
    loadGroup()
    loadOrders(true)
  }
})

onReachBottom(() => {
  if (hasMore.value) loadOrders()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f6f2e9;
  padding-bottom: 220rpx;
}

.head-card {
  margin: 20rpx 24rpx 0;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
}

.top {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.badge {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #35604f, #2f5a4c);
  color: #fff;
  font-size: 38rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.name {
  font-size: 32rpx;
  font-weight: 700;
}

.meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #9a9488;
}

.copy {
  font-size: 22rpx;
  color: #2f5a4c;
  background: #e9efe9;
  border-radius: 999rpx;
  padding: 8rpx 22rpx;
}

.notice {
  display: block;
  margin-top: 20rpx;
  background: #f8f4ec;
  border-radius: 14rpx;
  padding: 16rpx 20rpx;
  font-size: 24rpx;
  color: #5f6b63;
}

.list {
  padding: 20rpx 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 6rpx 18rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
}

.sorter {
  display: flex;
  gap: 8rpx;
  font-size: 23rpx;
  color: #9a9488;
}

.sorter .on {
  color: #2f5a4c;
  font-weight: 600;
}

.sep {
  color: #e5e5e8;
}

.footer-tip {
  text-align: center;
  font-size: 22rpx;
  color: #b3ada1;
  padding: 24rpx 0;
}

.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 28rpx;
  display: flex;
  gap: 16rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.btn-main {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #35604f, #2f5a4c);
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}

.btn-quit {
  width: 180rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  background: #f6f2e9;
  color: #5f6b63;
  font-size: 26rpx;
  padding: 0;
}

.safe {
  height: env(safe-area-inset-bottom);
}
</style>
