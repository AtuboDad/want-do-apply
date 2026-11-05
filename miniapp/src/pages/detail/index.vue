<template>
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
import { onLoad } from '@dcloudio/uni-app'
import OrderSheet from '../../components/OrderSheet.vue'

const visible = ref(false)
const orderId = ref(0)

onLoad((options) => {
  const id = Number(options?.id || 0)
  if (!id) {
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
  background: #f5f5f7;
}
</style>
