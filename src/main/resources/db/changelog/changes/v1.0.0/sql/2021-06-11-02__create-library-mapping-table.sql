CREATE TABLE library_mapping
(
    id                 UUID         NOT NULL,
    library_id         UUID         NOT NULL,
    ir_location_id     UUID         NOT NULL,
    central_server_id  UUID         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_library_mapping PRIMARY KEY (id),
    CONSTRAINT unq_library_mapping_server_lib UNIQUE (central_server_id, library_id),
    CONSTRAINT fk_library_mapping_ir_location FOREIGN KEY (ir_location_id)
        REFERENCES inn_reach_location (id),
    CONSTRAINT fk_library_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
);
