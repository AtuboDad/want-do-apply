<template>
  <!-- #ifdef MP-WEIXIN -->
  <!-- 弹层打开时禁掉页面原生滚动，否则手指在弹层上划，底下的页面会跟着动 -->
  <page-meta :page-style="pageStyle" />
  <!-- #endif -->
  <view class="page">
    <view class="hero">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="profile">
        <view class="avatar-wrap">
          <image class="avatar" :src="avatarUrl(user?.avatar)" mode="aspectFill" @tap="editProfile" />
          <VipBadge v-if="showMyVip" size="normal" />
        </view>
        <view class="grow">
          <text class="nick">{{ user?.nickName || '审批员' }}</text>
          <view class="credit">
            <text class="credit-label">信用分</text>
            <text class="credit-val">{{ user?.creditScore ?? 100 }}</text>
          </view>
        </view>
        <view class="bell" @tap="goNotice">
          <text class="bell-icon">🔔</text>
          <view v-if="unread > 0" class="dot" />
        </view>
      </view>

      <view class="stats">
        <view class="stat">
          <text class="num">{{ profile?.applyTotal || 0 }}</text>
          <text class="label">发起</text>
        </view>
        <view class="stat">
          <text class="num">{{ profile?.voteTotal || 0 }}</text>
          <text class="label">处理</text>
        </view>
        <view class="stat">
          <text class="num">{{ profile?.favoriteTotal || 0 }}</text>
          <text class="label">围观</text>
        </view>
        <view class="stat">
          <text class="num">¥{{ money(profile?.savedAmount) }}</text>
          <text class="label">帮人省下</text>
        </view>
      </view>
    </view>

    <!-- 搜索 -->
    <view class="search">
      <view class="search-box">
        <view class="mag">
          <text class="mag-glass" />
          <text class="mag-handle" />
        </view>
        <input
          v-model="keywordDraft"
          class="search-input"
          placeholder="搜商品名、购买理由"
          placeholder-class="search-ph"
          confirm-type="search"
          @input="onSearchInput"
          @confirm="applySearch"
        />
        <view v-if="keywordDraft" class="clear" @tap="clearSearch">
          <text class="clear-x" />
        </view>
      </view>
    </view>

    <view class="tabs">
      <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: type === t.key }" @tap="switchTab(t.key)">
        {{ t.label }}
      </view>
    </view>

    <!-- 状态过滤 -->
    <scroll-view class="filters" scroll-x :show-scrollbar="false">
      <view class="filter-inner">
        <view
          v-for="f in FILTERS"
          :key="f.key"
          class="filter"
          :class="{ on: status === f.key }"
          @tap="switchStatus(f.key)"
        >
          {{ f.label }}
        </view>
      </view>
    </scroll-view>

    <view class="list">
      <OrderCard v-for="o in orders" :key="o.id" :item="o" @open="openSheet" @favorite="handleFavorite" />
      <Empty v-if="!loading && orders.length === 0" :text="emptyText" emoji="📭" />
      <view v-if="orders.length > 0" class="footer-tip">
        {{ hasMore ? '上拉加载更多' : '— 到底了 —' }}
      </view>
    </view>

    <!-- 编辑资料 -->
    <view v-if="editVisible" class="mask" :style="{ '--tab-inset': inset + 'px' }" @tap="closeEdit">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">修改资料</text>
          <view class="sheet-close" @tap="closeEdit">
            <text class="close-line" />
            <text class="close-line" />
          </view>
        </view>

        <view class="cur-avatar">
          <image class="avatar-preview" :src="avatarUrl(editAvatar)" mode="aspectFill" />
          <text class="avatar-tip">挑一个顺眼的形象</text>
        </view>

        <!-- 「我们画的」和「会员专属」做成标签切换：
             两组同时铺开会把整个弹层顶出屏幕顶部（真机上确实被顶出去了），
             一次只显示一组，高度直接砍掉一半。 -->
        <view class="av-tabs">
          <view class="av-tab" :class="{ on: avTab === 'preset' }" @tap="avTab = 'preset'">
            <text class="av-tab-text">我们画的</text>
            <text class="av-tab-num">{{ PRESET_AVATARS.length }}</text>
          </view>
          <view class="av-tab vip" :class="{ on: avTab === 'vip' }" @tap="avTab = 'vip'">
            <text class="av-tab-star">✦</text>
            <text class="av-tab-text">会员专属</text>
            <text class="av-tab-num">{{ VIP_AVATARS.length }}</text>
          </view>
        </view>

        <!-- 中间这层统一滚动：头像再多也只是在里面滑，弹层本身高度固定 -->
        <scroll-view class="sheet-body" scroll-y :show-scrollbar="false">
          <view v-if="avTab === 'preset'" class="avatar-grid">
            <view
              v-for="id in PRESET_AVATARS"
              :key="id"
              class="av-cell"
              :class="{ on: editAvatar === presetAvatarValue(id) }"
              @tap="pickPreset(id)"
            >
              <image class="av-img" :src="presetAvatarPath(id)" mode="aspectFill" />
            </view>
          </view>

          <view v-else class="vip-panel">
            <view class="vip-toggle-row">
              <text class="vip-hint">头像右上角的 VIP 标</text>
              <view class="vip-toggle" @tap="toggleShowVip">
                <text class="toggle-label">显示角标</text>
                <view class="toggle" :class="{ on: editShowVip }">
                  <view class="knob" />
                </view>
              </view>
            </view>
            <view class="avatar-grid vip-grid">
              <view
                v-for="id in VIP_AVATARS"
                :key="id"
                class="av-cell vip-cell"
                :class="{ on: editAvatar === presetAvatarValue(id) }"
                @tap="pickPreset(id)"
              >
                <!-- 圆形容器单独一层：外层要 overflow:visible，否则右上角的 VIP 标会被裁掉 -->
                <view class="vip-cell-inner">
                  <image class="av-img" :src="presetAvatarPath(id)" mode="aspectFill" />
                </view>
                <VipBadge v-if="editShowVip" size="mini" />
              </view>
            </view>
            <text class="vip-note">换上之后头像右上角会挂 VIP 标；关掉开关只是不露标，头像本身不变。</text>
          </view>
        </scroll-view>

        <view class="sheet-foot">
          <button class="wx-avatar" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
            或者用微信头像
          </button>

          <input
            class="sheet-input"
            type="nickname"
            :value="editNick"
            placeholder="输入昵称"
            maxlength="20"
            @input="onNickInput"
          />
          <button class="submit" @tap="saveProfile">保存</button>
        </view>
      </view>
    </view>

    <OrderSheet :visible="sheetVisible" :order-id="sheetOrderId" @close="handleSheetClose" />
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { pageStyleWhen, tabBarInsetPx } from '../../utils/scroll-lock'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import OrderCard from '../../components/OrderCard.vue'
import OrderSheet from '../../components/OrderSheet.vue'
import Empty from '../../components/Empty.vue'
import VipBadge from '../../components/VipBadge.vue'
import { getMine, getProfile, toggleFavorite } from '../../api/order'
import { getUnread } from '../../api/group'
import { updateProfile } from '../../api/auth'
import { uploadFile } from '../../utils/request'
import { avatarUrl, isVipAvatar, PRESET_AVATARS, VIP_AVATARS, presetAvatarPath, presetAvatarValue, shouldShowVip } from '../../utils/avatar'
import { money } from '../../utils/format'
import { userStore } from '../../store/user'
import { ensureLogin } from '../../utils/auth'
import type { OrderItem } from '../../api/types'
import type { ProfileData } from '../../api/order'

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

