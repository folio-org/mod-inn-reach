CREATE TABLE agency_location_ac_mapping
(
    id                      UUID         NOT NULL,
    agency_code             VARCHAR(5)   NOT NULL,
    local_server_mapping_id UUID         NOT NULL,
    library_id              UUID         NOT NULL,
    location_id             UUID         NOT NULL,
    created_by              VARCHAR(255) NOT NULL DEFAULT 'admin',
    created_date            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by        VARCHAR(255),
    last_modified_date      TIMESTAMP,
    CONSTRAINT pk_agency_location_ac_mapping PRIMARY KEY (id),
    CONSTRAINT unq_agency_location_ac_mapping_lsm_ac UNIQUE (local_server_mapping_id, agency_code),
    CONSTRAINT fk_agency_location_ac_mapping_lsm_id FOREIGN KEY (local_server_mapping_id)
        REFERENCES agency_location_lsc_mapping (id) ON DELETE CASCADE
);
