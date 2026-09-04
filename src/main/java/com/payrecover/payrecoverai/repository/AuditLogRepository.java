package com.payrecover.payrecoverai.repository;

import com.payrecover.payrecoverai.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Queries over the audit_logs table.
 *
 * This table only ever grows, so the UI must never ask for "everything".
 * findTop100... puts a LIMIT 100 in the SQL, which keeps the Audit Logs page
 * fast no matter how long you demo for.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** The 100 most recent events -- what the Audit Logs screen shows. */
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    /** Full trail for one payment, newest first. Powers the payment timeline. */
    List<AuditLog> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    /** Filter by event type, e.g. only "AI_DIAGNOSIS" rows. */
    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
