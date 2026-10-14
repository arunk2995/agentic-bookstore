-- V2__seed_data.sql
-- Seed data for development and testing

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO roles (id, name, description) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'CUSTOMER',           'Regular customer with shopping capabilities'),
    ('a0000000-0000-0000-0000-000000000002', 'ADMIN',              'Administrator with full system access'),
    ('a0000000-0000-0000-0000-000000000003', 'INVENTORY_MANAGER',  'Can manage inventory and stock levels');

-- ============================================================
-- CATEGORIES
-- ============================================================
INSERT INTO categories (id, name, description, slug, active) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Programming',        'Software development and programming books',                     'programming',      TRUE),
    ('b0000000-0000-0000-0000-000000000002', 'Fiction',            'Novels, short stories, and literary fiction',                    'fiction',          TRUE),
    ('b0000000-0000-0000-0000-000000000003', 'Self-Help',          'Personal development and self-improvement',                      'self-help',        TRUE),
    ('b0000000-0000-0000-0000-000000000004', 'Science',            'Popular science and scientific research',                        'science',          TRUE),
    ('b0000000-0000-0000-0000-000000000005', 'History',            'World history, biography, and historical accounts',              'history',          TRUE),
    ('b0000000-0000-0000-0000-000000000006', 'Business',           'Business strategy, management, and entrepreneurship',            'business',         TRUE),
    ('b0000000-0000-0000-0000-000000000007', 'Computer Science',   'Algorithms, data structures, and theoretical CS',               'computer-science',  TRUE),
    ('b0000000-0000-0000-0000-000000000008', 'Mathematics',        'Pure and applied mathematics',                                   'mathematics',      TRUE);

-- ============================================================
-- AUTHORS
-- ============================================================
INSERT INTO authors (id, name, bio) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Joshua Bloch',          'Java programming expert and author of Effective Java'),
    ('c0000000-0000-0000-0000-000000000002', 'Robert C. Martin',      'Software craftsman and Clean Code advocate'),
    ('c0000000-0000-0000-0000-000000000003', 'Kathy Sierra',          'Co-author of Head First Java series'),
    ('c0000000-0000-0000-0000-000000000004', 'Bert Bates',            'Co-author of Head First Java series'),
    ('c0000000-0000-0000-0000-000000000005', 'Sanjay Kumar',          'Backend engineering and Java architecture specialist'),
    ('c0000000-0000-0000-0000-000000000006', 'Yuval Noah Harari',     'Israeli historian and author of bestselling non-fiction'),
    ('c0000000-0000-0000-0000-000000000007', 'James Clear',           'Author and speaker on habits and continuous improvement'),
    ('c0000000-0000-0000-0000-000000000008', 'Martin Kleppmann',      'Distributed systems expert and author'),
    ('c0000000-0000-0000-0000-000000000009', 'Eric Evans',            'Domain-Driven Design pioneer'),
    ('c0000000-0000-0000-0000-000000000010', 'Gang of Four',          'Authors of the Design Patterns book');

