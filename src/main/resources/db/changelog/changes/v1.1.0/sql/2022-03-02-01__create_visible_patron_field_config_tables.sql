CREATE TABLE visible_patron_field_config
(
    id                  UUID         NOT NULL,
    central_server_id UUID NOT NULL,
    created_by_userid   UUID         NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid   UUID,
    updated_by_username VARCHAR(255),
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    CONSTRAINT pk_visible_patron_field_config PRIMARY KEY (id),
    CONSTRAINT fk_visible_patron_field_config_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);

CREATE TABLE visible_patron_fields
(
    visible_patron_field_config_id UUID NOT NULL,
    visible_patron_field SMALLINT NOT NULL,
    PRIMARY KEY (visible_patron_field_config_id, visible_patron_field),
    FOREIGN KEY (visible_patron_field_config_id) REFERENCES visible_patron_field_config (id)
);

CREATE TABLE visible_patron_user_custom_fields
(
    visible_patron_field_config_id UUID NOT NULL,
    user_custom_field_id UUID NOT NULL,
    PRIMARY KEY (visible_patron_field_config_id, user_custom_field_id),
    FOREIGN KEY (visible_patron_field_config_id) REFERENCES visible_patron_field_config (id)
);

