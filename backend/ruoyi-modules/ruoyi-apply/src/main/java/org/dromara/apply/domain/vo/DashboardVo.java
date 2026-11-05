package org.dromara.apply.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端数据看板
 *
 * @author want-do-apply
 */
@Data
public class DashboardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户总数 */
    private Long userTotal;

    /** 今日新增用户 */
    private Long userToday;

    /** 审批单总数 */
    private Long orderTotal;

    /** 今日新增审批单 */
    private Long orderToday;

    /** 待审批数 */
    private Long pendingTotal;

    /** 批条总数 */
    private Long voteTotal;

    /** 今日批条数 */
    private Long voteToday;

    /** 驳回率(%) */
    private BigDecimal rejectRate;

    /** 帮用户省下的总额（被驳回的金额合计） */
    private BigDecimal savedAmount;

    /** 待处理举报数 */
    private Long reportPending;

    /** 近 7 天趋势 */
    private List<TrendItem> trend;

    /** 热门分类占比 */
    private List<CategoryItem> categories;

    /**
     * 趋势项
     */
    @Data
    public static class TrendItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 日期 yyyy-MM-dd */
        private String date;
        /** 审批单数 */
        private Long orderCount;
        /** 批条数 */
        private Long voteCount;
    }

    /**
     * 分类占比项
     */
    @Data
    public static class CategoryItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 分类 */
        private String name;
        /** 数量 */
        private Long value;
    }
}
