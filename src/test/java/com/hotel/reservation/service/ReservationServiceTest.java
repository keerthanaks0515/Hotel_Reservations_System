package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.ReservationRequest;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.entity.*;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.RoomNotAvailableException;
import com.hotel.reservation.exception.UnauthorizedException;
import com.hotel.reservation.repository.PaymentRepository;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import com.hotel.reservation.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private ReservationService reservationService;

    private User customer;
    private RoomCategory deluxeCategory;
    private Room room101;

    @BeforeEach
    void setUp() {
        customer = new User(1L, "Alice Johnson", "alice@example.com", "secretHash", "+1234567890", Role.ROLE_CUSTOMER, true);

        deluxeCategory = new RoomCategory(1L, "Deluxe Room", new BigDecimal("100.00"), 2, "Spacious room", "image.jpg");
        room101 = new Room(10L, "101", deluxeCategory, new BigDecimal("100.00"), 2, true, "Deluxe description", "image.jpg");
    }

    @Test
    @DisplayName("Should successfully create a reservation and calculate total cost with 12% tax")
    void testCreateReservation_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8); // 3 nights
        ReservationRequest request = new ReservationRequest(
                10L, checkIn, checkOut, 2, "Alice Johnson", "alice@example.com", "+1234567890", "Quiet room please"
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room101));
        when(reservationRepository.hasOverlappingReservation(eq(10L), eq(checkIn), eq(checkOut), isNull())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });
        when(roomService.mapToRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

        ReservationResponse response = reservationService.createReservation(request, "alice@example.com");

        assertNotNull(response);
        assertEquals(3, response.getNights());
        // Rate = 100.00, Subtotal = 300.00, Tax (12%) = 36.00, Total = 336.00
        assertEquals(new BigDecimal("100.00"), response.getRoomRate());
        assertEquals(new BigDecimal("300.00"), response.getSubtotal());
        assertEquals(new BigDecimal("36.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("336.00"), response.getTotalAmount());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertTrue(response.getBookingReference().startsWith("BK-"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject reservation when overlapping dates exist for the room")
    void testCreateReservation_OverlappingDates_ThrowsConflict() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        ReservationRequest request = new ReservationRequest(
                10L, checkIn, checkOut, 2, "Alice Johnson", "alice@example.com", "+1234567890", null
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room101));
        when(reservationRepository.hasOverlappingReservation(eq(10L), eq(checkIn), eq(checkOut), isNull())).thenReturn(true);

        assertThrows(RoomNotAvailableException.class, () ->
                reservationService.createReservation(request, "alice@example.com")
        );

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should reject reservation when check-out date is before or equal to check-in date")
    void testCreateReservation_InvalidDateOrder_ThrowsBadRequest() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        ReservationRequest request = new ReservationRequest(
                10L, checkIn, checkOut, 2, "Alice", "alice@example.com", null, null
        );

        assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "alice@example.com")
        );
    }

    @Test
    @DisplayName("Should reject reservation when check-in date is in the past")
    void testCreateReservation_PastCheckIn_ThrowsBadRequest() {
        LocalDate checkIn = LocalDate.now().minusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);
        ReservationRequest request = new ReservationRequest(
                10L, checkIn, checkOut, 2, "Alice", "alice@example.com", null, null
        );

        assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "alice@example.com")
        );
    }

    @Test
    @DisplayName("Should reject reservation when requested guests exceed maximum room capacity")
    void testCreateReservation_ExceedCapacity_ThrowsBadRequest() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(5);
        // Room 101 max capacity is 2, requesting 4
        ReservationRequest request = new ReservationRequest(
                10L, checkIn, checkOut, 4, "Alice", "alice@example.com", null, null
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room101));

        assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "alice@example.com")
        );
    }

    @Test
    @DisplayName("Should allow customer to cancel eligible future reservation")
    void testCancelReservation_Success() {
        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setBookingReference("BK-TEST-1234");
        reservation.setUser(customer);
        reservation.setRoom(room101);
        reservation.setCheckInDate(LocalDate.now().plusDays(3));
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(50L, "Travel plans changed", "alice@example.com", false);

        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        assertEquals("Travel plans changed", response.getCancellationReason());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    @DisplayName("Should reject customer cancellation attempt on another customer's reservation")
    void testCancelReservation_UnauthorizedCustomer_ThrowsForbidden() {
        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setUser(customer); // Belongs to Alice
        reservation.setCheckInDate(LocalDate.now().plusDays(3));
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        // Bob tries to cancel Alice's reservation
        assertThrows(UnauthorizedException.class, () ->
                reservationService.cancelReservation(50L, "Attempt", "bob@example.com", false)
        );
    }

    @Test
    @DisplayName("Should reject customer self-cancellation on or after check-in date per policy")
    void testCancelReservation_SameDayOrPast_ThrowsBadRequest() {
        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setUser(customer);
        reservation.setCheckInDate(LocalDate.now()); // Check-in is today
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        assertThrows(BadRequestException.class, () ->
                reservationService.cancelReservation(50L, "Late cancel", "alice@example.com", false)
        );
    }
}
