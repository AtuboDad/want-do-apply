<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { ensureLogin } from './utils/auth'

onLaunch(() => {
  ensureLogin().catch(() => {})
})
</script>

<style lang="scss">
page {
  background: $bg;
  color: $text;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  font-size: 28rpx;
  line-height: 1.5;
}

view,
text,
image,
input,
textarea,
button {
  box-sizing: border-box;
}

.card {
  background: $card;
  border-radius: $radius;
  padding: 32rpx;
}

.row {
  display: flex;
  align-items: center;
}

.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.grow {
  flex: 1;
  min-width: 0;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ellipsis-2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

button::after {
  border: none;
}

.plain-btn {
  background: transparent;
  padding: 0;
  margin: 0;
  line-height: normal;
  font-size: inherit;
  color: inherit;
}
</style>
