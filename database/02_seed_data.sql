USE stockflow_db;
SET NAMES utf8mb4;

-- SAFETY GUARD: this script is for a FIRST-TIME setup only. If the database already holds users or
-- products it stops with an error instead of duplicating or overwriting anything.
DROP PROCEDURE IF EXISTS sf_guard_seed;
DELIMITER $$
CREATE PROCEDURE sf_guard_seed()
BEGIN
    IF (SELECT COUNT(*) FROM users) > 0 OR (SELECT COUNT(*) FROM products) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'StockFlow seed aborted: database already contains data. Nothing was changed.';
    END IF;
END$$
DELIMITER ;
CALL sf_guard_seed();
DROP PROCEDURE sf_guard_seed;

-- DEV/DEMO CREDENTIALS ONLY (BCrypt hashed). Change or delete before real deployment.
-- admin/admin123, manager/manager123, staff/staff123
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin',   '$2a$10$samFYDigfSTFUno1yxwFfuieiDkwcSlJ1oQr/q4ymEhyet6TgF9YC', 'System Administrator', 'ADMIN'),
('manager', '$2a$10$XyGoK8X1zfw68bOYZRrOIOfDZSb3GollcaEtf7b1BbavIRXlOJUi.', 'Rafiqul Islam',       'MANAGER'),
('staff',   '$2a$10$53BR3Cc5Dh/Ew9hkajaAVuuLbH1WVNgRPthvUyM84.LGHaZ8yfGS.', 'Sumaiya Khatun',      'STAFF');

INSERT INTO categories (name, description) VALUES
('Computer & Laptop',    'Laptops, desktops and components'),
('Mobile & Accessories', 'Phones, chargers, cases'),
('Networking',           'Routers, switches, access points'),
('Storage Devices',      'SSD, HDD, pen drives, memory cards'),
('Office Equipment',     'Webcams, projectors, general office gear'),
('Printers & Scanners',  'Printers, scanners, toners'),
('Cables & Adapters',    'HDMI, LAN, USB and power cables'),
('Keyboard & Mouse',     'Keyboards, mice, combos'),
('Gaming Accessories',   'Gaming mice, headsets, cooling pads'),
('Power & UPS',          'UPS, stabilizers, power strips'),
('CCTV & Security',      'Cameras, DVR/NVR, accessories'),
('Electronics',          'Monitors and general electronics');

INSERT INTO suppliers (name, phone, email, address) VALUES
('TechLand Bangladesh',    '01711234567', 'sales@techland.com.bd',     'Multiplan Center, Elephant Road, Dhaka'),
('Ryans Computer',         '01812345678', 'b2b@ryanscomputers.example','Bashundhara City, Panthapath, Dhaka'),
('Star Tech Supplies',     '01913456789', 'supply@startech.example',   'Motijheel, Dhaka'),
('Smart Technologies BD',  '01614567890', 'info@smarttech.example',    'Uttara Sector 7, Dhaka'),
('Global Brand Pvt. Ltd.', '01315678901', 'orders@globalbrand.example','Agrabad, Chattogram'),
('Dhaka Electronics',      '01716789012', 'dhakaelec@example.com',     'Gulshan-1, Dhaka'),
('Tangail Computer Zone',  '01817890123', 'tcz.tangail@example.com',   'Victoria Road, Tangail'),
('Madhupur Traders',       '01918901234', 'madhupurtraders@example.com','Bazar Road, Madhupur, Tangail');

INSERT INTO customers (name, phone, email, address) VALUES
('Rahim Ahmed',   '01712345001', 'rahim.ahmed@example.com', 'Mirpur-10, Dhaka'),
('Karim Hossain', '01812345002', NULL,                      'Savar, Dhaka'),
('Nusrat Jahan',  '01912345003', 'nusrat.jahan@example.com','Dhanmondi, Dhaka'),
('Sadia Akter',   '01612345004', NULL,                      'Uttara Sector 4, Dhaka'),
('Tanvir Hasan',  '01312345005', 'tanvir.hasan@example.com','Tangail Sadar, Tangail'),
('Mehedi Hasan',  '01412345006', NULL,                      'Mymensingh Sadar'),
('Arafat Rahman', '01712345007', 'arafat.rahman@example.com','Gazipur Chowrasta'),
('Farhana Yasmin','01812345008', NULL,                      'Narayanganj Sadar'),
('Imran Khan',    '01912345009', 'imran.khan@example.com',  'Agrabad, Chattogram'),
('Shirin Sultana','01712345010', NULL,                      'Madhupur, Tangail');

