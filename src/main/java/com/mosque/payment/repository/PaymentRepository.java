package com.mosque.payment.repository;

import com.mosque.payment.data.model.Payment;
import com.mosque.payment.data.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findAllByOrderByCreatedAtDesc();

    /** Rows of [PaymentType, SUM(amount)] for payments in the given status. */
    @Query("select p.paymentType, sum(p.amount) from Payment p where p.status = :status group by p.paymentType")
    List<Object[]> sumByTypeForStatus(@Param("status") PaymentStatus status);
}
