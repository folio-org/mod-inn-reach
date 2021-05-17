CREATE TABLE central_server
(
    id                     UUID         NOT NULL PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL UNIQUE,
    description            TEXT,
    local_server_code      VARCHAR(5)   NOT NULL UNIQUE,
    central_server_address VARCHAR(255) NOT NULL,
    loan_type_id           VARCHAR(255) NOT NULL
);
