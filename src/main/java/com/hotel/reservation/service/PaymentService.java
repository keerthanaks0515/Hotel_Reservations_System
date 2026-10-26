package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.PaymentRequest;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.entity.Payment;
import com.hotel.reservation.entity.PaymentStatus;
import com.hotel.reservation.entity.Reservation;
import com.hotel.reservation.entity.ReservationStatus;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.UnauthorizedException;
import com.hotel.reservation.repository.PaymentRepository;
import com.hotel.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          ReservationService reservationService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Transactional
    public ReservationResponse processSimulatedPayment(PaymentRequest request, String userEmail, boolean isAdmin) {
        Reservation reservation = reservationRepository.findByBookingReference(request.getBookingReference().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for reference: " + request.getBookingReference()));

        // Check ownership
        if (!isAdmin && !reservation.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedException("You are not authorized to process payment for this reservation.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled reservation.");
        }

        Payment payment = paymentRepository.findByReservationId(reservation.getId())
                .orElseGet(() -> {
                    Payment newPay = new Payment();
                    newPay.setReservation(reservation);
                    newPay.setAmount(reservation.getTotalAmount());
                    return newPay;
                });

        if (payment.getStatus() == PaymentStatus.SUCCESSFUL && reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new BadRequestException("This reservation is already paid and confirmed.");
        }

        String paymentRef = "PAY-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setPaymentReference(paymentRef);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionDate(LocalDateTime.now());

        if (request.isSimulateSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESSFUL);
            payment.setSimulationNotes("Simulated transaction approved successfully. Purely mock payment with zero real monetary exchange.");
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setSimulationNotes("Simulated transaction declined (customer triggered test failure scenario).");
            // reservation remains PENDING
        }

        paymentRepository.save(payment);
        Reservation savedReservation = reservationRepository.save(reservation);

        return reservationService.mapToReservationResponse(savedReservation);
    }
}
