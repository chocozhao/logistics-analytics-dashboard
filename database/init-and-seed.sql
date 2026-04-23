-- SQL script to initialize database and seed sample data
-- Schema based on mock_logistics_data.csv
-- Idempotent: won't recreate existing tables or re-insert data

-- 1. Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(20),
    order_id VARCHAR(50) UNIQUE,
    order_date DATE NOT NULL,
    promised_delivery_date DATE NOT NULL,
    delivery_date DATE,
    carrier VARCHAR(50),
    origin_city VARCHAR(100),
    destination_city VARCHAR(100),
    status VARCHAR(20) CHECK (status IN ('delivered', 'delayed', 'in_transit', 'exception', 'canceled')),
    sku VARCHAR(50),
    product_category VARCHAR(50),
    quantity INTEGER,
    unit_price_usd DECIMAL(10,2),
    order_value_usd DECIMAL(10,2),
    is_promo BOOLEAN DEFAULT FALSE,
    promo_discount_pct DECIMAL(5,2) DEFAULT 0,
    region VARCHAR(20),
    warehouse VARCHAR(30)
);

-- 2. Indexes
CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_promised_delivery_date ON orders(promised_delivery_date);
CREATE INDEX IF NOT EXISTS idx_delivery_date ON orders(delivery_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_region ON orders(region);
CREATE INDEX IF NOT EXISTS idx_product_category ON orders(product_category);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);
CREATE INDEX IF NOT EXISTS idx_order_date_carrier ON orders(order_date, carrier);

-- 3. Seed data (only if table is empty)
DO $$
DECLARE
    carrier_list TEXT[] := ARRAY['UPS','FedEx','DHL','USPS','LaserShip','OnTrac','DPD','GLS','Royal Mail'];
    category_list TEXT[] := ARRAY['BOOK','PAPER','PENCIL','CRAYON','MARKER','BRUSH','PAINT','STICKER'];
    status_weights TEXT[] := ARRAY[
        'delivered','delivered','delivered','delivered','delivered','delivered',
        'delayed','delayed',
        'in_transit',
        'exception','canceled'
    ];
    region_config JSONB[] := ARRAY[
        '{"region":"US-W","cities":["Los Angeles, CA","San Francisco, CA","Seattle, WA","Portland, OR","San Diego, CA","Boise, ID","Las Vegas, NV","Denver, CO","Salt Lake City, UT","Sacramento, CA"],"warehouses":["LAX-DC1","SFO-DC2"]}'::jsonb,
        '{"region":"US-C","cities":["Chicago, IL","Dallas, TX","Houston, TX","St. Louis, MO","Minneapolis, MN","Kansas City, MO","Milwaukee, WI","Indianapolis, IN","Detroit, MI","Cleveland, OH"],"warehouses":["CHI-DC1","DFW-DC1"]}'::jsonb,
        '{"region":"US-E","cities":["Newark, NJ","Atlanta, GA","Boston, MA","Washington, DC","Philadelphia, PA","New York, NY","Baltimore, MD","Charlotte, NC","Orlando, FL","Tampa, FL"],"warehouses":["EWR-DC1","ATL-DC1"]}'::jsonb,
        '{"region":"EU","cities":["Berlin, DE","Amsterdam, NL","Paris, FR","Munich, DE","Zurich, CH","Warsaw, PL","Vienna, AT","Brussels, BE","Frankfurt, DE","Hamburg, DE","Prague, CZ","Cologne, DE"],"warehouses":["BER-FC1","AMS-FC1"]}'::jsonb,
        '{"region":"UK","cities":["London, UK","Edinburgh, UK","Manchester, UK","Glasgow, UK","Birmingham, UK","Leeds, UK"],"warehouses":["LON-FC1"]}'::jsonb
    ];
    r_idx INTEGER;
    cfg JSONB;
    origin_arr TEXT[];
    dest_arr TEXT[];
    wh_arr TEXT[];
    origin_city TEXT;
    dest_city TEXT;
    warehouse TEXT;
    carrier TEXT;
    category TEXT;
    status_val TEXT;
    order_dt DATE;
    promised_dt DATE;
    delivery_dt DATE;
    delivery_days INTEGER;
    is_promo_val BOOLEAN;
    promo_pct DECIMAL(5,2);
    unit_price DECIMAL(10,2);
    qty INTEGER;
    client_id_val TEXT;
    order_id_val TEXT;
    sku_val TEXT;
    seq INTEGER;
    region_val TEXT;
    num_range INTEGER;
