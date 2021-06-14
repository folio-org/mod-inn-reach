CREATE TABLE location_mapping
(
    id                 UUID         NOT NULL,
    location_id        UUID         NOT NULL,
    ir_location_id     UUID         NOT NULL,
    central_server_id  UUID         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_location_mapping PRIMARY KEY (id),
    CONSTRAINT unq_location_mapping_server_loc_irloc UNIQUE (central_server_id, location_id, ir_location_id),
    CONSTRAINT fk_location_mapping_ir_location FOREIGN KEY (ir_location_id)
        REFERENCES inn_reach_location (id),
    CONSTRAINT fk_location_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
);
