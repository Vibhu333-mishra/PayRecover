package com.payrecover.payrecoverai.repository;

import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    long countByStatus(PaymentStatus status);

    boolean existsByPaymentId(String paymentId);
}
