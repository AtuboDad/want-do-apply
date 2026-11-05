/**
 * 锁住底层页面滚动。
 *
 * 只有 H5 会出现「弹层打开后，鼠标滚轮把下面那一页滚走」——
 * 因为 H5 的页面滚动是浏览器原生滚动，弹层再高也拦不住遮罩外面的滚轮和触摸。
 * 小程序端的页面滚动是原生的，遮罩上的 catchtouchmove 已经够了，所以这里是空实现。
 *
 * 用引用计数：嵌套打开（详情弹层里再开批阅弹窗）时，只有最外层关闭才真正解锁。
 */

interface SavedState {
  htmlOverflow: string
  bodyOverflow: string
  bodyPaddingRight: string
  scrollY: number
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
    scrollY: window.scrollY || html.scrollTop || 0
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
  const restoreY = saved.scrollY

  html.style.overflow = saved.htmlOverflow
  body.style.overflow = saved.bodyOverflow
  body.style.paddingRight = saved.bodyPaddingRight
  saved = null

  // 在 html 上设 overflow:hidden 会把滚动位置清零，解锁后补回来
  if (restoreY > 0) {
    window.scrollTo(0, restoreY)
  }
  // #endif
}

/**
 * 把滚轮事件喂给弹层内部的 scroll-view。
 *
 * uni-app 编译到 H5 后，scroll-view 真正的滚动容器是里面那层 overflow:auto 的 div，
 * 鼠标停在操作栏、卡片留白这些「不可滚」的地方时，滚轮会直接冒泡到页面。
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

function pickScroller(root: HTMLElement): HTMLElement | null {
  const list = root.querySelectorAll<HTMLElement>('.panel-scroll .uni-scroll-view')
  // 由内向外找第一个真的能滚的
  for (let i = list.length - 1; i >= 0; i--) {
    const el = list[i]
    if (el.scrollHeight > el.clientHeight + 1) return el
  }
  return list.length ? list[list.length - 1] : null
}