BEGIN
    IF (SELECT COUNT(*) FROM orders) > 0 THEN
        RAISE NOTICE 'Table already has data, skipping seed.';
        RETURN;
    END IF;

    RAISE NOTICE 'Seeding orders table...';
    seq := 1;

    -- Generate ~3000 orders spanning 2025-01-01 to 2025-12-31
    FOR i IN 1..3000 LOOP
        -- Pick region (weighted: US-W=20%, US-C=25%, US-E=25%, EU=20%, UK=10%)
        num_range := (random() * 99)::INTEGER;
        IF num_range < 20 THEN r_idx := 1;
        ELSIF num_range < 45 THEN r_idx := 2;
        ELSIF num_range < 70 THEN r_idx := 3;
        ELSIF num_range < 90 THEN r_idx := 4;
        ELSE r_idx := 5;
        END IF;

        cfg := region_config[r_idx];
        region_val := cfg->>'region';
        origin_arr := ARRAY(SELECT jsonb_array_elements_text(cfg->'cities'));
        dest_arr := ARRAY(SELECT jsonb_array_elements_text(cfg->'cities'));
        wh_arr := ARRAY(SELECT jsonb_array_elements_text(cfg->'warehouses'));

        origin_city := origin_arr[1 + (random() * (array_length(origin_arr,1)-1))::INTEGER];
        dest_city := dest_arr[1 + (random() * (array_length(dest_arr,1)-1))::INTEGER];
        WHILE dest_city = origin_city LOOP
            dest_city := dest_arr[1 + (random() * (array_length(dest_arr,1)-1))::INTEGER];
        END LOOP;
        warehouse := wh_arr[1 + (random() * (array_length(wh_arr,1)-1))::INTEGER];

        -- Carrier (restrict to region-appropriate ones)
        IF region_val IN ('EU','UK') THEN
            carrier := (ARRAY['DHL','DPD','GLS','Royal Mail','UPS','FedEx'])[1 + (random()*5)::INTEGER];
        ELSE
            carrier := (ARRAY['UPS','FedEx','DHL','USPS','LaserShip','OnTrac'])[1 + (random()*5)::INTEGER];
        END IF;

        -- Order date: random within 2025
        order_dt := '2025-01-01'::DATE + (random() * 364)::INTEGER;

        -- Promised delivery: 3-5 days after order
        promised_dt := order_dt + (3 + (random() * 2)::INTEGER);

        -- Status (weighted)
        status_val := status_weights[1 + (random() * 10)::INTEGER];

        -- Delivery date logic
        IF status_val = 'delivered' THEN
            delivery_days := 1 + (random() * 5)::INTEGER;
            delivery_dt := order_dt + delivery_days;
        ELSIF status_val = 'delayed' THEN
            delivery_days := 6 + (random() * 9)::INTEGER;  -- 6-14 days
            delivery_dt := order_dt + delivery_days;
        ELSIF status_val = 'exception' THEN
            delivery_days := 8 + (random() * 14)::INTEGER;
            delivery_dt := order_dt + delivery_days;
        ELSE
            delivery_dt := NULL;  -- in_transit or canceled
        END IF;

        -- Don't let future delivery dates exceed year's end
        IF delivery_dt > '2025-12-31'::DATE THEN
            IF status_val = 'delivered' THEN
                status_val := 'in_transit';
                delivery_dt := NULL;
            ELSE
                delivery_dt := NULL;
            END IF;
        END IF;

        -- Product
        category := category_list[1 + (random() * 7)::INTEGER];
        sku_val := category || '-' || LPAD((1 + (random()*219)::INTEGER)::TEXT, 4, '0');

        -- Pricing
        IF category IN ('PAINT','BOOK') THEN
            unit_price := 8 + (random() * 22)::DECIMAL(10,2);
        ELSIF category IN ('MARKER','BRUSH') THEN
            unit_price := 5 + (random() * 18)::DECIMAL(10,2);
        ELSE
            unit_price := 2 + (random() * 12)::DECIMAL(10,2);
        END IF;
        unit_price := ROUND(unit_price, 2);

        qty := 1 + (random() * 12)::INTEGER;

        -- Promo
        is_promo_val := random() < 0.15;
        IF is_promo_val THEN
            promo_pct := 5 + (random() * 30)::DECIMAL(5,2);
            promo_pct := ROUND(promo_pct, 0);
        ELSE
            promo_pct := 0;
        END IF;

        -- Client
        client_id_val := 'CL-' || (1001 + (random() * 29)::INTEGER)::TEXT;

        -- Order ID
        order_id_val := 'ORD-2026-' || LPAD((100000 + (random()*899999)::INTEGER)::TEXT, 6, '0') || '-' || LPAD(seq::TEXT, 4, '0');

        INSERT INTO orders (
            client_id, order_id, order_date, promised_delivery_date, delivery_date,
            carrier, origin_city, destination_city,
            status, sku, product_category,
            quantity, unit_price_usd, order_value_usd,
            is_promo, promo_discount_pct,
            region, warehouse
        ) VALUES (
            client_id_val,
            order_id_val,
            order_dt,
            promised_dt,
            delivery_dt,
            carrier,
            origin_city,
            dest_city,
            status_val,
            sku_val,
            category,
            qty,
            unit_price,
            ROUND(unit_price * qty, 2),
            is_promo_val,
            promo_pct,
            region_val,
            warehouse
        );

        seq := seq + 1;
    END LOOP;

    RAISE NOTICE 'Seeded % orders.', seq - 1;
END $$;
