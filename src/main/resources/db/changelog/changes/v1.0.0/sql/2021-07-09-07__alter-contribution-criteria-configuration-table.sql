ALTER TABLE contribution_criteria_configuration
    ADD CONSTRAINT contribution_criteria_configuration_fkey FOREIGN KEY (central_server_id) REFERENCES central_server (id)
        ON DELETE CASCADE;
