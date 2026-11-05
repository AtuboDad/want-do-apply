<template>
  <view class="page">
    <view class="hero">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="hero-inner">
        <view class="brand">
          <text class="brand-name">钱包请等等</text>
          <text class="brand-dot">·</text>
          <text class="brand-slogan">买不买，网友说了算</text>
        </view>
        <view class="hero-stat">
          <text class="stat-num">{{ total }}</text>
          <text class="stat-label">个待批的消费申请</text>
        </view>
      </view>
    </view>

    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: sort === t.key }"
        @tap="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
      <view class="refresh" @tap="load(true)">
        <text class="refresh-icon">↻</text>
      </view>
    </view>

    <view class="list">
      <OrderCard v-for="item in list" :key="item.id" :item="item" />

      <Empty v-if="!loading && list.length === 0" text="广场还没人发单，来当第一个" emoji="🛒" />

      <view v-if="list.length > 0" class="footer-tip">
        {{ hasMore ? '上拉加载更多' : '— 到底了 —' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import OrderCard from '../../components/OrderCard.vue'
import Empty from '../../components/Empty.vue'
import { getSquare } from '../../api/order'
import { ensureLogin } from '../../utils/auth'
import type { OrderItem } from '../../api/types'

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

const tabs = [
  { key: 'new', label: '最新' },
  { key: 'hot', label: '最热' },
  { key: 'pending', label: '待审批' }
]

const sort = ref('new')
const list = ref<OrderItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(true)
const loading = ref(false)

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    await ensureLogin()
    const res = await getSquare({ sort: sort.value, pageNum: pageNum.value, pageSize })
    const rows = res.rows || []
    if (reset) {
      list.value = rows
    } else {
      list.value = list.value.concat(rows)
    }
    total.value = res.total || 0
    hasMore.value = rows.length >= pageSize
    pageNum.value += 1
  } catch (e) {
    // 错误已在 request 中提示
  } finally {
    loading.value = false
  }
}

function switchTab(key: string) {
  if (sort.value === key) return
  sort.value = key
  list.value = []
  load(true)
}

onShow(() => {
  load(true)
})

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (hasMore.value) load()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f5f7;
}

.hero {
  background: linear-gradient(135deg, #ff8a3d 0%, #ff6b35 55%, #f2542d 100%);
  padding-bottom: 56rpx;
  border-bottom-left-radius: 36rpx;
  border-bottom-right-radius: 36rpx;
}

.hero-inner {
  padding: 24rpx 36rpx 0;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}

.brand-name {
  font-size: 46rpx;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1rpx;
}

.brand-dot {
  color: rgba(255, 255, 255, 0.6);
  font-size: 34rpx;
}

.brand-slogan {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-stat {
  margin-top: 26rpx;
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}

.stat-num {
  font-size: 60rpx;
  font-weight: 800;
  color: #fff;
  line-height: 1;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
}

.tabs {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 30rpx 8rpx;
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

.refresh {
  margin-left: auto;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-icon {
  font-size: 32rpx;
  color: #9aa0a6;
}

.list {
  padding: 16rpx 24rpx 40rpx;
}

.footer-tip {
  text-align: center;
  font-size: 22rpx;
  color: #b8bcc2;
  padding: 30rpx 0 20rpx;
}
</style>
