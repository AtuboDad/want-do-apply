import { request } from '../utils/request'
import type { OrderItem, OrderDetail, PageData } from './types'

export interface CreateOrderParams {
  title: string
  price: number
  reason: string
  substitute?: string
  images?: string
  category?: string
  visibility: string
  /** 审批时长（小时），1-72 */
  durationHours?: number
  groupId?: number | null
}

/** 广场列表 */
export function getSquare(params: {
  sort?: string
  status?: string
  pageNum?: number
  pageSize?: number
}): Promise<PageData<OrderItem>> {
  return request({ url: '/app/order/square', data: params })
}

/** 详情 */
export function getDetail(id: number): Promise<OrderDetail> {
  return request({ url: `/app/order/${id}` })
}

/** 发起审批 */
export function createOrder(data: CreateOrderParams): Promise<number> {
  return request({ url: '/app/order', method: 'POST', data })
}

/** 撤销 */
export function cancelOrder(id: number): Promise<void> {
  return request({ url: `/app/order/${id}/cancel`, method: 'POST' })
}

/** 补充说明（发起人，仅审批中可用） */
export function updateNote(id: number, note: string): Promise<void> {
  return request({ url: `/app/order/${id}/note`, method: 'POST', data: { note } })
}

/** 提前结束（发起人），按当前票数立即结算，返回结算后的状态 */
export function finishEarly(id: number): Promise<string> {
  return request({ url: `/app/order/${id}/finish`, method: 'POST' })
}

/** 删除 */
export function removeOrder(id: number): Promise<void> {
  return request({ url: `/app/order/${id}`, method: 'DELETE' })
}

/** 收藏切换 */
export function toggleFavorite(id: number): Promise<boolean> {
  return request({ url: `/app/order/${id}/favorite`, method: 'POST' })
}

/** 我的：apply-我提交的 vote-我处理的 favorite-我围观的 */
export function getMine(type: 'apply' | 'vote' | 'favorite', pageNum = 1, pageSize = 10): Promise<PageData<OrderItem>> {
  return request({ url: '/app/order/mine', data: { type, pageNum, pageSize } })
}

export interface ProfileData {
  user: {
    id: number
    nickName: string
    avatar: string
    creditScore: number
    applyCount: number
    voteCount: number
    adoptedCount: number
  }
  applyTotal: number
  passedTotal: number
  voteTotal: number
  adoptedTotal: number
  favoriteTotal: number
  unreadNotice: number
  savedAmount: number
}

/** 个人中心统计 */
export function getProfile(): Promise<ProfileData> {
  return request({ url: '/app/auth/profile' })
}
