INSERT INTO visible_patron_field_config(id, central_server_id)
	VALUES ('58173d4f-5dce-407a-8f63-80d1a0df3218', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2');

INSERT INTO visible_patron_fields(visible_patron_field_config_id, visible_patron_field)
	VALUES ('58173d4f-5dce-407a-8f63-80d1a0df3218', 2), ('58173d4f-5dce-407a-8f63-80d1a0df3218', 3),
	('58173d4f-5dce-407a-8f63-80d1a0df3218', 4);

INSERT INTO visible_patron_user_custom_fields(visible_patron_field_config_id, user_custom_field)
	VALUES ('58173d4f-5dce-407a-8f63-80d1a0df3218', 'field1'), ('58173d4f-5dce-407a-8f63-80d1a0df3218', 'field2');
