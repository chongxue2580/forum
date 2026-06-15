package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.article.ArticleCreateRequest;
import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.article.ArticleUpdateRequest;
import com.example.blog_froum.dto.statistics.ArticleStatisticsResponse;
import com.example.blog_froum.entity.Article;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.enums.ArticleStatus;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.service.ArticleService;
import com.example.blog_froum.service.LikeService;
import com.example.blog_froum.service.NotificationService;
import com.example.blog_froum.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章服务实现类
 */
@Slf4j
@Service
@Transactional
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LikeService likeService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Override
    public ArticleResponse createArticle(Long authorId, ArticleCreateRequest request) {
        log.info("创建文章，作者ID: {}, 标题: {}", authorId, request.getTitle());

        // 验证作者是否存在
        User author = userMapper.findById(authorId);
        if (author == null) {
            throw new RuntimeException("作者不存在");
        }
        userService.assertCanCreateContent(authorId);

        // 创建文章实体
        Article article = new Article();
        article.setTitle(normalizeText(request.getTitle()));
        article.setContent(normalizeText(request.getContent()));
        article.setSummary(normalizeText(request.getSummary()));
        article.setCategoryId(request.getCategoryId());
        article.setCoverImage(normalizeText(request.getCoverImage()));
        article.setAuthorId(authorId);
        // 草稿保持 DRAFT；管理员/超管发布免审核直接 APPROVED；普通用户进入 PENDING 待审核
        if (Boolean.TRUE.equals(request.getIsDraft())) {
            article.setStatus(ArticleStatus.DRAFT.name());
        } else if (author.isAdmin()) {
            article.setStatus(ArticleStatus.APPROVED.name());
        } else {
            article.setStatus(ArticleStatus.PENDING.name());
        }
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setIsFeatured(false);
        article.setIsPinned(false);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        
        // 设置标签字符串
        List<String> normalizedTags = normalizeTags(request.getTags());
        if (!normalizedTags.isEmpty()) {
            article.setTagsString(String.join(",", normalizedTags));
        } else {
            article.setTagsString("");
        }

        // 保存文章
        Article savedArticle = articleRepository.save(article);
        log.info("文章创建成功，ID: {}", savedArticle.getId());

        // 重新查询文章以加载关联对象
        Article articleWithAssociations = articleRepository.findByIdWithAuthorAndCategory(savedArticle.getId())
                .orElseThrow(() -> new RuntimeException("创建的文章未找到"));

        return ArticleResponse.fromEntity(articleWithAssociations);
    }

    @Override
    public ArticleResponse updateArticle(Long articleId, Long authorId, ArticleUpdateRequest request) {
        log.info("更新文章，ID: {}, 作者ID: {}", articleId, authorId);

        // 查找文章
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 验证作者权限
        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限修改此文章");
        }
        userService.assertCanCreateContent(authorId);

        // 更新文章信息
        article.setTitle(normalizeText(request.getTitle()));
        article.setContent(normalizeText(request.getContent()));
        article.setSummary(normalizeText(request.getSummary()));
        article.setCategoryId(request.getCategoryId());
        article.setCoverImage(normalizeText(request.getCoverImage()));
        
        // 设置标签字符串
        if (request.getTags() != null) {
            article.setTagsString(String.join(",", normalizeTags(request.getTags())));
        } else {
            article.setTagsString("");
        }
        
        if (request.getIsDraft() != null) {
            article.setStatus(request.getIsDraft() ? ArticleStatus.DRAFT.name() : ArticleStatus.PENDING.name());
        }
        
        article.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        Article updatedArticle = articleRepository.save(article);
        log.info("文章更新成功，ID: {}", updatedArticle.getId());

        Article articleWithAssociations = articleRepository.findByIdWithAuthorAndCategory(updatedArticle.getId())
                .orElseThrow(() -> new RuntimeException("更新的文章未找到"));

        return ArticleResponse.fromEntity(articleWithAssociations);
    }

    @Override
    public void deleteArticle(Long articleId, Long authorId) {
        log.info("删除文章，ID: {}, 作者ID: {}", articleId, authorId);

        // 查找文章
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 验证作者权限
        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权限删除此文章");
        }

        // 删除文章
        articleRepository.delete(article);
        log.info("文章删除成功，ID: {}", articleId);
    }

    @Override
    @Transactional
    public ArticleResponse getArticleById(Long articleId) {
        Article article = articleRepository.findByIdWithAuthorAndCategory(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 增加浏览量
        Integer currentViewCount = article.getViewCount();
        article.setViewCount(currentViewCount != null ? currentViewCount + 1 : 1);
        articleRepository.save(article);

        return ArticleResponse.fromEntity(article);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticles(Pageable pageable, String status) {
        Page<Article> articlePage;
        if (status != null) {
            articlePage = articleRepository.findByStatusWithAuthorAndCategoryOrderByCreatedAtDesc(status, pageable);
        } else {
            articlePage = articleRepository.findByStatusWithAuthorAndCategoryOrderByCreatedAtDesc(
                    ArticleStatus.APPROVED.name(), pageable);
        }
        return articlePage.map(ArticleResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> searchPublicArticles(Long categoryId,
                                                      Long tagId,
                                                      String keyword,
                                                      String sort,
                                                      Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedSort = StringUtils.hasText(sort) ? sort.trim() : "newest";
        Page<Article> articlePage = articleRepository.searchPublicArticles(
                ArticleStatus.APPROVED.name(),
                categoryId,
                tagId,
                normalizedKeyword,
                normalizedSort,
                pageable);
        return articlePage.map(ArticleResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getUserArticles(Long userId, Pageable pageable) {
        Page<Article> articlePage = articleRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return articlePage.map(ArticleResponse::fromEntity);
    }

    @Override
    public void likeArticle(Long articleId, Long userId) {
        log.info("用户 {} 点赞文章 {}", userId, articleId);

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        // 使用LikeService处理点赞
        likeService.likeTarget(userId, "ARTICLE", articleId);

        // 创建点赞通知
        if (!article.getAuthorId().equals(userId)) {
            notificationService.createArticleLikeNotification(
                article.getAuthorId(), userId, articleId, article.getTitle());
        }
    }

    @Override
    public void unlikeArticle(Long articleId, Long userId) {
        log.info("用户 {} 取消点赞文章 {}", userId, articleId);

        // 使用LikeService处理取消点赞
        likeService.unlikeTarget(userId, "ARTICLE", articleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isArticleLikedByUser(Long articleId, Long userId) {
        return likeService.isTargetLikedByUser(userId, "ARTICLE", articleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> searchArticles(String keyword, Pageable pageable) {
        Page<Article> articlePage = articleRepository.searchByKeyword("APPROVED", keyword, pageable);
        return articlePage.map(ArticleResponse::fromEntity);
    }

    // 管理员功能实现
    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getAllArticles(Pageable pageable) {
        Page<Article> articlePage = articleRepository.findAllWithAuthorAndCategoryOrderByCreatedAtDesc(pageable);
        return articlePage.map(ArticleResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticlesByStatus(String status, Pageable pageable) {
        Page<Article> articlePage = articleRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return articlePage.map(ArticleResponse::fromEntity);
    }

    @Override
    public void approveArticle(Long articleId) {
        log.info("管理员审核通过文章，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setStatus("APPROVED");
        articleRepository.save(article);
        log.info("文章审核通过成功，ID: {}", articleId);
    }

    @Override
    public void rejectArticle(Long articleId, String reason) {
        log.info("管理员审核拒绝文章，ID: {}, 原因: {}", articleId, reason);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setStatus("REJECTED");
        articleRepository.save(article);
        notificationService.createArticleRejectedNotification(
                article.getAuthorId(),
                article.getId(),
                article.getTitle(),
                reason);
        log.info("文章审核拒绝成功，ID: {}", articleId);
    }

    @Override
    public void pinArticle(Long articleId) {
        log.info("管理员设置文章置顶，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setIsPinned(true);
        articleRepository.save(article);
        log.info("文章设置置顶成功，ID: {}", articleId);
    }

    @Override
    public void unpinArticle(Long articleId) {
        log.info("管理员取消文章置顶，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setIsPinned(false);
        articleRepository.save(article);
        log.info("文章取消置顶成功，ID: {}", articleId);
    }

    @Override
    public void featureArticle(Long articleId) {
        log.info("管理员设置文章精选，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setIsFeatured(true);
        articleRepository.save(article);
        log.info("文章设置精选成功，ID: {}", articleId);
    }

    @Override
    public void unfeatureArticle(Long articleId) {
        log.info("管理员取消文章精选，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        article.setIsFeatured(false);
        articleRepository.save(article);
        log.info("文章取消精选成功，ID: {}", articleId);
    }

    @Override
    public void adminDeleteArticle(Long articleId) {
        log.info("管理员删除文章，ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        articleRepository.delete(article);
        log.info("管理员删除文章成功，ID: {}", articleId);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleStatisticsResponse getArticleStatistics() {
        log.info("获取文章统计信息");

        // 统计各状态的文章数量
        long totalArticles = articleRepository.count();
        long approvedArticles = articleRepository.countByStatus("APPROVED");
        long pendingArticles = articleRepository.countByStatus("PENDING");
        long rejectedArticles = articleRepository.countByStatus("REJECTED");
        long pinnedArticles = articleRepository.countByStatusAndIsPinned("APPROVED", true);
        long featuredArticles = articleRepository.countByStatusAndIsFeatured("APPROVED", true);

        return new ArticleStatisticsResponse(
                totalArticles,
                approvedArticles,
                pendingArticles,
                rejectedArticles,
                pinnedArticles,
                featuredArticles);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> getPopularArticles(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Article> articlePage = articleRepository.findByStatusWithAuthorAndCategoryOrderByViewCountDesc(
                ArticleStatus.APPROVED.name(), pageable);
        return articlePage.getContent().stream()
                .map(ArticleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> getFeaturedArticles(int limit) {
        List<Article> featuredArticles = articleRepository.findByIsFeaturedTrueAndStatusWithAuthorAndCategoryOrderByCreatedAtDesc(
                ArticleStatus.APPROVED.name());
        
        if (featuredArticles.size() < limit) {
            Pageable pageable = PageRequest.of(0, limit - featuredArticles.size());
            Page<Article> likedArticles = articleRepository.findByStatusWithAuthorAndCategoryOrderByLikeCountDesc(
                    ArticleStatus.APPROVED.name(), pageable);
            featuredArticles.addAll(likedArticles.getContent());
        }
        
        if (featuredArticles.size() > limit) {
            featuredArticles = featuredArticles.subList(0, limit);
        }
        
        return featuredArticles.stream()
                .map(ArticleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }
}
