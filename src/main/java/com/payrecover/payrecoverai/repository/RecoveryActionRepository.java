package com.payrecover.payrecoverai.repository;

import com.payrecover.payrecoverai.entity.PolicyDecision;
import com.payrecover.payrecoverai.entity.RecoveryActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Queries over the recovery_actions table.
 *
 * Phase 6 (policy engine) and Phase 7 (recovery simulator) write here; the
 * Recovery History screen reads from here.
 */
public interface RecoveryActionRepository extends JpaRepository<RecoveryActionEntity, Long> {

    /** Recovery history for one payment, newest first. */
    List<RecoveryActionEntity> findByPayment_PaymentIdOrderByCreatedAtDesc(String paymentId);

    /** The whole recovery history table, newest first. */
    List<RecoveryActionEntity> findAllByOrderByCreatedAtDesc();

    /** e.g. how many recoveries the policy engine blocked. */
    long countByPolicyDecision(PolicyDecision policyDecision);

    /** e.g. countByOutcome("RECOVERED") for live recovery metrics. */
    long countByOutcome(String outcome);
}
