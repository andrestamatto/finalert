CREATE TABLE tb_create_alert_requests (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash CHAR(64) NOT NULL,
    alert_id UUID UNIQUE REFERENCES tb_alerts (id),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CHECK (
        (alert_id IS NULL AND completed_at IS NULL)
        OR (alert_id IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE TABLE tb_outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES tb_alerts (id),
    event_type VARCHAR(128) NOT NULL,
    exchange_name VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0
        CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,

    CHECK (
        status <> 'PUBLISHED'
        OR published_at IS NOT NULL
    )
);

CREATE INDEX idx_tb_outbox_events_due
    ON tb_outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';
