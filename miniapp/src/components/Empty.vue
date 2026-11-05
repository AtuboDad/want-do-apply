<template>
  <view class="empty">
    <view class="emoji">{{ emoji }}</view>
    <text class="text">{{ text }}</text>
    <slot />
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ text?: string; emoji?: string }>(), {
  text: '这里还什么都没有',
  emoji: '🧾'
})
</script>

<style lang="scss" scoped>
.empty {
  padding: 120rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.emoji {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.text {
  font-size: 26rpx;
  color: #9AA0A6;
}
</style>
