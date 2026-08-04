ALTER TABLE tb_push_message
    ADD COLUMN receipt_available_at datetime(6) NULL AFTER sent_at;
