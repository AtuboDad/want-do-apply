<template>
  <view class="order-card" @tap="go">
    <view class="head">
      <view class="avatar-wrap">
        <image class="avatar" :src="avatarUrl(item.userAvatar)" mode="aspectFill" />
        <VipBadge v-if="showVip" size="mini" />
      </view>
      <view class="grow">
        <view class="name-row">
          <text class="name">{{ item.userNickName || '匿名审批员' }}</text>
          <text v-if="item.mine" class="tag-mine">我</text>
          <text v-if="item.groupName" class="tag-group">{{ item.groupName }}</text>
        </view>
        <text class="time">{{ fromNow(item.createTime) }}</text>
      </view>
      <view class="status" :style="{ color: statusColor(item.status), borderColor: statusColor(item.status) }">
        {{ statusText(item.status) }}
      </view>
    </view>

    <view class="body">
      <view class="body-main">
        <text class="title">{{ item.title }}</text>
        <view class="price-row">
          <text class="currency">¥</text>
          <text class="price">{{ money(item.price) }}</text>
          <text v-if="item.isTop === '1'" class="tag-top">置顶</text>
        </view>
        <text v-if="item.reason" class="reason ellipsis-2">{{ item.reason }}</text>

        <view v-if="item.imageList && item.imageList.length" class="images">
          <image
            v-for="(img, i) in item.imageList.slice(0, 3)"
            :key="i"
            class="thumb"
            :src="fullUrl(img)"
            mode="aspectFill"
          />
          <view v-if="item.imageList.length > 3" class="more">+{{ item.imageList.length - 3 }}</view>
        </view>
      </view>
    </view>

    <view class="foot">
      <view class="votes">
        <view class="vote pass">
          <text class="dot">✓</text>
          <text class="num">{{ item.passCount || 0 }}</text>
        </view>
        <view class="vote reject">
          <text class="dot">✕</text>
          <text class="num">{{ item.rejectCount || 0 }}</text>
        </view>
        <view class="vote comment">
          <text class="dot">💬</text>
          <text class="num">{{ item.commentCount || 0 }}</text>
        </view>
        <text class="mode">{{ modeLabel }}</text>
      </view>
      <view class="action">
        <text v-if="item.status === '0'" class="cd">{{ countdown(item.remainSeconds) }}</text>
        <text v-if="item.myVoted" class="voted" :class="item.myResult === '1' ? 'is-pass' : 'is-reject'">
          已批{{ item.myResult === '1' ? '通过' : '驳回' }}
        </text>
        <text v-else-if="item.status === '0' && !item.mine" class="go">去审批 →</text>
        <text v-else-if="item.mine" class="go">看结果 →</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { fullUrl } from '../utils/request'
import { avatarUrl, shouldShowVip } from '../utils/avatar'
import VipBadge from './VipBadge.vue'
import { fromNow, money, countdown, statusText, statusColor } from '../utils/format'
import type { OrderItem } from '../api/types'

const props = defineProps<{ item: OrderItem }>()

const emit = defineEmits<{ (e: 'open', id: number | string): void }>()

/** 单人审批：一个人表态就出结果；多人审批：时长内攒意见 */
const modeLabel = computed(() => (props.item.approveMode === '0' ? '单人审批' : '多人审批'))

/** 会员头像 + 本人没关掉角标，才显示 VIP */
const showVip = computed(() => shouldShowVip(props.item.userAvatar, props.item.userShowVip))

function go() {
  // 由所在页面决定怎么展示详情（现在是弹出圆角弹层，不再是整页跳转）
  emit('open', props.item.id)
}
</script>

<style lang="scss" scoped>
.order-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(45, 58, 51, 0.04);
}

.head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 68rpx;
  height: 68rpx;
}

.avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: #f4efe4;
  flex-shrink: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1d2320;
}

.tag-mine {
  font-size: 20rpx;
  color: #2f5a4c;
  background: #e9efe9;
  border-radius: 6rpx;
  padding: 0 8rpx;
}

.tag-group {
  font-size: 20rpx;
  color: #7a7466;
  background: #f4efe4;
  border-radius: 6rpx;
  padding: 0 8rpx;
  max-width: 200rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.time {
  font-size: 22rpx;
  color: #a8a294;
  margin-top: 4rpx;
}

.status {
  font-size: 22rpx;
  border: 1rpx solid;
  border-radius: 999rpx;
  padding: 2rpx 16rpx;
  opacity: 0.95;
  flex-shrink: 0;
}

.body {
  margin-top: 24rpx;
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.body-main {
  flex: 1;
  min-width: 0;
}

/* 围观入口已经挪到审批详情里，卡片上不再挂按钮 */
.title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1d2320;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 8rpx;
  gap: 4rpx;
}

.currency {
  font-size: 24rpx;
  color: #b04b3c;
  font-weight: 600;
}

.price {
  font-size: 44rpx;
  color: #b04b3c;
  font-weight: 700;
  line-height: 1.1;
}

.tag-top {
  font-size: 20rpx;
  color: #fff;
  background: #c08a2e;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  margin-left: 12rpx;
  align-self: center;
}

.reason {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #6f6a5e;
  line-height: 1.6;
}

.images {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
  position: relative;
}

.thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background: #f4efe4;
}

.more {
  position: absolute;
  right: 0;
  bottom: 0;
  font-size: 22rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 16rpx;
  width: 160rpx;
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.foot {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1ebde;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.votes {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.vote {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.vote .dot {
  font-size: 22rpx;
}

.vote .num {
  font-size: 24rpx;
  color: #6f6a5e;
}

.pass .dot {
  color: #2f7d5b;
}

.reject .dot {
  color: #b04b3c;
}

.comment .dot {
  font-size: 20rpx;
}

.mode {
  font-size: 20rpx;
  color: #9a9488;
  background: #f7f3ea;
  border-radius: 999rpx;
  padding: 2rpx 14rpx;
}

.action {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cd {
  font-size: 22rpx;
  color: #a8a294;
}

.voted {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}

.is-pass {
  color: #2f7d5b;
  background: #e9f2ec;
}

.is-reject {
  color: #b04b3c;
  background: #fbece9;
}

.go {
  font-size: 24rpx;
  color: #2f5a4c;
  font-weight: 600;
}
</style>
