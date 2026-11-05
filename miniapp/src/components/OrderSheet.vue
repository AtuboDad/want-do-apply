<template>
  <view v-if="mounted" class="os-mask" :class="{ show: shown }" @tap="close">
    <!-- 只在顶部空白区拦截滑动，避免影响下面 sheet 里 scroll-view 的滚动 -->
    <view class="os-block" @touchmove.stop.prevent />
    <view class="os-sheet" :class="{ show: shown }" @tap.stop>
      <!-- 顶部拖动条 + 标题 -->
      <view class="os-head">
        <view class="os-grab" />
        <view class="os-bar">
          <text class="os-title">审批详情</text>
          <view class="os-acts">
            <view class="os-icon" @tap="onMore">
              <text>···</text>
            </view>
            <view class="os-icon" @tap="close">
              <text>✕</text>
            </view>
          </view>
        </view>
      </view>

      <OrderDetailPanel v-if="orderId" ref="panelRef" :order-id="orderId" @close="close" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import OrderDetailPanel from './OrderDetailPanel.vue'

const props = defineProps<{
  visible: boolean
  orderId: number
}>()

const emit = defineEmits<{ (e: 'close'): void }>()

/** mounted 控制是否渲染，shown 控制动画状态，两者分开才能做出「滑出 + 淡出」的关闭动画 */
const mounted = ref(false)
const shown = ref(false)
const panelRef = ref<InstanceType<typeof OrderDetailPanel> | null>(null)

let timer: ReturnType<typeof setTimeout> | null = null

/**
 * tabBar 是小程序的原生层，永远压在普通元素之上，
 * 不藏起来的话弹层底部的操作按钮会被它盖住。
 * 非 tabBar 页面调用这两个 API 会失败，忽略即可。
 */
function toggleTabBar(show: boolean) {
  const opts = { animation: false, fail: () => {} }
  if (show) {
    uni.showTabBar(opts)
  } else {
    uni.hideTabBar(opts)
  }
}

watch(
  () => props.visible,
  async (visible) => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    if (visible) {
      mounted.value = true
      toggleTabBar(false)
      // 等一帧再切 class，否则 transition 不会触发
      await nextTick()
      timer = setTimeout(() => {
        shown.value = true
      }, 20)
    } else {
      shown.value = false
      timer = setTimeout(() => {
        mounted.value = false
        toggleTabBar(true)
      }, 300)
    }
  },
  { immediate: true }
)

// 兜底：弹层没走正常关闭流程就被卸载时，别把 tabBar 留在隐藏状态
onUnmounted(() => {
  if (mounted.value) toggleTabBar(true)
})

function close() {
  emit('close')
}

function onMore() {
  panelRef.value?.onMore()
}
</script>

<style lang="scss" scoped>
.os-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 900;
  background: rgba(0, 0, 0, 0);
  transition: background 0.28s ease;
  display: flex;
  align-items: flex-end;
  /* 左右 + 底部留白，卡片从底部「浮」起来，不跟屏幕边缘粘在一起 */
  padding: 0 22rpx calc(22rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.os-mask.show {
  background: rgba(0, 0, 0, 0.5);
}

/* 遮罩上方的空白区域：挡住手势，防止底层页面跟着滚（滚动穿透） */
.os-block {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 21vh;
}

.os-sheet {
  width: 100%;
  height: 82vh;
  background: #f5f5f7;
  /* 四角都圆，像一张独立浮起的卡片 */
  border-radius: 44rpx;
  box-shadow: 0 16rpx 60rpx rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transform: translateY(110%);
  transition: transform 0.32s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.os-sheet.show {
  transform: translateY(0);
}

.os-head {
  position: relative;
  flex-shrink: 0;
  padding-top: 16rpx;
  background: #f5f5f7;
  border-top-left-radius: 44rpx;
  border-top-right-radius: 44rpx;
}

.os-grab {
  width: 72rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #c9c9d1;
  margin: 0 auto 12rpx;
}

.os-bar {
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28rpx 8rpx;
}

.os-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.os-acts {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.os-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #ebebef;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1;
}
</style>
