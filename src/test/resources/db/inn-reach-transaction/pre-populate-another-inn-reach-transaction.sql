INSERT INTO transaction_pickup_location (id, pickup_loc_code, print_name, delivery_stop) VALUES
('f1b9adc7-82f4-4146-84d3-681a0f2a2cc3', 'pickupLocCode1', 'printName1', 'deliveryStop1'),
('d5954db7-374e-4090-a345-4561e8152afa', 'pickupLocCode2', 'printName2', 'deliveryStop2'),
('4d4f5125-bc41-4613-9bb3-54d2aafe49a2', 'pickupLocCode3', 'printName3', 'deliveryStop3');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, title, author, folio_patron_id, folio_item_id, folio_request_id, folio_loan_id,
folio_item_barcode, folio_patron_barcode, central_patron_type, patron_name) VALUES
 ('d90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d', 1636020000, 'f1b9adc7-82f4-4146-84d3-681a0f2a2cc3',
 'ifkkmbcnljgy5elaav74pnxgxa', 'qwe12', 'asd34', 'item1', 1, 1638525600, 'title2', 'author2',
 '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', '9a326225-6530-41cc-9399-a61987bfab3c',
 'ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a', 'fd5109c7-8934-4294-9504-c1a4a4f07c96', 'ABC-abc-1234', '000001', 1, 'patronName1'),
('3ba02508-f216-4335-9812-ccc11a22a617', 1636106400, 'd5954db7-374e-4090-a345-4561e8152afa',
 'u6ct3wssbnhxvip3sobwmxvhoa', 'qwe56', 'asd78', 'item2', 2, null, 'title3', 'author3',
 'a7853dda-520b-4f7a-a1fb-9383665ea770', '4def31b0-2b60-4531-ad44-7eab60fa5428',
 '26278b3a-de32-4deb-b81b-896637b3dbeb', '06e820e3-71a0-455e-8c73-3963aea677d4', 'DEF-def-5678', '000002', 1, 'patronName2'),
('d1154622-f7a7-4759-bec5-911406d8edc5', 1636192800, '4d4f5125-bc41-4613-9bb3-54d2aafe49a2',
'vd76hs7wqjez3cj3i77z56zyam', 'qwe12', 'asd78', 'item3', 3, null, 'title2', 'author2',
'a8ffe3cb-f682-499d-893b-47ff9efb3803', 'c633da85-8112-4453-af9c-c250e417179d',
 '4106d147-9085-4dfa-a59f-b8d50d551a48', '7b43b4bc-3a57-4506-815a-78b01c38a2a1', 'GHI-ghi-9012', '000003', 1, 'patronName3');

INSERT INTO transaction_patron_hold (id, call_number, shipped_item_barcode) VALUES
('d90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d', '9876543210', '123-abc-ABCD');

INSERT INTO transaction_item_hold (id) VALUES
('3ba02508-f216-4335-9812-ccc11a22a617');

INSERT INTO transaction_local_hold (id, patron_home_library, patron_phone, call_number) VALUES
('d1154622-f7a7-4759-bec5-911406d8edc5', 'patronHomeLibrary2', '123123123','9876543210');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('0a8a62e1-77e3-4f36-8408-3b954a351b70', 'tracking4', 'd2ir1', 1, 1, 'd90cbfac-c70e-4d7c-a9b2-b5c00e6efe1d'),
('2d219267-4e51-4843-b2c6-d9c44d313739', 'tracking5', 'd2ir1', 0, 0, '3ba02508-f216-4335-9812-ccc11a22a617'),
('14eed838-d15c-4d4e-9d42-bebf99356e76', 'tracking6', 'd2ir1', 2, 2, 'd1154622-f7a7-4759-bec5-911406d8edc5');
