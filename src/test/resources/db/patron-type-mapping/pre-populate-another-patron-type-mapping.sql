INSERT INTO patron_type_mapping(id, patron_type, description, central_server_id, created_by, created_date,
last_modified_by, last_modified_date) VALUES
('70649b94-da26-48fa-a2e8-a90dfb381027', 2, 'description2', 'cfae4887-f8fb-4e4c-a5cc-34f74e353cf8',
 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

 INSERT INTO patron_group (patron_type_mapping_id, patron_group_id) VALUES
 ('70649b94-da26-48fa-a2e8-a90dfb381027', '943b91d6-cd24-4c8f-966a-6063b7704c60'),
 ('70649b94-da26-48fa-a2e8-a90dfb381027', '8c6d1879-c506-400a-a810-bd4281800f56');
