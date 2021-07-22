CREATE TABLE marc_excluded_marc_fields
(
    marc_transformation_options_settings_id UUID NOT NULL,
    marc_field VARCHAR(255) NOT NULL,
    CONSTRAINT pk_excluded_marc_fields PRIMARY KEY (marc_transformation_options_settings_id, marc_field),
    CONSTRAINT fk_excluded_marc_fields_marc_transformation_options_settings FOREIGN KEY (marc_transformation_options_settings_id)
    REFERENCES marc_transformation_options_settings (id) ON DELETE CASCADE
);
