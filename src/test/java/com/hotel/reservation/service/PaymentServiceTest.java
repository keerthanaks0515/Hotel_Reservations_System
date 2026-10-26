package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.PaymentRequest;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.entity.*;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.UnauthorizedException;
import com.hotel.reservation.repository.PaymentRepository;
import com.hotel.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private PaymentService paymentService;

    private User customer;
    private Room room;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        customer = new User(1L, "Charlie Brown", "charlie@example.com", "hash", "+11223344", Role.ROLE_CUSTOMER, true);
        RoomCategory category = new RoomCategory(1L, "Standard", new BigDecimal("80.00"), 2, "Desc", "url");
        room = new Room(5L, "105", category, new BigDecimal("80.00"), 2, true, "Standard room", "url");

        reservation = new Reservation();
        reservation.setId(200L);
        reservation.setBookingReference("BK-2026-CH77");
        reservation.setUser(customer);
        reservation.setRoom(room);
        reservation.setCheckInDate(LocalDate.now().plusDays(2));
        reservation.setCheckOutDate(LocalDate.now().plusDays(4));
        reservation.setTotalAmount(new BigDecimal("179.20"));
        reservation.setStatus(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("Should successfully confirm reservation upon successful simulated payment")
    void testProcessSimulatedPayment_Success() {
        PaymentRequest request = new PaymentRequest("BK-2026-CH77", "SIMULATED_CARD", true);

        when(reservationRepository.findByBookingReference("BK-2026-CH77")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(200L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationService.mapToReservationResponse(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            ReservationResponse resp = new ReservationResponse();
            resp.setBookingReference(r.getBookingReference());
            resp.setStatus(r.getStatus());
            return resp;
        });

        ReservationResponse response = paymentService.processSimulatedPayment(request, "charlie@example.com", false);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should leave reservation as PENDING when payment simulation is failed")
    void testProcessSimulatedPayment_FailedSimulation() {
        PaymentRequest request = new PaymentRequest("BK-2026-CH77", "SIMULATED_CARD", false); // simulate failure

        when(reservationRepository.findByBookingReference("BK-2026-CH77")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(200L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationService.mapToReservationResponse(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            ReservationResponse resp = new ReservationResponse();
            resp.setStatus(r.getStatus());
            return resp;
        });

        ReservationResponse response = paymentService.processSimulatedPayment(request, "charlie@example.com", false);

        assertNotNull(response);
        assertEquals(ReservationStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Should reject payment attempt from unauthorized user")
    void testProcessSimulatedPayment_UnauthorizedUser() {
        PaymentRequest request = new PaymentRequest("BK-2026-CH77", "SIMULATED_CARD", true);

        when(reservationRepository.findByBookingReference("BK-2026-CH77")).thenReturn(Optional.of(reservation));

        assertThrows(UnauthorizedException.class, () ->
                paymentService.processSimulatedPayment(request, "hacker@example.com", false)
        );
    }

    @Test
    @DisplayName("Should reject payment for cancelled reservation")
    void testProcessSimulatedPayment_CancelledReservation_ThrowsBadRequest() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        PaymentRequest request = new PaymentRequest("BK-2026-CH77", "SIMULATED_CARD", true);

        when(reservationRepository.findByBookingReference("BK-2026-CH77")).thenReturn(Optional.of(reservation));

        assertThrows(BadRequestException.class, () ->
                paymentService.processSimulatedPayment(request, "charlie@example.com", false)
        );
    }
}
