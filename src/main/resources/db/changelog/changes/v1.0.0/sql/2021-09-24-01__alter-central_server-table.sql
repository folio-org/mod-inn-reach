ALTER TABLE central_server
  ADD COLUMN central_server_code VARCHAR(5) NULL;

UPDATE central_server SET central_server_code = 'dri2';

ALTER TABLE central_server
  ALTER COLUMN central_server_code VARCHAR(5) NOT NULL;
