ALTER TABLE transaction_hold DROP COLUMN patron_id;
ALTER TABLE transaction_hold ADD COLUMN patron_id VARCHAR(32) CHECK(patron_id SIMILAR TO '[a-z,0-9]{1,32}');
ALTER TABLE transaction_item_hold DROP COLUMN patron_name;
ALTER TABLE transaction_item_hold ADD COLUMN patron_name VARCHAR(255);
