package com.hotel.reservation.service;

import com.hotel.reservation.dto.request.RoomRequest;
import com.hotel.reservation.dto.response.RoomResponse;
import com.hotel.reservation.entity.Room;
import com.hotel.reservation.entity.RoomCategory;
import com.hotel.reservation.exception.BadRequestException;
import com.hotel.reservation.repository.AmenityRepository;
import com.hotel.reservation.repository.RoomCategoryRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomCategoryRepository categoryRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @InjectMocks
    private RoomService roomService;

    private RoomCategory category;
    private Room room;

    @BeforeEach
    void setUp() {
        category = new RoomCategory(1L, "Deluxe", new BigDecimal("120.00"), 2, "Desc", "url");
        room = new Room(10L, "201", category, new BigDecimal("120.00"), 2, true, "Desc", "url");
    }

    @Test
    @DisplayName("Should return available rooms when valid date parameters are provided")
    void testGetAvailableRooms_Success() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);

        when(roomRepository.findAvailableRooms(eq(in), eq(out), eq(2), isNull(), isNull(), isNull()))
                .thenReturn(Collections.singletonList(room));

        List<RoomResponse> result = roomService.getAvailableRooms(in, out, 2, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("201", result.get(0).getRoomNumber());
    }

    @Test
    @DisplayName("Should reject search when check-out is before or equal to check-in")
    void testGetAvailableRooms_InvalidDateRange_ThrowsBadRequest() {
        LocalDate in = LocalDate.now().plusDays(5);
        LocalDate out = LocalDate.now().plusDays(5); // Same day

        assertThrows(BadRequestException.class, () ->
                roomService.getAvailableRooms(in, out, 1, null, null, null)
        );
    }

    @Test
    @DisplayName("Should reject room creation when room number already exists")
    void testCreateRoom_DuplicateNumber_ThrowsBadRequest() {
        RoomRequest request = new RoomRequest("201", 1L, new BigDecimal("150.00"), 2, true, "Desc", "url", null);

        when(roomRepository.existsByRoomNumber("201")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> roomService.createRoom(request));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should successfully create a new room with valid details")
    void testCreateRoom_Success() {
        RoomRequest request = new RoomRequest("305", 1L, new BigDecimal("180.00"), 3, true, "New suite", "url", null);

        when(roomRepository.existsByRoomNumber("305")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
            Room saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        RoomResponse response = roomService.createRoom(request);

        assertNotNull(response);
        assertEquals("305", response.getRoomNumber());
        assertEquals(new BigDecimal("180.00"), response.getPricePerNight());
        assertTrue(response.isActive());
    }
}
