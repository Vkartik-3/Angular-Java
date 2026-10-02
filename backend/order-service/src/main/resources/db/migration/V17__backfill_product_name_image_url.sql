UPDATE order_items
SET product_name = CASE product_id
        WHEN 'a1b2c3d4-0001-0001-0001-000000000001' THEN 'IV'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000002' THEN 'Animals'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000003' THEN 'Vol. 4'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000004' THEN 'Abbey Road'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000005' THEN 'In the Court of the Crimson King'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000006' THEN 'Let There Be Rock'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000007' THEN 'Jailbreak'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000008' THEN 'Making Movies'
    END,
    image_url = CASE product_id
        WHEN 'a1b2c3d4-0001-0001-0001-000000000001' THEN '/assets/covers/led-zeppelin-iv.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000002' THEN '/assets/covers/pink-floyd-animals.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000003' THEN '/assets/covers/black-sabbath-vol4.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000004' THEN '/assets/covers/beatles-abbey-road.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000005' THEN '/assets/covers/king-crimson-in-the-court.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000006' THEN '/assets/covers/acdc-let-there-be-rock.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000007' THEN '/assets/covers/thin-lizzy-jailbreak.jpg'
        WHEN 'a1b2c3d4-0001-0001-0001-000000000008' THEN '/assets/covers/dire-straits-making-movies.jpg'
    END
WHERE product_name IS NULL
  AND product_id IN (
    'a1b2c3d4-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000002',
    'a1b2c3d4-0001-0001-0001-000000000003',
    'a1b2c3d4-0001-0001-0001-000000000004',
    'a1b2c3d4-0001-0001-0001-000000000005',
    'a1b2c3d4-0001-0001-0001-000000000006',
    'a1b2c3d4-0001-0001-0001-000000000007',
    'a1b2c3d4-0001-0001-0001-000000000008'
  );