CREATE TABLE tb_processed_notifications (
    event_id UUID PRIMARY KEY,
    alert_id UUID NOT NULL UNIQUE,
    processed_at TIMESTAMPTZ NOT NULL
);

