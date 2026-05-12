-- Item hold transaction in ITEM_SHIPPED state (state=10)
INSERT INTO transaction_pickup_location (id, pickup_loc_code, print_name, delivery_stop) VALUES
('956214e2-6397-49ed-817b-70cc03089951', 'pickupLocCode2', 'printName2', 'deliveryStop2');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, title, author, folio_patron_id, folio_item_id, due_date_time, folio_request_id, folio_loan_id,
folio_item_barcode, folio_patron_barcode, folio_instance_id, folio_holding_id, central_patron_type, patron_name) VALUES
('891bfff3-ba79-4beb-8c25-f714f14c6a31', 1632039760, '956214e2-6397-49ed-817b-70cc03089951',
 'u6ct3wssbnhxvip3sobwmxvhoa', 'qwe56', 'asd78', 'item2', 2, extract(epoch from current_timestamp), 'title2', 'author2',
 'a7853dda-520b-4f7a-a1fb-9383665ea770', '4def31b0-2b60-4531-ad44-7eab60fa5428', '1640091901',
 '26278b3a-de32-4deb-b81b-896637b3dbeb', '06e820e3-71a0-455e-8c73-3963aea677d4', 'DEF-def-5678', '000002',
 '891bfff3-ba79-4beb-8c25-f714f14c6a32', '891bfff3-ba79-4beb-8c25-f714f14c6a33', 1, 'patronName2');

INSERT INTO transaction_item_hold (id) VALUES
('891bfff3-ba79-4beb-8c25-f714f14c6a31');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('ab2393a1-acc4-4849-82ac-8cc0c37339e1', 'tracking2', 'd2ir', 10, 0, '891bfff3-ba79-4beb-8c25-f714f14c6a31');

