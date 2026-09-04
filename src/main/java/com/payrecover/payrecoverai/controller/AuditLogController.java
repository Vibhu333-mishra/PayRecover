package com.payrecover.payrecoverai.controller;

import com.payrecover.payrecoverai.dto.AuditLogDto;
import com.payrecover.payrecoverai.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read endpoints for the audit trail.
 *
 * There is no POST here on purpose: nothing outside the app is allowed to write
 * an audit entry. See AuditLogService for the reasoning.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // GET http://localhost:8080/api/audit-logs
    @GetMapping
    public List<AuditLogDto> getRecentLogs() {
        return auditLogService.getRecentLogs();
    }

    // GET http://localhost:8080/api/audit-logs/PAY1001
    @GetMapping("/{paymentId}")
    public List<AuditLogDto> getLogsForPayment(@PathVariable String paymentId) {
        return auditLogService.getLogsForPayment(paymentId);
    }
}
