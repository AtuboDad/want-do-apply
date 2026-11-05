package org.dromara.apply.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.apply.domain.AppNotice;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知视图对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = AppNotice.class)
public class AppNoticeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    private Long id;

    /** 类型 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务ID */
    private Long bizId;

    /** 是否已读 */
    private String isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
