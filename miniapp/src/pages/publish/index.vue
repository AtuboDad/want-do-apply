<template>
  <view class="page">
    <view class="hero">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="hero-inner">
        <text class="hero-title">发起一个审批</text>
        <text class="hero-sub">把冲动消费交给网友审判</text>
      </view>
    </view>

    <view class="form">
      <!-- 商品 -->
      <view class="field">
        <text class="label">想买什么 <text class="req">*</text></text>
        <input v-model="form.title" class="input" placeholder="例：一把 1800 的机械键盘" maxlength="100" />
      </view>

      <view class="field">
        <text class="label">多少钱 <text class="req">*</text></text>
        <view class="price-input">
          <text class="yuan">¥</text>
          <input v-model="priceText" class="input price" type="digit" placeholder="0.00" />
        </view>
      </view>

      <view class="field">
        <text class="label">为什么要买 <text class="req">*</text></text>
        <textarea
          v-model="form.reason"
          class="textarea"
          placeholder="认真写，这是网友判决的依据（5-1000 字）"
          maxlength="1000"
          auto-height
        />
        <text class="counter">{{ form.reason.length }}/1000</text>
      </view>

      <view class="field">
        <text class="label">有没有替代品</text>
        <input v-model="form.substitute" class="input" placeholder="例：家里那把还能用（没有可不填）" maxlength="200" />
      </view>

      <view class="field">
        <text class="label">图片（最多 3 张）</text>
        <view class="uploader">
          <view v-for="(img, i) in images" :key="i" class="img-wrap">
            <image class="img" :src="img" mode="aspectFill" />
            <view class="del" @tap="removeImage(i)">✕</view>
          </view>
          <view v-if="images.length < 3" class="add" @tap="chooseImage">
            <text class="plus">+</text>
          </view>
        </view>
      </view>

      <view class="field">
        <text class="label">分类</text>
        <view class="chips">
          <text
            v-for="c in CATEGORIES"
            :key="c.value"
            class="chip"
            :class="{ on: form.category === c.value }"
            @tap="form.category = c.value"
          >
            {{ c.label }}
          </text>
        </view>
      </view>

      <view class="field">
        <text class="label">发给谁看</text>
        <view class="visibility">
          <view class="v-item" :class="{ on: form.visibility === '0' }" @tap="form.visibility = '0'">
            <text class="v-title">公开广场</text>
            <text class="v-desc">让所有网友帮你把关</text>
          </view>
          <view class="v-item" :class="{ on: form.visibility === '1' }" @tap="pickGroup">
            <text class="v-title">只发审批群</text>
            <text class="v-desc">只给熟人看，更放得开</text>
          </view>
        </view>
        <view v-if="form.visibility === '1'" class="group-pick" @tap="pickGroup">
          <text v-if="selectedGroup">{{ selectedGroup.name }}（{{ selectedGroup.memberCount }} 人）</text>
          <text v-else class="placeholder">选择审批群 ›</text>
        </view>
      </view>

      <!-- 审批方式 -->
      <view class="field">
        <text class="label">审批方式</text>
        <view class="modes">
          <view class="mode" :class="{ on: form.approveMode === '0' }" @tap="form.approveMode = '0'">
            <text class="mode-title">单人审批</text>
            <text class="mode-desc">一人处理后结束</text>
          </view>
          <view class="mode" :class="{ on: form.approveMode === '1' }" @tap="form.approveMode = '1'">
            <text class="mode-title">多人审批</text>
            <text class="mode-desc">汇总多人意见</text>
          </view>
        </view>
      </view>

      <view class="field">
        <text class="label">{{ form.approveMode === '0' ? '多久没结论自动结束' : '审批多久' }}</text>
        <text class="sub-label">
          {{
            form.approveMode === '0'
              ? '有人表态就立刻出结果，这段时间只是兜底'
              : '到点后按「少数服从多数」出结果，期间谁都能批'
          }}
        </text>
        <view class="chips">
          <text
            v-for="d in DURATIONS"
            :key="d.value"
            class="chip"
            :class="{ on: form.durationHours === d.value }"
            @tap="form.durationHours = d.value"
          >
            {{ d.label }}
          </text>
        </view>
      </view>

      <button class="submit" :disabled="submitting" @tap="submit">
        {{ submitting ? '提交中…' : '发到广场' }}
      </button>

      <text class="tip">
        发出去谁都能审，{{ durationLabel(form.durationHours) }}后自动结束；中途还能补一句说明
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createOrder } from '../../api/order'
import { getMyGroups, joinGroupByCode } from '../../api/group'
import { uploadFile } from '../../utils/request'
import { CATEGORIES, durationLabel } from '../../utils/format'
import { ensureLogin } from '../../utils/auth'
import type { GroupItem } from '../../api/types'

