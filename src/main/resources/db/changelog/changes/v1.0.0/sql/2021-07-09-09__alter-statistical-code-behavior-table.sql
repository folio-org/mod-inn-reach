ALTER TABLE statistical_code_behavior
    DROP CONSTRAINT fk_statistical_code_behavior__criteria_configuration;

ALTER TABLE statistical_code_behavior ALTER COLUMN criteria_configuration_id SET NOT NULL;

ALTER TABLE statistical_code_behavior
    ADD CONSTRAINT fk_statistical_code_behavior__criteria_configuration FOREIGN KEY (criteria_configuration_id)
        REFERENCES contribution_criteria_configuration (central_server_id)
        ON DELETE CASCADE;
