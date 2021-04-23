CREATE TABLE central_server_credentials
(
    central_server_id          INT PRIMARY KEY,
    central_server_key         VARCHAR(255) NOT NULL,
    central_server_secret      VARCHAR(255) NOT NULL,
    central_server_secret_salt VARCHAR(255) NOT NULL,
    local_server_key           VARCHAR(255),
    local_server_secret        VARCHAR(255),
    FOREIGN KEY (central_server_id) REFERENCES central_server (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
