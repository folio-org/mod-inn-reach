CREATE TABLE non_lendable_material_type
(
    item_contribution_options_configuration_id UUID NOT NULL,
    material_type_id UUID NOT NULL,
    PRIMARY KEY (item_contribution_options_configuration_id, material_type_id),
    FOREIGN KEY (item_contribution_options_configuration_id) REFERENCES item_contribution_options_configuration (central_server_id)
);
