package com.hotel.reservation.repository;

import com.hotel.reservation.entity.Reservation;
import com.hotel.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByBookingReference(String bookingReference);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Reservation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReservationStatus status);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status != com.hotel.reservation.entity.ReservationStatus.CANCELLED " +
           "AND r.checkInDate < :checkOutDate " +
           "AND r.checkOutDate > :checkInDate " +
           "AND (:excludeId IS NULL OR r.id != :excludeId)")
    boolean hasOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT r FROM Reservation r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:searchTerm IS NULL OR LOWER(r.bookingReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           " OR LOWER(r.guestName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           " OR LOWER(r.guestEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           " OR LOWER(r.room.roomNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> searchReservations(@Param("status") ReservationStatus status,
                                         @Param("searchTerm") String searchTerm);

    long countByStatus(ReservationStatus status);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r WHERE r.status != com.hotel.reservation.entity.ReservationStatus.CANCELLED")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(DISTINCT r.room.id) FROM Reservation r " +
           "WHERE r.status IN (com.hotel.reservation.entity.ReservationStatus.CONFIRMED, com.hotel.reservation.entity.ReservationStatus.CHECKED_IN) " +
           "AND :date >= r.checkInDate AND :date < r.checkOutDate")
    long countOccupiedRoomsOnDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.status IN (com.hotel.reservation.entity.ReservationStatus.CONFIRMED, com.hotel.reservation.entity.ReservationStatus.CHECKED_IN) " +
           "AND r.checkInDate = :date")
    long countCheckInsOnDate(@Param("date") LocalDate date);
}
