package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.ApplyReportBo;
import org.dromara.apply.service.ApplyReportService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序端 - 举报
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/report")
public class AppReportController {

    private final ApplyReportService reportService;

    /**
     * 提交举报
     */
    @PostMapping
    public R<Long> report(@Validated @RequestBody ApplyReportBo bo) {
        return R.ok(reportService.report(bo));
    }
}
