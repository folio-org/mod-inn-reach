ALTER TABLE transaction_hold ALTER COLUMN folio_patron_id DROP NOT NULL;
ALTER TABLE transaction_hold ALTER COLUMN folio_item_id DROP NOT NULL;
ALTER TABLE transaction_hold ALTER COLUMN folio_request_id DROP NOT NULL;

ALTER TABLE transaction_hold ALTER COLUMN transaction_time TYPE INTEGER USING CAST(EXTRACT(epoch FROM transaction_time) AS INTEGER);
ALTER TABLE transaction_hold ALTER COLUMN need_before TYPE INTEGER USING CAST(EXTRACT(epoch FROM need_before) AS INTEGER);
