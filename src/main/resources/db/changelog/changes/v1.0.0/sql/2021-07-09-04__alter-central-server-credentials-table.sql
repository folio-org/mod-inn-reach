ALTER TABLE central_server_credentials
    DROP CONSTRAINT central_server_credentials_central_server_id_fkey;

ALTER TABLE central_server_credentials
    ADD CONSTRAINT central_server_credentials_central_server_id_fkey FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
        ON DELETE CASCADE;
