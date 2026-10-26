package com.hotel.reservation.config;

import com.hotel.reservation.entity.*;
import com.hotel.reservation.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoomCategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoomCategoryRepository categoryRepository,
                           AmenityRepository amenityRepository,
                           RoomRepository roomRepository,
                           ReservationRepository reservationRepository,
                           PaymentRepository paymentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Database already initialized. Skipping seeding.");
            return;
        }

        logger.info("Initializing database with sample development and demo data...");

        // 1. Users
        User admin = new User();
        admin.setFullName("Hotel Administrator");
        admin.setEmail("admin@hotel.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setPhone("+1-555-0199");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        User customer = new User();
        customer.setFullName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPassword(passwordEncoder.encode("Customer@123"));
        customer.setPhone("+1-555-0142");
        customer.setRole(Role.ROLE_CUSTOMER);
        customer.setEnabled(true);
        userRepository.save(customer);

        // 2. Categories
        RoomCategory standard = new RoomCategory(
                null,
                "Standard Room",
                new BigDecimal("89.00"),
                2,
                "Comfortable and cozy room designed for short stays and budget-conscious travelers with essential modern amenities.",
                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80"
        );
        RoomCategory deluxe = new RoomCategory(
                null,
                "Deluxe Room",
                new BigDecimal("149.00"),
                2,
                "Spacious deluxe room featuring plush bedding, modern workstation, marble bath, and serene city skyline views.",
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80"
        );
        RoomCategory executive = new RoomCategory(
                null,
                "Executive Suite",
                new BigDecimal("239.00"),
                3,
                "Luxurious suite with private lounge, king-size master bed, walk-in closet, and panoramic city terrace.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80"
        );
        RoomCategory family = new RoomCategory(
                null,
                "Family Suite",
                new BigDecimal("319.00"),
                5,
                "Generously proportioned family sanctuary with two interconnected bedrooms, entertainment lounge, and dining area.",
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80"
        );

        standard = categoryRepository.save(standard);
        deluxe = categoryRepository.save(deluxe);
        executive = categoryRepository.save(executive);
        family = categoryRepository.save(family);

        // 3. Amenities
        Amenity wifi = amenityRepository.save(new Amenity(null, "High-Speed Wi-Fi", "fa-wifi"));
        Amenity ac = amenityRepository.save(new Amenity(null, "Climate Control AC", "fa-snowflake"));
        Amenity tv = amenityRepository.save(new Amenity(null, "55\" 4K Smart TV", "fa-tv"));
        Amenity minibar = amenityRepository.save(new Amenity(null, "Mini Bar & Espresso", "fa-coffee"));
        Amenity breakfast = amenityRepository.save(new Amenity(null, "Artisan Breakfast Included", "fa-utensils"));
        Amenity pool = amenityRepository.save(new Amenity(null, "Infinity Pool Access", "fa-water"));
        Amenity balcony = amenityRepository.save(new Amenity(null, "Private Skyline Balcony", "fa-mountain-sun"));

        // 4. Rooms
        Room r101 = new Room(null, "101", standard, new BigDecimal("89.00"), 2, true,
                "Well-appointed standard room on the 1st floor featuring twin or queen bed.",
                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        r101.setBedType("1 Queen Bed");
        r101.setSizeSqFt(340);
        r101.setRating(4.7);
        r101.setReviewCount(28);
        r101.setGalleryImages("https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80");
        r101.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv)));
        roomRepository.save(r101);

        Room r102 = new Room(null, "102", standard, new BigDecimal("95.00"), 2, true,
                "Quiet standard room with courtyard view and ergonomic workspace.",
                "https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=800&q=80");
        r102.setBedType("2 Twin Beds");
        r102.setSizeSqFt(360);
        r102.setRating(4.8);
        r102.setReviewCount(34);
        r102.setGalleryImages("https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80");
        r102.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv)));
        roomRepository.save(r102);

        Room r201 = new Room(null, "201", deluxe, new BigDecimal("149.00"), 2, true,
                "Corner deluxe room with floor-to-ceiling glass windows and king bed.",
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80");
        r201.setBedType("1 King Bed");
        r201.setSizeSqFt(480);
        r201.setRating(4.9);
        r201.setReviewCount(52);
        r201.setGalleryImages("https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80");
        r201.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv, minibar, breakfast)));
        roomRepository.save(r201);

        Room r202 = new Room(null, "202", deluxe, new BigDecimal("159.00"), 2, true,
                "Premium deluxe room with soaking tub and panoramic garden vistas.",
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80");
        r202.setBedType("1 King Bed");
        r202.setSizeSqFt(520);
        r202.setRating(4.9);
        r202.setReviewCount(47);
        r202.setGalleryImages("https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80");
        r202.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv, minibar, breakfast, pool)));
        roomRepository.save(r202);

        Room r301 = new Room(null, "301", executive, new BigDecimal("239.00"), 3, true,
                "Top-tier executive suite with dedicated dining table, deep-soak tub, and terrace.",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        r301.setBedType("1 California King");
        r301.setSizeSqFt(750);
        r301.setRating(5.0);
        r301.setReviewCount(89);
        r301.setGalleryImages("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80");
        r301.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv, minibar, breakfast, pool, balcony)));
        roomRepository.save(r301);

        Room r401 = new Room(null, "401", family, new BigDecimal("319.00"), 5, true,
                "Spacious multi-room suite suited for families, complete with kitchenette and lounge.",
                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80");
        r401.setBedType("2 King Beds + 1 Sofa Bed");
        r401.setSizeSqFt(980);
        r401.setRating(4.9);
        r401.setReviewCount(64);
        r401.setGalleryImages("https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80,https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80");
        r401.setAmenities(new HashSet<>(Arrays.asList(wifi, ac, tv, minibar, breakfast, pool, balcony)));
        roomRepository.save(r401);

        // 5. Seed Initial Demo Reservations for Rich Dashboard Analytics
        Reservation res1 = new Reservation();
        res1.setBookingReference("BK-DEMO-701A");
        res1.setUser(customer);
        res1.setRoom(r201);
        res1.setCheckInDate(LocalDate.now().plusDays(2));
        res1.setCheckOutDate(LocalDate.now().plusDays(5));
        res1.setNumberOfGuests(2);
        res1.setNights(3);
        res1.setRoomRate(r201.getPricePerNight());
        res1.setSubtotal(new BigDecimal("447.00"));
        res1.setTaxAmount(new BigDecimal("53.64"));
        res1.setTotalAmount(new BigDecimal("500.64"));
        res1.setStatus(ReservationStatus.CONFIRMED);
        res1.setGuestName("John Doe");
        res1.setGuestEmail("john@example.com");
        res1.setGuestPhone("+1-555-0142");
        res1 = reservationRepository.save(res1);

        Payment pay1 = new Payment(null, "PAY-DEMO-991", res1, res1.getTotalAmount(), "SIMULATED_CARD",
                PaymentStatus.SUCCESSFUL, LocalDateTime.now().minusHours(2), "Simulated transaction approved.");
        paymentRepository.save(pay1);

        Reservation res2 = new Reservation();
        res2.setBookingReference("BK-DEMO-802B");
        res2.setUser(customer);
        res2.setRoom(r301);
        res2.setCheckInDate(LocalDate.now().minusDays(4));
        res2.setCheckOutDate(LocalDate.now().minusDays(1));
        res2.setNumberOfGuests(2);
        res2.setNights(3);
        res2.setRoomRate(r301.getPricePerNight());
        res2.setSubtotal(new BigDecimal("717.00"));
        res2.setTaxAmount(new BigDecimal("86.04"));
        res2.setTotalAmount(new BigDecimal("803.04"));
        res2.setStatus(ReservationStatus.CHECKED_OUT);
        res2.setGuestName("John Doe");
        res2.setGuestEmail("john@example.com");
        res2.setGuestPhone("+1-555-0142");
        res2 = reservationRepository.save(res2);

        Payment pay2 = new Payment(null, "PAY-DEMO-992", res2, res2.getTotalAmount(), "SIMULATED_UPI",
                PaymentStatus.SUCCESSFUL, LocalDateTime.now().minusDays(4), "Simulated transaction approved.");
        paymentRepository.save(pay2);

        logger.info("Sample database initialization completed successfully.");
    }
}
