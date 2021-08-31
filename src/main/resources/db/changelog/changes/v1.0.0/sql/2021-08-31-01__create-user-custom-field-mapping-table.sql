CREATE TABLE user_custom_field_mapping
(
    id UUID NOT NULL,
    custom_field_id UUID NOT NULL,
    custom_field_value VARCHAR(255) NOT NULL,
    agency_code VARCHAR(5) NOT NULL,
    central_server_id UUID NOT NULL,
    created_by_userid UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) DEFAULT 'SYSTEM',
    updated_by_userid UUID,
    updated_by_username VARCHAR(255),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    CONSTRAINT pk_user_custom_field_mapping PRIMARY KEY (id),
    CONSTRAINT unq_custom_field_central_server UNIQUE (custom_field_id, custom_field_value, central_server_id),
    CONSTRAINT fk_user_custom_field_mapping_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);
