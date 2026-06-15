package com.example.blog_froum.repository;

import com.example.blog_froum.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query(value = "SELECT l FROM OperationLog l LEFT JOIN FETCH l.user u " +
            "WHERE (:type IS NULL OR l.operationType = :type) " +
            "AND (:action IS NULL OR l.operationAction = :action) " +
            "AND (:startTime IS NULL OR l.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR l.createdAt < :endTime) " +
            "AND (:keyword IS NULL OR " +
            "LOWER(l.operationDesc) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(l.targetName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY l.createdAt DESC",
            countQuery = "SELECT COUNT(l) FROM OperationLog l LEFT JOIN l.user u " +
                    "WHERE (:type IS NULL OR l.operationType = :type) " +
                    "AND (:action IS NULL OR l.operationAction = :action) " +
                    "AND (:startTime IS NULL OR l.createdAt >= :startTime) " +
                    "AND (:endTime IS NULL OR l.createdAt < :endTime) " +
                    "AND (:keyword IS NULL OR " +
                    "LOWER(l.operationDesc) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(l.targetName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<OperationLog> searchLogs(@Param("type") String type,
                                  @Param("action") String action,
                                  @Param("keyword") String keyword,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  Pageable pageable);

    Optional<OperationLog> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
