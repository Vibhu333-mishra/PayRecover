package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.config.LlmConfig;
import com.payrecover.payrecoverai.dto.AiDiagnosisResponseDto;
import com.payrecover.payrecoverai.dto.ParsedDiagnosis;
import com.payrecover.payrecoverai.entity.AiDiagnosis;
import com.payrecover.payrecoverai.entity.AiSource;
import com.payrecover.payrecoverai.entity.AuditLog;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import com.payrecover.payrecoverai.exception.LlmUnavailableException;
import com.payrecover.payrecoverai.exception.ResourceNotFoundException;
import com.payrecover.payrecoverai.repository.AiDiagnosisRepository;
import com.payrecover.payrecoverai.repository.AuditLogRepository;
import com.payrecover.payrecoverai.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * THE CONDUCTOR OF PHASE 5. Everything else in this phase is a specialist that
 * does one thing; this class decides the order and owns the failure handling.
 *
 * THE FLOW, IN ORDER
 *   1. Load the payment (404 if it does not exist).
 *   2. Refuse to analyse a payment that never failed -- there is nothing to
 *      diagnose, and analysing it would waste an API call.
 *   3. Ask AiPromptBuilder for the two prompts.
 *   4. Send them via LlmClient, and hand the reply to AiResponseParser.
 *   5. If ANY of step 4 fails -> FallbackClassifier answers instead.
 *   6. Save the result to ai_diagnoses.
 *   7. Write one row to audit_logs.
 *   8. Convert to a DTO and return it.
 *
 * THE MOST IMPORTANT LINE IN THE WHOLE PHASE is the catch block in diagnose().
 * Because it is there, steps 6-8 always run, which is why POST /analyze returns
 * 200 OK with a real answer even with no API key and no internet.
 *
 * WHAT THIS CLASS DELIBERATELY DOES NOT DO
 * It does not decide whether the recommended action is allowed, and it does not
 * change the payment's status. A diagnosis is an opinion. Turning an opinion
 * into an action is the Policy Engine's job (Phase 6) and the simulator's job
 * (Phase 7). Keeping that boundary is the entire "AI recommends, rules decide"
 * story -- if this class ever called payment.setStatus(RECOVERED), the project
 * would have broken its own rule 12.
 */
