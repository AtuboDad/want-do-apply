package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AdminOrderQueryBo;
import org.dromara.apply.domain.vo.ApplyOrderVo;
import org.dromara.apply.service.ApplyOrderService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 管理端 - 审批单
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/order")
public class ApplyOrderController {

    private final ApplyOrderService orderService;

    /**
     * 审批单列表
     */
    @SaCheckPermission("apply:order:list")
    @GetMapping("/list")
    public R<PageResult<ApplyOrderVo>> list(AdminOrderQueryBo bo, PageQuery pageQuery) {
        return R.ok(orderService.adminPage(bo, pageQuery));
    }

    /**
     * 审批单详情
     */
    @SaCheckPermission("apply:order:query")
    @GetMapping("/{id}")
    public R<ApplyOrderVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(orderService.adminDetail(id));
    }

    /**
     * 内容审核 / 下架
     */
    @SaCheckPermission("apply:order:audit")
    @Log(title = "审批单审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam String auditStatus,
                         @RequestParam(required = false) String remark) {
        orderService.audit(id, auditStatus, remark);
        return R.ok();
    }

    /**
     * 置顶
     */
    @SaCheckPermission("apply:order:edit")
    @Log(title = "审批单置顶", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/top")
    public R<Void> top(@PathVariable Long id, @RequestParam String isTop) {
        orderService.top(id, isTop);
        return R.ok();
    }

    /**
     * 删除
     */
    @SaCheckPermission("apply:order:remove")
    @Log(title = "审批单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        orderService.adminRemove(Arrays.asList(ids));
        return R.ok();
    }
}
