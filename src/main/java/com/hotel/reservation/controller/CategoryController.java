package com.hotel.reservation.controller;

import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.entity.Amenity;
import com.hotel.reservation.entity.RoomCategory;
import com.hotel.reservation.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomCategory>>> getAllCategories() {
        List<RoomCategory> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/amenities")
    public ResponseEntity<ApiResponse<List<Amenity>>> getAllAmenities() {
        List<Amenity> amenities = categoryService.getAllAmenities();
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }
}
