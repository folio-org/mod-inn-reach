INSERT INTO marc_transformation_options_settings (id, central_server_id, config_is_active,
created_by, created_date, last_modified_by, last_modified_date)
VALUES ('51768f15-41e8-494d-bc4d-a308568e7052', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO marc_excluded_marc_fields (marc_transformation_options_settings_id, marc_field)
VALUES ('51768f15-41e8-494d-bc4d-a308568e7052', '086');

INSERT INTO marc_field_configuration (id, resource_identifier_type_id, strip_prefix, marc_transformation_options_settings_id,
created_by, created_date, last_modified_by, last_modified_date)
VALUES ('0ac6f04a-b077-44a7-8a56-48a48421bfcf', '7506eb2a-aefc-49e9-af63-9b1f29a0f9a9', true, '51768f15-41e8-494d-bc4d-a308568e7052',
'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
       ('a07521f8-1532-4eea-a219-87275486329b', '7e591197-f335-4afb-bc6d-a6d76ca3bace', true, '51768f15-41e8-494d-bc4d-a308568e7052',
'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
       ('9f972d19-44af-40e8-bb46-dc32a8da3df5', '7e591197-f335-4afb-bc6d-a6d76ca3bace', true, '51768f15-41e8-494d-bc4d-a308568e7052',
'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO marc_ignored_prefixes (field_configuration_id, prefix)
VALUES ('0ac6f04a-b077-44a7-8a56-48a48421bfcf', 'FOO'), ('0ac6f04a-b077-44a7-8a56-48a48421bfcf', 'fod');
