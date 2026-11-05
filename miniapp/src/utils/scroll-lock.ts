import { computed } from 'vue'

/**
 * 绑定给 <page-meta :page-style>。
 *
 * 小程序里页面滚动是原生的，弹层再高也挡不住 touchmove 冒泡到页面，
 * 官方办法就是给 page 加 overflow:hidden —— 只在小程序端生效。
 * 用法：const pageStyle = pageStyleWhen(() => sheetVisible.value)
 */
export function pageStyleWhen(opened: () => boolean) {
  return computed(() => (opened() ? 'overflow: hidden;' : ''))
}

/**
 * 锁住底层页面滚动 + 把滚轮喂给弹层。
 *
 * 两类平台各有各的坑：
 *
 * 1) H5：uni-app 的页面滚动容器是 `body`（overflow-y:auto），不是 html。
 *    只锁 html 没用，只记录 window.scrollY 也没用（这里恒为 0），
 *    关掉弹层后页面会跳回顶部。所以 html / body 都锁，滚动位置也双双记住。
 *
 * 2) 小程序：页面滚动是原生的，`position:fixed` 的遮罩挡不住 touchmove 冒泡，
 *    手指在弹层上划一下，底下那页就跟着动。所以由页面上的 <page-meta> 改
 *    page-style 加 overflow:hidden（见各页 template 顶部），这里不重复处理。
 *
 * 用引用计数：嵌套打开（详情弹层里再开批阅弹窗）时，只有最外层关闭才真正解锁。
 */

interface SavedState {
  htmlOverflow: string
  bodyOverflow: string
  bodyPaddingRight: string
  windowY: number
  bodyTop: number
}

let depth = 0
let saved: SavedState | null = null

export function lockPageScroll(): void {
  // #ifdef H5
  depth += 1
  if (depth > 1 || saved) return

  const html = document.documentElement
  const body = document.body
  saved = {
    htmlOverflow: html.style.overflow,
    bodyOverflow: body.style.overflow,
    bodyPaddingRight: body.style.paddingRight,
    windowY: window.scrollY || 0,
    bodyTop: body.scrollTop || 0
  }

  // 滚动条消失会让页面横向抖一下，补上等宽内边距（移动端模拟下宽度为 0，不影响）
  const scrollbarWidth = window.innerWidth - html.clientWidth
  if (scrollbarWidth > 0) {
    body.style.paddingRight = `${scrollbarWidth}px`
  }
  body.style.overflow = 'hidden'
  html.style.overflow = 'hidden'
  // #endif
}

export function unlockPageScroll(): void {
  // #ifdef H5
  if (depth > 0) depth -= 1
  if (depth > 0 || !saved) return

  const html = document.documentElement
  const body = document.body
  const { windowY, bodyTop } = saved

  html.style.overflow = saved.htmlOverflow
  body.style.overflow = saved.bodyOverflow
  body.style.paddingRight = saved.bodyPaddingRight
  saved = null

  // 设 overflow:hidden 会把滚动位置清零，解锁后补回来
  if (bodyTop > 0) body.scrollTop = bodyTop
  if (windowY > 0) window.scrollTo(0, windowY)
  // #endif
}

/**
 * 弹层底部要额外避让的高度（px）。
 *
 * 小程序：页面视口本身不含 tabBar，弹层 bottom:0 就刚好在菜单栏上方，不用避让。
 * H5：tabBar 是一个 position:fixed、z-index:998 的 DOM 节点，会压在弹层上，
 *     所以量一下它有多高，让弹层悬在它上面。
 */
export function tabBarInsetPx(): number {
  // #ifdef H5
  const el = document.querySelector('uni-tabbar') as HTMLElement | null
  if (el) return el.offsetHeight || 0
  // #endif
  return 0
}

/**
 * 把滚轮事件喂给弹层内部的滚动区。
 *
 * 鼠标停在操作栏、卡片留白这些「不可滚」的地方时，滚轮原本会冒泡到页面。
 * 这里统一接管：只要滚轮落在弹层里，就滚详情区。
 */
export function forwardWheelToSheet(root: HTMLElement | null, event: WheelEvent): void {
  if (!root) return
  // 内层弹窗（批阅 / 补充说明）打开时，别再滚后面的详情
  if (root.querySelector('.mask')) return

  const scroller = pickScroller(root)
  if (!scroller) return

  // deltaMode=1 时单位是「行」，换算成像素
  const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? scroller.clientHeight : 1
  const next = scroller.scrollTop + event.deltaY * unit
  const max = scroller.scrollHeight - scroller.clientHeight
  scroller.scrollTop = Math.max(0, Math.min(next, max))
}

/**
 * 找弹层里真正能滚的那一层。
 *
 * 不写死 class：uni-app 各版本 scroll-view 的 DOM 结构、类名会变
 * （H5 下真身是第二层 .uni-scroll-view，第一层只是外壳）。
 * 直接按「overflow-y 是 auto/scroll 且内容比容器高」来找，最稳。
 */
function pickScroller(root: HTMLElement): HTMLElement | null {
  const nodes = root.querySelectorAll<HTMLElement>('*')
  let best: HTMLElement | null = null
  for (let i = 0; i < nodes.length; i++) {
    const el = nodes[i]
    if (el.clientHeight === 0) continue
    if (el.scrollHeight <= el.clientHeight + 1) continue
    const oy = getComputedStyle(el).overflowY
    if (oy !== 'auto' && oy !== 'scroll') continue
    // 越靠里越优先（真身通常在内层）
    best = el
  }
  return best
}
