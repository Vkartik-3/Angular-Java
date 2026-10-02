CREATE TABLE outbox_events (
    id              VARCHAR(36)     NOT NULL,
    aggregate_id    VARCHAR(36)     NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         TEXT            NOT NULL,
    processed       BOOLEAN         NOT NULL DEFAULT FALSE,
    retry_count     INT             NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL,
    processed_at    TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_unprocessed
    ON outbox_events (processed, retry_count);

CREATE INDEX idx_outbox_unprocessed_created
    ON outbox_events (processed, retry_count, created_at);