type MineTab = 'apply' | 'vote' | 'favorite'

const tabs: { key: MineTab; label: string }[] = [
  { key: 'apply', label: '我提交的' },
  { key: 'vote', label: '我处理的' },
  { key: 'favorite', label: '我围观的' }
]

/** 状态过滤：'' 表示全部 */
const FILTERS = [
  { key: '', label: '全部' },
  { key: '0', label: '审批中' },
  { key: '1', label: '已通过' },
  { key: '2', label: '已驳回' },
  { key: '4', label: '已结束' },
  { key: '3', label: '已撤回' }
]

const type = ref<MineTab>('apply')
const status = ref('')
const keyword = ref('')
const keywordDraft = ref('')
const orders = ref<OrderItem[]>([])
const profile = ref<ProfileData | null>(null)
const unread = ref(0)
const pageNum = ref(1)
const hasMore = ref(true)
const loading = ref(false)

const user = computed(() => userStore.user)

/** 自己头像上的角标：会员头像 + 自己没关开关 */
const showMyVip = computed(() => shouldShowVip(user.value?.avatar, user.value?.showVip))

const emptyText = computed(() => {
  if (keyword.value) return `没搜到「${keyword.value}」相关的单`
  if (status.value) return '这个状态下还没有单'
  if (type.value === 'apply') return '还没提交过审批'
  if (type.value === 'vote') return '还没处理过别人的单'
  return '还没围观过别人的单'
})

