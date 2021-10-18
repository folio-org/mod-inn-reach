DELETE FROM user_custom_field_mapping;

ALTER TABLE user_custom_field_mapping
  ALTER COLUMN custom_field_id TYPE VARCHAR(256);
