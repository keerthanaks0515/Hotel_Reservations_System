package com.hotel.reservation.service;

import com.hotel.reservation.dto.response.DashboardStatsResponse;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.entity.Payment;
import com.hotel.reservation.entity.PaymentStatus;
import com.hotel.reservation.entity.Reservation;
import com.hotel.reservation.entity.ReservationStatus;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.repository.PaymentRepository;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;

    public AdminService(ReservationRepository reservationRepository,
                        RoomRepository roomRepository,
                        PaymentRepository paymentRepository,
                        ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        long totalRooms = roomRepository.count();
        long activeRooms = roomRepository.countByActiveTrue();
        LocalDate today = LocalDate.now();

        long occupiedRoomsToday = reservationRepository.countOccupiedRoomsOnDate(today);
        long availableRoomsToday = Math.max(0, activeRooms - occupiedRoomsToday);

        stats.setTotalRooms(totalRooms);
        stats.setOccupiedRooms(occupiedRoomsToday);
        stats.setAvailableRooms(availableRoomsToday);

        long totalReservations = reservationRepository.count();
        long confirmed = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        long checkedIn = reservationRepository.countByStatus(ReservationStatus.CHECKED_IN);
        long cancelled = reservationRepository.countByStatus(ReservationStatus.CANCELLED);

        stats.setTotalReservations(totalReservations);
        stats.setConfirmedReservations(confirmed);
        stats.setCheckedInReservations(checkedIn);
        stats.setCancelledReservations(cancelled);

        BigDecimal revenue = reservationRepository.calculateTotalRevenue();
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        long checkInsToday = reservationRepository.countCheckInsOnDate(today);
        stats.setTodayCheckIns(checkInsToday);

        // Recent 10 reservations
        List<Reservation> allReservations = reservationRepository.searchReservations(null, null);
        List<ReservationResponse> recent = allReservations.stream()
                .limit(10)
                .map(reservationService::mapToReservationResponse)
                .collect(Collectors.toList());
        stats.setRecentReservations(recent);

        // Monthly revenue for the past 6 months
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = today.minusMonths(i);
            String label = monthDate.format(monthFormatter);
            monthlyRevenue.put(label, BigDecimal.ZERO);
        }

        // Populate monthly revenue from active reservations
        for (Reservation r : allReservations) {
            if (r.getStatus() != ReservationStatus.CANCELLED && r.getCreatedAt() != null) {
                String label = r.getCreatedAt().toLocalDate().format(monthFormatter);
                if (monthlyRevenue.containsKey(label)) {
                    monthlyRevenue.put(label, monthlyRevenue.get(label).add(r.getTotalAmount()));
                }
            }
        }
        stats.setMonthlyRevenue(monthlyRevenue);

        // Daily reservations for the past 7 days
        Map<String, Long> dailyReservations = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            dailyReservations.put(day.format(dayFormatter), 0L);
        }
        for (Reservation r : allReservations) {
            if (r.getCreatedAt() != null) {
                String label = r.getCreatedAt().toLocalDate().format(dayFormatter);
                if (dailyReservations.containsKey(label)) {
                    dailyReservations.put(label, dailyReservations.get(label) + 1);
                }
            }
        }
        stats.setDailyReservations(dailyReservations);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> searchReservations(ReservationStatus status, String searchTerm) {
        String query = (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm.trim() : null;
        return reservationRepository.searchReservations(status, query).stream()
                .map(reservationService::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatus newStatus, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));

        ReservationStatus currentStatus = reservation.getStatus();

        // Validated state transitions
        if (currentStatus == newStatus) {
            return reservationService.mapToReservationResponse(reservation);
        }

        if (currentStatus == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a cancelled reservation.");
        }
        if (currentStatus == ReservationStatus.CHECKED_OUT && newStatus != ReservationStatus.CHECKED_OUT) {
            throw new BadRequestException("Cannot modify a reservation that has already checked out.");
        }

        // Handle specific transitions
        if (newStatus == ReservationStatus.CANCELLED) {
            reservation.setCancellationReason(reason != null ? reason : "Cancelled by Admin");
            reservation.setCancelledAt(LocalDateTime.now());
            Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservation.getId());
            paymentOpt.ifPresent(p -> {
                if (p.getStatus() == PaymentStatus.SUCCESSFUL) {
                    p.setSimulationNotes("Cancelled by Admin. Refund processed in simulated environment.");
                    paymentRepository.save(p);
                }
            });
        }

        reservation.setStatus(newStatus);
        Reservation updated = reservationRepository.save(reservation);
        return reservationService.mapToReservationResponse(updated);
    }

    public void exportReservationsCsv(Writer writer) throws IOException {
        List<Reservation> reservations = reservationRepository.searchReservations(null, null);

        // CSV Header
        writer.write("Booking Reference,Guest Name,Guest Email,Guest Phone,Room Number,Category,Check-In Date,Check-Out Date,Nights,Guests,Room Rate,Subtotal,Tax,Total Amount,Status,Created At\n");

        for (Reservation r : reservations) {
            String roomNum = r.getRoom() != null ? r.getRoom().getRoomNumber() : "N/A";
            String catName = (r.getRoom() != null && r.getRoom().getCategory() != null) ? r.getRoom().getCategory().getName() : "N/A";

            writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,%.2f,%.2f,\"%s\",\"%s\"\n",
                    escapeCsv(r.getBookingReference()),
                    escapeCsv(r.getGuestName()),
                    escapeCsv(r.getGuestEmail()),
                    escapeCsv(r.getGuestPhone() != null ? r.getGuestPhone() : ""),
                    escapeCsv(roomNum),
                    escapeCsv(catName),
                    r.getCheckInDate(),
                    r.getCheckOutDate(),
                    r.getNights(),
                    r.getNumberOfGuests(),
                    r.getRoomRate(),
                    r.getSubtotal(),
                    r.getTaxAmount(),
                    r.getTotalAmount(),
                    r.getStatus().name(),
                    r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
            ));
        }
        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
