ALTER TABLE transaction_hold ADD COLUMN title VARCHAR(255);

UPDATE transaction_hold SET title = patron_and_local_hold_titles.title
FROM (SELECT id, title
      FROM transaction_patron_hold
      UNION ALL
      SELECT id, title
      FROM transaction_local_hold) AS patron_and_local_hold_titles
WHERE transaction_hold.id = patron_and_local_hold_titles.id;

ALTER TABLE transaction_patron_hold DROP COLUMN title;
ALTER TABLE transaction_local_hold DROP COLUMN title;
