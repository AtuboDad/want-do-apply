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

/** 绝对时间：2026-10-17 18:54 */
export function dateTime(time?: string): string {
  if (!time) return ''
  const t = parseTime(time)
  if (!t) return ''
  const d = new Date(t)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 审批时长文案：2 小时 / 1 天 */
export function durationLabel(hours?: number): string {
  const h = hours && hours > 0 ? hours : 24
  if (h % 24 === 0 && h >= 24) return `${h / 24} 天`
  return `${h} 小时`
}

/** 审批状态文案 */
export function statusText(status?: string): string {
  switch (status) {
    case '0': return '审批中'
    case '1': return '已通过'
    case '2': return '已驳回'
    case '3': return '已撤销'
    case '4': return '已结束'
    default: return ''
  }
}

/** 审批状态配色（与新版暖米 + 墨绿 + 砖红色板一致） */
export function statusColor(status?: string): string {
  switch (status) {
    case '0': return '#C08A2E'
    case '1': return '#2F7D5B'
    case '2': return '#B04B3C'
    default: return '#9A9488'
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

/** 审批方式文案：单人 / 多人 */
export function modeText(mode?: string): string {
  return mode === '0' ? '单人审批' : '多人审批'
}

/** 审批方式的规则说明 */
export function modeDesc(mode?: string): string {
  return mode === '0' ? '一人处理后结束' : '汇总多人意见'
}
