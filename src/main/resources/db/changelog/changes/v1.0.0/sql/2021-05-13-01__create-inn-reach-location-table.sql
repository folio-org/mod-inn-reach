CREATE TABLE inn_reach_location
(
    id                 UUID         NOT NULL,
    code               VARCHAR(5)   NOT NULL UNIQUE,
    description        VARCHAR(255),
    created_by         VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);
