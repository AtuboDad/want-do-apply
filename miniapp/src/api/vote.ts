import { request } from '../utils/request'
import type { VoteItem, CommentItem } from './types'

/** 批阅 */
export function submitVote(data: { orderId: number; result: string; reason: string }): Promise<boolean> {
  return request({ url: '/app/vote', method: 'POST', data })
}

/** 批条列表 */
export function getVotes(orderId: number, sort: 'hot' | 'new' = 'hot'): Promise<VoteItem[]> {
  return request({ url: '/app/vote/list', data: { orderId, sort } })
}

/** 点赞 */
export function likeVote(id: number): Promise<boolean> {
  return request({ url: `/app/vote/${id}/like`, method: 'POST' })
}

/** 评论列表 */
export function getComments(orderId: number): Promise<CommentItem[]> {
  return request({ url: '/app/comment/list', data: { orderId } })
}

/** 发表评论 */
export function addComment(data: {
  orderId: number
  content: string
  parentId?: number
  replyUserId?: number
}): Promise<number> {
  return request({ url: '/app/comment', method: 'POST', data })
}

/** 删除评论 */
export function removeComment(id: number): Promise<void> {
  return request({ url: `/app/comment/${id}`, method: 'DELETE' })
}
