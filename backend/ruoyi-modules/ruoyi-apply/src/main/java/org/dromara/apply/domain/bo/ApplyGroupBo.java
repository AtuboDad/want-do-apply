package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审批群业务对象
 *
 * @author want-do-apply
 */
@Data
public class ApplyGroupBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 群ID */
    private Long id;

    /** 群名称 */
    @NotBlank(message = "群名称不能为空")
    @Size(max = 50, message = "群名称不能超过50个字符")
    private String name;

    /** 群公告 */
    @Size(max = 200, message = "群公告不能超过200个字符")
    private String notice;

    /** 邀请码（加入时使用） */
    private String inviteCode;

    /** 状态 */
    private String status;
}
