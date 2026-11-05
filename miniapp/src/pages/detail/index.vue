<template>
  <!-- #ifdef MP-WEIXIN -->
  <!-- 弹层打开时禁掉页面原生滚动，否则手指在弹层上划，底下的页面会跟着动 -->
  <page-meta :page-style="pageStyle" />
  <!-- #endif -->
  <!--
    独立详情页现在只作为兜底入口：分享卡片、扫码、外部跳转等场景会落到这里。
    正常从列表点进去走的是弹层（OrderSheet），不会再跳这个页面。
    进来之后同样以弹层呈现，保证两处体验一致；关掉弹层就自动返回上一页。
  -->
  <view class="holder">
    <OrderSheet :visible="visible" :order-id="orderId" @close="onClose" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { pageStyleWhen } from '../../utils/scroll-lock'
import { onLoad } from '@dcloudio/uni-app'
import OrderSheet from '../../components/OrderSheet.vue'

const visible = ref(false)
/** 弹层打开时锁页面滚动（仅小程序生效） */
const pageStyle = pageStyleWhen(() => visible.value)
const orderId = ref<number | string>(0)

onLoad((options) => {
  // 雪花 ID 有 19 位，转 Number 会丢精度（末尾几位被抹平），接口就查不到单了
  const id = String(options?.id || '')
  if (!id || id === '0') {
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    return
  }
  orderId.value = id
  // 等页面渲染完再弹，滑入动画才看得出来
  setTimeout(() => {
    visible.value = true
  }, 60)
})

function onClose() {
  visible.value = false
  setTimeout(() => {
    uni.navigateBack({
      fail: () => uni.switchTab({ url: '/pages/index/index' })
    })
  }, 320)
}
</script>

<style lang="scss" scoped>
.holder {
  min-height: 100vh;
  background: #f6f2e9;
}
</style>
