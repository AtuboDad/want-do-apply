package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 举报对象 apply_report
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_report")
public class ApplyReport extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 举报ID */
    @TableId(value = "id")
    private Long id;

    /** 对象类型（1审批单 2批条 3评论） */
    private String targetType;

    /** 对象ID */
    private Long targetId;

    /** 举报人ID */
    private Long reporterId;

    /** 举报类型 */
    private String reasonType;

    /** 举报说明 */
    private String content;

    /** 状态（0待处理 1已处理 2已驳回） */
    private String status;

    /** 处理人 */
    private Long handleBy;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理意见 */
    private String handleRemark;

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
