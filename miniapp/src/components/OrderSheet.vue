<template>
  <view
    v-if="mounted"
    ref="maskRef"
    class="os-mask"
    :class="{ show: shown }"
    :style="{ '--tab-inset': inset + 'px' }"
    @tap="close"
  >
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
              <view class="dots">
                <text class="dot" />
                <text class="dot" />
                <text class="dot" />
              </view>
            </view>
            <view class="os-icon" @tap="close">
              <view class="cross">
                <text class="cross-line" />
                <text class="cross-line" />
              </view>
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
import { lockPageScroll, unlockPageScroll, forwardWheelToSheet, tabBarInsetPx } from '../utils/scroll-lock'

const props = defineProps<{
  visible: boolean
  /** 订单 ID 是雪花号，超过 JS 安全整数范围，必须按字符串传，别转 Number */
  orderId: number | string
}>()

const emit = defineEmits<{
  /** changed=true 表示弹层里真的改过这单，列表页可以据此决定要不要重拉 */
  (e: 'close', changed?: boolean): void
}>()

/** mounted 控制是否渲染，shown 控制动画状态，两者分开才能做出「滑出 + 淡出」的关闭动画 */
const mounted = ref(false)
const shown = ref(false)
const panelRef = ref<InstanceType<typeof OrderDetailPanel> | null>(null)
const maskRef = ref<unknown>(null)

/**
 * 底部菜单栏不再藏起来（藏起来用户会以为界面坏了），
 * 改成让弹层悬在它上面：H5 量 tabBar 高度做底部内边距，小程序页面视口本就不含 tabBar。
 */
const inset = ref(0)

let timer: ReturnType<typeof setTimeout> | null = null
/** 快速开关时 lock/unlock 必须成对且幂等，否则引用计数会漏加漏减 */
let pageLocked = false

function lockPage() {
  if (pageLocked) return
  pageLocked = true
  lockPageScroll()
}

function unlockPage() {
  if (!pageLocked) return
  pageLocked = false
  unlockPageScroll()
}

/**
 * H5 才能拿到真实 DOM（小程序端 ref 不是元素，也不会走这段）。
 * 滚轮统一由这里接管，见 utils/scroll-lock.ts 的说明。
 */
let wheelEl: HTMLElement | null = null

function maskElement(): HTMLElement | null {
  const r = maskRef.value as { $el?: HTMLElement } | HTMLElement | null
  if (!r) return null
  return (r as { $el?: HTMLElement }).$el || (r as HTMLElement)
}

function onWheel(e: WheelEvent) {
  forwardWheelToSheet(wheelEl, e)
  // 必须阻止默认，否则滚轮还是会漏到底层页面
  e.preventDefault()
}

function bindWheel() {
  // #ifdef H5
  const el = maskElement()
  if (!el || wheelEl === el) return
  unbindWheel()
  wheelEl = el
  el.addEventListener('wheel', onWheel, { passive: false })
  // #endif
}

function unbindWheel() {
  // #ifdef H5
  if (wheelEl) {
    wheelEl.removeEventListener('wheel', onWheel)
    wheelEl = null
  }
  // #endif
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
      inset.value = tabBarInsetPx()
      lockPage()
      // 等一帧再切 class，否则 transition 不会触发
      await nextTick()
      bindWheel()
      timer = setTimeout(() => {
        shown.value = true
      }, 20)
    } else {
      shown.value = false
      timer = setTimeout(() => {
        unbindWheel()
        mounted.value = false
        unlockPage()
      }, 300)
    }
  },
  { immediate: true }
)

// 兜底：弹层没走正常关闭流程就被卸载时，别把页面滚动留在锁定状态
onUnmounted(() => {
  unbindWheel()
  if (mounted.value) unlockPage()
})

function close() {
  emit('close', panelRef.value?.wasChanged?.() === true)
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
  /* 左右 + 底部留白，卡片从底部「浮」起来，不跟屏幕边缘粘在一起
     --tab-inset 由 JS 量 H5 的 uni-tabbar 高度；小程序视口本就不含 tabBar，值为 0。
     28rpx（14px）那版在真机 / 开发者工具上看还是贴着菜单栏，拉到 56rpx（28px）
     才明显是一张浮起来的卡片。 */
  padding: 0 22rpx calc(56rpx + var(--tab-inset, 0px) + env(safe-area-inset-bottom));
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
  position: relative;
  width: 100%;
  height: 82vh;
  /* 头部实际高度：16(padding-top) + 8(拖动条) + 12(间距) + 76(标题栏)。
     详情面板用绝对定位从这条线开始，rpx 会随屏宽自适应，不用为机型写死像素。 */
  --os-head-h: 112rpx;
  background: #f6f2e9;
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
  background: #f6f2e9;
  border-top-left-radius: 44rpx;
  border-top-right-radius: 44rpx;
}

.os-grab {
  width: 72rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #d8d2c4;
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
  color: #1d2320;
}

.os-acts {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.os-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(31, 74, 60, 0.07);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* 三个点：用实体圆点画，比字符「···」基线稳、也不会被字体带歪 */
.dots {
  display: flex;
  align-items: center;
  gap: 7rpx;
}

.dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #43514a;
}

.cross {
  position: relative;
  width: 26rpx;
  height: 26rpx;
}

.cross-line {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 3rpx;
  border-radius: 999rpx;
  background: #43514a;
}

.cross-line:first-child {
  transform: translateY(-50%) rotate(45deg);
}

.cross-line:last-child {
  transform: translateY(-50%) rotate(-45deg);
}
</style>
