package com.payrecover.payrecoverai.repository;

import com.payrecover.payrecoverai.entity.AiDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Queries over the ai_diagnoses table.
 *
 * NOTE ON THE UNDERSCORE IN THE METHOD NAMES
 * "findByPayment_PaymentId" reads as: navigate from AiDiagnosis to its
 * `payment` field, then into that Payment's `paymentId` field. The underscore
 * is Spring Data's explicit "step into the related entity" separator. Without
 * it, Spring would try to find a single field literally called
 * "paymentPaymentId" on AiDiagnosis and fail at startup.
 *
 * Spring generates the SQL (a JOIN) for us -- we write no SQL at all.
 */
public interface AiDiagnosisRepository extends JpaRepository<AiDiagnosis, Long> {

    /** Every diagnosis ever made for one payment, newest first. */
    List<AiDiagnosis> findByPayment_PaymentIdOrderByCreatedAtDesc(String paymentId);

    /**
     * Just the most recent diagnosis for one payment.
     * "First" tells Spring to add LIMIT 1 to the query.
     * Optional<> because a payment may never have been analysed.
     */
    Optional<AiDiagnosis> findFirstByPayment_PaymentIdOrderByCreatedAtDesc(String paymentId);

    /**
     * All diagnoses, newest first. Used to build the "AI Recommendation" column
     * of the failed-payments table in one query instead of one query per row.
     */
    List<AiDiagnosis> findAllByOrderByCreatedAtDesc();

    /**
     * Same as above, but the related Payment is loaded in the SAME query.
     *
     * WHY "JOIN FETCH" MATTERS
     * AiDiagnosis.payment is FetchType.LAZY, so normally Hibernate would only
     * load it when you call getPayment() -- and if that happens after the
     * database session has closed you get a LazyInitializationException, one of
     * the classic JPA errors. "join fetch" tells Hibernate to bring the payment
     * along immediately, which both avoids that error and turns N+1 queries
     * into one.
     *
     * @Query lets us write JPQL (SQL-like, but over entity names not table
     * names) when a method name alone cannot express what we need.
     */
    @Query("select d from AiDiagnosis d join fetch d.payment order by d.createdAt desc")
    List<AiDiagnosis> findAllWithPaymentNewestFirst();
}