const editVisible = ref(false)
const editNick = ref('')
const editAvatar = ref('')
/** 头像分组标签：preset = 我们画的，vip = 会员专属 */
const avTab = ref<'preset' | 'vip'>('preset')
/** 会员角标开关（弹层里的临时态，点了立即落库） */
const editShowVip = ref(true)
const inset = ref(0)

const sheetVisible = ref(false)
/** 弹层打开时锁页面滚动（仅小程序生效）—— 详情弹层和资料弹层都算 */
const pageStyle = pageStyleWhen(() => sheetVisible.value || editVisible.value)
const sheetOrderId = ref<number | string>(0)

/** 卡片上的围观按钮：点一下关注，再点取消 */
async function handleFavorite(item: OrderItem) {
  try {
    const fav = await toggleFavorite(item.id)
    item.favorited = fav
    uni.showToast({ title: fav ? '开始围观' : '取消围观', icon: 'none' })
  } catch (e) {
    // 提示已在 request 里统一弹过
  }
}

function openSheet(id: number | string) {
  sheetOrderId.value = id
  sheetVisible.value = true
}

/** 弹层里改过这单才刷新，什么都没干就别动列表 */
function handleSheetClose(changed?: boolean) {
  sheetVisible.value = false
  if (changed) loadOrders(true)
}

async function loadProfile() {
  await ensureLogin()
  profile.value = await getProfile()
  unread.value = await getUnread().catch(() => 0)
}

/**
 * 请求序号：只有最新一次请求的结果才允许落地。
 *
 * 这里踩过坑：原来开头一句 `if (loading.value) return`，
 * 用户在首屏列表还没回来时就去搜索 / 切 tab / 点状态，
 * 重置请求直接被吞掉 —— 表现就是「搜了没反应」「明明有这单却查不到」。
 * 现在：重置（搜索、切 tab、切状态）永远放行；翻页才防重；
 * 晚回来的旧请求按序号丢弃，免得把新结果又覆盖成旧列表。
 */
let reqSeq = 0

async function loadOrders(reset = false) {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  } else if (loading.value || !hasMore.value) {
    // 只有「加载更多」需要防重
    return
  }
  const seq = ++reqSeq
  loading.value = true
  try {
    // 冷启动直接落在这一页时还没有 token，不先登录会吃 401，
    // 表现就是「统计有数、列表空白」。广场页早就这么做了，这里补上。
    await ensureLogin()
    const res = await getMine(type.value, pageNum.value, 10, keyword.value, status.value)
    if (seq !== reqSeq) return // 期间又发了新请求，这次结果作废
    const rows = res.rows || []
    orders.value = reset ? rows : orders.value.concat(rows)
    hasMore.value = rows.length >= 10
    pageNum.value += 1
  } catch (e) {
    // 提示已在 request 里统一弹过了
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

// ---------------- 搜索 ----------------

let searchTimer: ReturnType<typeof setTimeout> | null = null

/** 输入即搜，但压一下节奏，别每敲一个字就打一次接口 */
function onSearchInput(e: any) {
  keywordDraft.value = e.detail.value
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => applySearch(), 400)
}

function applySearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
  const v = keywordDraft.value.trim()
  if (v === keyword.value) return
  keyword.value = v
  orders.value = []
  loadOrders(true)
}

function clearSearch() {
  keywordDraft.value = ''
  applySearch()
}

onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer)
})

// ---------------- 切换 ----------------

/**
 * 切换 tab。
 *
 * 点「已经选中的那个 tab」按老逻辑直接 return —— 用户发完单回到这一页，
 * 再点一次「我提交的」想让它刷新，结果一声不响，列表还是旧数据，
 * 而顶部「发起」统计已经是新数字，看着就像两条单丢了一条。
 * 所以：再点一次当前 tab = 手动刷新。
 */
function switchTab(k: MineTab) {
  if (type.value === k) {
    refreshList()
    return
  }
  type.value = k
  status.value = ''
  refreshList()
}

