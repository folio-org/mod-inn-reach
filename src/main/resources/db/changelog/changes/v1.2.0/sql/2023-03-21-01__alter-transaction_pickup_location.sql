ALTER TABLE transaction_pickup_location
DROP COLUMN delivery_stop;

ALTER TABLE transaction_pickup_location
RENAME COLUMN print_name TO delivery_stop;
