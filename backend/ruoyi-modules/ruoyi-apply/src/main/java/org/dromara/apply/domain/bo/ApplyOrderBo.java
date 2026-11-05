package org.dromara.apply.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.apply.domain.ApplyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发起审批业务对象
 *
 * @author want-do-apply
 */
@Data
@AutoMapper(target = ApplyOrder.class, reverseConvertGenerate = false)
public class ApplyOrderBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不能超过100个字符")
    private String title;

    /** 商品价格 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    @DecimalMax(value = "99999999.99", message = "价格超出范围")
    private BigDecimal price;

    /** 购买理由 */
    @NotBlank(message = "购买理由不能为空")
    @Size(min = 5, max = 500, message = "购买理由需要5-500个字符")
    private String reason;

    /** 替代品 */
    @Size(max = 200, message = "替代品不能超过200个字符")
    private String substitute;

    /** 图片，逗号分隔 */
    @Size(max = 1000, message = "图片过多")
    private String images;

    /** 分类 */
    @Size(max = 30, message = "分类过长")
    private String category;

    /** 可见范围（0公开 1群内） */
    private String visibility;

    /** 审批方式（0单人审批 1多人审批，默认多人） */
    @Pattern(regexp = "^[01]$", message = "审批方式不合法")
    private String approveMode;

    /** 审批时长（小时），1-72，默认 24 */
    @Min(value = 1, message = "审批时长至少 1 小时")
    @Max(value = 72, message = "审批时长最多 72 小时")
    private Integer durationHours;

    /** 审批群ID */
    private Long groupId;
}
