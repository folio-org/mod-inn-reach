ALTER TABLE transaction_hold ADD COLUMN author VARCHAR(255);

UPDATE transaction_hold SET author = patron_and_local_hold_authors.author
FROM (SELECT id, author
      FROM transaction_patron_hold
      UNION ALL
      SELECT id, author
      FROM transaction_local_hold) AS patron_and_local_hold_authors
WHERE transaction_hold.id = patron_and_local_hold_authors.id;

ALTER TABLE transaction_patron_hold DROP COLUMN author;
ALTER TABLE transaction_local_hold DROP COLUMN author;
