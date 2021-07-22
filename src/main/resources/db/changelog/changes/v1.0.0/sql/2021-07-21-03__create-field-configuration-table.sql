CREATE TABLE field_configuration
(
    id UUID NOT NULL,
    resource_identifier_type_id UUID NOT NULL,
    strip_prefix boolean NOT NULL,
    marc_transformation_options_settings_id UUID NOT NULL,
    CONSTRAINT pk_field_configuration PRIMARY KEY (id),
    CONSTRAINT fk_field_configuration_marc_transformation_options_settings FOREIGN KEY (marc_transformation_options_settings_id)
    REFERENCES marc_transformation_options_settings (id) ON DELETE CASCADE
);
