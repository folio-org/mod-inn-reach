ALTER TABLE contribution_criteria_excluded_location
    DROP CONSTRAINT fk_excluded_location__criteria_configuration;

ALTER TABLE contribution_criteria_excluded_location ALTER COLUMN criteria_configuration_id SET NOT NULL;

ALTER TABLE contribution_criteria_excluded_location
    ADD CONSTRAINT fk_excluded_location__criteria_configuration FOREIGN KEY (criteria_configuration_id)
        REFERENCES contribution_criteria_configuration (central_server_id)
        ON DELETE CASCADE;
