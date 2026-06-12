package com.example.blog_froum.dto.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 评论创建请求DTO
 */
@Data
@ApiModel(description = "评论创建请求")
public class CommentCreateRequest {

    @ApiModelProperty(value = "目标类型", required = true, example = "ARTICLE", allowableValues = "ARTICLE,QUESTION")
    @NotBlank(message = "目标类型不能为空")
    private String targetType; // ARTICLE, QUESTION

    @ApiModelProperty(value = "目标ID", required = true, example = "1")
    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @ApiModelProperty(value = "评论内容", required = true, example = "这篇文章写得很好！")
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @ApiModelProperty(value = "父评论ID", example = "1")
    private Long parentId; // 父评论ID，用于回复评论

    // 为了向后兼容，保留articleId字段
    @Deprecated
    public Long getArticleId() {
        return "ARTICLE".equals(targetType) ? targetId : null;
    }

    @Deprecated
    public void setArticleId(Long articleId) {
        if (articleId != null) {
            this.targetType = "ARTICLE";
            this.targetId = articleId;
        }
    }
}
