CREATE TABLE marc_field_configuration
(
    id UUID NOT NULL,
    resource_identifier_type_id UUID NOT NULL,
    strip_prefix boolean NOT NULL,
    marc_transformation_options_settings_id UUID NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    CONSTRAINT pk_field_configuration PRIMARY KEY (id),
    CONSTRAINT fk_field_configuration_marc_transformation_options_settings FOREIGN KEY (marc_transformation_options_settings_id)
    REFERENCES marc_transformation_options_settings (id) ON DELETE CASCADE
);
