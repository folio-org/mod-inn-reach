CREATE TABLE marc_transformation_options_settings
(
    id UUID NOT NULL,
    central_server_id UUID NOT NULL,
    config_is_active boolean NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_marc_transformation_options_settings PRIMARY KEY (id),
    CONSTRAINT fk_marc_transformation_options_settings_central_server FOREIGN KEY (central_server_id)
    REFERENCES central_server (id) ON DELETE CASCADE
);
