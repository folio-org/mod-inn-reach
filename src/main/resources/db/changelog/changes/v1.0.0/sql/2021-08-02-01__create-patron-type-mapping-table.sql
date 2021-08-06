CREATE TABLE patron_type_mapping
(
    id UUID NOT NULL,
    patron_group_id UUID NOT NULL UNIQUE,
    patron_type SMALLINT NOT NULL CHECK(patron_type BETWEEN 0 AND 255),
    description TEXT,
    central_server_id UUID NOT NULL,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_patron_type_mapping PRIMARY KEY (id),
    CONSTRAINT fk_patron_type_mapping_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);
