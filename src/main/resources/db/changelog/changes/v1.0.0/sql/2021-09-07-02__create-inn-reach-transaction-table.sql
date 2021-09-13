CREATE TABLE inn_reach_transaction
(
    id UUID NOT NULL,
    tracking_id UUID NOT NULL,
    state SMALLINT NOT NULL,
    type SMALLINT NOT NULL,
    central_server_code VARCHAR(5) NOT NULL,
    transaction_hold_id UUID NOT NULL,
    created_by_userid UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid UUID,
    updated_by_username VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    CONSTRAINT pk_inn_reach_transaction PRIMARY KEY (id),
    CONSTRAINT fk_transaction_hold_id FOREIGN KEY (transaction_hold_id)
    REFERENCES transaction_hold (id) ON DELETE CASCADE
);
