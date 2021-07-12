CREATE TABLE not_available_item_status
(
    item_contribution_options_configuration_id UUID NOT NULL,
    item_status VARCHAR(255) NOT NULL CHECK(item_status NOT IN ('Available', 'Checked out', 'Paged', 'In transit', 'Declared lost', 'Withdrawn', 'Awaiting pickup', 'Awaiting')),
    CONSTRAINT pk_not_available_item_status PRIMARY KEY (item_contribution_options_configuration_id, item_status),
    CONSTRAINT fk_not_available_item_status_item_contribution_options_configuration FOREIGN KEY (item_contribution_options_configuration_id)
    REFERENCES item_contribution_options_configuration (central_server_id) ON DELETE CASCADE
);
