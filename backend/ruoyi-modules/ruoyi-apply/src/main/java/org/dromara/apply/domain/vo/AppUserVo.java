package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.AppUser;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序用户视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = AppUser.class)
public class AppUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickName;

    /** 头像 */
    private String avatar;

    /** 性别 */
    private String gender;

    /** 手机号 */
    private String phone;

    /** 信用分 */
    private Integer creditScore;

    /** 发起审批数 */
    private Integer applyCount;

    /** 批阅数 */
    private Integer voteCount;

    /** 批条被采纳数 */
    private Integer adoptedCount;

    /** 状态（0正常 1封禁） */
    private String status;

    /** 封禁原因 */
    private String banRemark;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
