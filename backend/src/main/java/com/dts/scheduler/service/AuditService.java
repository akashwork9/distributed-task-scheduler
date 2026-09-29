package com.dts.scheduler.service;

import com.dts.scheduler.entity.AuditLog;
import com.dts.scheduler.entity.User;
import com.dts.scheduler.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void record(User user, String action, String entityType, String entityId, String metadata) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to persist audit log for action={}: {}", action, e.getMessage());
        }
    }
}
