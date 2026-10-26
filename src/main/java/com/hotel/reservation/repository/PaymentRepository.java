package com.hotel.reservation.repository;

import com.hotel.reservation.entity.Payment;
import com.hotel.reservation.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findByReservationBookingReference(String bookingReference);
    long countByStatus(PaymentStatus status);
}
