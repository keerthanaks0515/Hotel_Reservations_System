package com.hotel.reservation.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PaymentRequest {

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // e.g. "SIMULATED_CREDIT_CARD", "SIMULATED_UPI", "PAY_AT_CHECKIN"

    private boolean simulateSuccess = true;

    // Optional mock fields to display in UI only - never stored as sensitive real data
    private String dummyCardLast4;
    private String dummyUpiId;

    public PaymentRequest() {
    }

    public PaymentRequest(String bookingReference, String paymentMethod, boolean simulateSuccess) {
        this.bookingReference = bookingReference;
        this.paymentMethod = paymentMethod;
        this.simulateSuccess = simulateSuccess;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isSimulateSuccess() {
        return simulateSuccess;
    }

    public void setSimulateSuccess(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    public String getDummyCardLast4() {
        return dummyCardLast4;
    }

    public void setDummyCardLast4(String dummyCardLast4) {
        this.dummyCardLast4 = dummyCardLast4;
    }

    public String getDummyUpiId() {
        return dummyUpiId;
    }

    public void setDummyUpiId(String dummyUpiId) {
        this.dummyUpiId = dummyUpiId;
    }
}
