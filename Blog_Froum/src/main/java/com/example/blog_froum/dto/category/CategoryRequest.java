package com.example.blog_froum.dto.category;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "分类描述长度不能超过500个字符")
    private String description;

    @Size(max = 100, message = "分类图标长度不能超过100个字符")
    private String icon;

    @Min(value = 0, message = "显示顺序不能小于0")
    private Integer displayOrder;

    private String status;
}
