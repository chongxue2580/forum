package com.example.blog_froum.dto.tag;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过50个字符")
    private String name;

    @Size(max = 200, message = "标签描述长度不能超过200个字符")
    private String description;

    @Size(max = 20, message = "标签颜色长度不能超过20个字符")
    private String color;

    private String status;
}
