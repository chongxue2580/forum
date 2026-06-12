package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 问答Repository
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 根据状态分页查询问答
     */
    Page<Question> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 根据状态分页查询问答（急切加载作者信息）
     */
    @Query(value = "SELECT q FROM Question q LEFT JOIN FETCH q.author WHERE q.status = :status ORDER BY q.createdAt DESC",
           countQuery = "SELECT COUNT(q) FROM Question q WHERE q.status = :status")
    Page<Question> findByStatusWithAuthorOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    /**
     * 根据作者ID分页查询问答
     */
    Page<Question> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * 根据状态和作者ID分页查询问答
     */
    Page<Question> findByStatusAndAuthorIdOrderByCreatedAtDesc(String status, Long authorId, Pageable pageable);

    /**
     * 查询热门问答（按点赞数排序）
     */
    Page<Question> findByStatusOrderByLikeCountDescCreatedAtDesc(String status, Pageable pageable);

    /**
     * 查询已解决的问答
     */
    Page<Question> findByStatusAndIsSolvedOrderByCreatedAtDesc(String status, Boolean isSolved, Pageable pageable);

    /**
     * 查询置顶问答
     */
    List<Question> findByStatusAndIsPinnedOrderByCreatedAtDesc(String status, Boolean isPinned);

    /**
     * 查询精选问答
     */
    Page<Question> findByStatusAndIsFeaturedOrderByCreatedAtDesc(String status, Boolean isFeatured, Pageable pageable);

    /**
     * 根据标题或内容搜索问答
     */
    @Query("SELECT q FROM Question q WHERE q.status = :status AND (q.title LIKE %:keyword% OR q.content LIKE %:keyword%) ORDER BY q.createdAt DESC")
    Page<Question> searchByKeyword(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT q.* FROM questions q " +
            "LEFT JOIN question_tags qt ON q.id = qt.question_id " +
            "WHERE q.status = :status " +
            "AND (:tagId IS NULL OR qt.tag_id = :tagId) " +
            "AND (:solved IS NULL OR q.is_solved = :solved) " +
            "AND (:keyword IS NULL OR " +
            "LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(q.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'hottest' THEN q.view_count ELSE 0 END DESC, " +
            "CASE WHEN :sort = 'mostAnswers' THEN q.comment_count ELSE 0 END DESC, " +
            "CASE WHEN :sort = 'mostLiked' THEN q.like_count ELSE 0 END DESC, " +
            "q.created_at DESC",
            countQuery = "SELECT COUNT(DISTINCT q.id) FROM questions q " +
                    "LEFT JOIN question_tags qt ON q.id = qt.question_id " +
                    "WHERE q.status = :status " +
                    "AND (:tagId IS NULL OR qt.tag_id = :tagId) " +
                    "AND (:solved IS NULL OR q.is_solved = :solved) " +
                    "AND (:keyword IS NULL OR " +
                    "LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(q.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            nativeQuery = true)
    Page<Question> searchPublicQuestions(@Param("status") String status,
                                         @Param("tagId") Long tagId,
                                         @Param("solved") Boolean solved,
                                         @Param("keyword") String keyword,
                                         @Param("sort") String sort,
                                         Pageable pageable);

    /**
     * 统计用户的问答数量
     */
    long countByAuthorIdAndStatus(Long authorId, String status);

    /**
     * 统计用户的所有问答数量（不限制状态）
     */
    long countByAuthorId(Long authorId);

    /**
     * 查询用户关注的问答
     */
    @Query("SELECT q FROM Question q JOIN Follow f ON q.id = f.targetId WHERE f.followerId = :userId AND f.targetType = 'QUESTION' AND q.status = :status ORDER BY q.createdAt DESC")
    Page<Question> findFollowedQuestions(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);

    // 统计方法
    /**
     * 根据状态统计问答数量
     */
    long countByStatus(String status);

    /**
     * 统计已解决的问答数量
     */
    long countByStatusAndIsSolved(String status, Boolean isSolved);

    /**
     * 统计置顶问答数量
     */
    long countByStatusAndIsPinned(String status, Boolean isPinned);

    /**
     * 统计精选问答数量
     */
    long countByStatusAndIsFeatured(String status, Boolean isFeatured);

    /**
     * 根据ID查询问答详情（急切加载作者信息）
     */
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.author WHERE q.id = :id")
    Optional<Question> findByIdWithAuthor(@Param("id") Long id);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Question> findTop5ByOrderByCreatedAtDesc();
}
