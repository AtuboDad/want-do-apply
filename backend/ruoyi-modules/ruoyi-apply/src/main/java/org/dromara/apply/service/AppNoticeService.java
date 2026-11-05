package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.AppNotice;
import org.dromara.apply.domain.vo.AppNoticeVo;
import org.dromara.apply.mapper.AppNoticeMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息通知服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class AppNoticeService {

    private final AppNoticeMapper noticeMapper;

    /**
     * 我的通知列表
     *
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<AppNoticeVo> list(PageQuery pageQuery) {
        Long userId = AppLoginHelper.requireUserId();
        IPage<AppNotice> page = noticeMapper.selectPage(pageQuery.build(),
            Wrappers.<AppNotice>lambdaQuery()
                .eq(AppNotice::getUserId, userId)
                .orderByDesc(AppNotice::getCreateTime));
        List<AppNoticeVo> vos = page.getRecords().stream()
            .map(n -> MapstructUtils.convert(n, AppNoticeVo.class))
            .toList();
        return PageResult.build(vos, page.getTotal());
    }

    /**
     * 未读数量
     *
     * @return 数量
     */
    public Long unreadCount() {
        Long userId = AppLoginHelper.getUserId();
        if (userId == null) {
            return 0L;
        }
        return noticeMapper.selectCount(Wrappers.<AppNotice>lambdaQuery()
            .eq(AppNotice::getUserId, userId)
            .eq(AppNotice::getIsRead, "0"));
    }

    /**
     * 标记已读
     *
     * @param ids 通知ID集合，为空则全部已读
     */
    public void markRead(List<Long> ids) {
        Long userId = AppLoginHelper.requireUserId();
        var wrapper = Wrappers.<AppNotice>lambdaUpdate()
            .set(AppNotice::getIsRead, "1")
            .eq(AppNotice::getUserId, userId)
            .eq(AppNotice::getIsRead, "0");
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(AppNotice::getId, ids);
        }
        noticeMapper.update(null, wrapper);
    }
}
