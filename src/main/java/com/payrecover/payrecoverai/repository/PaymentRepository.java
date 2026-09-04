package com.payrecover.payrecoverai.repository;

import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface for us at
 * runtime -- we never write the SQL ourselves. JpaRepository<Payment, Long>
 * already gives us findAll(), findById(), save(), count(), deleteById(), etc.
 *
 * Below, any extra method we declare following Spring's naming convention
 * (findBy___, countBy___, existsBy___) is automatically turned into a query.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    long countByStatus(PaymentStatus status);

    boolean existsByPaymentId(String paymentId);
}
