ALTER TABLE order_saga
    ADD CONSTRAINT fk_order_saga_order
    FOREIGN KEY (order_id) REFERENCES orders (id);