-- category ids: 1 Computer,2 Mobile,3 Network,4 Storage,5 Office,6 Printer,7 Cables,8 KB/Mouse,9 Gaming,10 Power,11 CCTV,12 Electronics
INSERT INTO products (sku, name, category_id, supplier_id, brand, cost_price, selling_price, quantity, reorder_level) VALUES
('MSE-LOG-M185', 'Logitech M185 Wireless Mouse',    8, 1, 'Logitech',  780.00,   950.00, 45, 10),
('MSE-A4T-BLD',  'A4Tech Bloody Gaming Mouse',      9, 2, 'A4Tech',   1150.00,  1450.00, 22, 8),
('MSE-RAP-N100', 'Rapoo N100 Mouse',                8, 3, 'Rapoo',     420.00,   560.00, 60, 15),
('KBD-LOG-K120', 'Logitech K120 Keyboard',          8, 1, 'Logitech',  690.00,   850.00, 38, 10),
('KBD-A4T-KR85', 'A4Tech KR-85 Keyboard',           8, 2, 'A4Tech',    520.00,   690.00, 4,  10),
('NET-TPL-C6',   'TP-Link Archer C6 Router',        3, 4, 'TP-Link',  3200.00,  3850.00, 18, 5),
('NET-TPL-SG108','TP-Link TL-SG108 8-Port Switch',  3, 4, 'TP-Link',  1650.00,  2050.00, 12, 5),
('RAM-KNG-8D4',  'Kingston 8GB DDR4 RAM',           1, 5, 'Kingston', 2350.00,  2850.00, 30, 8),
('SSD-SAM-980',  'Samsung 980 500GB NVMe SSD',      4, 5, 'Samsung',  5900.00,  6900.00, 15, 5),
('USB-TRN-64',   'Transcend 64GB USB Drive',        4, 3, 'Transcend', 620.00,   820.00, 80, 20),
('PRN-HP-LJ',    'HP LaserJet Pro M15w Printer',    6, 6, 'HP',      11500.00, 13200.00, 6,  3),
('MON-DEL-24',   'Dell 24-inch Monitor P2422H',     12,6, 'Dell',    14800.00, 16900.00, 9,  4),
('MON-AOC-24',   'AOC 24-inch Monitor 24B1H',       12,6, 'AOC',     10400.00, 11900.00, 3,  4),
('UPS-APC-650',  'APC Back-UPS 650VA',              10,4, 'APC',      3900.00,  4650.00, 14, 5),
('CBL-HDMI-15',  'HDMI Cable 1.5m',                 7, 3, 'UGREEN',    180.00,   280.00, 120, 30),
('CBL-CAT6-305', 'Cat6 LAN Cable 305m Box',         7, 7, 'D-Link',  6800.00,  8200.00, 7,  3),
('CAM-WEB-C270', 'Logitech C270 HD Webcam',         5, 1, 'Logitech', 2450.00,  3050.00, 11, 4),
('GAM-COOL-PAD', 'Laptop Cooling Pad 5-Fan',        9, 8, 'Cooler Master',1100.00, 1450.00, 2, 6),
('CCT-HIK-2MP',  'Hikvision 2MP Dome Camera',       11,7, 'Hikvision',2600.00, 3300.00, 16, 6),
('MOB-ANK-20W',  'Anker 20W USB-C Charger',         2, 2, 'Anker',     950.00,  1250.00, 33, 10),
('LAP-HP-15',    'HP 15s Core i5 Laptop 8/512GB',   1, 2, 'HP',      56500.00, 62500.00, 5,  2),
('PRN-EPS-L3210','Epson L3210 Ink Tank Printer',    6, 6, 'Epson',   19800.00, 22500.00, 4,  2);
