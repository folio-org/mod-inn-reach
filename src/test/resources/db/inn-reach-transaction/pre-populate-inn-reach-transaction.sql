INSERT INTO transaction_pickup_location (id, pickup_loc_code, display_name, print_name, delivery_stop) VALUES
('809adcde-3e67-4822-9916-fd653a681358', 'pickupLocCode1', 'displayName1', 'printName1', 'deliveryStop1'),
('956214e2-6397-49ed-817b-70cc03089951', 'pickupLocCode2', 'displayName2', 'printName2', null),
('f66fa565-f94e-4984-9642-87196009feb4', 'pickupLocCode3', 'displayName3', 'printName3', 'deliveryStop3');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, folio_patron_id, folio_item_id, folio_request_id, folio_loan_id) VALUES
('76834d5a-08e8-45ea-84ca-4d9b10aa340c', extract(epoch from current_timestamp), '809adcde-3e67-4822-9916-fd653a681358',
 'patron1', 'qwe12', 'asd34', 'item1', 1,
 extract(epoch from current_timestamp), '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', '9a326225-6530-41cc-9399-a61987bfab3c',
 'ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a', 'fd5109c7-8934-4294-9504-c1a4a4f07c96'),
('891bfff3-ba79-4beb-8c25-f714f14c6a31', 1632039760, '956214e2-6397-49ed-817b-70cc03089951',
 'patron2', 'qwe56', 'asd78', 'item2', 2,
 extract(epoch from current_timestamp), 'a7853dda-520b-4f7a-a1fb-9383665ea770', '4def31b0-2b60-4531-ad44-7eab60fa5428',
 '26278b3a-de32-4deb-b81b-896637b3dbeb', '06e820e3-71a0-455e-8c73-3963aea677d4'),
('709c1075-0378-48af-a682-b4e7ac170423', extract(epoch from current_timestamp), 'f66fa565-f94e-4984-9642-87196009feb4',
'patron3', 'qwe12', 'asd78', 'item3', 3,
null, 'a8ffe3cb-f682-499d-893b-47ff9efb3803', 'c633da85-8112-4453-af9c-c250e417179d',
 '4106d147-9085-4dfa-a59f-b8d50d551a48', '7b43b4bc-3a57-4506-815a-78b01c38a2a1');

INSERT INTO transaction_patron_hold (id, title, author, call_number, shipped_item_barcode) VALUES
('76834d5a-08e8-45ea-84ca-4d9b10aa340c', 'title1', 'author1', '0123456789', 'ABC-abc-1234');

INSERT INTO transaction_item_hold (id, central_patron_type, patron_name) VALUES
('891bfff3-ba79-4beb-8c25-f714f14c6a31', 1, 'patronName1');

INSERT INTO transaction_local_hold (id, patron_home_library, patron_phone, title, author, call_number,
central_patron_type, patron_name) VALUES
('709c1075-0378-48af-a682-b4e7ac170423', 'patronHomeLibrary1', null, 'title1', 'author1', '0123456789', 0, 'patronName1');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('0aab1720-14b4-4210-9a19-0d0bf1cd64d3', 'tracking1', 'fli01', 1, 1, '76834d5a-08e8-45ea-84ca-4d9b10aa340c'),
('ab2393a1-acc4-4849-82ac-8cc0c37339e1', 'tracking2', 'fli01', 0, 0, '891bfff3-ba79-4beb-8c25-f714f14c6a31'),
('79b0a1fb-55be-4e55-9d84-01303aaec1ce', 'tracking3', 'fli01', 2, 2, '709c1075-0378-48af-a682-b4e7ac170423');
