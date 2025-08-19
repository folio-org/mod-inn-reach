INSERT INTO transaction_pickup_location (id, pickup_loc_code, print_name, delivery_stop) VALUES
('809adcde-3e67-4822-9916-fd653a681358', 'pickupLocCode1', 'printName1', 'deliveryStop1');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, title, author, folio_patron_id, folio_item_id, due_date_time, folio_request_id, folio_loan_id,
folio_item_barcode, folio_patron_barcode, folio_instance_id, folio_holding_id, central_patron_type, patron_name) VALUES
('76834d5a-08e8-45ea-84ca-4d9b10aa340c', extract(epoch from current_timestamp), '809adcde-3e67-4822-9916-fd653a681358',
 'ifkkmbcnljgy5elaav74pnxgxa', 'qwe12', 'asd34', 'item1', 1, extract(epoch from current_timestamp), 'title1', 'author1',
 '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', null, null, null, null, null, '000001', null, null, 2, 'patronName1');

INSERT INTO transaction_patron_hold (id) VALUES
('76834d5a-08e8-45ea-84ca-4d9b10aa340c');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('0aab1720-14b4-4210-9a19-0d0bf1cd64d3', 'tracking1', 'd2ir', 1, 1, '76834d5a-08e8-45ea-84ca-4d9b10aa340c');
