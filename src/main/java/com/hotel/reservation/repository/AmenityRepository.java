package com.hotel.reservation.repository;

import com.hotel.reservation.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByName(String name);
    Set<Amenity> findByIdIn(Set<Long> ids);
}
