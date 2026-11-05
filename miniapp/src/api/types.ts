export interface OrderItem {
  id: number
  userId: number
  userNickName: string
  userAvatar: string
  groupId?: number
  groupName?: string
  title: string
  price: number
  reason: string
  substitute?: string
  extraNote?: string
  images?: string
  imageList?: string[]
  category?: string
  visibility: string
  /** 审批方式：0 单人（一人表态即出结果） 1 多人（汇总意见） */
  approveMode?: string
  durationHours?: number
  status: string
  passCount: number
  rejectCount: number
  voteCount: number
  targetVotes: number
  commentCount: number
  hotScore: number
  expireTime?: string
  settleTime?: string
  auditStatus?: string
  isTop?: string
  createTime: string
  myVoted?: boolean
  myResult?: string
  favorited?: boolean
  mine?: boolean
  remainSeconds?: number
}

export interface VoteItem {
  id: number
  orderId: number
  userId: number
  userNickName: string
  userAvatar: string
  result: string
  reason: string
  likeCount: number
  liked?: boolean
  createTime: string
}

export interface CommentItem {
  id: number
  orderId: number
  userId: number
  userNickName: string
  userAvatar: string
  parentId: number
  replyUserId?: number
  replyNickName?: string
  content: string
  likeCount: number
  createTime: string
  children?: CommentItem[]
}

export interface OrderDetail {
  order: OrderItem
  votes: VoteItem[]
  comments: CommentItem[]
}

export interface GroupItem {
  id: number
  name: string
  ownerId: number
  ownerNickName: string
  inviteCode: string
  notice: string
  memberCount: number
  status: string
  joined?: boolean
  isOwner?: boolean
  createTime: string
}

export interface NoticeItem {
  id: number
  type: string
  title: string
  content: string
  bizId?: number
  isRead: string
  createTime: string
}

export interface PageData<T> {
  total: number
  rows: T[]
}
