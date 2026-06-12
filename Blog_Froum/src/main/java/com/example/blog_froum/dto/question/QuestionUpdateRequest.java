package com.example.blog_froum.dto.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 问答更新请求DTO
 */
@Data
@ApiModel(description = "问答更新请求")
public class QuestionUpdateRequest {

    @ApiModelProperty(value = "问题标题", required = true, example = "如何优化Java程序的性能？")
    @NotBlank(message = "问题标题不能为空")
    @Size(max = 200, message = "问题标题长度不能超过200个字符")
    private String title;

    @ApiModelProperty(value = "问题内容", required = true, example = "我的Java程序运行比较慢，主要是在处理大量数据时...")
    @NotBlank(message = "问题内容不能为空")
    private String content;

    @ApiModelProperty(value = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;
}
