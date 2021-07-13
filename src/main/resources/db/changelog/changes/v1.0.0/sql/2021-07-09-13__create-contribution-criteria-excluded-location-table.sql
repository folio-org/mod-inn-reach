CREATE TABLE contribution_criteria_excluded_location
(
    contribution_criteria_id            UUID         NOT NULL,
    location_id                         UUID         NOT NULL,
    CONSTRAINT pk_contribution_criteria_excl_location PRIMARY KEY (contribution_criteria_id, location_id),
    CONSTRAINT fk_contribution_criteria_excl_location FOREIGN KEY (contribution_criteria_id)
        REFERENCES contribution_criteria_configuration (id) ON DELETE CASCADE
);
