CREATE TABLE non_lendable_location
(
    item_contribution_options_configuration_id UUID NOT NULL,
    location_id UUID NOT NULL,
    CONSTRAINT pk_non_lendable_location PRIMARY KEY (item_contribution_options_configuration_id, location_id),
    CONSTRAINT fk_non_lendable_location_item_contribution_options_configuration
    FOREIGN KEY (item_contribution_options_configuration_id)
    REFERENCES item_contribution_options_configuration (central_server_id) ON DELETE CASCADE
);
