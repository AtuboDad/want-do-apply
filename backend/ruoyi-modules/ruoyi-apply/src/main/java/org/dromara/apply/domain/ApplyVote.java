package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 批条对象 apply_vote
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_vote")
public class ApplyVote extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 批条ID */
    @TableId(value = "id")
    private Long id;

    /** 审批单ID */
    private Long orderId;

    /** 批阅人ID */
    private Long userId;

    /** 审批结论（1通过 2驳回） */
    private String result;

    /** 批条理由 */
    private String reason;

    /** 点赞数 */
    private Integer likeCount;

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
