package org.dromara.apply.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 个人中心视图对象
 *
 * @author want-do-apply
 */
@Data
public class AppProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户信息 */
    private AppUserVo user;

    /** 发起的审批数 */
    private Long applyTotal;

    /** 通过的审批数 */
    private Long passedTotal;

    /** 批阅总数 */
    private Long voteTotal;

    /** 批条被采纳数 */
    private Long adoptedTotal;

    /** 收藏数 */
    private Long favoriteTotal;

    /** 未读通知数 */
    private Long unreadNotice;

    /** 帮别人省下的钱（被驳回的审批单金额合计） */
    private java.math.BigDecimal savedAmount;
}
