package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.entity.FailureCategory;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.RecoveryActionType;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * THE ONLY PLACE IN THE PROJECT THAT CONTAINS PROMPT TEXT.
 *
 * WHAT A PROMPT IS
 * A language model has no buttons or parameters for "classify this". The only
 * way to control it is the text you send. That text is the prompt. Writing it
 * is the actual engineering work in an LLM feature -- which is why it lives in
 * its own class instead of being buried inside a service method.
 *
 * WHY TWO PROMPTS (system + user)
 * Chat APIs take a list of messages, each with a role:
 *   - "system": standing instructions. Who you are, what format to answer in,
 *     what you are not allowed to do. Same on every single call.
 *   - "user":   the actual question. Different every call.
 * Separating them means the rules are stated once, clearly, and the model is
 * much less likely to drift out of format.
 *
 * WHY THE ALLOWED VALUES ARE BUILT FROM THE ENUMS
 * Look at allowedCategories() below -- it loops over FailureCategory.values().
 * If someone adds a new category to the enum next month, the prompt updates
 * itself. Hand-typing the list into the prompt text would silently rot.
 */
@Component
public class AiPromptBuilder {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Standing instructions. Note the three hard constraints:
     *  1. Only pick from OUR lists (keeps output machine-parseable).
     *  2. Return ONLY JSON (no prose, no markdown fences).
     *  3. You advise, you do not act (rule 12 of the project spec).
     */
    public String buildSystemPrompt() {
        return """
                You are a payment failure analyst for an Indian payment gateway.

                Your job is to look at ONE failed payment and produce a diagnosis.
                You are an ADVISOR ONLY. You never execute, retry, refund or move money.
                A separate deterministic policy engine decides whether your recommendation
                is actually allowed to run, and it can and will override you.

                Classify the failure into EXACTLY ONE of these categories:
                %s

                Recommend EXACTLY ONE of these actions:
                %s

                Guidance:
                - INSUFFICIENT_FUNDS and INVALID_PAYMENT_DETAILS cannot be fixed by
                  retrying the same payment. The customer has to do something.
                - BANK_TIMEOUT, NETWORK_ERROR and TEMPORARY_PROVIDER_FAILURE are
                  usually short-lived, so a retry is reasonable.
                - If the same payment has already been attempted several times, a
                  further blind retry is rarely the right advice.
                - If the evidence genuinely does not support a confident call, use
                  category UNKNOWN and action ESCALATE. Guessing is worse than
                  admitting uncertainty when money is involved.

                Reply with ONLY a JSON object, no explanation before or after it, no
                markdown code fences. Exactly these five keys:
                {
                  "failureCategory": "<one value from the category list>",
                  "probableReason": "<max 120 characters, technical cause>",
                  "recommendedAction": "<one value from the action list>",
                  "confidence": <number between 0 and 1, e.g. 0.91>,
                  "explanation": "<2 short sentences a non-technical merchant would understand>"
                }
                """.formatted(allowedCategories(), allowedActions());
    }

    /**
     * The facts of one specific payment. Only the fields listed in the project
     * spec go in -- amount, method, failure code, attempts, provider, timestamp,
     * customer context. No names, no card numbers, nothing sensitive: the model
     * does not need them and sending them would be a bad habit to learn.
     */
    public String buildUserPrompt(Payment payment) {
        return """
                Analyse this failed payment.

                Payment ID: %s
                Amount: INR %s
                Payment Method: %s
                Bank / Provider: %s
                Gateway Failure Code: %s
                Attempts So Far: %d
                Current Status: %s
                Attempted At: %s
                Customer Reference: %s
                """.formatted(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getProvider() == null ? "Unknown" : payment.getProvider(),
                payment.getFailureCode() == null ? "NOT_REPORTED" : payment.getFailureCode(),
                payment.getAttempts(),
                payment.getStatus(),
                payment.getCreatedAt() == null ? "Unknown" : payment.getCreatedAt().format(TIMESTAMP_FORMAT),
                payment.getCustomerId()
        );
    }

    /** e.g. "- BANK_TIMEOUT: Bank Timeout\n- NETWORK_ERROR: Network Error ..." */
    private String allowedCategories() {
        return Stream.of(FailureCategory.values())
                .map(c -> "- " + c.name() + ": " + c.getMerchantHint())
                .collect(Collectors.joining("\n"));
    }

    private String allowedActions() {
        return Stream.of(RecoveryActionType.values())
                .map(a -> "- " + a.name() + ": " + a.getDescription())
                .collect(Collectors.joining("\n"));
    }
}
