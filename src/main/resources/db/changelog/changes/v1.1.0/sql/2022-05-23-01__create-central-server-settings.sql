CREATE TABLE central_server_settings
(
  central_server_id       UUID         NOT NULL PRIMARY KEY,
  check_pickup_location   BOOLEAN      NOT NULL DEFAULT FALSE,
  FOREIGN KEY (central_server_id) REFERENCES central_server (id) ON DELETE CASCADE
);
