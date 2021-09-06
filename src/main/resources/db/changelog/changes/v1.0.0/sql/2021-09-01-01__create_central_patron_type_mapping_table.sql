CREATE TABLE central_patron_type_mapping
(
    id                  UUID         NOT NULL,
    central_patron_type INT          NOT NULL CHECK (central_patron_type BETWEEN 0 AND 255),
    folio_user_barcode  VARCHAR(255) NOT NULL,
    central_server_id   UUID         NOT NULL,
    created_by          VARCHAR(255) NOT NULL,
    created_date        TIMESTAMP    NOT NULL,
    last_modified_by    VARCHAR(255),
    last_modified_date  TIMESTAMP,
    CONSTRAINT pk_central_patron_type_mapping PRIMARY KEY (id),
    CONSTRAINT fk_central_patron_type_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id) ON DELETE CASCADE
);

