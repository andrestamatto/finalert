CREATE TABLE tb_alerts (
   id UUID PRIMARY KEY,

   user_email VARCHAR(320) NOT NULL,
   symbol VARCHAR(32) NOT NULL,

   target_price NUMERIC(24, 12) NOT NULL
       CHECK (target_price > 0),

   trigger_operator VARCHAR(3) NOT NULL DEFAULT 'GTE'
       CHECK (trigger_operator IN ('GTE', 'LTE')),

   status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
       CHECK (status IN ('PENDING', 'TRIGGERED', 'CANCELLED')),

   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   triggered_at TIMESTAMPTZ,
   cancelled_at TIMESTAMPTZ,

   version BIGINT NOT NULL DEFAULT 0,

   CHECK (
       status <> 'TRIGGERED'
           OR triggered_at IS NOT NULL
       )
);

CREATE INDEX idx_tb_alerts_pending_symbol
    ON tb_alerts (symbol)
    WHERE status = 'PENDING';

CREATE INDEX idx_tb_alerts_user_email
    ON tb_alerts (user_email);