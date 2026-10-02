CREATE TABLE order_processed_events (
    event_id        VARCHAR(36)     NOT NULL,
    processed_at    TIMESTAMP       NOT NULL,
    PRIMARY KEY (event_id)
);