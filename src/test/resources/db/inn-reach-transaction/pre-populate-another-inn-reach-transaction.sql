INSERT INTO transaction_pickup_location (id, pickup_loc_code, display_name, print_name, delivery_stop) VALUES
('f1b9adc7-82f4-4146-84d3-681a0f2a2cc3', 'pickupLocCode1', 'displayName1', 'printName1', 'deliveryStop1'),
('d5954db7-374e-4090-a345-4561e8152afa', 'pickupLocCode2', 'displayName2', 'printName2', 'deliveryStop2'),
('4d4f5125-bc41-4613-9bb3-54d2aafe49a2', 'pickupLocCode3', 'displayName3', 'printName3', null);

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, folio_patron_id, folio_item_id, folio_request_id, folio_loan_id,
folio_item_barcode, folio_patron_barcode) VALUES
 ('d90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d', 1636020000, 'f1b9adc7-82f4-4146-84d3-681a0f2a2cc3',
 'patron1', 'qwe12', 'asd34', 'item1', 1, 1638525600, '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', '9a326225-6530-41cc-9399-a61987bfab3c',
 'ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a', 'fd5109c7-8934-4294-9504-c1a4a4f07c96', 'ABC-abc-1234', '000001'),
('3ba02508-f216-4335-9812-ccc11a22a617', 1636106400, 'd5954db7-374e-4090-a345-4561e8152afa',
 'patron2', 'qwe56', 'asd78', 'item2', 2, null, 'a7853dda-520b-4f7a-a1fb-9383665ea770', '4def31b0-2b60-4531-ad44-7eab60fa5428',
 '26278b3a-de32-4deb-b81b-896637b3dbeb', '06e820e3-71a0-455e-8c73-3963aea677d4', 'DEF-def-5678', '000002'),
('d1154622-f7a7-4759-bec5-911406d8edc5', 1636192800, '4d4f5125-bc41-4613-9bb3-54d2aafe49a2',
'patron3', 'qwe12', 'asd78', 'item3', 3, null, 'a8ffe3cb-f682-499d-893b-47ff9efb3803', 'c633da85-8112-4453-af9c-c250e417179d',
 '4106d147-9085-4dfa-a59f-b8d50d551a48', '7b43b4bc-3a57-4506-815a-78b01c38a2a1', 'GHI-ghi-9012', '000003');

INSERT INTO transaction_patron_hold (id, title, author, call_number, shipped_item_barcode) VALUES
('d90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d', 'title2', 'AUTHOR2', '9876543210', '123-abc-ABCD');

INSERT INTO transaction_item_hold (id, central_patron_type, patron_name) VALUES
('3ba02508-f216-4335-9812-ccc11a22a617', 2, 'patronName2');

INSERT INTO transaction_local_hold (id, patron_home_library, patron_phone, title, author, call_number,
central_patron_type, patron_name) VALUES
('d1154622-f7a7-4759-bec5-911406d8edc5', 'patronHomeLibrary2', '123123123', 'title2', 'AUTHOR2', '9876543210', 1, 'patronName2');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('0a8a62e1-77e3-4f36-8408-3b954a351b70', 'tracking4', 'd2ir1', 1, 1, 'd90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d'),
('2d219267-4e51-4843-b2c6-d9c44d313739', 'tracking5', 'd2ir1', 0, 0, '3ba02508-f216-4335-9812-ccc11a22a617'),
('14eed838-d15c-4d4e-9d42-bebf99356e76', 'tracking6', 'd2ir1', 2, 2, 'd1154622-f7a7-4759-bec5-911406d8edc5');
