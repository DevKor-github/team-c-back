-- NOT APPLIED. Primary MySQL only; durable business-event handoff to Neon.
CREATE TABLE tb_push_domain_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(32) NOT NULL,
  audience VARCHAR(24) NOT NULL,
  payload TEXT NOT NULL,
  attempts INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  completed_at DATETIME(6) NULL,
  INDEX idx_push_domain_event_due (audience, completed_at, next_attempt_at)
);
