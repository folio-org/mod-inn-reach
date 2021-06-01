CREATE TABLE contribution_criteria_excluded_location
(
    id uuid NOT NULL,
    excluded_location_id uuid,
    criteria_configuration_id uuid,
    CONSTRAINT contribution_criteria_excluded_location_pkey PRIMARY KEY (id),
    CONSTRAINT fk_excluded_location__criteria_configuration FOREIGN KEY (criteria_configuration_id)
        REFERENCES contribution_criteria_configuration (central_server_id) MATCH SIMPLE
);
