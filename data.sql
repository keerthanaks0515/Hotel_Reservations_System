-- ==============================================================================
-- Sample Development and Seed Data for MySQL
-- Note: Passwords are BCrypt hashed:
--   'Admin@123'    -> $2a$10$w8gJtC7c6qZ0BqXJ2d1X2O1w7eB0Q9fU2q.K4R9YwQ0eB4m6V8xZy
--   'Customer@123' -> $2a$10$vN9dGzV9iV6i6aM3wZ0z7uK.N8R2uK4t6W7YvQ0eB4m6V8xZy1234
-- (The application also initializes demo accounts automatically via DataInitializer)
-- ==============================================================================

USE hotel_reservation_db;

-- 1. Insert Initial Users
INSERT INTO users (id, full_name, email, password, phone, role, enabled, created_at, updated_at)
VALUES
(1, 'Hotel Administrator', 'admin@hotel.com', '$2a$10$k1r3QzW4fQ3F5C7rO7oGZ.nN.4v2bM5t1yK.4wQ0eB4m6V8xZy12.', '+1-555-0199', 'ROLE_ADMIN', 1, NOW(), NOW()),
(2, 'John Doe', 'john@example.com', '$2a$10$k1r3QzW4fQ3F5C7rO7oGZ.nN.4v2bM5t1yK.4wQ0eB4m6V8xZy12.', '+1-555-0142', 'ROLE_CUSTOMER', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id=id;

-- 2. Insert Room Categories
INSERT INTO room_categories (id, name, base_price, capacity, description, image_url)
VALUES
(1, 'Standard Room', 89.00, 2, 'Comfortable and cozy room designed for short stays and budget-conscious travelers with essential modern amenities.', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80'),
(2, 'Deluxe Room', 149.00, 2, 'Spacious deluxe room featuring plush bedding, modern workstation, marble bath, and serene city skyline views.', 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80'),
(3, 'Executive Suite', 239.00, 3, 'Luxurious suite with private lounge, king-size master bed, walk-in closet, and panoramic city terrace.', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80'),
(4, 'Family Suite', 319.00, 5, 'Generously proportioned family sanctuary with two interconnected bedrooms, entertainment lounge, and dining area.', 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80')
ON DUPLICATE KEY UPDATE id=id;

-- 3. Insert Amenities
INSERT INTO amenities (id, name, icon)
VALUES
(1, 'High-Speed Wi-Fi', 'fa-wifi'),
(2, 'Climate Control AC', 'fa-snowflake'),
(3, '55" 4K Smart TV', 'fa-tv'),
(4, 'Mini Bar & Espresso', 'fa-coffee'),
(5, 'Artisan Breakfast Included', 'fa-utensils'),
(6, 'Infinity Pool Access', 'fa-water'),
(7, 'Private Skyline Balcony', 'fa-mountain-sun')
ON DUPLICATE KEY UPDATE id=id;

-- 4. Insert Rooms
INSERT INTO rooms (id, room_number, category_id, price_per_night, max_occupancy, active, description, image_url, version)
VALUES
(1, '101', 1, 89.00, 2, 1, 'Well-appointed standard room on the 1st floor featuring queen bed.', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80', 0),
(2, '102', 1, 95.00, 2, 1, 'Quiet standard room with courtyard view and ergonomic workspace.', 'https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?auto=format&fit=crop&w=800&q=80', 0),
(3, '201', 2, 149.00, 2, 1, 'Corner deluxe room with floor-to-ceiling glass windows and king bed.', 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80', 0),
(4, '202', 2, 159.00, 2, 1, 'Premium deluxe room with soaking tub and panoramic garden vistas.', 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80', 0),
(5, '301', 3, 239.00, 3, 1, 'Top-tier executive suite with dedicated dining table, deep-soak tub, and terrace.', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80', 0),
(6, '401', 4, 319.00, 5, 1, 'Spacious multi-room suite suited for families, complete with kitchenette and lounge.', 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80', 0)
ON DUPLICATE KEY UPDATE id=id;
