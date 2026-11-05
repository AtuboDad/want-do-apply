<template>
  <!--
    会员角标：挂在头像右上角。
    迷你号给列表用（头像 72rpx 左右），普通号给「我的」大头像用。
  -->
  <view class="vip" :class="size">
    <text class="vip-text">VIP</text>
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: 'mini' | 'normal' }>(), {
  size: 'mini'
})
</script>

<style lang="scss" scoped>
.vip {
  position: absolute;
  right: -6rpx;
  top: -6rpx;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  /* 暖金渐变：跟暖米底色配，不用亮黄，避免太跳 */
  background: linear-gradient(135deg, #e8b563 0%, #c9902f 100%);
  border: 2rpx solid #fffaf0;
  box-shadow: 0 2rpx 6rpx rgba(160, 110, 20, 0.35);
}

.vip-text {
  color: #fffdf6;
  font-weight: 700;
  letter-spacing: 0.5rpx;
}

.mini {
  padding: 0 8rpx;
  height: 28rpx;

  .vip-text {
    font-size: 17rpx;
    line-height: 28rpx;
  }
}

.normal {
  padding: 0 10rpx;
  height: 34rpx;

  .vip-text {
    font-size: 20rpx;
    line-height: 34rpx;
  }
}
</style>
