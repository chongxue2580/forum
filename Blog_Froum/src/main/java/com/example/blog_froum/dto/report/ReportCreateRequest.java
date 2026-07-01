package com.example.blog_froum.dto.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "举报创建请求")
public class ReportCreateRequest {

    @NotBlank(message = "举报目标类型不能为空")
    @ApiModelProperty(value = "举报目标类型：ARTICLE, QUESTION, COMMENT, USER", required = true, example = "ARTICLE")
    private String targetType;

    @NotNull(message = "举报目标ID不能为空")
    @Positive(message = "举报目标ID必须大于0")
    @ApiModelProperty(value = "举报目标ID", required = true, example = "1")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 120, message = "举报原因不能超过120个字符")
    @ApiModelProperty(value = "举报原因", required = true, example = "违规内容")
    private String reason;

    @Size(max = 2000, message = "举报说明不能超过2000个字符")
    @ApiModelProperty(value = "举报补充说明", example = "内容包含广告或攻击性言论")
    private String description;
}
