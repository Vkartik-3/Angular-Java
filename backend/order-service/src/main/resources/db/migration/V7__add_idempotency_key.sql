ALTER TABLE orders ADD COLUMN idempotency_key UUID;
ALTER TABLE orders ADD CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key);
