-- 공지 관리자 CRUD를 위한 추가 컬럼이다.
-- 적용 전 tb_update_notice를 백업하고, 각 컬럼의 존재 여부를 확인한 뒤 한 번만 실행한다.
-- 기존 공지 기록은 모두 게시 상태로 보존한다.

ALTER TABLE tb_update_notice
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' AFTER link_label,
    ADD COLUMN created_at DATETIME NULL AFTER status,
    ADD COLUMN created_by BIGINT NULL AFTER created_at,
    ADD COLUMN modified_at DATETIME NULL AFTER created_by,
    ADD COLUMN modified_by BIGINT NULL AFTER modified_at;

UPDATE tb_update_notice
SET status = 'PUBLISHED'
WHERE status IS NULL OR status NOT IN ('DRAFT', 'PUBLISHED', 'ARCHIVED');

CREATE INDEX idx_update_notice_status_published_at
    ON tb_update_notice (status, published_at);
