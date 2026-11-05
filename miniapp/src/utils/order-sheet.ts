/**
 * 跨页面传递「发完单后回广场并自动打开详情」的信号。
 *
 * ⚠️ 弹层的显隐状态没有放全局，而是由各页面自己持有（OrderSheet 走 props）。
 * 原因：tabBar 页面（广场、我的）是常驻不销毁的，如果 visible 放全局，
 * 打开一次弹层会让所有挂载了 <OrderSheet /> 的页面同时渲染、同时请求详情接口。
 */
let pendingOpenId = 0

/** 记下一条待打开的审批单，等广场页 onShow 时消费 */
export function setPendingOpenOrder(id: number) {
  pendingOpenId = id
}

/** 取出并清空待打开的审批单（一次性） */
export function takePendingOpenOrder(): number {
  const id = pendingOpenId
  pendingOpenId = 0
  return id
}
