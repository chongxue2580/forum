package com.example.blog_froum.service;

import com.example.blog_froum.dto.article.ArticleCreateRequest;
import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.article.ArticleUpdateRequest;
import com.example.blog_froum.dto.statistics.ArticleStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService {
    
    /**
     * 创建文章
     */
    ArticleResponse createArticle(Long authorId, ArticleCreateRequest request);
    
    /**
     * 更新文章
     */
    ArticleResponse updateArticle(Long articleId, Long authorId, ArticleUpdateRequest request);
    
    /**
     * 删除文章
     */
    void deleteArticle(Long articleId, Long authorId);
    
    /**
     * 获取文章详情
     */
    ArticleResponse getArticleById(Long articleId);
    
    /**
     * 获取文章列表
     */
    Page<ArticleResponse> getArticles(Pageable pageable, String status);

    /**
     * 公共文章列表筛选。
     */
    Page<ArticleResponse> searchPublicArticles(Long categoryId,
                                               Long tagId,
                                               String keyword,
                                               String sort,
                                               Pageable pageable);
    
    /**
     * 获取用户的文章列表
     */
    Page<ArticleResponse> getUserArticles(Long userId, Pageable pageable);

    /**
     * 搜索文章
     */
    Page<ArticleResponse> searchArticles(String keyword, Pageable pageable);

    // 管理员功能
    /**
     * 获取所有文章（管理员）
     */
    Page<ArticleResponse> getAllArticles(Pageable pageable);

    /**
     * 根据状态获取文章
     */
    Page<ArticleResponse> getArticlesByStatus(String status, Pageable pageable);

    /**
     * 审核通过文章
     */
    void approveArticle(Long articleId);

    /**
     * 审核拒绝文章
     */
    void rejectArticle(Long articleId, String reason);

    /**
     * 设置文章为置顶
     */
    void pinArticle(Long articleId);

    /**
     * 取消文章置顶
     */
    void unpinArticle(Long articleId);

    /**
     * 设置文章为精选
     */
    void featureArticle(Long articleId);

    /**
     * 取消文章精选
     */
    void unfeatureArticle(Long articleId);

    /**
     * 管理员删除文章
     */
    void adminDeleteArticle(Long articleId);

    /**
     * 获取文章统计信息
     */
    ArticleStatisticsResponse getArticleStatistics();
    
    /**
     * 点赞文章
     */
    void likeArticle(Long articleId, Long userId);
    
    /**
     * 取消点赞文章
     */
    void unlikeArticle(Long articleId, Long userId);
    
    /**
     * 检查用户是否点赞了文章
     */
    boolean isArticleLikedByUser(Long articleId, Long userId);
    
    /**
     * 获取热门文章
     */
    List<ArticleResponse> getPopularArticles(int limit);
    
    /**
     * 获取推荐文章
     */
    List<ArticleResponse> getFeaturedArticles(int limit);
}
