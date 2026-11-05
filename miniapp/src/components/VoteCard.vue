<template>
  <view class="vote-card">
    <view class="head">
      <image class="avatar" :src="fullUrl(item.userAvatar) || defaultAvatar" mode="aspectFill" />
      <view class="grow">
        <text class="name">{{ item.userNickName || '匿名审批员' }}</text>
        <text class="time">{{ fromNow(item.createTime) }}</text>
      </view>
      <view class="result" :class="item.result === '1' ? 'pass' : 'reject'">
        {{ item.result === '1' ? '通过' : '驳回' }}
      </view>
    </view>

    <text class="reason">{{ item.reason }}</text>

    <view class="foot">
      <view class="like" :class="{ liked: item.liked }" @tap.stop="onLike">
        <text class="icon">{{ item.liked ? '❤️' : '🤍' }}</text>
        <text class="count">{{ item.likeCount || 0 }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { fullUrl } from '../utils/request'
import { fromNow } from '../utils/format'
import type { VoteItem } from '../api/types'

const props = defineProps<{ item: VoteItem }>()
const emit = defineEmits<{ (e: 'like', id: number): void }>()

const defaultAvatar =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcng9IjUwIiBmaWxsPSIjRkVFRkU2Ii8+PGNpcmNsZSBjeD0iNTAiIGN5PSIzOCIgcj0iMTYiIGZpbGw9IiNGMEM4QTQiLz48cGF0aCBkPSJNMjIgODJjMC0xNiAxMi0yOCAyOC0yOHMyOCAxMiAyOCAyOCIgZmlsbD0iI0YwQzhBNCIvPjwvc3ZnPg=='

function onLike() {
  emit('like', props.item.id)
}
</script>

<style lang="scss" scoped>
.vote-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx;
  margin-bottom: 16rpx;
}

.head {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #f5f5f7;
}

.name {
  font-size: 25rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.time {
  font-size: 21rpx;
  color: #9aa0a6;
  margin-top: 2rpx;
  display: block;
}

.result {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-weight: 600;
}

.pass {
  color: #22c55e;
  background: #ecfdf5;
}

.reject {
  color: #ef4444;
  background: #fef2f2;
}

.reason {
  display: block;
  margin-top: 18rpx;
  font-size: 27rpx;
  color: #333;
  line-height: 1.65;
}

.foot {
  margin-top: 16rpx;
  display: flex;
  justify-content: flex-end;
}

.like {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 4rpx 8rpx;
}

.icon {
  font-size: 26rpx;
}

.count {
  font-size: 23rpx;
  color: #9aa0a6;
}

.liked .count {
  color: #ef4444;
}
</style>
