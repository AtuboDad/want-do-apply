<template>
  <!-- #ifdef MP-WEIXIN -->
  <!-- 弹层打开时禁掉页面原生滚动，否则手指在弹层上划，底下的页面会跟着动 -->
  <page-meta :page-style="pageStyle" />
  <!-- #endif -->
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
      <OrderCard v-for="item in list" :key="item.id" :item="item" @open="openSheet" />

      <Empty v-if="!loading && list.length === 0 && !loadError" text="广场还没人发单，来当第一个" emoji="🛒" />

      <!-- 请求失败要说清楚是「连不上」还是「真没数据」，否则页面空着根本没法查 -->
      <view v-else-if="!loading && list.length === 0" class="load-fail">
        <text class="fail-title">没拉到数据</text>
        <text class="fail-url">接口地址：{{ apiBase }}</text>
        <text class="fail-msg">{{ loadError }}</text>
        <view class="fail-btn" @tap="load(true)">重新加载</view>
      </view>

      <view v-if="list.length > 0" class="footer-tip">
        {{ hasMore ? '上拉加载更多' : '— 到底了 —' }}
      </view>
    </view>

    <OrderSheet :visible="sheetVisible" :order-id="sheetOrderId" @close="handleSheetClose" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { pageStyleWhen } from '../../utils/scroll-lock'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import OrderCard from '../../components/OrderCard.vue'
import OrderSheet from '../../components/OrderSheet.vue'
import Empty from '../../components/Empty.vue'
import { getSquare } from '../../api/order'
import { ensureLogin } from '../../utils/auth'
import { BASE_URL } from '../../utils/config'
import { takePendingOpenOrder } from '../../utils/order-sheet'
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

const sheetVisible = ref(false)
/** 弹层打开时锁页面滚动（仅小程序生效） */
const pageStyle = pageStyleWhen(() => sheetVisible.value)
const sheetOrderId = ref<number | string>(0)

/** 卡片上的围观按钮已挪到审批详情里，这里不再处理 */
function openSheet(id: number | string) {
  sheetOrderId.value = id
  sheetVisible.value = true
}

/**
 * 弹层里可能批过票 / 撤回 / 提前结束 / 补过说明，
 * 改过就静默刷一遍，免得卡片还是旧状态；什么都没干就不动，别把翻到第几页的列表打回首页。
 */
function handleSheetClose(changed?: boolean) {
  sheetVisible.value = false
  if (changed) load(true)
}

/** 只有最新一次请求的结果才落地，见 mine 页同名变量的说明 */
let reqSeq = 0

/** 列表为空时用来区分「真没单」和「请求没成功」 */
const loadError = ref('')
const apiBase = BASE_URL || '（H5 走代理）'

function errText(e: any): string {
  const raw = e?.errMsg || e?.message || e
  return typeof raw === 'string' ? raw : '未知错误'
}

async function load(reset = false) {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  } else if (loading.value || !hasMore.value) {
    // 只有「加载更多」需要防重，切 tab 这类重置请求必须放行
    return
  }
  const seq = ++reqSeq
  loading.value = true
  try {
    await ensureLogin()
    const res = await getSquare({ sort: sort.value, pageNum: pageNum.value, pageSize })
    if (seq !== reqSeq) return
    const rows = res.rows || []
    loadError.value = ''
    if (reset) {
      list.value = rows
    } else {
      list.value = list.value.concat(rows)
    }
    total.value = res.total || 0
    hasMore.value = rows.length >= pageSize
    pageNum.value += 1
  } catch (e) {
    // request 里已弹过 toast，这里记下来给空态展示
    if (seq === reqSeq) loadError.value = errText(e)
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

function switchTab(key: string) {
  if (sort.value === key) return
  sort.value = key
  list.value = []
  load(true)
}

/**
 * 提前把「发一个」页预载好。
 *
 * 小程序里 tabBar 页第一次点进去才现加载 + 渲染那一长串表单，
 * 会有肉眼可见的白一下；广场页停留时先预热，点下去就是现成的。
 */
let publishPreloaded = false
function preloadPublish() {
  // #ifdef MP-WEIXIN
  if (publishPreloaded) return
  publishPreloaded = true
  try {
    // uni.preloadPage 仅微信小程序支持，低版本基础库没有就当没这回事
    ;(uni as any).preloadPage({ url: '/pages/publish/index' })
  } catch (e) {
    // 预载失败不影响正常跳转
  }
  // #endif
}

onShow(() => {
  load(true)
  preloadPublish()
  // 发完单后从「发一个」切回来，自动弹出刚发的那条
  const pending = takePendingOpenOrder()
  if (pending) setTimeout(() => openSheet(pending), 260)
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
  background: #f6f2e9;
}

.hero {
  /* 主色改成墨绿，配暖米底色，比橘色更「贵」也更耐看 */
  background: linear-gradient(150deg, #35604f 0%, #2a4e41 58%, #234439 100%);
  padding-bottom: 56rpx;
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
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
  color: #fbf7ee;
  letter-spacing: 1rpx;
}

.brand-dot {
  color: rgba(251, 247, 238, 0.5);
  font-size: 34rpx;
}

.brand-slogan {
  font-size: 24rpx;
  color: rgba(251, 247, 238, 0.78);
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
  color: #fbf7ee;
  line-height: 1;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(251, 247, 238, 0.76);
}

.tabs {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 30rpx 8rpx;
}

.tab {
  font-size: 26rpx;
  color: #5f6b63;
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid #ede6d7;
}

.tab.active {
  color: #fbf7ee;
  background: #2f5a4c;
  border-color: #2f5a4c;
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
  color: #9a9488;
}

.list {
  padding: 16rpx 24rpx 40rpx;
}

.footer-tip {
  text-align: center;
  font-size: 22rpx;
  color: #b3ada1;
  padding: 30rpx 0 20rpx;
}

/* 拉取失败：把地址和原因摊开，别再让人对着空列表猜 */
.load-fail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 120rpx 48rpx 60rpx;
}

.fail-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2f5a4c;
}

.fail-url,
.fail-msg {
  font-size: 22rpx;
  line-height: 1.6;
  color: #9a9488;
  text-align: center;
  word-break: break-all;
}

.fail-btn {
  margin-top: 16rpx;
  padding: 14rpx 44rpx;
  border-radius: 999rpx;
  background: #2f5a4c;
  color: #fff;
  font-size: 26rpx;
}
</style>
