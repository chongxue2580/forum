package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.admin.OperationLogResponse;
import com.example.blog_froum.entity.OperationLog;
import com.example.blog_froum.repository.OperationLogRepository;
import com.example.blog_froum.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId,
                       String operationType,
                       String action,
                       String description,
                       String targetType,
                       Long targetId,
                       String targetName,
                       HttpServletRequest request) {
        try {
            OperationLog logEntry = new OperationLog();
            logEntry.setUserId(userId);
            logEntry.setOperationType(truncate(operationType, 50));
            logEntry.setOperationAction(truncate(action, 50));
            logEntry.setOperationDesc(truncate(description, 500));
            logEntry.setTargetType(truncate(targetType, 50));
            logEntry.setTargetId(targetId);
            logEntry.setTargetName(truncate(targetName, 200));
            logEntry.setIpAddress(truncate(resolveClientIp(request), 50));
            logEntry.setUserAgent(truncate(request == null ? null : request.getHeader("User-Agent"), 500));
            operationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationLogResponse> searchLogs(String type,
                                                 String action,
                                                 String keyword,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 Pageable pageable) {
        String normalizedType = normalizeBlank(type);
        String normalizedAction = normalizeBlank(action);
        String normalizedKeyword = normalizeBlank(keyword);
        return operationLogRepository
                .searchLogs(normalizedType, normalizedAction, normalizedKeyword, startTime, endTime, pageable)
                .map(OperationLogResponse::fromEntity);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
