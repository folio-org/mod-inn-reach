CREATE TABLE item_contribution_options_configuration
(
    central_server_id UUID       NOT NULL PRIMARY KEY,
    FOREIGN KEY (central_server_id) REFERENCES central_server (id)
);
