CREATE TABLE non_lendable_loan_type
(
    item_contribution_options_configuration_id UUID NOT NULL,
    loan_type_id UUID NOT NULL,
    PRIMARY KEY (item_contribution_options_configuration_id, loan_type_id),
    FOREIGN KEY (item_contribution_options_configuration_id) REFERENCES item_contribution_options_configuration (central_server_id)
);
