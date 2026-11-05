package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.ApplyComment;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyComment.class)
public class ApplyCommentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long id;

    /** 审批单ID */
    private Long orderId;

    /** 评论人ID */
    private Long userId;

    /** 评论人昵称 */
    private String userNickName;

    /** 评论人头像 */
    private String userAvatar;

    /** 父评论ID */
    private Long parentId;

    /** 被回复人ID */
    private Long replyUserId;

    /** 被回复人昵称 */
    private String replyNickName;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子回复 */
    private List<ApplyCommentVo> children;
}
