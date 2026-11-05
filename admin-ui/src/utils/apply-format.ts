/** 消费审批 - 通用格式化工具 */
export const CATEGORIES = [
  { value: 'food', label: '吃喝' },
  { value: 'digital', label: '数码' },
  { value: 'cloth', label: '穿搭' },
  { value: 'beauty', label: '美妆' },
  { value: 'game', label: '玩乐' },
  { value: 'home', label: '家居' },
  { value: 'pet', label: '宠物' },
  { value: 'other', label: '其他' }
];

export function categoryLabel(v?: string): string {
  return CATEGORIES.find((c) => c.value === v)?.label || '其他';
}

export const ORDER_STATUS = [
  { value: '0', label: '待审批', type: 'warning' },
  { value: '1', label: '已通过', type: 'success' },
  { value: '2', label: '已驳回', type: 'danger' },
  { value: '3', label: '已撤销', type: 'info' },
  { value: '4', label: '已过期', type: 'info' }
];

export function orderStatusText(v?: string): string {
  return ORDER_STATUS.find((s) => s.value === v)?.label || '-';
}

export function orderStatusType(v?: string): string {
  return ORDER_STATUS.find((s) => s.value === v)?.type || 'info';
}

export const REPORT_STATUS = [
  { value: '0', label: '待处理', type: 'warning' },
  { value: '1', label: '已处理', type: 'success' },
  { value: '2', label: '已驳回', type: 'info' }
];

export function reportStatusText(v?: string): string {
  return REPORT_STATUS.find((s) => s.value === v)?.label || '-';
}

export function reportStatusType(v?: string): string {
  return REPORT_STATUS.find((s) => s.value === v)?.type || 'info';
}

export const TARGET_TYPES = [
  { value: '1', label: '审批单' },
  { value: '2', label: '批条' },
  { value: '3', label: '评论' }
];

export function targetTypeText(v?: string): string {
  return TARGET_TYPES.find((s) => s.value === v)?.label || '-';
}
