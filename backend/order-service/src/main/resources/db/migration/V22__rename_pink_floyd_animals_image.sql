-- pink-floyd-animals.jpg was renamed to pf-animals.jpg.
UPDATE order_items
SET image_url = '/assets/products/pf-animals.jpg'
WHERE image_url = '/assets/products/pink-floyd-animals.jpg';