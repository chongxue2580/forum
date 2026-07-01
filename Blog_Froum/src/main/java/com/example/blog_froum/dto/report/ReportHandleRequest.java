package com.example.blog_froum.dto.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "举报处理请求")
public class ReportHandleRequest {

    @NotBlank(message = "处理状态不能为空")
    @ApiModelProperty(value = "处理状态：RESOLVED=已处理，REJECTED=驳回", required = true, example = "RESOLVED")
    private String status;

    @Size(max = 1000, message = "处理备注不能超过1000个字符")
    @ApiModelProperty(value = "处理备注", example = "已删除违规内容")
    private String handlerNote;
}
