package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.ApplyReport;
import org.dromara.apply.domain.ApplyVote;
import org.dromara.apply.domain.vo.DashboardVo;
import org.dromara.apply.mapper.AppUserMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.mapper.ApplyReportMapper;
import org.dromara.apply.mapper.ApplyVoteMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppUserMapper appUserMapper;
    private final ApplyOrderMapper orderMapper;
    private final ApplyVoteMapper voteMapper;
    private final ApplyReportMapper reportMapper;

    /**
     * 看板总览
     *
     * @return 统计数据
     */
    public DashboardVo overview() {
        DashboardVo vo = new DashboardVo();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        vo.setUserTotal(appUserMapper.selectCount(null));
        vo.setUserToday(appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
            .ge(AppUser::getCreateTime, todayStart)));
        vo.setOrderTotal(orderMapper.selectCount(null));
        vo.setOrderToday(orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .ge(ApplyOrder::getCreateTime, todayStart)));
        vo.setPendingTotal(orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_PENDING)));
        vo.setVoteTotal(voteMapper.selectCount(null));
        vo.setVoteToday(voteMapper.selectCount(Wrappers.<ApplyVote>lambdaQuery()
            .ge(ApplyVote::getCreateTime, todayStart)));
        vo.setReportPending(reportMapper.selectCount(Wrappers.<ApplyReport>lambdaQuery()
            .eq(ApplyReport::getStatus, ApplyConstants.REPORT_WAIT)));

        // 驳回率与省钱总额
        Long settled = orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .in(ApplyOrder::getStatus, List.of(ApplyConstants.ORDER_PASSED, ApplyConstants.ORDER_REJECTED)));
        Long rejected = orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_REJECTED));
        if (settled != null && settled > 0) {
            vo.setRejectRate(BigDecimal.valueOf(rejected == null ? 0 : rejected)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(settled), 1, RoundingMode.HALF_UP));
        } else {
            vo.setRejectRate(BigDecimal.ZERO);
        }
        Map<String, Object> sum = orderMapper.sumRejected();
        vo.setSavedAmount(toDecimal(sum == null ? null : sum.get("amount")));

        vo.setTrend(buildTrend(7));
        vo.setCategories(buildCategories());
        return vo;
    }

    private List<DashboardVo.TrendItem> buildTrend(int days) {
        Map<String, Long> orderMap = toCountMap(orderMapper.countDailyOrders(days));
        Map<String, Long> voteMap = toCountMap(voteMapper.countDailyVotes(days));
        List<DashboardVo.TrendItem> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(fmt);
            DashboardVo.TrendItem item = new DashboardVo.TrendItem();
            item.setDate(date);
            item.setOrderCount(orderMap.getOrDefault(date, 0L));
            item.setVoteCount(voteMap.getOrDefault(date, 0L));
            list.add(item);
        }
        return list;
    }

    private List<DashboardVo.CategoryItem> buildCategories() {
        List<Map<String, Object>> rows = orderMapper.countByCategory();
        List<DashboardVo.CategoryItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            DashboardVo.CategoryItem item = new DashboardVo.CategoryItem();
            Object name = row.get("name");
            Object value = row.get("value");
            item.setName(name == null ? "other" : name.toString());
            item.setValue(value == null ? 0L : Long.parseLong(value.toString()));
            list.add(item);
        }
        return list;
    }

    /**
     * 把 [date, cnt] 列表转成 map（key 统一为字符串）
     */
    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            Object cnt = row.get("cnt");
            if (date != null) {
                map.put(date.toString(), cnt == null ? 0L : Long.parseLong(cnt.toString()));
            }
        }
        return map;
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
