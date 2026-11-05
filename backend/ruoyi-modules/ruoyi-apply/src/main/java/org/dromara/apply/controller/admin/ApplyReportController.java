package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.bo.ApplyReportBo;
import org.dromara.apply.domain.vo.ApplyReportVo;
import org.dromara.apply.service.ApplyReportService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 管理端 - 举报
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/report")
public class ApplyReportController {

    private final ApplyReportService reportService;

    /**
     * 举报列表
     */
    @SaCheckPermission("apply:report:list")
    @GetMapping("/list")
    public R<PageResult<ApplyReportVo>> list(AdminQueryBo bo, PageQuery pageQuery) {
        return R.ok(reportService.adminPage(bo, pageQuery));
    }

    /**
     * 待处理数量
     */
    @SaCheckPermission("apply:report:list")
    @GetMapping("/pending")
    public R<Long> pending() {
        return R.ok(reportService.pendingCount());
    }

    /**
     * 处理举报
     *
     * @param takeDown 是否同时下架被举报内容
     */
    @SaCheckPermission("apply:report:handle")
    @Log(title = "举报处理", businessType = BusinessType.UPDATE)
    @PutMapping("/handle")
    public R<Void> handle(@Validated @RequestBody ApplyReportBo bo,
                          @RequestParam(defaultValue = "true") boolean takeDown) {
        reportService.handle(bo, takeDown);
        return R.ok();
    }

    /**
     * 删除举报记录
     */
    @SaCheckPermission("apply:report:remove")
    @Log(title = "举报记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        reportService.adminRemove(Arrays.asList(ids));
        return R.ok();
    }
}
