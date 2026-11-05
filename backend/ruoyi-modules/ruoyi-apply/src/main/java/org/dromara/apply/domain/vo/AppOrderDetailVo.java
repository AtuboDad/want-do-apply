package org.dromara.apply.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 审批单详情视图
 *
 * @author want-do-apply
 */
@Data
public class AppOrderDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单主体 */
    private ApplyOrderVo order;

    /** 批条列表 */
    private List<ApplyVoteVo> votes;

    /** 评论树 */
    private List<ApplyCommentVo> comments;
}
