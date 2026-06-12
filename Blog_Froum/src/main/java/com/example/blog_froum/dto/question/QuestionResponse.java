package com.example.blog_froum.dto.question;

import com.example.blog_froum.dto.tag.TagResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.Question;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答响应DTO
 */
@Data
@ApiModel(description = "问答响应")
public class QuestionResponse {

    @ApiModelProperty(value = "问题ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "问题标题", example = "如何优化Java程序的性能？")
    private String title;

    @ApiModelProperty(value = "问题内容", example = "我的Java程序运行比较慢...")
    private String content;

    @ApiModelProperty(value = "作者信息")
    private UserResponse author;

    @ApiModelProperty(value = "浏览量", example = "100")
    private Integer viewCount;

    @ApiModelProperty(value = "点赞数", example = "15")
    private Integer likeCount;

    @ApiModelProperty(value = "评论数", example = "8")
    private Integer commentCount;

    @ApiModelProperty(value = "关注数", example = "5")
    private Integer followCount;

    @ApiModelProperty(value = "是否已解决", example = "false")
    private Boolean isSolved;

    @ApiModelProperty(value = "是否置顶", example = "false")
    private Boolean isPinned;

    @ApiModelProperty(value = "是否精选", example = "false")
    private Boolean isFeatured;

    @ApiModelProperty(value = "状态", example = "APPROVED")
    private String status;

    @ApiModelProperty(value = "标签列表")
    private List<TagResponse> tags;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static QuestionResponse fromEntity(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setViewCount(question.getViewCount());
        response.setLikeCount(question.getLikeCount());
        response.setCommentCount(question.getCommentCount());
        response.setFollowCount(question.getFollowCount());
        response.setIsSolved(question.getIsSolved());
        response.setIsPinned(question.getIsPinned());
        response.setIsFeatured(question.getIsFeatured());
        response.setStatus(question.getStatus());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());

        // 设置作者信息
        if (question.getAuthor() != null) {
            response.setAuthor(UserResponse.fromUser(question.getAuthor()));
        }

        return response;
    }
}