/** 清空重拉 + 回到顶部；搜索词会保留，切 tab 时状态筛选由 switchTab 清掉 */
function refreshList() {
  orders.value = []
  loadOrders(true)
  uni.pageScrollTo({ scrollTop: 0, duration: 200 })
}

function switchStatus(k: string) {
  if (status.value === k) return
  status.value = k
  orders.value = []
  loadOrders(true)
}

function goNotice() {
  uni.navigateTo({ url: '/pages/notice/index' })
}

// ---------------- 资料 ----------------

function editProfile() {
  editNick.value = user.value?.nickName || ''
  editAvatar.value = user.value?.avatar || ''
  editShowVip.value = user.value?.showVip !== '0'
  // 现在用的是会员头像就直接落在会员那一栏，省得用户再点一次
  avTab.value = isVipAvatar(editAvatar.value) ? 'vip' : 'preset'
  inset.value = tabBarInsetPx()
  editVisible.value = true
}

function closeEdit() {
  editVisible.value = false
}

/** 选预设头像：存 preset:av03 这种标记，前端渲染走本地图片 */
function pickPreset(id: string) {
  editAvatar.value = presetAvatarValue(id)
}

/**
 * 会员角标开关。
 *
 * 这个开关和头像分开存：头像是「是不是会员」，开关是「愿不愿意露」。
 * 点了立刻落库，不必等下面那个「保存」—— 不然用户会以为没生效。
 */
async function toggleShowVip() {
  const next = !editShowVip.value
  try {
    await updateProfile(undefined, undefined, next ? '1' : '0')
    editShowVip.value = next
    uni.showToast({ title: next ? '角标已打开' : '角标已隐藏', icon: 'none' })
  } catch (e) {
    // 提示已由 request 统一弹出，这里保持开关原状
  }
}

function onNickInput(e: any) {
  editNick.value = e.detail.value
}

async function onChooseAvatar(e: any) {
  const temp = e.detail.avatarUrl as string
  if (!temp) return
  uni.showLoading({ title: '上传中' })
  try {
    const up = await uploadFile(temp)
    editAvatar.value = up.url
  } catch (err) {
    // ignore
  } finally {
    uni.hideLoading()
  }
}

async function saveProfile() {
  if (!editNick.value.trim()) {
    uni.showToast({ title: '填个昵称', icon: 'none' })
    return
  }
  await updateProfile(editNick.value.trim(), editAvatar.value)
  editVisible.value = false
  uni.showToast({ title: '已保存', icon: 'none' })
  await loadProfile()
  // 列表里的作者头像也要跟着换，否则得手动下拉才看得到
  await loadOrders(true)
}

onShow(() => {
  loadProfile()
  loadOrders(true)
})

