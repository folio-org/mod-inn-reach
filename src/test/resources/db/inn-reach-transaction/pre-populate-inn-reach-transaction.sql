INSERT INTO transaction_pickup_location (id, pickup_loc_code, display_name, print_name, delivery_stop) VALUES
('809adcde-3e67-4822-9916-fd653a681358', 'pickupLocCode1', 'displayName1', 'printName1', 'deliveryStop1'),
('956214e2-6397-49ed-817b-70cc03089951', 'pickupLocCode2', 'displayName2', 'printName2', null),
('f66fa565-f94e-4984-9642-87196009feb4', 'pickupLocCode3', 'displayName3', 'printName3', 'deliveryStop3');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, folio_patron_id, folio_item_id, folio_request_id, folio_loan_id) VALUES
('76834d5a-08e8-45ea-84ca-4d9b10aa340c', CURRENT_TIMESTAMP, '809adcde-3e67-4822-9916-fd653a681358',
 '5335cd61-0ec8-4a04-94a7-c36370d9d26a', 'qwe12', 'asd34', 'a6327eeb-e2ae-461e-9022-7abbfeece5c5', 1,
 CURRENT_TIMESTAMP, '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', '9a326225-6530-41cc-9399-a61987bfab3c',
 'ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a', 'fd5109c7-8934-4294-9504-c1a4a4f07c96'),
('891bfff3-ba79-4beb-8c25-f714f14c6a31', CURRENT_TIMESTAMP, '956214e2-6397-49ed-817b-70cc03089951',
 'f2cb071b-55df-47e0-9e6e-54ea69728447', 'qwe56', 'asd78', 'd2adb00c-dced-4068-b752-80fac5607e1f', 2,
 CURRENT_TIMESTAMP, 'a7853dda-520b-4f7a-a1fb-9383665ea770', '4def31b0-2b60-4531-ad44-7eab60fa5428',
 '26278b3a-de32-4deb-b81b-896637b3dbeb', '06e820e3-71a0-455e-8c73-3963aea677d4'),
('709c1075-0378-48af-a682-b4e7ac170423', CURRENT_TIMESTAMP, 'f66fa565-f94e-4984-9642-87196009feb4',
'a919efc3-9067-4ae4-a60f-9c01cb264646', 'qwe12', 'asd78', '1d0e38cd-351f-4958-bcfb-403b39a216e8', 3,
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
('0aab1720-14b4-4210-9a19-0d0bf1cd64d3', '65097d7c-2697-468d-ad20-1568d9cffccc', 'fli01', 1, 1, '76834d5a-08e8-45ea-84ca-4d9b10aa340c'),
('ab2393a1-acc4-4849-82ac-8cc0c37339e1', '86ace75a-7d17-4320-8363-1d0f0283cb42', 'fli01', 0, 0, '891bfff3-ba79-4beb-8c25-f714f14c6a31'),
('79b0a1fb-55be-4e55-9d84-01303aaec1ce', '4e00c413-381e-4883-9c1c-2a9de6fb71e3', 'fli01', 2, 2, '709c1075-0378-48af-a682-b4e7ac170423');
