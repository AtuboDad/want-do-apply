import { request } from '../utils/request'
import type { GroupItem, OrderItem, PageData, NoticeItem } from './types'

/** 我的群 */
export function getMyGroups(): Promise<GroupItem[]> {
  return request({ url: '/app/group/mine' })
}

/** 创建群 */
export function createGroup(data: { name: string; notice?: string }): Promise<number> {
  return request({ url: '/app/group', method: 'POST', data })
}

/** 邀请码加入 */
export function joinGroupByCode(inviteCode: string): Promise<number> {
  return request({ url: `/app/group/join?inviteCode=${encodeURIComponent(inviteCode)}`, method: 'POST' })
}

/** 按 ID 加入 */
export function joinGroup(id: number): Promise<number> {
  return request({ url: `/app/group/${id}/join`, method: 'POST' })
}

/** 群详情 */
export function getGroup(id: number): Promise<GroupItem> {
  return request({ url: `/app/group/${id}` })
}

/** 群成员 */
export function getGroupMembers(id: number): Promise<any[]> {
  return request({ url: `/app/group/${id}/members` })
}

/** 群内审批单 */
export function getGroupOrders(id: number, params: { sort?: string; pageNum?: number; pageSize?: number }): Promise<PageData<OrderItem>> {
  return request({ url: `/app/group/${id}/orders`, data: params })
}

/** 退群 */
export function quitGroup(id: number): Promise<void> {
  return request({ url: `/app/group/${id}/quit`, method: 'POST' })
}

/** 解散群 */
export function dissolveGroup(id: number): Promise<void> {
  return request({ url: `/app/group/${id}/dissolve`, method: 'POST' })
}

/** 通知列表 */
export function getNotices(pageNum = 1, pageSize = 20): Promise<PageData<NoticeItem>> {
  return request({ url: '/app/notice/list', data: { pageNum, pageSize } })
}

/** 未读数 */
export function getUnread(): Promise<number> {
  return request({ url: '/app/notice/unread', silent: true })
}

/** 标记已读 */
export function markRead(ids?: number[]): Promise<void> {
  return request({ url: '/app/notice/read', method: 'POST', data: ids || [] })
}

/** 举报 */
export function report(data: {
  targetType: string
  targetId: number | string
  reasonType?: string
  content?: string
}): Promise<number> {
  return request({ url: '/app/report', method: 'POST', data })
}
