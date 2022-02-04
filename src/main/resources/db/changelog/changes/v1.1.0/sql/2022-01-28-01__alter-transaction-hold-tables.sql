
ALTER TABLE transaction_hold
  ALTER COLUMN patron_id DROP NOT NULL;
ALTER TABLE transaction_item_hold
  ALTER COLUMN patron_name DROP NOT NULL;
