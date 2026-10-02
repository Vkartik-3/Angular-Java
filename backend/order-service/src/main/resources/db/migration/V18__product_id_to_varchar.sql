-- product-service moved from PostgreSQL (UUID ids) to MongoDB (ObjectId hex
-- strings), so order_items.product_id can no longer be a native UUID column.
ALTER TABLE order_items
    ALTER COLUMN product_id TYPE VARCHAR(64);