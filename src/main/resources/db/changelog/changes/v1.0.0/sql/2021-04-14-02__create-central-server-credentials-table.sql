CREATE TABLE central_server_credentials
(
    central_server_id     UUID         NOT NULL PRIMARY KEY,
    central_server_key    VARCHAR(255) NOT NULL,
    central_server_secret VARCHAR(255) NOT NULL,
    FOREIGN KEY (central_server_id) REFERENCES central_server (id)
);
