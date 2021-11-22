ALTER TABLE central_server
    ADD COLUMN inn_reach_recall_user_id UUID;

ALTER TABLE central_server
    ADD CONSTRAINT fk_inn_reach_recall_user_table FOREIGN KEY (inn_reach_recall_user_id) REFERENCES inn_reach_recall_user (id);
