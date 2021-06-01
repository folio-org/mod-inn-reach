CREATE TABLE material_type_mapping
(
    id                 UUID         NOT NULL,
    material_type_id   UUID         NOT NULL,
    central_item_type  INTEGER      NOT NULL,
    central_server_id  UUID         NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_material_type_mapping PRIMARY KEY (id),
    CONSTRAINT unq_mtype_mapping_server_mtype UNIQUE (central_server_id, material_type_id),
    CONSTRAINT fk_mtype_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
);
