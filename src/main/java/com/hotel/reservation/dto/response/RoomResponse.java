package com.hotel.reservation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Long categoryId;
    private String categoryName;
    private BigDecimal pricePerNight;
    private Integer maxOccupancy;
    private boolean active;
    private String description;
    private String imageUrl;
    private List<String> amenities;
    private String bedType;
    private Integer sizeSqFt;
    private Double rating;
    private Integer reviewCount;
    private List<String> gallery;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String roomNumber, Long categoryId, String categoryName,
                        BigDecimal pricePerNight, Integer maxOccupancy, boolean active,
                        String description, String imageUrl, List<String> amenities) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.pricePerNight = pricePerNight;
        this.maxOccupancy = maxOccupancy;
        this.active = active;
        this.description = description;
        this.imageUrl = imageUrl;
        this.amenities = amenities;
        this.bedType = "1 King Bed";
        this.sizeSqFt = 400;
        this.rating = 4.8;
        this.reviewCount = 25;
    }

    public RoomResponse(Long id, String roomNumber, Long categoryId, String categoryName,
                        BigDecimal pricePerNight, Integer maxOccupancy, boolean active,
                        String description, String imageUrl, List<String> amenities,
                        String bedType, Integer sizeSqFt, Double rating, Integer reviewCount, List<String> gallery) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.pricePerNight = pricePerNight;
        this.maxOccupancy = maxOccupancy;
        this.active = active;
        this.description = description;
        this.imageUrl = imageUrl;
        this.amenities = amenities;
        this.bedType = bedType;
        this.sizeSqFt = sizeSqFt;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.gallery = gallery;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public Integer getSizeSqFt() {
        return sizeSqFt;
    }

    public void setSizeSqFt(Integer sizeSqFt) {
        this.sizeSqFt = sizeSqFt;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<String> getGallery() {
        return gallery;
    }

    public void setGallery(List<String> gallery) {
        this.gallery = gallery;
    }
}
