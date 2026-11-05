package org.dromara.apply.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发起人补充说明
 *
 * @author want-do-apply
 */
@Data
public class ApplyNoteBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 补充说明正文 */
    @NotBlank(message = "补充说明不能为空")
    @Size(max = 500, message = "补充说明最多 500 字")
    private String note;
}
