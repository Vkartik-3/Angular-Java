CREATE TABLE inbox_event (
    event_id      VARCHAR(255) NOT NULL,
    event_type    VARCHAR(255) NOT NULL,
    received_at   TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    error_message TEXT,
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_inbox_cleanup ON inbox_event (status, received_at);
