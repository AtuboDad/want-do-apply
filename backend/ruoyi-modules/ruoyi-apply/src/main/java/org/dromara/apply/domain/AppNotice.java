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
 * 消息通知对象 app_notice
 *
 * @author want-do-apply
 */
@Data
@TableName("app_notice")
public class AppNotice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    @TableId(value = "id")
    private Long id;

    /** 接收人ID */
    private Long userId;

    /** 类型（1收到批条 2审批结果 3评论回复 4群邀请 5系统消息） */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务ID */
    private Long bizId;

    /** 是否已读（0未读 1已读） */
    private String isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
