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
 * 小程序用户对象 app_user
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class AppUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(value = "id")
    private Long id;

    /** 微信 openid */
    private String openId;

    /** 微信 unionid */
    private String unionId;

    /** 昵称 */
    private String nickName;

    /** 头像地址 */
    private String avatar;

    /** 性别（0未知 1男 2女） */
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

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
