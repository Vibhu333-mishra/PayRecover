package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.AuditLogDto;
import com.payrecover.payrecoverai.entity.AuditLog;
import com.payrecover.payrecoverai.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * READ-ONLY view over the audit trail.
 *
 * WHY THERE IS NO create() METHOD HERE
 * Audit rows are written by whichever service made the decision, at the moment
 * it made it, inside that decision's transaction (see
 * AiDiagnosisService.writeAuditEntry). If writing were exposed here as a public
 * API, someone could later add an audit row that does not correspond to a real
 * event -- which would make the trail worthless. So this class only reads.
 *
 * @Transactional(readOnly = true) tells the database this connection will not
 * write, which lets it skip some bookkeeping. It is also a clear statement of
 * intent to anyone reading the class.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** The 100 most recent events across all payments, newest first. */
    public List<AuditLogDto> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Full trail for one payment -- the timeline in the detail panel. */
    public List<AuditLogDto> getLogsForPayment(String paymentId) {
        return auditLogRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private AuditLogDto toDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setTimestamp(log.getCreatedAt());
        dto.setPaymentId(log.getPaymentId());
        dto.setEventType(log.getEventType());

        // Every enum below is nullable in the table, so each needs a null guard
        // before calling .name(). Forgetting one of these is the single most
        // common NullPointerException in DTO mapping code.
        dto.setFailureCategory(log.getFailureCategory() == null
                ? null : log.getFailureCategory().name());
        dto.setAiRecommendation(log.getAiRecommendation() == null
                ? null : log.getAiRecommendation().name());
        dto.setAiSource(log.getAiSource() == null
                ? null : log.getAiSource().name());
        dto.setPolicyDecision(log.getPolicyDecision() == null
                ? null : log.getPolicyDecision().name());
        dto.setFinalAction(log.getFinalAction() == null
                ? null : log.getFinalAction().name());

        dto.setConfidencePercent(log.getConfidence() == null
                ? null : (int) Math.round(log.getConfidence() * 100));

        dto.setResult(log.getResult());
        dto.setDetails(log.getDetails());
        return dto;
    }
}
