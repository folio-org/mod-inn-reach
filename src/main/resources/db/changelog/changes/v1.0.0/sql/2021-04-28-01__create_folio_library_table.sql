CREATE TABLE folio_library
(
    local_agency_id  UUID NOT NULL,
    folio_library_id UUID NOT NULL,
    PRIMARY KEY (local_agency_id, folio_library_id),
    FOREIGN KEY (local_agency_id) REFERENCES local_agency (id)
);
