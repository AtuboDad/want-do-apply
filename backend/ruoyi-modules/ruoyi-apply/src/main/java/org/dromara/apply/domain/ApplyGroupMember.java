package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批群成员对象 apply_group_member
 *
 * @author want-do-apply
 */
@Data
@TableName("apply_group_member")
public class ApplyGroupMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 群ID */
    private Long groupId;

    /** 用户ID */
    private Long userId;

    /** 角色（0成员 1群主） */
    private String role;

    /** 加入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
