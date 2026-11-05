/** 相对时间 */
export function fromNow(time?: string): string {
  if (!time) return ''
  const t = parseTime(time)
  if (!t) return ''
  const diff = Date.now() - t
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 7 * 24 * 60 * 60 * 1000) return Math.floor(diff / 86400000) + ' 天前'
  const d = new Date(t)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function parseTime(time: string): number {
  // 兼容 iOS 不认 "yyyy-MM-dd HH:mm:ss"
  const normalized = time.replace(/-/g, '/')
  const t = new Date(normalized).getTime()
  return isNaN(t) ? 0 : t
}

/** 金额：整数不带小数，小数保留两位 */
export function money(v?: number | string): string {
  const n = Number(v || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

/** 大数字缩写 */
export function shortNum(v?: number): string {
  const n = Number(v || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return String(n)
}

/** 秒 → 倒计时文案 */
export function countdown(seconds?: number): string {
  if (!seconds || seconds <= 0) return '已结束'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h} 小时 ${m} 分`
  if (m > 0) return `${m} 分 ${s} 秒`
  return `${s} 秒`
}

/** 审批状态文案 */
export function statusText(status?: string): string {
  switch (status) {
    case '0': return '待审批'
    case '1': return '已通过'
    case '2': return '已驳回'
    case '3': return '已撤销'
    case '4': return '已过期'
    default: return ''
  }
}

/** 审批状态配色 */
export function statusColor(status?: string): string {
  switch (status) {
    case '0': return '#FF6B35'
    case '1': return '#22C55E'
    case '2': return '#EF4444'
    default: return '#9AA0A6'
  }
}

export const CATEGORIES = [
  { value: 'food', label: '吃喝' },
  { value: 'digital', label: '数码' },
  { value: 'cloth', label: '穿搭' },
  { value: 'beauty', label: '美妆' },
  { value: 'game', label: '玩乐' },
  { value: 'home', label: '家居' },
  { value: 'pet', label: '宠物' },
  { value: 'other', label: '其他' }
]

export function categoryLabel(v?: string): string {
  return CATEGORIES.find((c) => c.value === v)?.label || '其他'
}
