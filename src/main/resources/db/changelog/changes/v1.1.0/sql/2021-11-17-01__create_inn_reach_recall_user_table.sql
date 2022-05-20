CREATE TABLE inn_reach_recall_user
(
    id                  UUID         NOT NULL,
    user_id             UUID         NOT NULL,
    created_by_userid   UUID         NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid   UUID,
    updated_by_username VARCHAR(255),
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    CONSTRAINT pk_inn_reach_recall_user PRIMARY KEY (id)
);
