CREATE TABLE agency_location_lsc_mapping
(
    id                        UUID       NOT NULL,
    local_server_code         VARCHAR(5) NOT NULL,
    central_server_mapping_id UUID       NOT NULL,
    library_id                UUID       NOT NULL,
    location_id               UUID       NOT NULL,
    CONSTRAINT pk_agency_location_lsc_mapping PRIMARY KEY (id),
    CONSTRAINT unq_agency_location_lsc_mapping_csm_lsc UNIQUE (central_server_mapping_id, local_server_code),
    CONSTRAINT fk_agency_location_lsc_mapping_csm_id FOREIGN KEY (central_server_mapping_id)
        REFERENCES agency_location_mapping (central_server_id) ON DELETE CASCADE
);
