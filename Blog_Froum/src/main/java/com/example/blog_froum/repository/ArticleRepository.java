package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 按作者ID查询文章
    Page<Article> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, String status, Pageable pageable);

    // 按作者ID查询文章（急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.authorId = :authorId AND a.status = :status ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.authorId = :authorId AND a.status = :status")
    Page<Article> findByAuthorIdAndStatusWithAuthorAndCategoryOrderByCreatedAtDesc(@Param("authorId") Long authorId, @Param("status") String status, Pageable pageable);

    // 按分类ID查询文章
    Page<Article> findByCategoryIdAndStatusOrderByCreatedAtDesc(Long categoryId, String status, Pageable pageable);

    // 按分类ID查询文章（急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.categoryId = :categoryId AND a.status = :status ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.categoryId = :categoryId AND a.status = :status")
    Page<Article> findByCategoryIdAndStatusWithAuthorAndCategoryOrderByCreatedAtDesc(@Param("categoryId") Long categoryId, @Param("status") String status, Pageable pageable);
    
    // 按标签ID查询文章
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t.id = :tagId AND a.status = :status ORDER BY a.createdAt DESC")
    Page<Article> findByTagIdAndStatusOrderByCreatedAtDesc(@Param("tagId") Long tagId, @Param("status") String status, Pageable pageable);
    
    // 查询热门文章（按浏览量排序）
    Page<Article> findByStatusOrderByViewCountDesc(String status, Pageable pageable);

    // 查询热门文章（按浏览量排序，急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.viewCount DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    Page<Article> findByStatusWithAuthorAndCategoryOrderByViewCountDesc(@Param("status") String status, Pageable pageable);

    // 查询推荐文章（按点赞数排序）
    Page<Article> findByStatusOrderByLikeCountDesc(String status, Pageable pageable);

    // 查询推荐文章（按点赞数排序，急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.likeCount DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    Page<Article> findByStatusWithAuthorAndCategoryOrderByLikeCountDesc(@Param("status") String status, Pageable pageable);
    
    // 查询置顶文章
    List<Article> findByIsPinnedTrueAndStatusOrderByCreatedAtDesc(String status);

    // 查询置顶文章（急切加载作者和分类信息）
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.isPinned = true AND a.status = :status ORDER BY a.createdAt DESC")
    List<Article> findByIsPinnedTrueAndStatusWithAuthorAndCategoryOrderByCreatedAtDesc(@Param("status") String status);

    // 查询精选文章
    List<Article> findByIsFeaturedTrueAndStatusOrderByCreatedAtDesc(String status);

    // 查询精选文章（急切加载作者和分类信息）
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.isFeatured = true AND a.status = :status ORDER BY a.createdAt DESC")
    List<Article> findByIsFeaturedTrueAndStatusWithAuthorAndCategoryOrderByCreatedAtDesc(@Param("status") String status);
    
    // 按状态查询所有文章
    Page<Article> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // 查询所有文章（急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a")
    Page<Article> findAllWithAuthorAndCategoryOrderByCreatedAtDesc(Pageable pageable);

    // 按状态查询所有文章（急切加载作者和分类信息）
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    Page<Article> findByStatusWithAuthorAndCategoryOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    // 按作者ID查询文章（不限状态）
    Page<Article> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    // 根据ID查询文章详情（急切加载作者和分类信息）
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category WHERE a.id = :id")
    Optional<Article> findByIdWithAuthorAndCategory(@Param("id") Long id);

    /**
     * 根据标题或内容搜索文章
     */
    @Query("SELECT a FROM Article a WHERE a.status = :status AND (a.title LIKE %:keyword% OR a.content LIKE %:keyword%) ORDER BY a.createdAt DESC")
    Page<Article> searchByKeyword(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Article a " +
            "LEFT JOIN FETCH a.author " +
            "LEFT JOIN FETCH a.category " +
            "LEFT JOIN a.tags t " +
            "WHERE a.status = :status " +
            "AND (:categoryId IS NULL OR a.categoryId = :categoryId) " +
            "AND (:tagId IS NULL OR t.id = :tagId) " +
            "AND (:keyword IS NULL OR " +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(a.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(a.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'hottest' THEN a.viewCount ELSE 0 END DESC, " +
            "CASE WHEN :sort = 'mostComments' THEN a.commentCount ELSE 0 END DESC, " +
            "CASE WHEN :sort = 'mostLiked' THEN a.likeCount ELSE 0 END DESC, " +
            "a.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT a) FROM Article a LEFT JOIN a.tags t " +
                    "WHERE a.status = :status " +
                    "AND (:categoryId IS NULL OR a.categoryId = :categoryId) " +
                    "AND (:tagId IS NULL OR t.id = :tagId) " +
                    "AND (:keyword IS NULL OR " +
                    "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(a.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(a.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Article> searchPublicArticles(@Param("status") String status,
                                       @Param("categoryId") Long categoryId,
                                       @Param("tagId") Long tagId,
                                       @Param("keyword") String keyword,
                                       @Param("sort") String sort,
                                       Pageable pageable);

    // 统计方法
    /**
     * 根据状态统计文章数量
     */
    long countByStatus(String status);

    /**
     * 统计某分类下指定状态的文章数量
     */
    long countByCategoryIdAndStatus(Long categoryId, String status);

    /**
     * 统计置顶文章数量
     */
    long countByStatusAndIsPinned(String status, Boolean isPinned);

    /**
     * 统计精选文章数量
     */
    long countByStatusAndIsFeatured(String status, Boolean isFeatured);

    /**
     * 统计用户发布的文章数量
     */
    long countByAuthorId(Long authorId);

    /**
     * 统计用户发布的已发布文章数量
     */
    long countByAuthorIdAndStatus(Long authorId, String status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Article> findTop5ByOrderByCreatedAtDesc();
}
