CREATE TABLE patron_group
(
    patron_type_mapping_id UUID NOT NULL,
    patron_group_id UUID NOT NULL,
    CONSTRAINT pk_patron_group PRIMARY KEY (patron_type_mapping_id, patron_group_id),
    CONSTRAINT fk_patron_type_mapping_id FOREIGN KEY (patron_type_mapping_id)
    REFERENCES patron_type_mapping (id) ON DELETE CASCADE
);
