package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.ApplyGroup;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批群视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyGroup.class)
public class ApplyGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 群ID */
    private Long id;

    /** 群名称 */
    private String name;

    /** 群主用户ID */
    private Long ownerId;

    /** 群主昵称 */
    private String ownerNickName;

    /** 邀请码 */
    private String inviteCode;

    /** 群公告 */
    private String notice;

    /** 成员数 */
    private Integer memberCount;

    /** 状态 */
    private String status;

    /** 当前用户是否已加入 */
    private Boolean joined;

    /** 当前用户是否群主 */
    private Boolean isOwner;

    /** 创建时间 */
    private LocalDateTime createTime;
}
