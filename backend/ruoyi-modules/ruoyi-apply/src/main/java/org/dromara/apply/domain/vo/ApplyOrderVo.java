package org.dromara.apply.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.ApplyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批单视图对象
 *
 * @author want-do-apply
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoMapper(target = ApplyOrder.class)
public class ApplyOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批单ID */
    private Long id;

    /** 发起人ID */
    private Long userId;

    /** 发起人昵称 */
    private String userNickName;

    /** 发起人头像 */
    private String userAvatar;

    /** 审批群ID */
    private Long groupId;

    /** 审批群名称 */
    private String groupName;

    /** 商品名称 */
    private String title;

    /** 商品价格 */
    private BigDecimal price;

    /** 购买理由 */
    private String reason;

    /** 替代品 */
    private String substitute;

    /** 发起人补充说明 */
    private String extraNote;

    /** 图片（逗号分隔原值） */
    private String images;

    /** 图片列表 */
    private List<String> imageList;

    /** 分类 */
    private String category;

    /** 可见范围 */
    private String visibility;

    /** 审批时长（小时） */
    private Integer durationHours;

    /** 状态 */
    private String status;

    /** 通过票数 */
    private Integer passCount;

    /** 驳回票数 */
    private Integer rejectCount;

    /** 已批阅人数 */
    private Integer voteCount;

    /** 结算所需票数 */
    private Integer targetVotes;

    /** 评论数 */
    private Integer commentCount;

    /** 热度分 */
    private Integer hotScore;

    /** 审批截止时间 */
    private LocalDateTime expireTime;

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 审核状态 */
    private String auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 是否置顶 */
    private String isTop;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 当前用户是否已批阅 */
    private Boolean myVoted;

    /** 当前用户的批阅结论 */
    private String myResult;

    /** 当前用户是否收藏 */
    private Boolean favorited;

    /** 是否为本人发起 */
    private Boolean mine;

    /** 批条列表（详情返回） */
    private List<ApplyVoteVo> votes;

    /** 评论列表（详情返回） */
    private List<ApplyCommentVo> comments;

    /** 剩余秒数（前端倒计时） */
    private Long remainSeconds;
}
