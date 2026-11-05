package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.AppNotice;
import org.dromara.apply.domain.ApplyComment;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.bo.ApplyCommentBo;
import org.dromara.apply.domain.vo.ApplyCommentVo;
import org.dromara.apply.mapper.AppNoticeMapper;
import org.dromara.apply.mapper.ApplyCommentMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class ApplyCommentService {

    private final ApplyCommentMapper commentMapper;
    private final ApplyOrderMapper orderMapper;
    private final AppNoticeMapper noticeMapper;

    /**
     * 发表评论
     *
     * @param bo 评论内容
     * @return 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long add(ApplyCommentBo bo) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(bo.getOrderId());
        if (order == null) {
            throw new ServiceException("审批单不存在");
        }
        if (!ApplyConstants.VISIBILITY_PUBLIC.equals(order.getVisibility())) {
            throw new ServiceException("群内审批暂不支持公开评论");
        }

        ApplyComment comment = MapstructUtils.convert(bo, ApplyComment.class);
        comment.setUserId(userId);
        comment.setParentId(bo.getParentId() == null ? 0L : bo.getParentId());
        comment.setLikeCount(0);
        comment.setDelFlag("0");
        comment.setCreateBy(userId);
        comment.setUpdateBy(userId);
        commentMapper.insert(comment);

        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .setSql("comment_count = comment_count + 1")
            .setSql("hot_score = hot_score + 1")
            .eq(ApplyOrder::getId, bo.getOrderId()));

        // 回复他人时通知被回复人，否则通知发起人
        Long target = bo.getReplyUserId() != null ? bo.getReplyUserId() : order.getUserId();
        if (target != null && !target.equals(userId)) {
            String who = bo.getReplyUserId() != null ? "回复了你的评论" : "评论了你的审批单";
            AppNotice notice = new AppNotice();
            notice.setUserId(target);
            notice.setType(ApplyConstants.NOTICE_REPLY);
            notice.setTitle("有人" + who);
            notice.setContent(truncate(bo.getContent(), 60));
            notice.setBizId(order.getId());
            notice.setIsRead("0");
            noticeMapper.insert(notice);
        }
        return comment.getId();
    }

    /**
     * 审批单评论树
     *
     * @param orderId 审批单ID
     * @return 评论树
     */
    public List<ApplyCommentVo> list(Long orderId) {
        List<ApplyCommentVo> flat = commentMapper.selectByOrderId(orderId);
        if (flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<ApplyCommentVo>> children = flat.stream()
            .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
            .collect(Collectors.groupingBy(ApplyCommentVo::getParentId));
        List<ApplyCommentVo> roots = flat.stream()
            .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
            .collect(Collectors.toList());
        for (ApplyCommentVo root : roots) {
            root.setChildren(children.getOrDefault(root.getId(), new ArrayList<>()));
        }
        return roots;
    }

    /**
     * 删除自己的评论
     *
     * @param id 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeMine(Long id) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyComment comment = commentMapper.selectById(id);
        if (comment == null || !userId.equals(comment.getUserId())) {
            throw new ServiceException("评论不存在");
        }
        commentMapper.deleteById(id);
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .setSql("comment_count = greatest(comment_count - 1, 0)")
            .eq(ApplyOrder::getId, comment.getOrderId()));
    }

    // ==================== 管理端 ====================

    /**
     * 管理端分页
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyCommentVo> adminPage(AdminQueryBo bo, PageQuery pageQuery) {
        IPage<ApplyCommentVo> page = commentMapper.selectAdminPage(pageQuery.build(), bo);
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
            ApplyComment comment = commentMapper.selectById(id);
            if (comment == null) {
                continue;
            }
            commentMapper.deleteById(id);
            orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
                .setSql("comment_count = greatest(comment_count - 1, 0)")
                .eq(ApplyOrder::getId, comment.getOrderId()));
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
