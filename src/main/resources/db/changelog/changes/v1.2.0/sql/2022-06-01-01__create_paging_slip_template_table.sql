CREATE TABLE paging_slip_template
(
    id                  UUID         NOT NULL,
    description         VARCHAR(1023),
    template            VARCHAR(2047)NOT NULL,
    central_server_id   UUID         NOT NULL UNIQUE,
    created_by_userid   UUID         NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid   UUID,
    updated_by_username VARCHAR(255),
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    CONSTRAINT pk_paging_slip_template PRIMARY KEY (id),
    CONSTRAINT fk_paging_slip_template_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);
