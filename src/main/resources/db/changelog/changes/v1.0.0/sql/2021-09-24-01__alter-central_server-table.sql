ALTER TABLE central_server
  ADD COLUMN central_server_code VARCHAR(5) NULL;

UPDATE central_server SET central_server_code = 'd2ir';

ALTER TABLE central_server
  ALTER COLUMN central_server_code SET NOT NULL;
