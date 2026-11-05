<template>
  <view class="navbar" :style="{ paddingTop: statusBarHeight + 'px', background: bg }">
    <view class="bar">
      <view class="left">
        <view v-if="back" class="back" @tap="goBack">
          <text class="arrow">‹</text>
        </view>
        <text class="title" :style="{ color: color }">{{ title }}</text>
      </view>
      <view class="right">
        <slot name="right" />
      </view>
    </view>
  </view>
  <view :style="{ height: totalHeight + 'px' }" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    back?: boolean
    bg?: string
    color?: string
    transparent?: boolean
  }>(),
  {
    title: '',
    back: false,
    bg: 'transparent',
    color: '#1D2320',
    transparent: false
  }
)

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

const barHeight = 44
const totalHeight = computed(() => statusBarHeight.value + barHeight)

function goBack() {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/index/index' })
  })
}
</script>

<style lang="scss" scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.bar {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  position: relative;
}

.left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.back {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: -12rpx;
}

.arrow {
  font-size: 48rpx;
  line-height: 1;
  padding-bottom: 4rpx;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
}

.right {
  display: flex;
  align-items: center;
}
</style>
