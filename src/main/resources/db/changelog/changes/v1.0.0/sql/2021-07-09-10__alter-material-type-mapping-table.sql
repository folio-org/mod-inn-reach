ALTER TABLE material_type_mapping
    DROP CONSTRAINT fk_mtype_mapping_central_server;

ALTER TABLE material_type_mapping
    ADD CONSTRAINT fk_mtype_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
        ON DELETE CASCADE;
