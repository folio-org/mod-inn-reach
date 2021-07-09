ALTER TABLE location_mapping
    DROP CONSTRAINT fk_location_mapping_central_server;

ALTER TABLE location_mapping
    ADD CONSTRAINT fk_location_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
        ON DELETE CASCADE;
