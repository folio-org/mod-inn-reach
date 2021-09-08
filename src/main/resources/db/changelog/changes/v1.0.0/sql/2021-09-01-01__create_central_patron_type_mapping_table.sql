CREATE TABLE central_patron_type_mapping
(
    id                  UUID         NOT NULL,
    central_patron_type INT          NOT NULL CHECK (central_patron_type BETWEEN 0 AND 255),
    folio_user_barcode  VARCHAR(255) NOT NULL,
    central_server_id   UUID         NOT NULL,
    created_by_userid   UUID         NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid   UUID,
    updated_by_username VARCHAR(255),
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    CONSTRAINT pk_central_patron_type_mapping PRIMARY KEY (id),
    CONSTRAINT fk_central_patron_type_mapping_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id) ON DELETE CASCADE
);

