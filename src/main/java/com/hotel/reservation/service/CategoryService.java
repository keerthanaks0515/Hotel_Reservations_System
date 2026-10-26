package com.hotel.reservation.service;

import com.hotel.reservation.entity.Amenity;
import com.hotel.reservation.entity.RoomCategory;
import com.hotel.reservation.repository.AmenityRepository;
import com.hotel.reservation.repository.RoomCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final RoomCategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;

    public CategoryService(RoomCategoryRepository categoryRepository, AmenityRepository amenityRepository) {
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }
}
