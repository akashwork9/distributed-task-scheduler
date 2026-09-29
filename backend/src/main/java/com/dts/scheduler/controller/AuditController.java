package com.dts.scheduler.controller;

import com.dts.scheduler.dto.PageResponse;
import com.dts.scheduler.dto.audit.AuditLogResponse;
import com.dts.scheduler.entity.AuditLog;
import com.dts.scheduler.repository.AuditLogRepository;
import com.dts.scheduler.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for inspecting system actions and administrative audit trail")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "List audit logs", description = "Retrieves recent audit log events")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLog> page;
        if (SecurityUtils.isAdmin()) {
            page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            page = auditLogRepository.findByUserId(SecurityUtils.getCurrentUserId(), pageable);
        }

        return ResponseEntity.ok(PageResponse.of(page.map(this::mapToAuditLogResponse)));
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : "SYSTEM")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
