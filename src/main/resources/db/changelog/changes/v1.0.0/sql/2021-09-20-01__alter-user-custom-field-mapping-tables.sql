CREATE TABLE user_custom_field_configured_options
(
    user_custom_field_mapping_id UUID NOT NULL,
    custom_field_value VARCHAR(255) NOT NULL,
    agency_code VARCHAR(5) NOT NULL,
    CONSTRAINT pk_configured_options PRIMARY KEY (user_custom_field_mapping_id, custom_field_value),
    CONSTRAINT fk_configured_options_custom_field FOREIGN KEY (user_custom_field_mapping_id)
    REFERENCES user_custom_field_mapping (id) ON DELETE CASCADE
);

INSERT INTO user_custom_field_configured_options(user_custom_field_mapping_id, custom_field_value, agency_code)
SELECT id, custom_field_value, agency_code FROM
(SELECT DISTINCT ON (central_server_id) central_server_id, id, custom_field_id FROM user_custom_field_mapping) ids INNER JOIN
(SELECT custom_field_id, central_server_id, custom_field_value, agency_code FROM user_custom_field_mapping) fields
ON ids.central_server_id = fields.central_server_id AND ids.custom_field_id = fields.custom_field_id;

DELETE FROM user_custom_field_mapping WHERE user_custom_field_mapping.id NOT IN
(SELECT DISTINCT user_custom_field_mapping_id FROM user_custom_field_configured_options);

ALTER TABLE user_custom_field_mapping DROP CONSTRAINT unq_custom_field_central_server;
ALTER TABLE user_custom_field_mapping DROP COLUMN custom_field_value;
ALTER TABLE user_custom_field_mapping DROP COLUMN agency_code;
ALTER TABLE user_custom_field_mapping ADD CONSTRAINT unq_central_server UNIQUE (central_server_id);
