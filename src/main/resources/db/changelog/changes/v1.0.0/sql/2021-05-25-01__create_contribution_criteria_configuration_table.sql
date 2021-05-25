CREATE TABLE contribution_criteria_configuration
(
    central_server_id uuid NOT NULL,
    created_by CHARACTER VARYING(255),
    created_date TIMESTAMP,
    last_modified_by CHARACTER VARYING(255),
    last_modified_date TIMESTAMP,
    update_counter INTEGER,
    CONSTRAINT contribution_criteria_configuration_pkey PRIMARY KEY (central_server_id)
);
