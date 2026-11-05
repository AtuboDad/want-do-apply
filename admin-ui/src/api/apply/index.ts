import type { ApplyOrderQuery, ApplyOrderVO, AdminQuery, ApplyVoteVO, ApplyCommentVO, ApplyGroupVO, AppUserVO, ApplyReportVO, DashboardVO } from './types';
import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

// ---------------- 审批单 ----------------
export const listOrder = (query?: ApplyOrderQuery): AxiosPromise<PageResult<ApplyOrderVO>> => {
  return request({ url: '/apply/order/list', method: 'get', params: query });
};

export const getOrder = (id: number | string): AxiosPromise<ApplyOrderVO> => {
  return request({ url: '/apply/order/' + id, method: 'get' });
};

export const auditOrder = (id: number | string, auditStatus: string, remark?: string) => {
  return request({
    url: `/apply/order/${id}/audit`,
    method: 'put',
    params: { auditStatus, remark }
  });
};

export const topOrder = (id: number | string, isTop: string) => {
  return request({ url: `/apply/order/${id}/top`, method: 'put', params: { isTop } });
};

export const delOrder = (ids: number | string | (number | string)[]) => {
  return request({ url: '/apply/order/' + ids, method: 'delete' });
};

// ---------------- 批条 ----------------
export const listVote = (query?: AdminQuery): AxiosPromise<PageResult<ApplyVoteVO>> => {
  return request({ url: '/apply/vote/list', method: 'get', params: query });
};

export const delVote = (ids: number | string | (number | string)[]) => {
  return request({ url: '/apply/vote/' + ids, method: 'delete' });
};

// ---------------- 评论 ----------------
export const listComment = (query?: AdminQuery): AxiosPromise<PageResult<ApplyCommentVO>> => {
  return request({ url: '/apply/comment/list', method: 'get', params: query });
};

export const delComment = (ids: number | string | (number | string)[]) => {
  return request({ url: '/apply/comment/' + ids, method: 'delete' });
};

// ---------------- 审批群 ----------------
export const listGroup = (query?: AdminQuery): AxiosPromise<PageResult<ApplyGroupVO>> => {
  return request({ url: '/apply/group/list', method: 'get', params: query });
};

export const delGroup = (ids: number | string | (number | string)[]) => {
  return request({ url: '/apply/group/' + ids, method: 'delete' });
};

// ---------------- 小程序用户 ----------------
export const listAppUser = (query?: AdminQuery): AxiosPromise<PageResult<AppUserVO>> => {
  return request({ url: '/apply/user/list', method: 'get', params: query });
};

export const updateAppUser = (data: {
  id: number;
  status: string;
  banRemark?: string;
  creditScore?: number;
  nickName?: string;
}) => {
  return request({ url: '/apply/user', method: 'put', data });
};

// ---------------- 举报 ----------------
export const listReport = (query?: AdminQuery): AxiosPromise<PageResult<ApplyReportVO>> => {
  return request({ url: '/apply/report/list', method: 'get', params: query });
};

export const pendingReport = (): AxiosPromise<number> => {
  return request({ url: '/apply/report/pending', method: 'get' });
};

export interface HandleReportPayload {
  id: number;
  targetType: string;
  targetId: number;
  status: string;
  handleRemark?: string;
}

export const handleReport = (data: HandleReportPayload, takeDown: boolean) => {
  return request({ url: '/apply/report/handle', method: 'put', params: { takeDown }, data });
};

export const delReport = (ids: number | string | (number | string)[]) => {
  return request({ url: '/apply/report/' + ids, method: 'delete' });
};

// ---------------- 看板 ----------------
export const getDashboard = (): AxiosPromise<DashboardVO> => {
  return request({ url: '/apply/dashboard/overview', method: 'get' });
};
