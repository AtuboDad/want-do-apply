package org.dromara.apply.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序登录返回
 *
 * @author want-do-apply
 */
@Data
public class AppLoginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String token;

    /** 过期时间戳(毫秒) */
    private Long expireAt;

    /** 用户信息 */
    private AppUserVo user;

    /** 是否新用户 */
    private Boolean newUser;
}
