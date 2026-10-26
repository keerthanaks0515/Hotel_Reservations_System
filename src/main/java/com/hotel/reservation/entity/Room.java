package com.hotel.reservation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private RoomCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(nullable = false)
    private Integer maxOccupancy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String bedType = "1 King Bed";

    private Integer sizeSqFt = 400;

    private Double rating = 4.8;

    private Integer reviewCount = 35;

    @Column(length = 1500)
    private String galleryImages;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_amenities",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<Amenity> amenities = new HashSet<>();

    @Version
    private Long version;

    public Room() {
    }

    public Room(Long id, String roomNumber, RoomCategory category, BigDecimal pricePerNight,
                Integer maxOccupancy, boolean active, String description, String imageUrl) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.category = category;
        this.pricePerNight = pricePerNight;
        this.maxOccupancy = maxOccupancy;
        this.active = active;
        this.description = description;
        this.imageUrl = imageUrl;
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

    public RoomCategory getCategory() {
        return category;
    }

    public void setCategory(RoomCategory category) {
        this.category = category;
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

    public String getBedType() {
        return bedType != null ? bedType : "1 King Bed";
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public Integer getSizeSqFt() {
        return sizeSqFt != null ? sizeSqFt : 400;
    }

    public void setSizeSqFt(Integer sizeSqFt) {
        this.sizeSqFt = sizeSqFt;
    }

    public Double getRating() {
        return rating != null ? rating : 4.8;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount != null ? reviewCount : 25;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(String galleryImages) {
        this.galleryImages = galleryImages;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<Amenity> amenities) {
        this.amenities = amenities;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
