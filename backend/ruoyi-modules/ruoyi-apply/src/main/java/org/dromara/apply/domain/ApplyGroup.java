package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 审批群对象 apply_group
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_group")
public class ApplyGroup extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 群ID */
    @TableId(value = "id")
    private Long id;

    /** 群名称 */
    private String name;

    /** 群主用户ID */
    private Long ownerId;

    /** 邀请码 */
    private String inviteCode;

    /** 群公告 */
    private String notice;

    /** 成员数 */
    private Integer memberCount;

    /** 状态（0正常 1解散） */
    private String status;

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
