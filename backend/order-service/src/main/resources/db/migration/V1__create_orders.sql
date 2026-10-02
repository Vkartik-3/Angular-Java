CREATE TABLE orders (
    id      UUID            NOT NULL,
    status  VARCHAR(20)     NOT NULL,
    version BIGINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE order_items (
    id          UUID            NOT NULL,
    order_id    UUID            NOT NULL,
    product_id  UUID            NOT NULL,
    quantity    INT             NOT NULL,
    price       NUMERIC(19, 2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);
