-- Sample data for H2 development
-- Generates 500 realistic order records covering 2024-01-01 to 2026-04-05
-- Ensures continuous data for forecasting (especially Oct 2024 - Mar 2025)

-- Clear existing data if needed (commented out for safety)
-- DELETE FROM orders;

-- Insert generated data using H2-compatible syntax
INSERT INTO orders (
    order_date,
    promised_delivery_date,
    actual_delivery_date,
    status,
    carrier,
    destination_city,
    destination_state,
    destination_region,
    order_value,
    sku,
    quantity
)
WITH RECURSIVE numbers(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 500
),
base_data AS (
    SELECT
        -- Order date: evenly distributed from 2024-01-01 to 2026-04-05 (825 days)
        DATEADD('DAY', CAST(RAND() * 825 AS INTEGER), DATE '2024-01-01') AS order_date,

        -- Promised delivery: 2-7 days after order date
        DATEADD('DAY', 2 + CAST(RAND() * 5 AS INTEGER), DATEADD('DAY', CAST(RAND() * 825 AS INTEGER), DATE '2024-01-01')) AS promised_delivery_date,

        -- Status and actual delivery (85% delivered, 10% in transit, 5% pending/cancelled)
        CASE
            WHEN RAND() < 0.85 THEN 'delivered'
            WHEN RAND() < 0.95 THEN 'in_transit'
            WHEN RAND() < 0.98 THEN 'pending'
            ELSE 'cancelled'
        END AS status,

        -- Carrier with weighted distribution
        CASE
            WHEN RAND() < 0.35 THEN 'UPS'
            WHEN RAND() < 0.60 THEN 'FedEx'
            WHEN RAND() < 0.80 THEN 'DHL'
            WHEN RAND() < 0.90 THEN 'USPS'
            WHEN RAND() < 0.95 THEN 'Amazon Logistics'
            ELSE 'Regional Carrier'
        END AS carrier,

        -- Destination city from predefined list
        CASE CAST(RAND() * 20 AS INTEGER)
            WHEN 0 THEN 'New York'
            WHEN 1 THEN 'Los Angeles'
            WHEN 2 THEN 'Chicago'
            WHEN 3 THEN 'Houston'
            WHEN 4 THEN 'Phoenix'
            WHEN 5 THEN 'Philadelphia'
            WHEN 6 THEN 'San Antonio'
            WHEN 7 THEN 'San Diego'
            WHEN 8 THEN 'Dallas'
            WHEN 9 THEN 'San Jose'
            WHEN 10 THEN 'Austin'
            WHEN 11 THEN 'Jacksonville'
            WHEN 12 THEN 'Fort Worth'
            WHEN 13 THEN 'Columbus'
            WHEN 14 THEN 'Charlotte'
            WHEN 15 THEN 'San Francisco'
            WHEN 16 THEN 'Indianapolis'
            WHEN 17 THEN 'Seattle'
            WHEN 18 THEN 'Denver'
            WHEN 19 THEN 'Boston'
            ELSE 'New York'
        END AS destination_city,

        -- Order value: $50-$5000, skewed toward lower values
        CAST(50 + (RAND() * 4950 * (1 - POWER(RAND(), 2)) AS DECIMAL(10,2))) AS order_value,

        -- SKU: random product code
        CONCAT('SKU-', LPAD(CAST(1000 + CAST(RAND() * 8999 AS INTEGER) AS VARCHAR), 4, '0')) AS sku,

        -- Quantity: 1-50 items
        1 + CAST(RAND() * 49 AS INTEGER) AS quantity
    FROM numbers
),
state_data AS (
    SELECT
        order_date,
        promised_delivery_date,
        CASE
            WHEN status = 'delivered' THEN
                -- Actual delivery: sometimes on time, sometimes delayed
                DATEADD('DAY',
                    CASE
                        WHEN RAND() < 0.2 THEN 1 + CAST(RAND() * 3 AS INTEGER)
                        ELSE 0
                    END,
                    promised_delivery_date)
            ELSE NULL
        END AS actual_delivery_date,
        status,
        carrier,
        destination_city,
        -- Destination state based on city
        CASE destination_city
            WHEN 'New York' THEN 'NY'
            WHEN 'Los Angeles' THEN 'CA'
            WHEN 'San Diego' THEN 'CA'
            WHEN 'San Jose' THEN 'CA'
            WHEN 'San Francisco' THEN 'CA'
            WHEN 'Chicago' THEN 'IL'
            WHEN 'Houston' THEN 'TX'
            WHEN 'Dallas' THEN 'TX'
            WHEN 'San Antonio' THEN 'TX'
            WHEN 'Austin' THEN 'TX'
            WHEN 'Fort Worth' THEN 'TX'
            WHEN 'Phoenix' THEN 'AZ'
            WHEN 'Philadelphia' THEN 'PA'
            WHEN 'Jacksonville' THEN 'FL'
            WHEN 'Columbus' THEN 'OH'
            WHEN 'Charlotte' THEN 'NC'
            WHEN 'Indianapolis' THEN 'IN'
            WHEN 'Seattle' THEN 'WA'
            WHEN 'Denver' THEN 'CO'
            WHEN 'Boston' THEN 'MA'
            ELSE 'CA'
        END AS destination_state,
        order_value,
        sku,
        quantity
    FROM base_data
)
SELECT
    order_date,
    promised_delivery_date,
    actual_delivery_date,
    status,
    carrier,
    destination_city,
    destination_state,
    -- Destination region based on state
    CASE
        WHEN destination_state IN ('NY', 'NJ', 'PA', 'CT', 'MA', 'RI', 'NH', 'VT', 'ME') THEN 'Northeast'
        WHEN destination_state IN ('CA', 'OR', 'WA', 'NV', 'AZ', 'UT', 'CO', 'NM', 'WY', 'MT', 'ID') THEN 'West'
        WHEN destination_state IN ('TX', 'OK', 'AR', 'LA', 'MS', 'AL', 'GA', 'FL', 'SC', 'NC', 'TN', 'KY') THEN 'South'
        WHEN destination_state IN ('IL', 'IN', 'OH', 'MI', 'WI', 'MN', 'IA', 'MO', 'KS', 'NE', 'SD', 'ND') THEN 'Midwest'
        ELSE 'Other'
    END AS destination_region,
    order_value,
    sku,
    quantity
FROM state_data
ORDER BY order_date;

-- Verify data insertion
-- SELECT COUNT(*) AS total_orders FROM orders;
-- SELECT MIN(order_date), MAX(order_date) FROM orders;
-- SELECT status, COUNT(*) FROM orders GROUP BY status ORDER BY COUNT(*) DESC;
-- SELECT carrier, COUNT(*) FROM orders GROUP BY carrier ORDER BY COUNT(*) DESC;