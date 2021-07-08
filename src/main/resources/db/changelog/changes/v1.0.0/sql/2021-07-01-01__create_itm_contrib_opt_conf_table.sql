CREATE TABLE item_contribution_options_configuration
(
    central_server_id UUID NOT NULL PRIMARY KEY,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    update_counter INTEGER,
    CONSTRAINT fk_item_contribution_options_configuration_central_server FOREIGN KEY (central_server_id) REFERENCES central_server (id)
);
