CREATE TABLE local_agency
(
    id                UUID       NOT NULL PRIMARY KEY,
    code              VARCHAR(5) NOT NULL,
    central_server_id UUID       NOT NULL,
    FOREIGN KEY (central_server_id) REFERENCES central_server (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
