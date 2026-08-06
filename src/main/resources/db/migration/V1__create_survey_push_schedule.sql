CREATE TABLE tb_survey_push_schedule (
    survey_push_schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    survey_key VARCHAR(64) NOT NULL,
    notification_stage VARCHAR(40) NOT NULL,
    target_user_id BIGINT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    reward_point INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    PRIMARY KEY (survey_push_schedule_id),
    CONSTRAINT uk_survey_push_schedule_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_survey_push_schedule_reward_point CHECK (reward_point >= 0)
);

CREATE INDEX idx_survey_push_schedule_status_scheduled_at
    ON tb_survey_push_schedule (status, scheduled_at);
