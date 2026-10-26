package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.ReservationRequest;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.entity.*;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.RoomNotAvailableException;
import com.hotel.reservation.exception.UnauthorizedException;
import com.hotel.reservation.repository.PaymentRepository;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import com.hotel.reservation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.12"); // 12% Hotel GST/Tax

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RoomService roomService;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository,
                              PaymentRepository paymentRepository,
                              RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.roomService = roomService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createReservation(ReservationRequest request, String userEmail) {
        // Rule 1: Check-out must be after check-in
        if (request.getCheckOutDate().isBefore(request.getCheckInDate()) ||
                request.getCheckOutDate().isEqual(request.getCheckInDate())) {
            throw new BadRequestException("Check-out date must be strictly after check-in date.");
        }

        // Rule 1b: Check-in cannot be in the past
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in date cannot be in the past.");
        }

        // Fetch User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Fetch Room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        if (!room.isActive()) {
            throw new BadRequestException("The selected room is currently unavailable for booking.");
        }

        // Rule 2: Guest count must be valid for the selected room
        if (request.getNumberOfGuests() < 1) {
            throw new BadRequestException("Number of guests must be at least 1.");
        }
        if (request.getNumberOfGuests() > room.getMaxOccupancy()) {
            throw new BadRequestException("The selected room has a maximum capacity of " +
                    room.getMaxOccupancy() + " guests (requested: " + request.getNumberOfGuests() + ").");
        }

        // Rule 3 & 4: Strict Server-Side Overlap & Concurrency Check
        boolean isOverlapping = reservationRepository.hasOverlappingReservation(
                room.getId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                null
        );

        if (isOverlapping) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() +
                    " is no longer available for the dates " + request.getCheckInDate() + " to " + request.getCheckOutDate() + ".");
        }

        // Rule 5: Backend Pricing Calculation (never trust client amounts)
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            throw new BadRequestException("Reservation duration must be at least 1 night.");
        }

        BigDecimal roomRate = room.getPricePerNight();
        BigDecimal subtotal = roomRate.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // Generate unique booking reference
        String bookingReference = generateUniqueBookingReference();

        Reservation reservation = new Reservation();
        reservation.setBookingReference(bookingReference);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfGuests(request.getNumberOfGuests());
        reservation.setNights((int) nights);
        reservation.setRoomRate(roomRate);
        reservation.setSubtotal(subtotal);
        reservation.setTaxAmount(taxAmount);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setSpecialRequests(request.getSpecialRequests());
        reservation.setGuestName(request.getGuestName().trim());
        reservation.setGuestEmail(request.getGuestEmail().trim());
        reservation.setGuestPhone(request.getGuestPhone());

        Reservation savedReservation = reservationRepository.save(reservation);

        // Create initial pending payment record
        Payment initialPayment = new Payment();
        initialPayment.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        initialPayment.setReservation(savedReservation);
        initialPayment.setAmount(totalAmount);
        initialPayment.setPaymentMethod("PENDING");
        initialPayment.setStatus(PaymentStatus.PENDING);
        initialPayment.setTransactionDate(LocalDateTime.now());
        initialPayment.setSimulationNotes("Awaiting customer payment simulation");
        paymentRepository.save(initialPayment);

        return mapToReservationResponse(savedReservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByReference(String bookingReference, String userEmail, boolean isAdmin) {
        Reservation reservation = reservationRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with reference: " + bookingReference));

        // Rule 6: Customer isolation - customers can only view their own reservations
        if (!isAdmin && userEmail != null && !reservation.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedException("You are not authorized to view this reservation.");
        }

        return mapToReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(String userEmail, ReservationStatus status) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<Reservation> list;
        if (status != null) {
            list = reservationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        } else {
            list = reservationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        return list.stream().map(this::mapToReservationResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, String reason, String userEmail, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        // Rule 6: Customer isolation
        if (!isAdmin && !reservation.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedException("You are not authorized to cancel this reservation.");
        }

        // Validate status
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled.");
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BadRequestException("Cannot cancel a reservation that has already checked in or checked out.");
        }

        // Rule 9: Cancellation Policy
        // Free cancellation is permitted up to 24 hours prior to check-in date
        if (!isAdmin) {
            if (LocalDate.now().isAfter(reservation.getCheckInDate()) ||
                    LocalDate.now().isEqual(reservation.getCheckInDate())) {
                throw new BadRequestException("Reservations cannot be self-cancelled on or after the scheduled check-in date. Please contact hotel support.");
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason((reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Cancelled by guest");
        reservation.setCancelledAt(LocalDateTime.now());

        // Update payment notes if already paid
        Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservation.getId());
        paymentOpt.ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.SUCCESSFUL) {
                p.setSimulationNotes("Reservation cancelled. Simulated full refund processed to customer.");
                paymentRepository.save(p);
            }
        });

        Reservation updated = reservationRepository.save(reservation);
        return mapToReservationResponse(updated);
    }

    public ReservationResponse mapToReservationResponse(Reservation res) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(res.getId());
        dto.setBookingReference(res.getBookingReference());
        dto.setUserId(res.getUser().getId());

        RoomResponse roomResponse = roomService.mapToRoomResponse(res.getRoom());
        dto.setRoom(roomResponse);

        dto.setCheckInDate(res.getCheckInDate());
        dto.setCheckOutDate(res.getCheckOutDate());
        dto.setNumberOfGuests(res.getNumberOfGuests());
        dto.setNights(res.getNights());
        dto.setRoomRate(res.getRoomRate());
        dto.setSubtotal(res.getSubtotal());
        dto.setTaxAmount(res.getTaxAmount());
        dto.setTotalAmount(res.getTotalAmount());
        dto.setStatus(res.getStatus());
        dto.setSpecialRequests(res.getSpecialRequests());
        dto.setGuestName(res.getGuestName());
        dto.setGuestEmail(res.getGuestEmail());
        dto.setGuestPhone(res.getGuestPhone());
        dto.setCreatedAt(res.getCreatedAt());
        dto.setCancellationReason(res.getCancellationReason());
        dto.setCancelledAt(res.getCancelledAt());

        paymentRepository.findByReservationId(res.getId()).ifPresent(p -> {
            dto.setPaymentStatus(p.getStatus());
            dto.setPaymentMethod(p.getPaymentMethod());
            dto.setPaymentReference(p.getPaymentReference());
        });

        return dto;
    }

    private String generateUniqueBookingReference() {
        String reference;
        do {
            String shortCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            reference = "BK-" + LocalDate.now().getYear() + "-" + shortCode;
        } while (reservationRepository.findByBookingReference(reference).isPresent());
        return reference;
    }
}
