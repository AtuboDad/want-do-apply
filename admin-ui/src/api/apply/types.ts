/** 审批单 */
export interface ApplyOrderVO {
  id: number;
  userId: number;
  userNickName: string;
  userAvatar: string;
  groupId?: number;
  groupName?: string;
  title: string;
  price: number;
  reason: string;
  substitute?: string;
  images?: string;
  imageList?: string[];
  category?: string;
  visibility: string;
  status: string;
  passCount: number;
  rejectCount: number;
  voteCount: number;
  targetVotes: number;
  commentCount: number;
  hotScore: number;
  expireTime?: string;
  settleTime?: string;
  auditStatus: string;
  auditRemark?: string;
  isTop: string;
  createTime: string;
}

export interface ApplyOrderQuery {
  id?: number | string;
  userId?: number | string;
  nickName?: string;
  title?: string;
  status?: string;
  visibility?: string;
  auditStatus?: string;
  groupId?: number | string;
  isTop?: string;
  beginTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 批条 */
export interface ApplyVoteVO {
  id: number;
  orderId: number;
  userId: number;
  userNickName: string;
  userAvatar: string;
  result: string;
  reason: string;
  likeCount: number;
  createTime: string;
}

/** 评论 */
export interface ApplyCommentVO {
  id: number;
  orderId: number;
  userId: number;
  userNickName: string;
  userAvatar: string;
  parentId: number;
  replyUserId?: number;
  replyNickName?: string;
  content: string;
  likeCount: number;
  createTime: string;
}

/** 审批群 */
export interface ApplyGroupVO {
  id: number;
  name: string;
  ownerId: number;
  ownerNickName: string;
  inviteCode: string;
  notice: string;
  memberCount: number;
  status: string;
  createTime: string;
}

/** 小程序用户 */
export interface AppUserVO {
  id: number;
  nickName: string;
  avatar: string;
  gender: string;
  phone: string;
  creditScore: number;
  applyCount: number;
  voteCount: number;
  adoptedCount: number;
  status: string;
  banRemark?: string;
  lastLoginTime?: string;
  createTime: string;
}

/** 举报 */
export interface ApplyReportVO {
  id: number;
  targetType: string;
  targetId: number;
  reporterId: number;
  reporterNickName: string;
  reasonType: string;
  content: string;
  status: string;
  handleBy?: number;
  handleTime?: string;
  handleRemark?: string;
  createTime: string;
  targetSummary?: string;
}

/** 看板 */
export interface DashboardVO {
  userTotal: number;
  userToday: number;
  orderTotal: number;
  orderToday: number;
  pendingTotal: number;
  voteTotal: number;
  voteToday: number;
  rejectRate: number;
  savedAmount: number;
  reportPending: number;
  trend: { date: string; orderCount: number; voteCount: number }[];
  categories: { name: string; value: number }[];
}

/** 通用查询 */
export interface AdminQuery {
  id?: number | string;
  orderId?: number | string;
  userId?: number | string;
  groupId?: number | string;
  keyword?: string;
  status?: string;
  result?: string;
  targetType?: string;
  beginTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}
