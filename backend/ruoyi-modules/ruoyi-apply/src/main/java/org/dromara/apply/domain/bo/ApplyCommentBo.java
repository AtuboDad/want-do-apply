package org.dromara.apply.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.apply.domain.ApplyComment;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论业务对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyComment.class, reverseConvertGenerate = false)
public class ApplyCommentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单ID */
    @NotNull(message = "审批单不能为空")
    private Long orderId;

    /** 父评论ID */
    private Long parentId;

    /** 被回复人ID */
    private Long replyUserId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论不能超过500个字符")
    private String content;
}
