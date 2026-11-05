package org.dromara.apply.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.dromara.apply.domain.ApplyComment;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyCommentVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 评论 Mapper
 *
 * @author want-do-apply
 */
public interface ApplyCommentMapper extends BaseMapperPlus<ApplyComment, ApplyCommentVo> {

    /**
     * 查询审批单下的评论（带评论人信息）
     *
     * @param orderId 审批单ID
     * @return 评论列表
     */
    List<ApplyCommentVo> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 管理端评论分页
     *
     * @param page 分页
     * @param bo   查询条件
     * @return 分页结果
     */
    IPage<ApplyCommentVo> selectAdminPage(IPage<ApplyCommentVo> page, @Param("bo") AdminQueryBo bo);
}