/** 审批时长候选（小时） */
const DURATIONS = [
  { value: 1, label: '1 小时' },
  { value: 2, label: '2 小时' },
  { value: 6, label: '6 小时' },
  { value: 12, label: '12 小时' },
  { value: 24, label: '1 天' }
]

const statusBarHeight = ref(0)
// #ifdef MP-WEIXIN
statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
// #endif

const form = ref({
  title: '',
  reason: '',
  substitute: '',
  category: 'other',
  visibility: '0',
  /** 审批方式：0 单人 1 多人 */
  approveMode: '1',
  durationHours: 2,
  groupId: null as number | null
})

const priceText = ref('')
const images = ref<string[]>([])
const groups = ref<GroupItem[]>([])
const submitting = ref(false)

const selectedGroup = computed(() => groups.value.find((g) => g.id === form.value.groupId) || null)

async function loadGroups() {
  try {
    // 冷启动直接落在这页时还没有 token，不先静默登录就会吃一个 401，
    // 闪一句「登录已失效」再被踢回广场
    await ensureLogin()
    groups.value = await getMyGroups()
  } catch (e) {
    groups.value = []
  }
}

function chooseImage() {
  uni.chooseImage({
    count: 3 - images.value.length,
    sizeType: ['compressed'],
    success: async (res) => {
      const files = res.tempFilePaths as string[]
      for (const f of files) {
        try {
          uni.showLoading({ title: '上传中' })
          const up = await uploadFile(f)
          images.value.push(up.url)
        } catch (e) {
          // ignore
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

function removeImage(i: number) {
  images.value.splice(i, 1)
}

async function pickGroup() {
  if (groups.value.length === 0) {
    uni.showModal({
      title: '还没有审批群',
      content: '去创建一个，或者用邀请码加入朋友的群',
      confirmText: '去创建',
      success: (res) => {
        if (res.confirm) uni.switchTab({ url: '/pages/group/index' })
      }
    })
    return
  }
  uni.showActionSheet({
    itemList: groups.value.map((g) => g.name),
    success: (res) => {
      const g = groups.value[res.tapIndex]
      form.value.groupId = g.id
      form.value.visibility = '1'
    }
  })
}

async function submit() {
  if (submitting.value) return
  if (!form.value.title.trim()) {
    uni.showToast({ title: '想买什么都还没说', icon: 'none' })
    return
  }
  const price = Number(priceText.value)
  if (!priceText.value || isNaN(price) || price <= 0) {
    uni.showToast({ title: '填个价格吧', icon: 'none' })
    return
  }
  if (form.value.reason.trim().length < 5) {
    uni.showToast({ title: '理由至少写 5 个字', icon: 'none' })
    return
  }
  if (form.value.visibility === '1' && !form.value.groupId) {
    uni.showToast({ title: '请选择审批群', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await ensureLogin()
    await createOrder({
      title: form.value.title.trim(),
      price,
      reason: form.value.reason.trim(),
      substitute: form.value.substitute.trim(),
      images: images.value.join(','),
      category: form.value.category,
      visibility: form.value.visibility,
      approveMode: form.value.approveMode,
      durationHours: form.value.durationHours,
      groupId: form.value.groupId
    })
    reset()
    // 发完直接把人送到「我提交的」—— 之前跳广场再弹详情，
    // 用户回头去「我的」找自己刚发的单，找不到就以为没存上
    uni.showToast({ title: '发好了，在「我提交的」里', icon: 'none' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/mine/index' })
    }, 400)
  } catch (e: any) {
    // 发单失败必须让人看清原因：toast 一闪而过，用户会以为已经发出去了
    const msg: string = e?.message || e?.errMsg || '网络或服务异常，稍后再试'
    if (msg !== '未登录') {
      uni.showModal({
        title: '没发出去',
        content: msg,
        showCancel: false,
        confirmText: '知道了'
      })
    }
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.value = {
    title: '',
    reason: '',
    substitute: '',
    category: 'other',
    visibility: '0',
    approveMode: '1',
    durationHours: 2,
    groupId: null
  }
  priceText.value = ''
  images.value = []
}

onShow(() => {
  loadGroups()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f6f2e9;
  padding-bottom: 60rpx;
}

.hero {
  background: linear-gradient(150deg, #35604f 0%, #2a4e41 58%, #234439 100%);
  padding-bottom: 60rpx;
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
}

.hero-inner {
  padding: 16rpx 36rpx 0;
}

.hero-title {
  font-size: 44rpx;
  font-weight: 800;
  color: #fbf7ee;
}

.hero-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(251, 247, 238, 0.76);
}

.form {
  padding: 24rpx;
}

.field {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.label {
  font-size: 27rpx;
  font-weight: 600;
  color: #1d2320;
}

.req {
  color: #b04b3c;
}

.sub-label {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9a9488;
}

.input {
  margin-top: 16rpx;
  font-size: 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  color: #1d2320;
}

.price-input {
  display: flex;
  align-items: baseline;
  margin-top: 12rpx;
  gap: 8rpx;
}

.yuan {
  font-size: 32rpx;
  color: #b04b3c;
  font-weight: 700;
}

.input.price {
  font-size: 44rpx;
  font-weight: 700;
  color: #b04b3c;
  margin-top: 0;
  height: 70rpx;
}

.textarea {
  margin-top: 16rpx;
  width: 100%;
  min-height: 160rpx;
  background: #f8f4ec;
  border-radius: 16rpx;
  padding: 22rpx;
  font-size: 28rpx;
  line-height: 1.65;
  color: #1d2320;
  box-sizing: border-box;
}

.counter {
  display: block;
  text-align: right;
  font-size: 21rpx;
  color: #b3ada1;
  margin-top: 8rpx;
}

.uploader {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
  flex-wrap: wrap;
}

.img-wrap {
  position: relative;
  width: 180rpx;
  height: 180rpx;
}

.img {
  width: 180rpx;
  height: 180rpx;
  border-radius: 16rpx;
  background: #f4efe4;
}

.del {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add {
  width: 180rpx;
  height: 180rpx;
  border-radius: 16rpx;
  background: #faf6ee;
  border: 2rpx dashed #ddd5c4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plus {
  font-size: 56rpx;
  color: #b3ada1;
  line-height: 1;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.chip {
  font-size: 24rpx;
  color: #5f6b63;
  background: #f4efe4;
  border-radius: 999rpx;
  padding: 10rpx 20rpx;
}

.chip.on {
  color: #fbf7ee;
  background: #2f5a4c;
  font-weight: 600;
}

/* ---------- 审批方式 ---------- */
.modes {
  display: flex;
  gap: 18rpx;
  margin-top: 18rpx;
}

.mode {
  flex: 1;
  border: 2rpx solid #eee7d8;
  border-radius: 24rpx;
  padding: 24rpx 22rpx;
  background: #fff;
  transition: all 0.16s ease;
}

.mode.on {
  border-color: #2f5a4c;
  background: #edf3ee;
  box-shadow: inset 0 0 0 1rpx rgba(47, 90, 76, 0.12);
}

.mode-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1d2320;
}

.mode.on .mode-title {
  color: #234439;
}

.mode-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 21rpx;
  color: #8b8578;
}

.visibility {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
}

.v-item {
  flex: 1;
  border: 2rpx solid #eee7d8;
  border-radius: 20rpx;
  padding: 20rpx;
}

.v-item.on {
  border-color: #2f5a4c;
  background: #edf3ee;
}

.v-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #1d2320;
}

.v-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #8b8578;
}

.group-pick {
  margin-top: 16rpx;
  background: #f8f4ec;
  border-radius: 16rpx;
  padding: 22rpx;
  font-size: 26rpx;
}

.placeholder {
  color: #9a9488;
}

.submit {
  margin-top: 32rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 999rpx;
  background: #2f5a4c;
  color: #fbf7ee;
  font-size: 32rpx;
  font-weight: 700;
  box-shadow: 0 10rpx 24rpx rgba(47, 90, 76, 0.22);
}

.tip {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: #b3ada1;
  margin-top: 24rpx;
}
</style>
