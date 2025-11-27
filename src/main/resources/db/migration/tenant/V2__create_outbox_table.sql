-- Create outbox table
CREATE TABLE outbox
(
    id            BIGSERIAL PRIMARY KEY,
    event_type    VARCHAR(100)             NOT NULL,
    payload       TEXT                     NOT NULL,
    tenant_id     VARCHAR(100),
    processed     BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at  TIMESTAMP WITH TIME ZONE,
    retry_count   INTEGER                  NOT NULL DEFAULT 0,
    error_message TEXT
);

-- Create indexes for outbox table
CREATE INDEX idx_outbox_processed ON outbox (processed);
CREATE INDEX idx_outbox_created_at ON outbox (created_at);

-- Create composite index for efficient cleanup queries
CREATE INDEX idx_outbox_processed_processed_at ON outbox (processed, processed_at);
