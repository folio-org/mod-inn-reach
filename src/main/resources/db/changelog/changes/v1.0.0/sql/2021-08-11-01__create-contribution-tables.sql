CREATE TABLE contribution
(
    id                    UUID         NOT NULL,
    central_server_id     UUID         NOT NULL,
    status                SMALLINT     NOT NULL DEFAULT 0,
    complete_date         TIMESTAMP,
    records_total         BIGINT       NOT NULL,
    records_processed     BIGINT       NOT NULL DEFAULT 0,
    records_contributed   BIGINT       NOT NULL DEFAULT 0,
    records_updated       BIGINT       NOT NULL DEFAULT 0,
    records_decontributed BIGINT       NOT NULL DEFAULT 0,
    created_by            VARCHAR(255) NOT NULL DEFAULT 'admin',
    created_date          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by      VARCHAR(255),
    last_modified_date    TIMESTAMP,
    CONSTRAINT pk_contribution PRIMARY KEY (id),
    CONSTRAINT fk_contribution_cs_id FOREIGN KEY (central_server_id)
        REFERENCES central_server (id) ON DELETE CASCADE
);

CREATE TABLE contribution_error
(
    id              UUID          NOT NULL,
    contribution_id UUID          NOT NULL,
    message         VARCHAR(1023) NOT NULL,
    record_id       UUID          NOT NULL,
    CONSTRAINT pk_contribution_error PRIMARY KEY (id),
    CONSTRAINT fk_contribution_error_contribution_id FOREIGN KEY (contribution_id)
        REFERENCES contribution (id) ON DELETE CASCADE
)
