package org.dromara.apply.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端审批单查询条件
 *
 * @author want-do-apply
 */
@Data
public class AdminOrderQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单ID */
    private Long id;

    /** 发起人ID */
    private Long userId;

    /** 发起人昵称 */
    private String nickName;

    /** 商品名称 */
    private String title;

    /** 状态 */
    private String status;

    /** 可见范围 */
    private String visibility;

    /** 审核状态 */
    private String auditStatus;

    /** 审批群ID */
    private Long groupId;

    /** 是否置顶 */
    private String isTop;

    /** 开始时间 */
    private LocalDateTime beginTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
