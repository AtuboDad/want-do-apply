package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.vo.AppNoticeVo;
import org.dromara.apply.service.AppNoticeService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端 - 消息通知
 *
 * @author want-do-apply
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/notice")
public class AppNoticeController {

    private final AppNoticeService noticeService;

    /**
     * 通知列表
     */
    @GetMapping("/list")
    public R<PageResult<AppNoticeVo>> list(PageQuery pageQuery) {
        return R.ok(noticeService.list(pageQuery));
    }

    /**
     * 未读数量
     */
    @GetMapping("/unread")
    public R<Long> unread() {
        return R.ok(noticeService.unreadCount());
    }

    /**
     * 标记已读（不传 ids 则全部已读）
     */
    @PostMapping("/read")
    public R<Void> read(@RequestBody(required = false) List<Long> ids) {
        noticeService.markRead(ids);
        return R.ok();
    }
}
