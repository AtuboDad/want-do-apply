package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.AppNotice;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.ApplyVote;
import org.dromara.apply.domain.ApplyVoteLike;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.bo.ApplyVoteBo;
import org.dromara.apply.domain.vo.ApplyVoteVo;
import org.dromara.apply.mapper.AppNoticeMapper;
import org.dromara.apply.mapper.AppUserMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.mapper.ApplyVoteLikeMapper;
import org.dromara.apply.mapper.ApplyVoteMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批条服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class ApplyVoteService {

    private final ApplyVoteMapper voteMapper;
    private final ApplyOrderMapper orderMapper;
    private final ApplyVoteLikeMapper voteLikeMapper;
    private final AppUserMapper appUserMapper;
    private final AppNoticeMapper noticeMapper;

    /**
     * 批阅（写批条）
     *
     * @param bo 批条内容
     * @return 本次批阅是否触发了结算
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean vote(ApplyVoteBo bo) {
        Long userId = AppLoginHelper.requireUserId();

        ApplyOrder order = orderMapper.selectById(bo.getOrderId());
        if (order == null) {
            throw new ServiceException("审批单不存在");
        }
        if (!ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            throw new ServiceException("审批已经结束啦，下次早点来");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException("这单已经过期啦，下次早点来");
        }
        if (userId.equals(order.getUserId())) {
            throw new ServiceException("不能审批自己发的单哦");
        }
        if (!ApplyConstants.VOTE_PASS.equals(bo.getResult()) && !ApplyConstants.VOTE_REJECT.equals(bo.getResult())) {
            throw new ServiceException("审批结论不合法");
        }

        // 每天批阅额度
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long todayCount = voteMapper.selectCount(Wrappers.<ApplyVote>lambdaQuery()
            .eq(ApplyVote::getUserId, userId)
            .ge(ApplyVote::getCreateTime, dayStart));
        if (todayCount >= ApplyConstants.DAILY_VOTE_LIMIT) {
            throw new ServiceException("今天批了 " + ApplyConstants.DAILY_VOTE_LIMIT + " 单，休息一下吧");
        }

        Long exist = voteMapper.selectCount(Wrappers.<ApplyVote>lambdaQuery()
            .eq(ApplyVote::getOrderId, bo.getOrderId())
            .eq(ApplyVote::getUserId, userId));
        if (exist != null && exist > 0) {
            throw new ServiceException("你已经批过这单了");
        }

        ApplyVote vote = new ApplyVote();
        vote.setOrderId(bo.getOrderId());
        vote.setUserId(userId);
        vote.setResult(bo.getResult());
        vote.setReason(bo.getReason() == null ? "" : bo.getReason().trim());
        vote.setLikeCount(0);
        vote.setDelFlag("0");
        vote.setCreateBy(userId);
        vote.setUpdateBy(userId);
        voteMapper.insert(vote);

        boolean pass = ApplyConstants.VOTE_PASS.equals(bo.getResult());
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .setSql(pass ? "pass_count = pass_count + 1" : "reject_count = reject_count + 1")
            .setSql("vote_count = vote_count + 1")
            .setSql("hot_score = hot_score + 1")
            .eq(ApplyOrder::getId, bo.getOrderId()));

        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
            .setSql("vote_count = vote_count + 1")
            .eq(AppUser::getId, userId));

        // 给发起人发通知
        sendNotice(order.getUserId(), ApplyConstants.NOTICE_VOTE,
            "有人批了你的单",
            (pass ? "通过了 ✓ " : "驳回了 ✗ ") + truncate(vote.getReason(), 60),
            order.getId());

        // 单人审批：一个人表态就出结果，不用等到审批时间结束
        if (ApplyConstants.MODE_SINGLE.equals(order.getApproveMode())) {
            return settle(order.getId(), false);
        }
        return settleIfNeeded(order.getId());
    }

    /**
     * 点赞 / 取消点赞
     *
     * @param voteId 批条ID
     * @return true 已赞
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long voteId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyVote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw new ServiceException("批条不存在");
        }
        ApplyVoteLike exist = voteLikeMapper.selectOne(Wrappers.<ApplyVoteLike>lambdaQuery()
            .eq(ApplyVoteLike::getVoteId, voteId)
            .eq(ApplyVoteLike::getUserId, userId));
        if (exist != null) {
            voteLikeMapper.deleteById(exist.getId());
            voteMapper.update(null, Wrappers.<ApplyVote>lambdaUpdate()
                .setSql("like_count = greatest(like_count - 1, 0)")
                .eq(ApplyVote::getId, voteId));
            return false;
        }
        ApplyVoteLike like = new ApplyVoteLike();
        like.setVoteId(voteId);
        like.setUserId(userId);
        voteLikeMapper.insert(like);
        voteMapper.update(null, Wrappers.<ApplyVote>lambdaUpdate()
            .setSql("like_count = like_count + 1")
            .eq(ApplyVote::getId, voteId));
        return true;
    }

    /**
     * 某个审批单下的批条
     *
     * @param orderId 审批单ID
     * @param sort    排序
     * @return 批条列表
     */
    public List<ApplyVoteVo> listByOrder(Long orderId, String sort) {
        Long userId = AppLoginHelper.getUserId();
        return voteMapper.selectByOrderId(orderId, userId == null ? -1L : userId,
            "hot".equals(sort) ? "hot" : "new");
    }

    // ==================== 管理端 ====================

    /**
     * 管理端分页
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyVoteVo> adminPage(AdminQueryBo bo, PageQuery pageQuery) {
        IPage<ApplyVoteVo> page = voteMapper.selectAdminPage(pageQuery.build(), bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 管理端删除
     *
     * @param ids 主键集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            ApplyVote vote = voteMapper.selectById(id);
            if (vote == null) {
                continue;
            }
            voteMapper.deleteById(id);
            orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
                .setSql("vote_count = greatest(vote_count - 1, 0)")
                .setSql(ApplyConstants.VOTE_PASS.equals(vote.getResult())
                    ? "pass_count = greatest(pass_count - 1, 0)"
                    : "reject_count = greatest(reject_count - 1, 0)")
                .eq(ApplyOrder::getId, vote.getOrderId()));
        }
    }

    // ==================== 内部 ====================

    /**
     * 到点结算（投票时顺带检查一次）
     *
     * @param orderId 审批单ID
     * @return 是否完成结算
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean settleIfNeeded(Long orderId) {
        ApplyOrder order = orderMapper.selectById(orderId);
        if (order == null || !ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            return false;
        }
        // 只在「到点」时结算：审批时长内由大家自由表态，票数多少都不提前结案
        boolean expired = order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now());
        if (!expired) {
            return false;
        }
        return settle(orderId, false);
    }

    /**
     * 发起人提前结束：按当前票数立即走一遍「少数服从多数」
     *
     * @param orderId 审批单ID
     * @return 结算后的状态
     */
    @Transactional(rollbackFor = Exception.class)
    public String finishEarly(Long orderId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException("审批单不存在");
        }
        if (!ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            throw new ServiceException("审批已经结束了");
        }
        if (order.getVoteCount() == null || order.getVoteCount() == 0) {
            throw new ServiceException("还没有人表态，结束就算流拍了");
        }
        settle(orderId, true);
        ApplyOrder after = orderMapper.selectById(orderId);
        return after == null ? null : after.getStatus();
    }

    /**
     * 结算：少数服从多数，平票按驳回处理（保守，向"省钱"一侧倾斜）
     *
     * @param orderId 审批单ID
     * @param early   是否为发起人提前结束
     * @return 是否完成结算
     */
    private boolean settle(Long orderId, boolean early) {
        ApplyOrder order = orderMapper.selectById(orderId);
        if (order == null || !ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            return false;
        }

        String when = early ? "（发起人提前结束）" : "";
        String status;
        String title;
        String content;
        if (order.getVoteCount() == null || order.getVoteCount() == 0) {
            // 无人表态：不算驳回，直接算流拍
            status = ApplyConstants.ORDER_EXPIRED;
            title = "审批流拍了";
            content = "「" + truncate(order.getTitle(), 30) + "」" + when + "没人来得及表态，已自动关闭";
        } else {
            boolean passed = order.getPassCount() != null && order.getRejectCount() != null
                && order.getPassCount() > order.getRejectCount();
            status = passed ? ApplyConstants.ORDER_PASSED : ApplyConstants.ORDER_REJECTED;
            title = passed ? "审批通过：买吧" : "审批驳回：省下钱了";
            content = "「" + truncate(order.getTitle(), 30) + "」" + when
                + (passed ? " 通过了，" : " 被驳回了，")
                + " 通过 " + order.getPassCount() + " / 驳回 " + order.getRejectCount();
        }

        // 条件更新：只有仍处于待审批时才写入，避免并发重复结算
        int affected = orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .set(ApplyOrder::getStatus, status)
            .set(ApplyOrder::getSettleTime, LocalDateTime.now())
            .eq(ApplyOrder::getId, orderId)
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_PENDING));
        if (affected == 0) {
            return false;
        }

        // 投票给多数方的用户，批条被采纳
        if (ApplyConstants.ORDER_PASSED.equals(status) || ApplyConstants.ORDER_REJECTED.equals(status)) {
            String winner = ApplyConstants.ORDER_PASSED.equals(status)
                ? ApplyConstants.VOTE_PASS : ApplyConstants.VOTE_REJECT;
            List<ApplyVote> winners = voteMapper.selectList(Wrappers.<ApplyVote>lambdaQuery()
                .eq(ApplyVote::getOrderId, orderId)
                .eq(ApplyVote::getResult, winner));
            for (ApplyVote v : winners) {
                appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                    .setSql("adopted_count = adopted_count + 1")
                    .eq(AppUser::getId, v.getUserId()));
            }
        }

        sendNotice(order.getUserId(), ApplyConstants.NOTICE_RESULT, title, content, orderId);
        return true;
    }

    /**
     * 批量结算过期单（定时任务调用）
     *
     * @return 处理数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int settleExpired() {
        List<ApplyOrder> expired = orderMapper.selectList(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_PENDING)
            .lt(ApplyOrder::getExpireTime, LocalDateTime.now())
            .last("limit 200"));
        int count = 0;
        for (ApplyOrder order : expired) {
            if (settleIfNeeded(order.getId())) {
                count++;
            }
        }
        return count;
    }

    private void sendNotice(Long userId, String type, String title, String content, Long bizId) {
        AppNotice notice = new AppNotice();
        notice.setUserId(userId);
        notice.setType(type);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setBizId(bizId);
        notice.setIsRead("0");
        noticeMapper.insert(notice);
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
