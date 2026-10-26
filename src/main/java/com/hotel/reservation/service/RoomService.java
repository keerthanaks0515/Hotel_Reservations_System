package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.RoomRequest;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.entity.Amenity;
import com.hotel.reservation.entity.Room;
import com.hotel.reservation.entity.RoomCategory;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.repository.AmenityRepository;
import com.hotel.reservation.repository.RoomCategoryRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomCategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomCategoryRepository categoryRepository,
                       AmenityRepository amenityRepository) {
        this.roomRepository = roomRepository;
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkInDate,
                                                LocalDate checkOutDate,
                                                Integer guests,
                                                Long categoryId,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice,
                                                String bedType,
                                                String sortBy) {
        LocalDate effectiveCheckIn = (checkInDate != null) ? checkInDate : LocalDate.now();
        LocalDate effectiveCheckOut = (checkOutDate != null) ? checkOutDate : effectiveCheckIn.plusDays(1);
        int effectiveGuests = (guests != null && guests > 0) ? guests : 1;

        if (effectiveCheckOut.isBefore(effectiveCheckIn) || effectiveCheckOut.isEqual(effectiveCheckIn)) {
            throw new BadRequestException("Check-out date must be strictly after check-in date.");
        }

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                effectiveCheckIn,
                effectiveCheckOut,
                effectiveGuests,
                categoryId,
                minPrice,
                maxPrice
        );

        List<RoomResponse> responses = availableRooms.stream()
                .filter(r -> bedType == null || bedType.trim().isEmpty() || r.getBedType().toLowerCase().contains(bedType.toLowerCase()))
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparing(RoomResponse::getPricePerNight));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparing(RoomResponse::getPricePerNight).reversed());
        } else if ("rating_desc".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparing(RoomResponse::getRating).reversed());
        } else if ("capacity_desc".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparing(RoomResponse::getMaxOccupancy).reversed());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkInDate,
                                                LocalDate checkOutDate,
                                                Integer guests,
                                                Long categoryId,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice) {
        return getAvailableRooms(checkInDate, checkOutDate, guests, categoryId, minPrice, maxPrice, null, null);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getSimilarRooms(Long roomId) {
        Room current = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        return roomRepository.findByActiveTrue().stream()
                .filter(r -> !r.getId().equals(roomId))
                .filter(r -> (r.getCategory() != null && current.getCategory() != null && r.getCategory().getId().equals(current.getCategory().getId()))
                        || Math.abs(r.getMaxOccupancy() - current.getMaxOccupancy()) <= 1)
                .limit(3)
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
        return mapToRoomResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getFeaturedRooms() {
        return roomRepository.findByActiveTrue().stream()
                .limit(6)
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber().trim())) {
            throw new BadRequestException("Room with number " + request.getRoomNumber() + " already exists.");
        }

        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Room category not found with ID: " + request.getCategoryId()));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setCategory(category);
        room.setPricePerNight(request.getPricePerNight());
        room.setMaxOccupancy(request.getMaxOccupancy());
        room.setActive(request.isActive());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = amenityRepository.findByIdIn(request.getAmenityIds());
            room.setAmenities(amenities);
        }

        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));

        if (!room.getRoomNumber().equalsIgnoreCase(request.getRoomNumber().trim()) &&
                roomRepository.existsByRoomNumber(request.getRoomNumber().trim())) {
            throw new BadRequestException("Room with number " + request.getRoomNumber() + " already exists.");
        }

        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Room category not found with ID: " + request.getCategoryId()));

        room.setRoomNumber(request.getRoomNumber().trim());
        room.setCategory(category);
        room.setPricePerNight(request.getPricePerNight());
        room.setMaxOccupancy(request.getMaxOccupancy());
        room.setActive(request.isActive());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());

        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = amenityRepository.findByIdIn(request.getAmenityIds());
            room.setAmenities(amenities);
        } else {
            room.setAmenities(new HashSet<>());
        }

        Room updated = roomRepository.save(room);
        return mapToRoomResponse(updated);
    }

    @Transactional
    public RoomResponse toggleRoomActive(Long id, boolean active) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
        room.setActive(active);
        return mapToRoomResponse(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
        roomRepository.delete(room);
    }

    public RoomResponse mapToRoomResponse(Room room) {
        List<String> amenityNames = (room.getAmenities() != null)
                ? room.getAmenities().stream().map(Amenity::getName).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> gallery;
        if (room.getGalleryImages() != null && !room.getGalleryImages().trim().isEmpty()) {
            gallery = Arrays.stream(room.getGalleryImages().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        } else {
            gallery = Arrays.asList(
                    room.getImageUrl(),
                    "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80",
                    "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80"
            );
        }

        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getCategory() != null ? room.getCategory().getId() : null,
                room.getCategory() != null ? room.getCategory().getName() : "Uncategorized",
                room.getPricePerNight(),
                room.getMaxOccupancy(),
                room.isActive(),
                room.getDescription(),
                room.getImageUrl(),
                amenityNames,
                room.getBedType(),
                room.getSizeSqFt(),
                room.getRating(),
                room.getReviewCount(),
                gallery
        );
    }
}
