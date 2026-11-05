package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端用户编辑对象
 *
 * @author want-do-apply
 */
@Data
public class AppUserBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 昵称 */
    @Size(max = 64, message = "昵称过长")
    private String nickName;

    /** 头像 */
    private String avatar;

    /** 状态（0正常 1封禁） */
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 封禁原因 */
    @Size(max = 200, message = "封禁原因过长")
    private String banRemark;

    /** 信用分 */
    private Integer creditScore;
}
