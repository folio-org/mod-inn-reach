BEGIN;
ALTER TABLE transaction_hold
  ADD COLUMN central_patron_type SMALLINT NOT NULL CHECK(central_patron_type BETWEEN 0 AND 255),
  ADD COLUMN patron_name VARCHAR(255);
COMMIT;

UPDATE transaction_hold SET central_patron_type = item_hold.central_patron_type, patron_name = item_hold.patron_name
FROM (SELECT id, central_patron_type, patron_name
      FROM transaction_item_hold
      UNION ALL
      SELECT id, central_patron_type, patron_name
      FROM transaction_local_hold) AS item_hold
WHERE transaction_hold.id = item_hold.id;

ALTER TABLE transaction_item_hold DROP COLUMN central_patron_type,
   DROP COLUMN patron_name;
ALTER TABLE transaction_local_hold DROP COLUMN central_patron_type,
   DROP COLUMN patron_name;
