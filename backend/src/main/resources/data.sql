-- Sample data for H2 development
-- Insert test orders for development and testing

INSERT INTO orders (order_date, promised_delivery_date, actual_delivery_date, status, carrier, destination_city, destination_state, destination_region, order_value, sku, quantity) VALUES
('2024-01-15', '2024-01-20', '2024-01-19', 'delivered', 'UPS', 'New York', 'NY', 'Northeast', 1250.50, 'SKU001', 5),
('2024-01-16', '2024-01-22', '2024-01-23', 'delivered', 'FedEx', 'Los Angeles', 'CA', 'West', 890.75, 'SKU002', 3),
('2024-01-17', '2024-01-21', '2024-01-20', 'delivered', 'DHL', 'Chicago', 'IL', 'Midwest', 2100.00, 'SKU003', 10),
('2024-01-18', '2024-01-23', '2024-01-25', 'delivered', 'UPS', 'Houston', 'TX', 'South', 450.25, 'SKU004', 2),
('2024-01-19', '2024-01-24', '2024-01-24', 'delivered', 'FedEx', 'Phoenix', 'AZ', 'West', 1200.00, 'SKU005', 6),
('2024-01-20', '2024-01-25', NULL, 'in_transit', 'DHL', 'Philadelphia', 'PA', 'Northeast', 780.50, 'SKU006', 4),
('2024-01-21', '2024-01-26', NULL, 'in_transit', 'UPS', 'San Antonio', 'TX', 'South', 950.00, 'SKU007', 5),
('2024-01-22', '2024-01-27', NULL, 'pending', 'FedEx', 'San Diego', 'CA', 'West', 320.75, 'SKU008', 1),
('2024-01-23', '2024-01-28', '2024-01-30', 'delivered', 'DHL', 'Dallas', 'TX', 'South', 1650.00, 'SKU009', 8),
('2024-01-24', '2024-01-29', '2024-01-28', 'delivered', 'UPS', 'San Jose', 'CA', 'West', 210.25, 'SKU010', 1),
('2024-02-01', '2024-02-06', '2024-02-05', 'delivered', 'FedEx', 'Austin', 'TX', 'South', 875.50, 'SKU011', 3),
('2024-02-02', '2024-02-07', '2024-02-08', 'delivered', 'DHL', 'Jacksonville', 'FL', 'South', 1420.00, 'SKU012', 7),
('2024-02-03', '2024-02-08', '2024-02-07', 'delivered', 'UPS', 'Fort Worth', 'TX', 'South', 530.75, 'SKU013', 2),
('2024-02-04', '2024-02-09', NULL, 'in_transit', 'FedEx', 'Columbus', 'OH', 'Midwest', 1120.00, 'SKU014', 5),
('2024-02-05', '2024-02-10', '2024-02-11', 'delivered', 'DHL', 'Charlotte', 'NC', 'South', 960.25, 'SKU015', 4),
('2024-02-06', '2024-02-11', '2024-02-10', 'delivered', 'UPS', 'San Francisco', 'CA', 'West', 1850.50, 'SKU016', 9),
('2024-02-07', '2024-02-12', NULL, 'pending', 'FedEx', 'Indianapolis', 'IN', 'Midwest', 420.00, 'SKU017', 2),
('2024-02-08', '2024-02-13', '2024-02-14', 'delivered', 'DHL', 'Seattle', 'WA', 'West', 730.75, 'SKU018', 3),
('2024-02-09', '2024-02-14', '2024-02-13', 'delivered', 'UPS', 'Denver', 'CO', 'West', 1520.00, 'SKU019', 7),
('2024-02-10', '2024-02-15', '2024-02-16', 'delivered', 'FedEx', 'Washington', 'DC', 'Northeast', 890.25, 'SKU020', 4);