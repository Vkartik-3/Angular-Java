-- Vinyl covers and turntable images now live together under
-- apps/web/public/assets/products/ instead of separate /covers/ and
-- /turntables/ folders — update the historical order snapshots to match.
UPDATE order_items
SET image_url = REPLACE(REPLACE(image_url, '/assets/covers/', '/assets/products/'), '/assets/turntables/', '/assets/products/')
WHERE image_url LIKE '/assets/covers/%' OR image_url LIKE '/assets/turntables/%';