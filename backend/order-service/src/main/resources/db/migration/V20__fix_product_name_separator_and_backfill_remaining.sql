-- V19 used a plain hyphen; the frontend settled on an em dash ("—") as the
-- artist/title separator, so bring those same 8 rows in line.
UPDATE order_items
SET product_name = REPLACE(product_name, ' - ', ' — ')
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

-- A handful of pre-existing orders (predating this migration series) were
-- placed back when the app only ever stored the bare album title. Their
-- product_ids belong to a since-replaced product catalogue, so they can't be
-- looked up anymore — backfilled here by order_item id, artist taken from
-- the matching sample CSV (apps/web/public/assets/sample-data/).
UPDATE order_items SET product_name = 'Van Halen — 1984'
WHERE id = '5da7f802-ab3b-4a3c-9a2e-f349a3827843';

UPDATE order_items SET product_name = 'The Beatles — Abbey Road'
WHERE id = 'a922c9bf-992e-47a8-a5d0-0aff8dc013fa';

UPDATE order_items SET product_name = 'Black Sabbath — Vol. 4'
WHERE id = '2e1b9c0c-5275-4ca8-99e2-5a427a82f96b';