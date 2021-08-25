CREATE TABLE item_type_mapping
(
    id UUID NOT NULL,
    central_item_type SMALLINT NOT NULL CHECK(central_item_type BETWEEN 0 AND 255),
    material_type_id UUID NOT NULL,
    local_server_code VARCHAR(5) NOT NULL,
    inn_reach_central_server_id UUID NOT NULL,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_item_type_mapping PRIMARY KEY (id),
    CONSTRAINT unq_item_type_inn_reach_central_server UNIQUE (central_item_type, local_server_code, inn_reach_central_server_id),
    CONSTRAINT fk_item_type_mapping_local_server_code FOREIGN KEY (local_server_code)
    REFERENCES central_server (local_server_code) ON DELETE CASCADE
);
