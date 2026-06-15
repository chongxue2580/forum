package com.example.blog_froum.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "管理员封禁用户请求")
public class AdminUserBanRequest {

    @ApiModelProperty(value = "封禁类型：LOGIN=禁止登录，CONTENT=禁止发布内容", example = "LOGIN")
    private String banType = "LOGIN";

    @NotBlank(message = "封禁理由不能为空")
    @Size(max = 500, message = "封禁理由不能超过500个字符")
    @ApiModelProperty(value = "封禁理由", required = true, example = "发布违规内容")
    private String reason;

    @Min(value = 1, message = "封禁天数最少为1天")
    @Max(value = 3650, message = "封禁天数不能超过3650天")
    @ApiModelProperty(value = "封禁天数，空表示永久封禁", example = "7")
    private Integer durationDays;
}
