-- Rename old status values to new ones
UPDATE orders SET status = 'CREATED'            WHERE status = 'PENDING';
UPDATE orders SET status = 'INVENTORY_APPROVED' WHERE status = 'COMPLETED';

-- Order status history table
CREATE TABLE order_status_history (
    id          UUID                     PRIMARY KEY,
    order_id    UUID                     NOT NULL REFERENCES orders(id),
    status      VARCHAR(50)              NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);