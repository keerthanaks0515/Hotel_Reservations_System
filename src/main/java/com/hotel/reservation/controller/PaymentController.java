package com.hotel.reservation.controller;

import com.hotel.reservation.dto.request.PaymentRequest;
import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.dto.response.ReservationResponse;
import com.hotel.reservation.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<ReservationResponse>> simulatePayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        ReservationResponse response = paymentService.processSimulatedPayment(request, userEmail, isAdmin);

        String message = request.isSimulateSuccess()
                ? "Simulated payment successful! Your reservation is confirmed."
                : "Simulated payment failed as requested for testing.";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
