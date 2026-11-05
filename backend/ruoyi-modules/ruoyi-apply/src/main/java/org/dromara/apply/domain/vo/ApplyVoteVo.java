package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.ApplyVote;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 批条视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyVote.class)
public class ApplyVoteVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 批条ID */
    private Long id;

    /** 审批单ID */
    private Long orderId;

    /** 批阅人ID */
    private Long userId;

    /** 批阅人昵称 */
    private String userNickName;

    /** 批阅人头像 */
    private String userAvatar;

    /** 结论（1通过 2驳回） */
    private String result;

    /** 批条理由 */
    private String reason;

    /** 点赞数 */
    private Integer likeCount;

    /** 当前用户是否点赞 */
    private Boolean liked;

    /** 创建时间 */
    private LocalDateTime createTime;
}
