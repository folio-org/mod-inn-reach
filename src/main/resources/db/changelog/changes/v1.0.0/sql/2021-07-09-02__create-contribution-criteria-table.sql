CREATE TABLE contribution_criteria_configuration
(
    id                                  UUID         NOT NULL,
    contribute_but_suppress_code_id     UUID         NOT NULL,
    contribute_as_system_owned_code_id  UUID         NOT NULL,
    do_not_contribute_code_id           UUID         NOT NULL,
    central_server_id                   UUID         NOT NULL,
    created_by                          VARCHAR(255) NOT NULL,
    created_date                        TIMESTAMP    NOT NULL,
    last_modified_by                    VARCHAR(255),
    last_modified_date                  TIMESTAMP,
    CONSTRAINT pk_contribution_criteria PRIMARY KEY (id),
    CONSTRAINT unq_contribution_criteria_server UNIQUE (central_server_id),
    CONSTRAINT fk_contribution_criteria_central_server FOREIGN KEY (central_server_id)
        REFERENCES central_server (id) ON DELETE CASCADE
);
