package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId,
                                                                String targetType,
                                                                Long targetId,
                                                                String status);

    long countByStatus(String status);

    @Query(value = "SELECT r FROM Report r " +
            "LEFT JOIN FETCH r.reporter " +
            "LEFT JOIN FETCH r.handler " +
            "WHERE r.reporterId = :reporterId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Report r " +
                    "WHERE r.reporterId = :reporterId " +
                    "AND (:status IS NULL OR r.status = :status)")
    Page<Report> findByReporterForUser(@Param("reporterId") Long reporterId,
                                        @Param("status") String status,
                                        Pageable pageable);

    @Query("SELECT r FROM Report r " +
            "LEFT JOIN FETCH r.reporter " +
            "LEFT JOIN FETCH r.handler " +
            "WHERE r.id = :id")
    Optional<Report> findByIdWithUsers(@Param("id") Long id);

    @Query(value = "SELECT r FROM Report r " +
            "LEFT JOIN FETCH r.reporter reporter " +
            "LEFT JOIN FETCH r.handler handler " +
            "WHERE (:targetType IS NULL OR r.targetType = :targetType) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:startTime IS NULL OR r.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR r.createdAt < :endTime) " +
            "AND (:keyword IS NULL OR " +
            "LOWER(COALESCE(r.targetTitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.reason, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(reporter.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(reporter.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Report r " +
                    "LEFT JOIN r.reporter reporter " +
                    "WHERE (:targetType IS NULL OR r.targetType = :targetType) " +
                    "AND (:status IS NULL OR r.status = :status) " +
                    "AND (:startTime IS NULL OR r.createdAt >= :startTime) " +
                    "AND (:endTime IS NULL OR r.createdAt < :endTime) " +
                    "AND (:keyword IS NULL OR " +
                    "LOWER(COALESCE(r.targetTitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(r.reason, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(reporter.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(reporter.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Report> searchForAdmin(@Param("targetType") String targetType,
                                @Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                Pageable pageable);
}
