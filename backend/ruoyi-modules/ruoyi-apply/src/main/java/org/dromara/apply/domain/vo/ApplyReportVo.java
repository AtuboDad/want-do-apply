package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.ApplyReport;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyReport.class)
public class ApplyReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 举报ID */
    private Long id;

    /** 对象类型（1审批单 2批条 3评论） */
    private String targetType;

    /** 对象ID */
    private Long targetId;

    /** 举报人ID */
    private Long reporterId;

    /** 举报人昵称 */
    private String reporterNickName;

    /** 举报类型 */
    private String reasonType;

    /** 举报说明 */
    private String content;

    /** 状态 */
    private String status;

    /** 处理人 */
    private Long handleBy;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理意见 */
    private String handleRemark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 被举报内容摘要 */
    private String targetSummary;
}
