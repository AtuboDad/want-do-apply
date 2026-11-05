<template>
  <view class="vote-card">
    <view class="head">
      <image class="avatar" :src="avatarUrl(item.userAvatar)" mode="aspectFill" />
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
import { avatarUrl } from '../utils/avatar'
import type { VoteItem } from '../api/types'

const props = defineProps<{ item: VoteItem }>()
const emit = defineEmits<{ (e: 'like', id: number): void }>()

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
  background: #f6f2e9;
}

.name {
  font-size: 25rpx;
  font-weight: 600;
  color: #1d2320;
}

.time {
  font-size: 21rpx;
  color: #9a9488;
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
  color: #2f7d5b;
  background: #e9f2ec;
}

.reject {
  color: #b04b3c;
  background: #fbece9;
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
  color: #9a9488;
}

.liked .count {
  color: #b04b3c;
}
</style>