-- ============================================================
-- BOOKS
-- ============================================================
INSERT INTO books (id, isbn, title, description, publisher, publication_date, language, pages, price, currency, discount_percent, cover_image_url, average_rating, review_count, active, category_id) VALUES
    ('d0000000-0000-0000-0000-000000000001',
     '978-0134685991', 'Effective Java, 3rd Edition',
     'Best practices for the Java platform from a recognized Java expert. Covers lambdas, streams, generics, and more.',
     'Addison-Wesley', '2018-01-06', 'en', 412, 899.00, 'INR', 10.00,
     'https://images.example.com/effective-java.jpg', 4.80, 245, TRUE,
     'b0000000-0000-0000-0000-000000000001'),

    ('d0000000-0000-0000-0000-000000000002',
     '978-0132350884', 'Clean Code: A Handbook of Agile Software Craftsmanship',
     'A handbook of agile software craftsmanship. Learn to write clean, readable, and maintainable code.',
     'Prentice Hall', '2008-08-01', 'en', 431, 799.00, 'INR', 5.00,
     'https://images.example.com/clean-code.jpg', 4.60, 312, TRUE,
     'b0000000-0000-0000-0000-000000000001'),

    ('d0000000-0000-0000-0000-000000000003',
     '978-0596007126', 'Head First Java, 2nd Edition',
     'A brain-friendly guide to Java programming. Perfect for beginners.',
     'O''Reilly Media', '2005-02-09', 'en', 688, 749.00, 'INR', 0.00,
     'https://images.example.com/head-first-java.jpg', 4.50, 178, TRUE,
     'b0000000-0000-0000-0000-000000000001'),

    ('d0000000-0000-0000-0000-000000000004',
     '978-1449373320', 'Designing Data-Intensive Applications',
     'The big ideas behind reliable, scalable, and maintainable systems.',
     'O''Reilly Media', '2017-04-02', 'en', 611, 1099.00, 'INR', 15.00,
     'https://images.example.com/ddia.jpg', 4.90, 521, TRUE,
     'b0000000-0000-0000-0000-000000000007'),

    ('d0000000-0000-0000-0000-000000000005',
     '978-0321125217', 'Domain-Driven Design',
     'Tackling complexity in the heart of software. The foundational DDD book.',
     'Addison-Wesley', '2003-08-22', 'en', 560, 999.00, 'INR', 0.00,
     'https://images.example.com/ddd.jpg', 4.40, 89, TRUE,
     'b0000000-0000-0000-0000-000000000007'),

    ('d0000000-0000-0000-0000-000000000006',
     '978-0735224292', 'Atomic Habits',
     'An easy and proven way to build good habits and break bad ones.',
     'Avery', '2018-10-16', 'en', 320, 499.00, 'INR', 0.00,
     'https://images.example.com/atomic-habits.jpg', 4.80, 890, TRUE,
     'b0000000-0000-0000-0000-000000000003'),

    ('d0000000-0000-0000-0000-000000000007',
     '978-0062316097', 'Sapiens: A Brief History of Humankind',
     'A bold and groundbreaking overview of the entire history of humankind.',
     'Harper', '2015-02-10', 'en', 443, 599.00, 'INR', 10.00,
     'https://images.example.com/sapiens.jpg', 4.70, 654, TRUE,
     'b0000000-0000-0000-0000-000000000005'),

    ('d0000000-0000-0000-0000-000000000008',
     '978-0201633610', 'Design Patterns: Elements of Reusable Object-Oriented Software',
     'Classic software design patterns from the Gang of Four.',
     'Addison-Wesley', '1994-11-10', 'en', 395, 849.00, 'INR', 0.00,
     'https://images.example.com/gof.jpg', 4.20, 201, TRUE,
     'b0000000-0000-0000-0000-000000000001'),

    ('d0000000-0000-0000-0000-000000000009',
     '978-0307465351', 'The 7 Habits of Highly Effective People',
     'Powerful lessons in personal change and leadership.',
     'Free Press', '2004-11-09', 'en', 432, 449.00, 'INR', 0.00,
     'https://images.example.com/7-habits.jpg', 4.50, 432, TRUE,
     'b0000000-0000-0000-0000-000000000003'),

    ('d0000000-0000-0000-0000-000000000010',
     '978-0201485677', 'The Mythical Man-Month',
     'Essays on software engineering. A classic on software project management.',
     'Addison-Wesley', '1995-08-02', 'en', 322, 649.00, 'INR', 5.00,
     'https://images.example.com/mythical-man.jpg', 4.30, 112, TRUE,
     'b0000000-0000-0000-0000-000000000001');

-- ============================================================
-- BOOK-AUTHOR MAPPINGS
-- ============================================================
INSERT INTO book_authors (book_id, author_id) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001'),  -- Effective Java -> Joshua Bloch
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002'),  -- Clean Code -> Robert C. Martin
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003'),  -- Head First Java -> Kathy Sierra
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004'),  -- Head First Java -> Bert Bates
    ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000008'),  -- DDIA -> Martin Kleppmann
    ('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000009'),  -- DDD -> Eric Evans
    ('d0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000007'),  -- Atomic Habits -> James Clear
    ('d0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000006'),  -- Sapiens -> Yuval Noah Harari
    ('d0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000010'),  -- Design Patterns -> GoF
    ('d0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000005'),  -- 7 Habits -> Sanjay Kumar (mapped)
    ('d0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000005');  -- Mythical Man-Month

-- ============================================================
-- INVENTORY
-- ============================================================
INSERT INTO inventory (id, book_id, quantity_on_hand, quantity_reserved, quantity_available, reorder_point) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 100, 0, 100, 15),
    ('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 75,  0, 75,  10),
    ('e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 50,  0, 50,  10),
    ('e0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 120, 0, 120, 20),
    ('e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', 30,  0, 30,  5),
    ('e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000006', 200, 0, 200, 25),
    ('e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000007', 90,  0, 90,  15),
    ('e0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000008', 8,   0, 8,   10),  -- low stock
    ('e0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000009', 150, 0, 150, 20),
    ('e0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000010', 5,   0, 5,   10);  -- low stock

-- ============================================================
-- TEST ADMIN USER (password: Admin@1234)
-- BCrypt hash of 'Admin@1234' with strength 12
-- ============================================================
INSERT INTO users (id, username, email, password_hash, first_name, last_name, status) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'admin', 'admin@bookstore.example.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj3oRRVZYx0C',
     'Admin', 'User', 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000002', 'customer1', 'customer1@example.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj3oRRVZYx0C',
     'Test', 'Customer', 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000003', 'invmgr', 'invmgr@bookstore.example.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj3oRRVZYx0C',
     'Inventory', 'Manager', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002'),  -- admin -> ADMIN
    ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),  -- admin -> CUSTOMER
    ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001'),  -- customer1 -> CUSTOMER
    ('f0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003'),  -- invmgr -> INVENTORY_MANAGER
    ('f0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001');  -- invmgr -> CUSTOMER
