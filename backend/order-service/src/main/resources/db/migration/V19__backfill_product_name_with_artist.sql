-- V17 backfilled product_name with just the album title. Now that the
-- catalogue displays "Artist - Title", bring these same demo rows in line.
UPDATE order_items
SET product_name = CASE product_id
        WHEN 'a1b2c3d4-0001-0001-0001-000000000001' THEN 'Led Zeppelin - IV'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000002' THEN 'Pink Floyd - Animals'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000003' THEN 'Black Sabbath - Vol. 4'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000004' THEN 'The Beatles - Abbey Road'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000005' THEN 'King Crimson - In the Court of the Crimson King'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000006' THEN 'AC/DC - Let There Be Rock'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000007' THEN 'Thin Lizzy - Jailbreak'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000008' THEN 'Dire Straits - Making Movies'
    END
WHERE product_id IN (
    'a1b2c3d4-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000002',
    'a1b2c3d4-0001-0001-0001-000000000003',
    'a1b2c3d4-0001-0001-0001-000000000004',
    'a1b2c3d4-0001-0001-0001-000000000005',
    'a1b2c3d4-0001-0001-0001-000000000006',
    'a1b2c3d4-0001-0001-0001-000000000007',
    'a1b2c3d4-0001-0001-0001-000000000008'
);