@Service
public class AiDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(AiDiagnosisService.class);

    /** Event type written into audit_logs by this service. */
    private static final String EVENT_AI_DIAGNOSIS = "AI_DIAGNOSIS";

    private final PaymentRepository paymentRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AiResponseParser responseParser;
    private final FallbackClassifier fallbackClassifier;
    private final LlmConfig llmConfig;

    public AiDiagnosisService(PaymentRepository paymentRepository,
                             AiDiagnosisRepository aiDiagnosisRepository,
                             AuditLogRepository auditLogRepository,
                             AiPromptBuilder promptBuilder,
                             LlmClient llmClient,
                             AiResponseParser responseParser,
                             FallbackClassifier fallbackClassifier,
                             LlmConfig llmConfig) {
        this.paymentRepository = paymentRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
        this.auditLogRepository = auditLogRepository;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
        this.fallbackClassifier = fallbackClassifier;
        this.llmConfig = llmConfig;
    }

    /**
     * Analyse one payment and persist the result.
     *
     * @Transactional means the two saves below (diagnosis + audit row) either
     * both commit or both roll back. You never end up with a diagnosis that has
     * no audit trail, which for a financial audit log is the whole point.
     */
    @Transactional
    public AiDiagnosisResponseDto analyze(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Handled by GlobalExceptionHandler as a clean HTTP 400.
            throw new IllegalArgumentException(
                    "Payment " + paymentId + " succeeded and has no failure to diagnose.");
        }

        long startedAt = System.currentTimeMillis();
        ParsedDiagnosis diagnosis = diagnose(payment);
        long latencyMs = System.currentTimeMillis() - startedAt;

        AiDiagnosis saved = save(payment, diagnosis, latencyMs);
        writeAuditEntry(payment, diagnosis);

        log.info("[Diagnosis] {} -> {} / {} ({}% via {}, {}ms)",
                paymentId,
                diagnosis.getFailureCategory(),
                diagnosis.getRecommendedAction(),
                Math.round(diagnosis.getConfidence() * 100),
                diagnosis.getSource(),
                latencyMs);

        return toDto(payment, saved);
    }

    /**
     * Try the LLM; fall back to rules on any problem.
     *
     * Note the SECOND catch block. LlmUnavailableException is the expected
     * failure we designed for. RuntimeException is the one we did not predict
     * -- a Jackson quirk, a provider changing its response shape, anything.
     * Catching it too means an unknown bug degrades the feature instead of
     * returning HTTP 500 in front of an audience. We log it at error level so it
     * is still obvious in the console that something needs fixing.
     */
    private ParsedDiagnosis diagnose(Payment payment) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(payment);

            String rawReply = llmClient.complete(systemPrompt, userPrompt);
            return responseParser.parse(rawReply, llmConfig.getModel());

        } catch (LlmUnavailableException ex) {
            log.warn("[Diagnosis] LLM unavailable for {} ({}). Using fallback rules.",
                    payment.getPaymentId(), ex.getMessage());
            return fallbackClassifier.classify(payment);

        } catch (RuntimeException ex) {
            log.error("[Diagnosis] Unexpected error during AI diagnosis of {}. Using fallback rules.",
                    payment.getPaymentId(), ex);
            return fallbackClassifier.classify(payment);
        }
    }

    /** Latest stored diagnosis for a payment, without calling the LLM again. */
    @Transactional(readOnly = true)
    public Optional<AiDiagnosisResponseDto> getLatestDiagnosis(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId));

        return aiDiagnosisRepository
                .findFirstByPayment_PaymentIdOrderByCreatedAtDesc(paymentId)
                .map(diagnosis -> toDto(payment, diagnosis));
    }

    // ===== helpers =====

    private AiDiagnosis save(Payment payment, ParsedDiagnosis parsed, long latencyMs) {
        AiDiagnosis entity = new AiDiagnosis();
        entity.setPayment(payment);
        entity.setFailureCategory(parsed.getFailureCategory());
        entity.setProbableReason(parsed.getProbableReason());
        entity.setRecommendedAction(parsed.getRecommendedAction());
        entity.setConfidence(parsed.getConfidence());
        entity.setExplanation(parsed.getExplanation());
        entity.setAiSource(parsed.getSource());
        entity.setModelName(parsed.getModelName());
        entity.setLatencyMs(latencyMs);
        // createdAt is filled in by @PrePersist inside the entity.
        return aiDiagnosisRepository.save(entity);
    }

    /**
     * One audit row per diagnosis.
     *
     * policyDecision and finalAction are intentionally left null here: at this
     * point in the flow the policy engine has not run, and writing a guess into
     * an audit log would defeat its purpose. Phase 6 adds a second row with
     * eventType = "POLICY_DECISION" once a real verdict exists.
     */
    private void writeAuditEntry(Payment payment, ParsedDiagnosis parsed) {
        AuditLog entry = new AuditLog();
        entry.setPaymentId(payment.getPaymentId());
        entry.setEventType(EVENT_AI_DIAGNOSIS);
        entry.setFailureCategory(parsed.getFailureCategory());
        entry.setAiRecommendation(parsed.getRecommendedAction());
        entry.setConfidence(parsed.getConfidence());
        entry.setAiSource(parsed.getSource());
        entry.setResult("DIAGNOSED");
        entry.setDetails(buildAuditDetails(parsed));
        auditLogRepository.save(entry);
    }

    private String buildAuditDetails(ParsedDiagnosis parsed) {
        String details = "reason=" + parsed.getProbableReason()
                + "; model=" + parsed.getModelName();
        if (parsed.getSource() == AiSource.FALLBACK_RULES) {
            details = details + "; " + FallbackClassifier.NOTICE;
        }
        return details;
    }

    /** Entity -> API shape. See AiDiagnosisResponseDto for why we send both
     *  the raw enum name and a display label. */
    private AiDiagnosisResponseDto toDto(Payment payment, AiDiagnosis diagnosis) {
        AiDiagnosisResponseDto dto = new AiDiagnosisResponseDto();

        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setProvider(payment.getProvider());
        dto.setFailureCode(payment.getFailureCode());
        dto.setAttempts(payment.getAttempts());

        dto.setFailureCategory(diagnosis.getFailureCategory().name());
        dto.setFailureCategoryLabel(diagnosis.getFailureCategory().getDisplayName());
        dto.setProbableReason(diagnosis.getProbableReason());
        dto.setRecommendedAction(diagnosis.getRecommendedAction().name());
        dto.setRecommendedActionLabel(diagnosis.getRecommendedAction().getDisplayName());
        dto.setRecommendedActionDescription(diagnosis.getRecommendedAction().getDescription());

        double confidence = diagnosis.getConfidence() == null ? 0.0 : diagnosis.getConfidence();
        dto.setConfidence(confidence);
        dto.setConfidencePercent((int) Math.round(confidence * 100));
        dto.setExplanation(diagnosis.getExplanation());

        AiSource source = diagnosis.getAiSource();
        dto.setAiSource(source.name());
        dto.setAiAvailable(source.isAiAvailable());
        dto.setNotice(source == AiSource.FALLBACK_RULES ? FallbackClassifier.NOTICE : null);
        dto.setModelName(diagnosis.getModelName());
        dto.setLatencyMs(diagnosis.getLatencyMs());
        dto.setAnalyzedAt(diagnosis.getCreatedAt());

        return dto;
    }
}
