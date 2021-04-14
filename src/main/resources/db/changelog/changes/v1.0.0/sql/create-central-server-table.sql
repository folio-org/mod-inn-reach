CREATE TABLE central_server (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    local_server_code VARCHAR(20) NOT NULL UNIQUE,
    -- central server configuration properties
    central_server_address VARCHAR(255) NOT NULL,
    central_server_key VARCHAR(255) NOT NULL,
    central_server_secret VARCHAR(255) NOT NULL,
    local_server_key VARCHAR(255),
    local_server_secret VARCHAR(255)
);
