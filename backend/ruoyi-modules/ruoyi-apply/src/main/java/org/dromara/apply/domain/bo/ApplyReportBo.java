package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 举报业务对象
 *
 * @author want-do-apply
 */
@Data
public class ApplyReportBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 举报ID（管理端处理使用） */
    private Long id;

    /** 对象类型（1审批单 2批条 3评论） */
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    /** 对象ID */
    @NotNull(message = "举报对象不能为空")
    private Long targetId;

    /** 举报类型 */
    private String reasonType;

    /** 举报说明 */
    @Size(max = 500, message = "举报说明不能超过500个字符")
    private String content;

    /** 状态 */
    private String status;

    /** 处理意见 */
    @Size(max = 200, message = "处理意见不能超过200个字符")
    private String handleRemark;
}
