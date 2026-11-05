package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序登录业务对象
 *
 * @author want-do-apply
 */
@Data
public class AppLoginBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 微信登录 code */
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 昵称（wx.getUserProfile 已废弃，由前端选择头像昵称后回传） */
    private String nickName;

    /** 头像 */
    private String avatar;

    /** 性别 */
    private String gender;
}
