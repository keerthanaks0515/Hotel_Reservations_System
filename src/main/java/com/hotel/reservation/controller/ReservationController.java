package com.hotel.reservation.controller;

import com.hotel.reservation.dto.request.ReservationRequest;
import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.entity.ReservationStatus;
import com.hotel.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        ReservationResponse response = reservationService.createReservation(request, userEmail);
        return ResponseEntity.ok(ApiResponse.success(response, "Reservation created successfully. Please proceed to payment."));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMyReservations(
            @RequestParam(required = false) ReservationStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        List<ReservationResponse> response = reservationService.getUserReservations(userEmail, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/lookup/{reference}")
    public ResponseEntity<ApiResponse<ReservationResponse>> lookupReservation(
            @PathVariable String reference,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = (userDetails != null) ? userDetails.getUsername() : null;
        boolean isAdmin = (userDetails != null) && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        ReservationResponse response = reservationService.getReservationByReference(reference, userEmail, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String reason = (body != null) ? body.get("reason") : null;
        ReservationResponse response = reservationService.cancelReservation(id, reason, userEmail, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response, "Reservation cancelled successfully."));
    }
}
