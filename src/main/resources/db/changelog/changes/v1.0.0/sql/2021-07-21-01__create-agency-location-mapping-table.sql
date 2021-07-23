CREATE TABLE agency_location_mapping
(
    central_server_id  UUID         NOT NULL,
    library_id         UUID         NOT NULL,
    location_id        UUID         NOT NULL,
    created_by         VARCHAR(255) NOT NULL DEFAULT 'admin',
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_agency_location_mapping PRIMARY KEY (central_server_id),
    CONSTRAINT fk_agency_location_mapping_cs_id FOREIGN KEY (central_server_id)
        REFERENCES central_server (id) ON DELETE CASCADE
);
