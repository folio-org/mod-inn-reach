CREATE TABLE non_lendable_loan_type
(
    item_contribution_options_configuration_id UUID NOT NULL,
    loan_type_id UUID NOT NULL,
    CONSTRAINT pk_non_lendable_loan_type PRIMARY KEY (item_contribution_options_configuration_id, loan_type_id),
    CONSTRAINT fk_non_lendable_loan_type_item_contribution_options_configuration
    FOREIGN KEY (item_contribution_options_configuration_id)
    REFERENCES item_contribution_options_configuration (central_server_id) ON DELETE CASCADE
);
