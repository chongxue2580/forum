package com.example.blog_froum.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 密码更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "密码更新请求")
public class PasswordUpdateRequest {

    @NotBlank(message = "当前密码不能为空")
    @ApiModelProperty(value = "当前密码", required = true, example = "currentPassword123")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20之间")
    @ApiModelProperty(value = "新密码", required = true, example = "newPassword123")
    private String newPassword;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "邮箱验证码必须是6位数字")
    @ApiModelProperty(value = "邮箱验证码", required = true, example = "123456")
    private String verificationCode;
}
