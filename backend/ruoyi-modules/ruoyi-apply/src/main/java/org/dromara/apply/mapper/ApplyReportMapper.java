package org.dromara.apply.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.dromara.apply.domain.ApplyReport;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyReportVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 举报 Mapper
 *
 * @author want-do-apply
 */
public interface ApplyReportMapper extends BaseMapperPlus<ApplyReport, ApplyReportVo> {

    /**
     * 管理端举报分页
     *
     * @param page 分页
     * @param bo   查询条件
     * @return 分页结果
     */
    IPage<ApplyReportVo> selectAdminPage(IPage<ApplyReportVo> page, @Param("bo") AdminQueryBo bo);
}
