/**
 * 微信开发者工具模拟器验证工具
 *
 * 为什么需要它：小程序的 scroll-view、自定义组件这些在 H5 里行为不同
 * （H5 的 flex 是标准的，所以「按钮被裁掉」这类问题浏览器里根本复现不出来）。
 * 这个脚本直接连模拟器，把真实渲染截出来。
 *
 * 前置（每次重新 build 小程序后都要做一遍，否则工具跑的还是旧包）：
 *   1. "D:\softs\Tencent\微信web开发者工具\cli.bat" close --project <小程序目录>
 *   2. "D:\softs\Tencent\微信web开发者工具\cli.bat" open  --project <小程序目录>
 *   3. "D:\softs\Tencent\微信web开发者工具\cli.bat" auto  --project <小程序目录> --auto-port 9421 --trust-project
 *   4. 等约 10 秒
 *
 * 注意：
 * - 一个连接里做完所有事，不要 disconnect 后再连（会提示 automation 未开启）
 * - page.$ 只能查页面自己的节点，查不到自定义组件（order-card 等）内部
 * - page.data() 里字段被 vue 编译压成单字母：f = 列表，f[i].a = 订单 id
 *
 * 用法：WS=ws://127.0.0.1:9421 node verify-detail.js [订单id]
 */
const automator = require('miniprogram-automator')
const path = require('path')
const fs = require('fs')

const OUT = path.resolve(__dirname, '../../.playwright-cli/final')
const WS = process.env.WS || 'ws://127.0.0.1:9421'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function firstOrderId(page) {
  for (let i = 0; i < 24; i++) {
    const d = await page.data()
    const list = d.f || []
    if (list.length && list[0].a) return String(list[0].a)
    await sleep(500)
  }
  return null
}

async function main() {
  if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true })
  const mp = await automator.connect({ wsEndpoint: WS })

  let id = process.argv[2]
  if (!id) {
    const p0 = await mp.reLaunch('/pages/index/index')
    id = await firstOrderId(p0)
  }
  if (!id) {
    console.error('广场没有任何单，先造一条再看')
    process.exit(1)
  }

  // 广场页支持 ?id= 直接打开详情（onLoad 里处理），不用点卡片
  await mp.reLaunch(`/pages/index/index?id=${id}`)
  await sleep(3500)
  const shot = path.join(OUT, 'mp-detail.png')
  await mp.screenshot({ path: shot })

  const page = await mp.currentPage()
  const d = await page.data()
  console.log('订单:', id)
  console.log('截图:', shot)
  // 弹层显隐在编译后的 data 里是 p（传给 order-sheet 的 props）
  console.log('弹层 props 是否为空:', d.p == null)

  await mp.disconnect()
}

main().catch((e) => {
  console.error('ERR', e && e.message)
  process.exit(1)
})
