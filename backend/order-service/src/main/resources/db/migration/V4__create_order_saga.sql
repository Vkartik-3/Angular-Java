CREATE TABLE order_saga (
    order_id       UUID         NOT NULL,
    step           VARCHAR(50)  NOT NULL,
    deadline       TIMESTAMPTZ  NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (order_id)
);

CREATE INDEX idx_saga_timeout ON order_saga (step, deadline);
