DELETE FROM user_custom_field_mapping;

ALTER TABLE user_custom_field_mapping
  ALTER COLUMN custom_field_id TYPE VARCHAR(256);

ALTER TABLE user_custom_field_configured_options
  ADD CONSTRAINT unq_user_custom_field_options_value UNIQUE (user_custom_field_mapping_id, custom_field_value)
