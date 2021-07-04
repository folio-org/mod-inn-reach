CREATE TABLE location
(
    item_contribution_options_configuration_id UUID NOT NULL,
    location_id UUID NOT NULL,
    PRIMARY KEY (item_contribution_options_configuration_id, location_id),
    FOREIGN KEY (item_contribution_options_configuration_id) REFERENCES item_contribution_options_configuration (central_server_id),
    FOREIGN KEY (location_id) REFERENCES inn_reach_location (id)
);
