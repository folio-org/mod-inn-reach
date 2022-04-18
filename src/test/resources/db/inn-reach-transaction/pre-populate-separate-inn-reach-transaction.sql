INSERT INTO inn_reach_recall_user (id, user_id) VALUES ('1c0f40a9-a140-4ab8-9cdd-c52ecae7415b', 'f75ffab1-2e2f-43be-b159-3031e2cfc458');

INSERT INTO central_server(id, name, description, local_server_code, central_server_code, central_server_address, loan_type_id,
 inn_reach_recall_user_id) VALUES ('cfae4887-f8fb-4e4c-a5cc-34f74e353cf8', 'other', 'other description', 'def12', 'd5ir',
 'https://rssandbox-api.iii.com', 'be248906-34e7-4b6e-a09b-a2d31025ae09', '1c0f40a9-a140-4ab8-9cdd-c52ecae7415b');

INSERT INTO transaction_pickup_location (id, pickup_loc_code, display_name, print_name, delivery_stop) VALUES
 ('f66fa565-f94e-4984-9642-87196009feb4', 'pickupLocCode3', 'displayName3', 'printName3', 'deliveryStop3');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, title, folio_patron_id, folio_item_id, due_date_time, folio_request_id, folio_loan_id,
folio_item_barcode, folio_patron_barcode, folio_instance_id, folio_holding_id, central_patron_type, patron_name) VALUES
 ('c18ad51a-6757-4e02-b2cd-dda691033099', extract(epoch from current_timestamp), 'f66fa565-f94e-4984-9642-87196009feb4',
 'fd76hs7wqjez3cj3i77z56zyam', 'nwe14', 'bsd79', 'item4', 4, null, 'TITLE4',
 '2f14a0b1-2660-4e1c-923c-dec8b989207a', 'bea8b22f-c9a9-4303-b318-dcd9439d8c3c', '1640091807',
 '18a4fbb1-16c2-45f3-94e0-d0f427393f35', '3c9f9745-e26b-4173-aa47-bedbcbdc6d31', 'MHI-ghi-9014', '000004',
 '006d8c3a-9926-47dc-b271-7d66b2e4ffa1', 'c01d704f-2d8a-46a6-821a-1a873124dc15', 4, 'patronName4');

INSERT INTO transaction_item_hold (id) VALUES
 ('c18ad51a-6757-4e02-b2cd-dda691033099');


INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('aa5daccd-8788-4bb7-8f9a-6ae0b21bd18d', 'tracking4', 'd5ir', 9, 0, 'c18ad51a-6757-4e02-b2cd-dda691033099');
