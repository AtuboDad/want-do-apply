package org.dromara.apply.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.apply.service.ApplyVoteService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 审批单过期结算任务
 * <p>
 * 到期的审批单在用户提交批条时也会即时结算，这里兜底处理「到期后没人再批」的情况。
 *
 * @author want-do-apply
 */
@Slf4j
@Configuration
@EnableScheduling
public class ApplySettleJob {

    private final ApplyVoteService voteService;

    public ApplySettleJob(ApplyVoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * 每 5 分钟扫描一次过期审批单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void settleExpired() {
        try {
            int count = voteService.settleExpired();
            if (count > 0) {
                log.info("[apply] 过期审批单结算完成，共 {} 单", count);
            }
        } catch (Exception e) {
            log.error("[apply] 过期审批单结算失败", e);
        }
    }
}
