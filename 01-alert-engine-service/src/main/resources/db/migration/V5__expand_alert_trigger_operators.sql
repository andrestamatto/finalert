ALTER TABLE tb_alerts
    DROP CONSTRAINT tb_alerts_trigger_operator_check;

ALTER TABLE tb_alerts
    ADD CONSTRAINT ck_tb_alerts_trigger_operator
        CHECK (trigger_operator IN ('GTE', 'LTE', 'GT', 'LT', 'EQ', 'NEQ'));
