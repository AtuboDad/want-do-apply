package org.dromara.apply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 评论对象 apply_comment
 *
 * @author want-do-apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_comment")
public class ApplyComment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    @TableId(value = "id")
    private Long id;

    /** 审批单ID */
    private Long orderId;

    /** 评论人ID */
    private Long userId;

    /** 父评论ID（0为顶层） */
    private Long parentId;

    /** 被回复人ID */
    private Long replyUserId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 删除标志（0存在 1删除） */
    @TableLogic
    private String delFlag;
}
