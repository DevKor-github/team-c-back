-- SUPERSEDED: Neon 통합안에서는 이 MySQL 변경을 적용하지 않는다. docs/push-neon-cutover.md 참고.
-- 배포 전 공유 DB에 한 번 적용. 과거 기록은 추정하지 않고 NULL을 유지한다.
ALTER TABLE tb_push_dispatch ADD COLUMN admin_only BOOLEAN NULL;
CREATE INDEX idx_push_dispatch_audience_created ON tb_push_dispatch (admin_only, created_at);
