package org.dromara.apply.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端通用查询条件（用户/批条/评论/群/举报共用）
 *
 * @author want-do-apply
 */
@Data
public class AdminQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 审批单ID */
    private Long orderId;

    /** 用户ID / 群主ID / 举报人ID */
    private Long userId;

    /** 审批群ID */
    private Long groupId;

    /** 昵称 / 群名 / 内容 关键字 */
    private String keyword;

    /** 状态 */
    private String status;

    /** 结论（批条） */
    private String result;

    /** 对象类型（举报） */
    private String targetType;

    /** 开始时间 */
    private LocalDateTime beginTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
