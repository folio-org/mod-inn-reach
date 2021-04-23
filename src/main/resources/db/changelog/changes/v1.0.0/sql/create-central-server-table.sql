CREATE TABLE central_server
(
    id                     SERIAL PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL UNIQUE,
    description            TEXT,
    local_server_code      VARCHAR(20)  NOT NULL UNIQUE,
    central_server_address VARCHAR(255) NOT NULL
);
