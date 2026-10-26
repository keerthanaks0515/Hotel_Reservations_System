package com.hotel.reservation.dto.request;

import com.hotel.reservation.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {

    @NotNull(message = "New reservation status is required")
    private ReservationStatus status;

    private String reason;

    public StatusUpdateRequest() {
    }

    public StatusUpdateRequest(ReservationStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
