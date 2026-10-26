package com.hotel.reservation.repository;

import com.hotel.reservation.entity.RoomCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    Optional<RoomCategory> findByName(String name);
    boolean existsByName(String name);
}
