USE stockflow_db;

-- SAFETY GUARD: demo transactions are for a FRESH demo database only (needs the 02 seed data, ids 1-22).
-- It stops with an error if any purchases or sales already exist.
DROP PROCEDURE IF EXISTS sf_guard_demo;
DELIMITER $$
CREATE PROCEDURE sf_guard_demo()
BEGIN
    IF (SELECT COUNT(*) FROM purchases) > 0 OR (SELECT COUNT(*) FROM sales) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Demo data aborted: purchases/sales already exist. Nothing was changed.';
    END IF;
    IF (SELECT COUNT(*) FROM products WHERE id BETWEEN 1 AND 22) <> 22 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Demo data aborted: run 02_seed_data.sql first on an empty database.';
    END IF;
END$$
DELIMITER ;
CALL sf_guard_demo();
DROP PROCEDURE sf_guard_demo;

-- Demo transactions for dashboard/reports (dates relative to NOW()).
-- Totals are computed from line items. Product stock is NOT changed by this script.

INSERT INTO purchases (supplier_id, user_id, total_amount, purchase_date, note) VALUES
(1, 2, 0, NOW() - INTERVAL 20 DAY, 'Monthly restock'),
(5, 2, 0, NOW() - INTERVAL 12 DAY, 'RAM and SSD order'),
(6, 2, 0, NOW() - INTERVAL 5 DAY,  'Monitors and printer'),
(3, 2, 0, NOW(),                   'Cables and USB drives');

INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, subtotal) VALUES
(1, 1, 50, 780.00,   39000.00),
(1, 4, 30, 690.00,   20700.00),
(1, 17, 3, 2450.00,   7350.00),
(2, 8, 20, 2350.00,  47000.00),
(2, 9, 12, 5900.00,  70800.00),
(3, 12, 6, 14800.00, 88800.00),
(3, 11, 5, 11500.00, 57500.00),
(4, 15, 60, 180.00,  10800.00),
(4, 10, 35, 620.00,  21700.00);

UPDATE purchases p
SET total_amount = (SELECT SUM(subtotal) FROM purchase_items WHERE purchase_id = p.id);

INSERT INTO sales (invoice_no, customer_id, user_id, total_amount, sale_date) VALUES
('INV-0001', 1,    3, 0, NOW() - INTERVAL 9 DAY),
('INV-0002', 3,    3, 0, NOW() - INTERVAL 7 DAY),
('INV-0003', 5,    2, 0, NOW() - INTERVAL 6 DAY),
('INV-0004', NULL, 3, 0, NOW() - INTERVAL 4 DAY),
('INV-0005', 7,    3, 0, NOW() - INTERVAL 3 DAY),
('INV-0006', 2,    3, 0, NOW() - INTERVAL 2 DAY),
('INV-0007', 9,    2, 0, NOW() - INTERVAL 1 DAY),
('INV-0008', 4,    3, 0, NOW()),
('INV-0009', 10,   3, 0, NOW());

INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, unit_cost, subtotal) VALUES
(1, 1, 1, 950.00,   780.00,   950.00),
(1, 4, 1, 850.00,   690.00,   850.00),
(2, 12, 1, 16900.00, 14800.00, 16900.00),
(3, 6, 2, 3850.00,  3200.00,  7700.00),
(3, 14, 1, 4650.00, 3900.00,  4650.00),
(4, 8, 1, 2850.00,  2350.00,  2850.00),
(4, 15, 3, 280.00,  180.00,   840.00),
(4, 20, 1, 1250.00, 950.00,   1250.00),
(5, 22, 1, 22500.00, 19800.00, 22500.00),
(6, 2, 1, 1450.00,  1150.00,  1450.00),
(6, 20, 1, 1250.00, 950.00,   1250.00),
(7, 9, 1, 6900.00,  5900.00,  6900.00),
(7, 17, 1, 3050.00, 2450.00,  3050.00),
(7, 10, 2, 820.00,  620.00,   1640.00),
(8, 21, 1, 62500.00, 56500.00, 62500.00),
(9, 4, 1, 850.00,   690.00,   850.00),
(9, 3, 1, 560.00,   420.00,   560.00),
(9, 15, 1, 280.00,  180.00,   280.00);

UPDATE sales s
SET total_amount = (SELECT SUM(subtotal) FROM sale_items WHERE sale_id = s.id);
