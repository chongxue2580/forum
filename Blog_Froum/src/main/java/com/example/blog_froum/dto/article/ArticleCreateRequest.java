package com.example.blog_froum.dto.article;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 文章创建请求DTO
 */
@Data
public class ArticleCreateRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    @Size(max = 500, message = "文章摘要长度不能超过500个字符")
    private String summary;

    private Long categoryId;

    private List<String> tags;

    private Boolean isDraft = false;

    private String coverImage;
}
