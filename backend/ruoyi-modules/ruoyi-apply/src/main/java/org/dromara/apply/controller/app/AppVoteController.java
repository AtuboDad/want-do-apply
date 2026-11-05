package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.ApplyVoteBo;
import org.dromara.apply.domain.vo.ApplyVoteVo;
import org.dromara.apply.service.ApplyVoteService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端 - 批条
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/vote")
public class AppVoteController {

    private final ApplyVoteService voteService;

    /**
     * 批阅
     *
     * @return true 表示本次批阅触发了结算
     */
    @PostMapping
    public R<Boolean> vote(@Validated @RequestBody ApplyVoteBo bo) {
        return R.ok(voteService.vote(bo));
    }

    /**
     * 某单的批条列表
     */
    @GetMapping("/list")
    public R<List<ApplyVoteVo>> list(@RequestParam Long orderId,
                                     @RequestParam(required = false) String sort) {
        return R.ok(voteService.listByOrder(orderId, sort));
    }

    /**
     * 点赞 / 取消点赞
     */
    @PostMapping("/{id}/like")
    public R<Boolean> like(@PathVariable Long id) {
        return R.ok(voteService.toggleLike(id));
    }
}
