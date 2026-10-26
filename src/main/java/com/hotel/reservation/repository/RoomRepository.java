package com.hotel.reservation.repository;

import com.hotel.reservation.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByActiveTrue();

    @Query("SELECT r FROM Room r WHERE r.active = true " +
           "AND r.maxOccupancy >= :guests " +
           "AND (:categoryId IS NULL OR r.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
           "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice) " +
           "AND r.id NOT IN (" +
           "  SELECT res.room.id FROM Reservation res " +
           "  WHERE res.status != com.hotel.reservation.entity.ReservationStatus.CANCELLED " +
           "  AND res.checkInDate < :checkOutDate " +
           "  AND res.checkOutDate > :checkInDate" +
           ")")
    List<Room> findAvailableRooms(
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("guests") Integer guests,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    long countByActiveTrue();
    long countByActiveFalse();
}
