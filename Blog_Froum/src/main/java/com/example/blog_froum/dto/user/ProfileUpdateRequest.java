package com.example.blog_froum.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 个人资料更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "个人资料更新请求")
public class ProfileUpdateRequest {

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @ApiModelProperty(value = "昵称", example = "技术大牛")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "邮箱", example = "user@example.com")
    private String email;

    @Pattern(regexp = "\\d{6}", message = "邮箱验证码必须是6位数字")
    @ApiModelProperty(value = "更换邮箱验证码", example = "123456")
    private String verificationCode;

    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    @ApiModelProperty(value = "个人简介", example = "热爱技术，专注分享")
    private String bio;

    @ApiModelProperty(value = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}
