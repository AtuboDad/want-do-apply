package org.dromara.apply.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.bo.AdminOrderQueryBo;
import org.dromara.apply.domain.vo.ApplyOrderVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;
import java.util.Map;

/**
 * 审批单 Mapper
 *
 * @author want-do-apply
 */
public interface ApplyOrderMapper extends BaseMapperPlus<ApplyOrder, ApplyOrderVo> {

    /**
     * 广场 / 群内审批单分页（联结发起人信息）
     *
     * @param page    分页
     * @param groupId 群ID（null 表示只看公开广场）
     * @param userId  当前用户ID（用于标记是否已批阅）
     * @param sort    排序：new / hot / pending
     * @param status  状态筛选
     * @return 分页结果
     */
    IPage<ApplyOrderVo> selectSquarePage(IPage<ApplyOrderVo> page,
                                         @Param("groupId") Long groupId,
                                         @Param("userId") Long userId,
                                         @Param("sort") String sort,
                                         @Param("status") String status);

    /**
     * 管理端审批单分页
     *
     * @param page 分页
     * @param bo   查询条件
     * @return 分页结果
     */
    IPage<ApplyOrderVo> selectAdminPage(IPage<ApplyOrderVo> page, @Param("bo") AdminOrderQueryBo bo);

    /**
     * 我的审批单分页
     *
     * @param page   分页
     * @param userId 用户ID
     * @param type   apply-我发起的 vote-我批过的 favorite-我收藏的
     * @return 分页结果
     */
    IPage<ApplyOrderVo> selectMinePage(IPage<ApplyOrderVo> page,
                                       @Param("userId") Long userId,
                                       @Param("type") String type);

    /**
     * 按分类统计
     *
     * @return 分类 -> 数量
     */
    List<Map<String, Object>> countByCategory();

    /**
     * 近 N 天每日审批单数
     *
     * @param days 天数
     * @return date / cnt
     */
    List<Map<String, Object>> countDailyOrders(@Param("days") int days);

    /**
     * 结算汇总
     *
     * @return rejectAmount / rejectCount
     */
    Map<String, Object> sumRejected();
}
