package com.hotel.reservation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public class RoomRequest {

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Room category is required")
    private Long categoryId;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "1.00", message = "Price per night must be at least 1.00")
    private BigDecimal pricePerNight;

    @NotNull(message = "Max occupancy is required")
    @Min(value = 1, message = "Max occupancy must be at least 1")
    private Integer maxOccupancy;

    private boolean active = true;

    private String description;

    private String imageUrl;

    private Set<Long> amenityIds;

    public RoomRequest() {
    }

    public RoomRequest(String roomNumber, Long categoryId, BigDecimal pricePerNight, Integer maxOccupancy,
                       boolean active, String description, String imageUrl, Set<Long> amenityIds) {
        this.roomNumber = roomNumber;
        this.categoryId = categoryId;
        this.pricePerNight = pricePerNight;
        this.maxOccupancy = maxOccupancy;
        this.active = active;
        this.description = description;
        this.imageUrl = imageUrl;
        this.amenityIds = amenityIds;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Set<Long> getAmenityIds() {
        return amenityIds;
    }

    public void setAmenityIds(Set<Long> amenityIds) {
        this.amenityIds = amenityIds;
    }
}
