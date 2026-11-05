package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批单对象 apply_order
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_order")
public class ApplyOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单ID */
    @TableId(value = "id")
    private Long id;

    /** 发起人ID */
    private Long userId;

    /** 审批群ID（为空表示公开广场） */
    private Long groupId;

    /** 商品名称 */
    private String title;

    /** 商品价格 */
    private BigDecimal price;

    /** 购买理由 */
    private String reason;

    /** 替代品 */
    private String substitute;

    /** 发起人补充说明（审批过程中可追加） */
    private String extraNote;

    /** 图片，多个用逗号分隔 */
    private String images;

    /** 分类 */
    private String category;

    /** 可见范围（0公开 1群内） */
    private String visibility;

    /** 审批时长（小时），到点后按少数服从多数结算 */
    private Integer durationHours;

    /** 状态（0待审批 1已通过 2已驳回 3已撤销 4已过期） */
    private String status;

    /** 通过票数 */
    private Integer passCount;

    /** 驳回票数 */
    private Integer rejectCount;

    /** 已批阅人数 */
    private Integer voteCount;

    /** 已废弃：早期按票数结案，现改为到点/提前结束时按少数服从多数结算 */
    private Integer targetVotes;

    /** 评论数 */
    private Integer commentCount;

    /** 热度分 */
    private Integer hotScore;

    /** 审批截止时间 */
    private LocalDateTime expireTime;

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 审核状态（0待审 1通过 2下架） */
    private String auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 是否置顶（0否 1是） */
    private String isTop;

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