onReachBottom(() => {
  if (hasMore.value) loadOrders()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f6f2e9;
}

.hero {
  background: linear-gradient(150deg, #35604f 0%, #2a4e41 58%, #234439 100%);
  padding-bottom: 40rpx;
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.profile {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 36rpx 0;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 108rpx;
  height: 108rpx;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  border: 3rpx solid rgba(251, 247, 238, 0.5);
}

.nick {
  font-size: 36rpx;
  font-weight: 700;
  color: #fbf7ee;
}

.credit {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 10rpx;
  background: rgba(251, 247, 238, 0.16);
  border-radius: 999rpx;
  padding: 4rpx 18rpx;
}

.credit-label {
  font-size: 20rpx;
  color: rgba(251, 247, 238, 0.78);
}

.credit-val {
  font-size: 22rpx;
  font-weight: 700;
  color: #fbf7ee;
}

.bell {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(251, 247, 238, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.bell-icon {
  font-size: 32rpx;
}

.dot {
  position: absolute;
  top: 12rpx;
  right: 14rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #e9a13b;
}

.stats {
  display: flex;
  margin: 32rpx 30rpx 0;
  background: rgba(251, 247, 238, 0.12);
  border-radius: 24rpx;
  padding: 22rpx 0;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-size: 34rpx;
  font-weight: 800;
  color: #fbf7ee;
}

.label {
  font-size: 21rpx;
  color: rgba(251, 247, 238, 0.75);
  margin-top: 4rpx;
}

/* ---------- 搜索 ---------- */
.search {
  padding: 24rpx 24rpx 0;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  height: 80rpx;
  background: #fff;
  border-radius: 999rpx;
  padding: 0 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(45, 58, 51, 0.05);
}

/* 放大镜：一个圆环 + 一根斜柄，比 emoji 干净 */
.mag {
  position: relative;
  width: 30rpx;
  height: 30rpx;
  flex-shrink: 0;
}

.mag-glass {
  position: absolute;
  top: 0;
  left: 0;
  width: 20rpx;
  height: 20rpx;
  border: 3rpx solid #9a9488;
  border-radius: 50%;
  box-sizing: border-box;
}

.mag-handle {
  position: absolute;
  right: 1rpx;
  bottom: 2rpx;
  width: 11rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: #9a9488;
  transform: rotate(45deg);
}

.search-input {
  flex: 1;
  height: 80rpx;
  font-size: 27rpx;
  color: #1d2320;
}

.search-ph {
  color: #b3ada1;
}

.clear {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #eae4d6;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.clear-x {
  position: relative;
  width: 16rpx;
  height: 3rpx;
  background: #8b8578;
  border-radius: 999rpx;
  transform: rotate(45deg);
}

.clear-x::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 16rpx;
  height: 3rpx;
  background: #8b8578;
  border-radius: 999rpx;
  transform: rotate(90deg);
}

/* ---------- tabs ---------- */
.tabs {
  display: flex;
  gap: 12rpx;
  padding: 22rpx 24rpx 0;
}

.tab {
  font-size: 26rpx;
  color: #5f6b63;
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid #ede6d7;
}

.tab.active {
  color: #fbf7ee;
  background: #2f5a4c;
  border-color: #2f5a4c;
  font-weight: 600;
}

/* ---------- 状态过滤 ---------- */
.filters {
  margin-top: 18rpx;
  white-space: nowrap;
  width: 100%;
}

.filter-inner {
  display: inline-flex;
  gap: 10rpx;
  padding: 0 24rpx;
}

.filter {
  font-size: 23rpx;
  color: #7a7466;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #efe9dc;
  flex-shrink: 0;
}

.filter.on {
  color: #2f5a4c;
  background: #e2ebe3;
  font-weight: 600;
  box-shadow: inset 0 0 0 1rpx #bed2c3;
}

.list {
  padding: 20rpx 24rpx 40rpx;
}

.footer-tip {
  text-align: center;
  font-size: 22rpx;
  color: #b3ada1;
  padding: 24rpx 0;
}

/* ---------- 资料弹层 ---------- */
.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(20, 24, 22, 0.45);
  z-index: 200;
  display: flex;
  align-items: flex-end;
  padding-left: 20rpx;
  padding-right: 20rpx;
  /* H5 要把 tabBar 高度算进去，再留 56rpx 的缝，不然弹层底边和菜单栏粘在一起 */
  padding-bottom: calc(56rpx + var(--tab-inset, 0px) + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* 高度封顶 + 中间滚动：
   原来靠内容撑，20 个普通头像加 6 个会员头像一起铺开，比屏幕还高，
   弹层就从顶部顶出去了 —— 真机 / 开发者工具里上面的标题和预览全被切掉。
   现在标题、预览、标签、底部按钮都是固定区，只有头像区在中间滚，
   封顶 82vh 而不是写死高度：会员那栏只有 6 个，弹层就该矮一点。
   注意不能写 height:0 撑满，那样内容少时会把整层压塌。 */
.sheet {
  width: 100%;
  max-height: 82vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 40rpx;
  padding: 30rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  /* 兜底：中间区万一没被压住，也不许内容漏到弹层外面 */
  overflow: hidden;
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1d2320;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sheet-close {
  position: relative;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #f4efe4;
}

.close-line {
  position: absolute;
  top: 50%;
  left: 28%;
  width: 44%;
  height: 3rpx;
  border-radius: 999rpx;
  background: #6b736d;
}

.close-line:first-child {
  transform: translateY(-50%) rotate(45deg);
}

.close-line:last-child {
  transform: translateY(-50%) rotate(-45deg);
}

.cur-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  margin-top: 10rpx;
  flex-shrink: 0;
}

.avatar-preview {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: #f4efe4;
  border: 4rpx solid #fff;
  box-shadow: 0 6rpx 24rpx rgba(47, 90, 76, 0.16);
}

.avatar-tip {
  font-size: 22rpx;
  color: #9a9488;
}

/* ===== 头像分组标签 ===== */
.av-tabs {
  flex-shrink: 0;
  display: flex;
  gap: 8rpx;
  margin-top: 22rpx;
  padding: 6rpx;
  background: #f4efe4;
  border-radius: 999rpx;
}

.av-tab {
  flex: 1;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border-radius: 999rpx;
  color: #7a7466;
  font-size: 25rpx;
  transition: background 0.2s ease, color 0.2s ease;
}

.av-tab.on {
  background: #fff;
  color: #2f5a4c;
  font-weight: 700;
  box-shadow: 0 4rpx 14rpx rgba(47, 90, 76, 0.12);
}

.av-tab.vip.on {
  color: #8a6a20;
}

.av-tab-star {
  font-size: 22rpx;
  color: #c9902f;
  line-height: 1;
}

.av-tab-num {
  font-size: 20rpx;
  color: #b3ada0;
  line-height: 1;
}

.av-tab.on .av-tab-num {
  color: #8fa79a;
}

/* 中间滚动区。
   uni-app 的 scroll-view 内层高度是按内容撑出来的，外层被 flex 压缩后
   内层不跟着缩（实测外层 335、内层仍 397），头像就漏到下面的按钮上。
   内层要按外层高度百分比算，就得让外层自己有确定高度 ——
   所以这里用 calc 直接把「82vh 减去标题/预览/标签/按钮的固定高度」算出来。
   700rpx 是这些固定块的合计（略宽松，宁可少一行也别溢出）。 */
.sheet-body {
  flex: 0 0 auto;
  height: calc(82vh - 700rpx);
  min-height: 200rpx;
  margin-top: 20rpx;
}

/* 底部操作区固定，不跟着头像滚 */
.sheet-foot {
  flex-shrink: 0;
  padding-top: 18rpx;
  border-top: 1rpx solid #f1ece0;
}

/* ===== 会员专属头像 ===== */
.vip-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
  padding: 16rpx 20rpx;
  border-radius: 18rpx;
  background: #fdf7e9;
}

.vip-hint {
  font-size: 23rpx;
  color: #8a6a20;
}

.vip-toggle {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.toggle-label {
  font-size: 22rpx;
  color: #9a9488;
}

.toggle {
  width: 64rpx;
  height: 36rpx;
  border-radius: 999rpx;
  background: #ded7c7;
  padding: 4rpx;
  box-sizing: border-box;
  transition: background 0.2s ease;
}

.toggle.on {
  background: #c9902f;
}

.knob {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2rpx 5rpx rgba(90, 70, 20, 0.22);
  transition: transform 0.2s ease;
}

.toggle.on .knob {
  transform: translateX(28rpx);
}

.vip-grid {
  margin-top: 4rpx;
}

.vip-note {
  display: block;
  margin-top: 22rpx;
  padding: 0 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #b09a6a;
  text-align: center;
}

/* 外层不裁切，好让右上角的 VIP 标露出来。
   要写成 .av-cell.vip-cell：单独一个 .vip-cell 特异性不够，
   会被上面的 .av-cell { overflow: hidden } 盖掉，角标就看不见了。 */
.av-cell.vip-cell {
  position: relative;
  overflow: visible;
  border: none;
  background: transparent;
}

.vip-cell-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  box-sizing: border-box;
  background: #f7f3ea;
  border: 4rpx solid rgba(201, 144, 47, 0.34);
}

.vip-cell.on .vip-cell-inner {
  border-color: #c9902f;
  box-shadow: 0 0 0 4rpx rgba(201, 144, 47, 0.18);
}

/* 4 列：4×140 + 3×16 = 608rpx，排在 686rpx 的可用宽度里居中 */
.avatar-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16rpx;
}

.av-cell {
  width: 140rpx;
  height: 140rpx;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  box-sizing: border-box;
  background: #f7f3ea;
  border: 4rpx solid transparent;
}

.av-cell.on {
  border-color: #2f5a4c;
  box-shadow: 0 0 0 4rpx rgba(47, 90, 76, 0.14);
}

.av-img {
  width: 100%;
  height: 100%;
  display: block;
}

.wx-avatar {
  margin-top: 0;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 999rpx;
  background: #f4efe4;
  color: #5f6b63;
  font-size: 26rpx;
  font-weight: 500;
  padding: 0;
}

.wx-avatar::after {
  border: none;
}

.sheet-input {
  margin-top: 16rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: #f7f3ea;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  text-align: center;
  color: #1d2320;
}

.submit {
  margin-top: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: #2f5a4c;
  color: #fbf7ee;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
