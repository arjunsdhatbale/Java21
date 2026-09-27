package com.main.repo;

import com.main.model.entity.Payment;
import com.main.model.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    long countByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'PAID'")
    BigDecimal calculateTotalVolume();
}
