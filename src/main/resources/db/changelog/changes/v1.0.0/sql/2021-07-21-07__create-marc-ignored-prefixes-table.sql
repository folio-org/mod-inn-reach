CREATE TABLE marc_ignored_prefixes
(
    field_configuration_id UUID NOT NULL,
    prefix VARCHAR(255) NOT NULL,
    CONSTRAINT pk_ignored_prefixes PRIMARY KEY (field_configuration_id, prefix),
    CONSTRAINT fk_ignored_prefixes_field_configuration FOREIGN KEY (field_configuration_id)
    REFERENCES marc_field_configuration (id) ON DELETE CASCADE
);
