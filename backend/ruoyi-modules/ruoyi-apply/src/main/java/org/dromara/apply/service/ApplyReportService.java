package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.ApplyComment;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.ApplyReport;
import org.dromara.apply.domain.ApplyVote;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.bo.ApplyReportBo;
import org.dromara.apply.domain.vo.ApplyReportVo;
import org.dromara.apply.mapper.ApplyCommentMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.mapper.ApplyReportMapper;
import org.dromara.apply.mapper.ApplyVoteMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class ApplyReportService {

    private final ApplyReportMapper reportMapper;
    private final ApplyOrderMapper orderMapper;
    private final ApplyVoteMapper voteMapper;
    private final ApplyCommentMapper commentMapper;

    /**
     * 提交举报
     *
     * @param bo 举报内容
     * @return 举报ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long report(ApplyReportBo bo) {
        Long userId = AppLoginHelper.requireUserId();
        // 同一用户对同一对象只允许举报一次
        Long exist = reportMapper.selectCount(Wrappers.<ApplyReport>lambdaQuery()
            .eq(ApplyReport::getTargetType, bo.getTargetType())
            .eq(ApplyReport::getTargetId, bo.getTargetId())
            .eq(ApplyReport::getReporterId, userId));
        if (exist != null && exist > 0) {
            throw new ServiceException("你已经举报过了，我们会尽快处理");
        }

        ApplyReport report = new ApplyReport();
        report.setTargetType(bo.getTargetType());
        report.setTargetId(bo.getTargetId());
        report.setReporterId(userId);
        report.setReasonType(bo.getReasonType() == null ? "other" : bo.getReasonType());
        report.setContent(bo.getContent() == null ? "" : bo.getContent());
        report.setStatus(ApplyConstants.REPORT_WAIT);
        report.setDelFlag("0");
        report.setCreateBy(userId);
        report.setUpdateBy(userId);
        reportMapper.insert(report);
        return report.getId();
    }

    // ==================== 管理端 ====================

    /**
     * 管理端分页
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyReportVo> adminPage(AdminQueryBo bo, PageQuery pageQuery) {
        IPage<ApplyReportVo> page = reportMapper.selectAdminPage(pageQuery.build(), bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 处理举报
     *
     * @param bo      处理参数（id + status + handleRemark）
     * @param takeDown 是否同步下架被举报内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(ApplyReportBo bo, boolean takeDown) {
        if (bo.getId() == null) {
            throw new ServiceException("举报ID不能为空");
        }
        ApplyReport report = reportMapper.selectById(bo.getId());
        if (report == null) {
            throw new ServiceException("举报不存在");
        }
        String status = ApplyConstants.REPORT_REJECT.equals(bo.getStatus())
            ? ApplyConstants.REPORT_REJECT : ApplyConstants.REPORT_DONE;

        reportMapper.update(null, Wrappers.<ApplyReport>lambdaUpdate()
            .set(ApplyReport::getStatus, status)
            .set(ApplyReport::getHandleBy, LoginHelper.getUserId())
            .set(ApplyReport::getHandleTime, LocalDateTime.now())
            .set(ApplyReport::getHandleRemark, bo.getHandleRemark() == null ? "" : bo.getHandleRemark())
            .eq(ApplyReport::getId, bo.getId()));

        if (takeDown && ApplyConstants.REPORT_DONE.equals(status)) {
            takeDown(report.getTargetType(), report.getTargetId());
        }
    }

    /**
     * 下架/删除被举报内容
     */
    private void takeDown(String targetType, Long targetId) {
        switch (targetType) {
            case "1" -> orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
                .set(ApplyOrder::getAuditStatus, ApplyConstants.AUDIT_OFF)
                .set(ApplyOrder::getAuditRemark, "被举报下架")
                .eq(ApplyOrder::getId, targetId));
            case "2" -> voteMapper.deleteById(targetId);
            case "3" -> {
                ApplyComment comment = commentMapper.selectById(targetId);
                if (comment != null) {
                    commentMapper.deleteById(targetId);
                    orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
                        .setSql("comment_count = greatest(comment_count - 1, 0)")
                        .eq(ApplyOrder::getId, comment.getOrderId()));
                }
            }
            default -> {
            }
        }
    }

    /**
     * 待处理举报数
     *
     * @return 数量
     */
    public Long pendingCount() {
        return reportMapper.selectCount(Wrappers.<ApplyReport>lambdaQuery()
            .eq(ApplyReport::getStatus, ApplyConstants.REPORT_WAIT));
    }

    /**
     * 批量删除举报记录
     *
     * @param ids 主键集合
     */
    public void adminRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        reportMapper.deleteByIds(ids);
    }
}
