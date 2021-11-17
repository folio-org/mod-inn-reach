INSERT INTO transaction_pickup_location (id, pickup_loc_code, display_name, print_name, delivery_stop) VALUES
('5d8e62f8-53b8-4e40-b4cd-3cdb36721f7b', 'pickupLocCode1', 'displayName1', 'printName1', 'deliveryStop1');

INSERT INTO transaction_hold (id, transaction_time, pickup_location_id, patron_id, patron_agency_code, item_agency_code, item_id,
central_item_type, need_before, folio_patron_id, folio_item_id, folio_request_id, folio_loan_id, folio_item_barcode) VALUES
('d62d667c-d693-4423-bdff-3c69f9518cf7', extract(epoch from current_timestamp), '5d8e62f8-53b8-4e40-b4cd-3cdb36721f7b',
 'patron1', 'qwe12', 'asd34', 'item1', 1, extract(epoch from current_timestamp), '4154a604-4d5a-4d8e-9160-057fc7b6e6b8', '9a326225-6530-41cc-9399-a61987bfab3c',
 'ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a', 'fd5109c7-8934-4294-9504-c1a4a4f07c96', '1111111');

INSERT INTO transaction_patron_hold (id, title, author, call_number, shipped_item_barcode) VALUES
('d62d667c-d693-4423-bdff-3c69f9518cf7', 'title1', 'author1', '0123456789', '1111111');

INSERT INTO inn_reach_transaction (id, tracking_id, central_server_code, state, type, transaction_hold_id) VALUES
('7106c3ac-890a-4126-bf9b-a10b67555b6e', 'tracking1', 'd2ir', 10, 1, 'd62d667c-d693-4423-bdff-3c69f9518cf7');
