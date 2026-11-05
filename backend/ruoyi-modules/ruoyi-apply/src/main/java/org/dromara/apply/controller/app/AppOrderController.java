package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.ApplyNoteBo;
import org.dromara.apply.domain.bo.ApplyOrderBo;
import org.dromara.apply.domain.vo.AppOrderDetailVo;
import org.dromara.apply.domain.vo.ApplyOrderVo;
import org.dromara.apply.service.ApplyOrderService;
import org.dromara.apply.service.ApplyVoteService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序端 - 审批单
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/order")
public class AppOrderController {

    private final ApplyOrderService orderService;
    private final ApplyVoteService voteService;

    /**
     * 广场列表
     *
     * @param sort   new / hot / pending
     * @param status 状态筛选
     */
    @GetMapping("/square")
    public R<PageResult<ApplyOrderVo>> square(@RequestParam(required = false) String sort,
                                              @RequestParam(required = false) String status,
                                              PageQuery pageQuery) {
        return R.ok(orderService.square(null, sort, status, pageQuery));
    }

    /**
     * 审批单详情
     */
    @GetMapping("/{id}")
    public R<AppOrderDetailVo> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    /**
     * 发起审批
     */
    @PostMapping
    public R<Long> create(@Validated @RequestBody ApplyOrderBo bo) {
        return R.ok(orderService.create(bo));
    }

    /**
     * 撤销
     */
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    /**
     * 补充说明（发起人）
     */
    @PostMapping("/{id}/note")
    public R<Void> note(@PathVariable Long id, @Validated @RequestBody ApplyNoteBo bo) {
        orderService.updateNote(id, bo.getNote());
        return R.ok();
    }

    /**
     * 提前结束（发起人），按当前票数立即结算
     * <p>
     * 注意：这里必须用 ok(msg, data) 重载，直接用 ok(status) 会命中 ok(String msg)，
     * 结果状态会被当成提示语，data 变成 null。
     */
    @PostMapping("/{id}/finish")
    public R<String> finish(@PathVariable Long id) {
        return R.ok("操作成功", voteService.finishEarly(id));
    }

    /**
     * 删除自己的审批单
     */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        orderService.removeMine(id);
        return R.ok();
    }

    /**
     * 收藏 / 取消收藏
     */
    @PostMapping("/{id}/favorite")
    public R<Boolean> favorite(@PathVariable Long id) {
        return R.ok(orderService.toggleFavorite(id));
    }

    /**
     * 我的：apply-发起 vote-批过 favorite-收藏
     */
    @GetMapping("/mine")
    public R<PageResult<ApplyOrderVo>> mine(@RequestParam(defaultValue = "apply") String type,
                                            PageQuery pageQuery) {
        return R.ok(orderService.mine(type, pageQuery));
    }
}
