<template>
  <view class="page">
    <view class="hero">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="hero-inner">
        <text class="hero-title">审批群</text>
        <text class="hero-sub">只给熟人看的那种审判</text>
        <view class="hero-actions">
          <view class="h-btn" @tap="openCreate">
            <text>＋ 创建群</text>
          </view>
          <view class="h-btn ghost" @tap="openJoin">
            <text>用邀请码加入</text>
          </view>
        </view>
      </view>
    </view>

    <view class="list">
      <view v-for="g in groups" :key="g.id" class="group-card" @tap="goDetail(g)">
        <view class="g-head">
          <view class="g-badge">{{ g.name.slice(0, 1) }}</view>
          <view class="grow">
            <view class="g-name-row">
              <text class="g-name ellipsis">{{ g.name }}</text>
              <text v-if="g.isOwner" class="owner-tag">群主</text>
            </view>
            <text class="g-meta">{{ g.memberCount }} 人 · 创建于 {{ fromNow(g.createTime) }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
        <text v-if="g.notice" class="g-notice ellipsis">{{ g.notice }}</text>
        <view class="g-foot">
          <view class="code" @tap.stop="copyCode(g.inviteCode)">
            <text class="code-label">邀请码</text>
            <text class="code-val">{{ g.inviteCode }}</text>
            <text class="copy">复制</text>
          </view>
          <text class="g-go">进入 →</text>
        </view>
      </view>

      <Empty v-if="groups.length === 0" text="还没有群，创建一个拉朋友进来" emoji="👥" />
    </view>

    <!-- 创建 -->
    <view v-if="createVisible" class="mask" @tap="createVisible = false">
      <view class="sheet" @tap.stop>
        <text class="sheet-title">创建审批群</text>
        <input v-model="newName" class="sheet-input" placeholder="给群起个名字，例：寝室的账本" maxlength="50" />
        <input v-model="newNotice" class="sheet-input" placeholder="群公告（可不填）" maxlength="200" />
        <button class="submit" @tap="doCreate">创建</button>
      </view>
    </view>

    <!-- 加入 -->
    <view v-if="joinVisible" class="mask" @tap="joinVisible = false">
      <view class="sheet" @tap.stop>
        <text class="sheet-title">用邀请码加入</text>
        <input v-model="joinCode" class="sheet-input code-input" placeholder="6 位邀请码" maxlength="6" />
        <button class="submit" @tap="doJoin">加入</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import Empty from '../../components/Empty.vue'
import { getMyGroups, createGroup, joinGroupByCode } from '../../api/group'
import { fromNow } from '../../utils/format'
import { ensureLogin } from '../../utils/auth'
import type { GroupItem } from '../../api/types'

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

const groups = ref<GroupItem[]>([])
const createVisible = ref(false)
const joinVisible = ref(false)
const newName = ref('')
const newNotice = ref('')
const joinCode = ref('')

async function load() {
  try {
    await ensureLogin()
    groups.value = await getMyGroups()
  } catch (e) {
    groups.value = []
  }
}

function openCreate() {
  newName.value = ''
  newNotice.value = ''
  createVisible.value = true
}

function openJoin() {
  joinCode.value = ''
  joinVisible.value = true
}

async function doCreate() {
  if (!newName.value.trim()) {
    uni.showToast({ title: '起个名字吧', icon: 'none' })
    return
  }
  await createGroup({ name: newName.value.trim(), notice: newNotice.value.trim() })
  createVisible.value = false
  uni.showToast({ title: '创建成功', icon: 'none' })
  await load()
}

async function doJoin() {
  if (!joinCode.value.trim()) {
    uni.showToast({ title: '填邀请码', icon: 'none' })
    return
  }
  await joinGroupByCode(joinCode.value.trim())
  joinVisible.value = false
  uni.showToast({ title: '已加入', icon: 'none' })
  await load()
}

function copyCode(code: string) {
  uni.setClipboardData({
    data: code,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'none' })
  })
}

function goDetail(g: GroupItem) {
  uni.navigateTo({ url: `/pages/group/detail?id=${g.id}` })
}

onShow(() => {
  load()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f6f2e9;
}

.hero {
  background: linear-gradient(135deg, #35604f 0%, #2f5a4c 55%, #234439 100%);
  padding-bottom: 40rpx;
  border-bottom-left-radius: 36rpx;
  border-bottom-right-radius: 36rpx;
}

.hero-inner {
  padding: 16rpx 36rpx 0;
}

.hero-title {
  font-size: 44rpx;
  font-weight: 800;
  color: #fff;
}

.hero-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.h-btn {
  background: #fff;
  color: #2f5a4c;
  font-size: 25rpx;
  font-weight: 600;
  border-radius: 999rpx;
  padding: 14rpx 32rpx;
}

.h-btn.ghost {
  background: rgba(255, 255, 255, 0.14);
  color: #f2ede2;
  border: 1rpx solid rgba(251, 247, 238, 0.55);
}

.list {
  padding: 24rpx;
}

.group-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.g-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.g-badge {
  width: 84rpx;
  height: 84rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #35604f, #2f5a4c);
  color: #fff;
  font-size: 36rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.g-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.g-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1d2320;
  max-width: 340rpx;
}

.owner-tag {
  font-size: 20rpx;
  color: #2f5a4c;
  background: #e9efe9;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}

.g-meta {
  display: block;
  font-size: 22rpx;
  color: #9a9488;
  margin-top: 6rpx;
}

.arrow {
  font-size: 36rpx;
  color: #c4c8ce;
}

.g-notice {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #5f6b63;
  background: #f8f4ec;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
}

.g-foot {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f1ebde;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.code {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.code-label {
  font-size: 21rpx;
  color: #9a9488;
}

.code-val {
  font-size: 24rpx;
  font-weight: 700;
  color: #1d2320;
  letter-spacing: 2rpx;
}

.copy {
  font-size: 20rpx;
  color: #2f5a4c;
  background: #e9efe9;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}

.g-go {
  font-size: 24rpx;
  color: #2f5a4c;
  font-weight: 600;
}

.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fff;
  border-top-left-radius: 32rpx;
  border-top-right-radius: 32rpx;
  padding: 40rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
}

.sheet-input {
  margin-top: 24rpx;
  background: #f8f4ec;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
}

.code-input {
  text-align: center;
  letter-spacing: 8rpx;
  font-size: 36rpx;
  font-weight: 700;
}

.submit {
  margin-top: 32rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #35604f, #2f5a4c);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
