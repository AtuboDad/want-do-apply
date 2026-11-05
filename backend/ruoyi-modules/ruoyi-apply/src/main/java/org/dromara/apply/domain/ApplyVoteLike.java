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
 * 批条点赞对象 apply_vote_like
 *
 * @author want-do-apply
 */
@Data
@TableName("apply_vote_like")
public class ApplyVoteLike implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 批条ID */
    private Long voteId;

    /** 用户ID */
    private Long userId;

    /** 点赞时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
