package com.hotel.reservation.controller;

import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> searchRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Integer adults,
            @RequestParam(required = false) Integer children,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String bedType,
            @RequestParam(required = false) String sortBy) {

        int effectiveGuests = (guests != null && guests > 0) ? guests : 1;
        if (adults != null && adults > 0) {
            effectiveGuests = adults + (children != null ? children : 0);
        }

        List<RoomResponse> rooms = roomService.getAvailableRooms(
                checkInDate, checkOutDate, effectiveGuests, categoryId, minPrice, maxPrice, bedType, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success(rooms, "Available rooms retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getSimilarRooms(@PathVariable Long id) {
        List<RoomResponse> similar = roomService.getSimilarRooms(id);
        return ResponseEntity.ok(ApiResponse.success(similar));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getFeaturedRooms() {
        List<RoomResponse> rooms = roomService.getFeaturedRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
