CREATE TABLE local_server_credentials
(
    central_server_id        UUID         NOT NULL PRIMARY KEY,
    local_server_key         VARCHAR(255) NOT NULL,
    local_server_secret      VARCHAR(255) NOT NULL,
    local_server_secret_salt VARCHAR(255) NOT NULL,
    FOREIGN KEY (central_server_id) REFERENCES central_server (id)
);
