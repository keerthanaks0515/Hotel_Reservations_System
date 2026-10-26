package com.hotel.reservation.controller;

import com.hotel.reservation.dto.request.LoginRequest;
import com.hotel.reservation.dto.request.RegisterRequest;
import com.hotel.reservation.dto.response.ApiResponse;
import com.hotel.reservation.dto.response.AuthResponse;
import com.hotel.reservation.entity.Role;
import com.hotel.reservation.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should register new user and return JWT auth token")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("David Warner", "david@example.com", "Password@123", "+15551234");
        AuthResponse authResponse = new AuthResponse("mock-jwt-token", 10L, "david@example.com", "David Warner", Role.ROLE_CUSTOMER);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.register(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("mock-jwt-token", response.getBody().getData().getToken());
    }

    @Test
    @DisplayName("Should authenticate valid credentials and return JWT auth token")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("david@example.com", "Password@123");
        AuthResponse authResponse = new AuthResponse("mock-jwt-token", 10L, "david@example.com", "David Warner", Role.ROLE_CUSTOMER);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("david@example.com", response.getBody().getData().getEmail());
    }
}
