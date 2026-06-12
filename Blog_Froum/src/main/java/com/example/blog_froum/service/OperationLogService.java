package com.example.blog_froum.service;

import com.example.blog_froum.dto.admin.OperationLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

public interface OperationLogService {

    void record(Long userId,
                String operationType,
                String action,
                String description,
                String targetType,
                Long targetId,
                String targetName,
                HttpServletRequest request);

    Page<OperationLogResponse> searchLogs(String type,
                                          String action,
                                          String keyword,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          Pageable pageable);
}
