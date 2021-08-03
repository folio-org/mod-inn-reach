INSERT INTO patron_type_mapping(id, patron_type, description, central_server_id, created_by, created_date,
last_modified_by, last_modified_date) VALUES
('5c39c67f-1373-4ec9-b356-fb71aba3e659', 1, 'description1', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO patron_group (patron_type_mapping_id, patron_group_id) VALUES
('5c39c67f-1373-4ec9-b356-fb71aba3e659', '54e17c4c-e315-4d20-8879-efc694dea1ce');
