DELETE FROM item_contribution_options_configuration item1 USING item_contribution_options_configuration item2
WHERE item1.central_server_id = item2.central_server_id AND
GREATEST(item1.created_date, item1.last_modified_date) < GREATEST(item2.created_date, item2.last_modified_date);

ALTER TABLE item_contribution_options_configuration ADD CONSTRAINT unq_central_server_id UNIQUE (central_server_id);
