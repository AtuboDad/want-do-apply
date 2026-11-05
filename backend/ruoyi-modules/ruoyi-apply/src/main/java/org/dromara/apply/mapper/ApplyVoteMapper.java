package org.dromara.apply.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.dromara.apply.domain.ApplyVote;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyVoteVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;
import java.util.Map;

/**
 * 批条 Mapper
 *
 * @author want-do-apply
 */
public interface ApplyVoteMapper extends BaseMapperPlus<ApplyVote, ApplyVoteVo> {

    /**
     * 查询某审批单下的批条（带批阅人信息）
     *
     * @param orderId 审批单ID
     * @param userId  当前用户ID（用于标记点赞）
     * @param sort    排序：hot / new
     * @return 批条列表
     */
    List<ApplyVoteVo> selectByOrderId(@Param("orderId") Long orderId,
                                      @Param("userId") Long userId,
                                      @Param("sort") String sort);

    /**
     * 管理端批条分页
     *
     * @param page 分页
     * @param bo   查询条件
     * @return 分页结果
     */
    IPage<ApplyVoteVo> selectAdminPage(IPage<ApplyVoteVo> page, @Param("bo") AdminQueryBo bo);

    /**
     * 近 N 天每日批条数
     *
     * @param days 天数
     * @return date / cnt
     */
    List<Map<String, Object>> countDailyVotes(@Param("days") int days);
}
