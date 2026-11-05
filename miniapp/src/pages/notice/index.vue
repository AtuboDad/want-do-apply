<template>
  <view class="page">
    <NavBar title="消息" back bg="#ffffff">
      <template #right>
        <text class="read-all" @tap="readAll">全部已读</text>
      </template>
    </NavBar>

    <view class="list">
      <view v-for="n in list" :key="n.id" class="notice" :class="{ unread: n.isRead === '0' }" @tap="openNotice(n)">
        <view class="icon">{{ iconOf(n.type) }}</view>
        <view class="grow">
          <view class="head">
            <text class="title">{{ n.title }}</text>
            <text class="time">{{ fromNow(n.createTime) }}</text>
          </view>
          <text class="content">{{ n.content }}</text>
        </view>
      </view>

      <Empty v-if="list.length === 0" text="还没有消息" emoji="🔔" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import NavBar from '../../components/NavBar.vue'
import Empty from '../../components/Empty.vue'
import { getNotices, markRead } from '../../api/group'
import { fromNow } from '../../utils/format'
import { ensureLogin } from '../../utils/auth'
import type { NoticeItem } from '../../api/types'

const list = ref<NoticeItem[]>([])
const pageNum = ref(1)
const hasMore = ref(true)
const loading = ref(false)

function iconOf(type: string) {
  switch (type) {
    case '1': return '✍️'
    case '2': return '⚖️'
    case '3': return '💬'
    case '4': return '👥'
    default: return '📢'
  }
}

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
    const res = await getNotices(pageNum.value, 20)
    const rows = res.rows || []
    list.value = reset ? rows : list.value.concat(rows)
    hasMore.value = rows.length >= 20
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

async function readAll() {
  await markRead()
  list.value = list.value.map((n) => ({ ...n, isRead: '1' }))
  uni.showToast({ title: '已全部已读', icon: 'none' })
}

function openNotice(n: NoticeItem) {
  if (n.isRead === '0') {
    markRead([n.id]).catch(() => {})
    n.isRead = '1'
  }
  if (n.bizId) {
    uni.navigateTo({ url: `/pages/detail/index?id=${n.bizId}` })
  }
}

onLoad(() => {
  load(true)
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

.read-all {
  font-size: 24rpx;
  color: #ff6b35;
  padding: 8rpx;
}

.list {
  padding: 20rpx 24rpx 40rpx;
}

.notice {
  display: flex;
  gap: 18rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx;
  margin-bottom: 16rpx;
}

.notice.unread {
  background: #fffaf7;
}

.icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  background: #f5f5f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  flex-shrink: 0;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.title {
  font-size: 27rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.time {
  font-size: 21rpx;
  color: #b8bcc2;
  flex-shrink: 0;
}

.content {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: #6b7280;
  line-height: 1.6;
}
</style>
