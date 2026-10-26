package com.hotel.reservation.controller;

import com.hotel.reservation.dto.request.RoomRequest;
import com.hotel.reservation.dto.request.StatusUpdateRequest;
import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.dto.response.DashboardStatsResponse;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.entity.ReservationStatus;
import com.hotel.reservation.service.AdminService;
import com.hotel.reservation.service.RoomService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final RoomService roomService;

    public AdminController(AdminService adminService, RoomService roomService) {
        this.adminService = adminService;
        this.roomService = roomService;
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.createRoom(request);
        return ResponseEntity.ok(ApiResponse.success(room, "Room created successfully"));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success(room, "Room updated successfully"));
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<ApiResponse<RoomResponse>> toggleRoomStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        RoomResponse room = roomService.toggleRoomActive(id, active);
        return ResponseEntity.ok(ApiResponse.success(room, "Room status updated successfully"));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Room deleted successfully"));
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> searchReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String search) {

        List<ReservationResponse> reservations = adminService.searchReservations(status, search);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @PatchMapping("/reservations/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {

        ReservationResponse updated = adminService.updateReservationStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(updated, "Reservation status updated to " + request.getStatus()));
    }

    @GetMapping("/reports/reservations/export-csv")
    public void exportReservationsCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reservations_report.csv\"");
        adminService.exportReservationsCsv(response.getWriter());
    }
}
