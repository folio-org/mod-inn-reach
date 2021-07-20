ALTER TABLE central_server
    ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'admin';
ALTER TABLE central_server
    ADD COLUMN created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE central_server
    ADD COLUMN last_modified_by VARCHAR(255);
ALTER TABLE central_server
    ADD COLUMN last_modified_date TIMESTAMP;
