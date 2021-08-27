CREATE TABLE item_type_mapping
(
    id UUID NOT NULL,
    central_item_type SMALLINT NOT NULL CHECK(central_item_type BETWEEN 0 AND 255),
    material_type_id UUID NOT NULL,
    central_server_id UUID NOT NULL,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_item_type_mapping PRIMARY KEY (id),
    CONSTRAINT unq_item_type_central_server UNIQUE (central_item_type, central_server_id),
    CONSTRAINT fk_item_type_mapping_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);
