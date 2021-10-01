ALTER TABLE transaction_hold DROP CONSTRAINT transaction_hold_item_id_check;
ALTER TABLE transaction_hold ALTER COLUMN item_id TYPE UUID USING item_id::uuid;
