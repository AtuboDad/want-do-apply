package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批条业务对象
 *
 * @author want-do-apply
 */
@Data
public class ApplyVoteBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单ID */
    @NotNull(message = "审批单不能为空")
    private Long orderId;

    /** 结论（1通过 2驳回） */
    @NotBlank(message = "请选择审批结论")
    private String result;

    /** 批条理由 */
    @NotBlank(message = "写点理由吧，这是灵魂")
    @Size(min = 2, max = 200, message = "批条理由需要2-200个字符")
    private String reason;
}
