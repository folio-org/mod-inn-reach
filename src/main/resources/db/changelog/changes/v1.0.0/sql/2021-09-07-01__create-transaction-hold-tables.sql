CREATE TABLE transaction_pickup_location
(
    id UUID NOT NULL,
    pickup_loc_code VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    print_name VARCHAR(255) NOT NULL,
    delivery_stop VARCHAR(255),
    created_by_userid UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid UUID,
    updated_by_username VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    CONSTRAINT pk_transaction_pickup_location PRIMARY KEY (id)
);

CREATE TABLE transaction_hold
(
    id UUID NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    pickup_location_id UUID NOT NULL,
    patron_id VARCHAR(32) NOT NULL CHECK(patron_id SIMILAR TO '[a-z,0-9]{1,32}'),
    patron_agency_code VARCHAR(5) NOT NULL,
    item_agency_code VARCHAR(5) NOT NULL,
    item_id VARCHAR(32) NOT NULL CHECK(item_id SIMILAR TO '[a-z,0-9]{1,32}'),
    central_item_type SMALLINT NOT NULL CHECK(central_item_type BETWEEN 0 AND 255),
    need_before TIMESTAMP,
    folio_patron_id UUID NOT NULL,
    folio_item_id UUID NOT NULL,
    folio_request_id UUID NOT NULL,
    folio_loan_id UUID,
    created_by_userid UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_by_username VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by_userid UUID,
    updated_by_username VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    CONSTRAINT pk_transaction_hold PRIMARY KEY (id),
    CONSTRAINT fk_transaction_pickup_location FOREIGN KEY (pickup_location_id)
    REFERENCES transaction_pickup_location (id) ON DELETE CASCADE
);

CREATE TABLE transaction_patron_hold
(
    id UUID NOT NULL,
    title VARCHAR(255),
    author VARCHAR(255),
    call_number VARCHAR(255),
    shipped_item_barcode VARCHAR(255),
    CONSTRAINT pk_transaction_patron_hold PRIMARY KEY (id),
    CONSTRAINT fk_transaction_patron_hold FOREIGN KEY (id)
    REFERENCES transaction_hold (id) ON DELETE CASCADE
);

CREATE TABLE transaction_item_hold
(
    id UUID NOT NULL,
    central_patron_type SMALLINT NOT NULL CHECK(central_patron_type BETWEEN 0 AND 255),
    patron_name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_transaction_item_hold PRIMARY KEY (id),
    CONSTRAINT fk_transaction_item_hold FOREIGN KEY (id)
    REFERENCES transaction_hold (id) ON DELETE CASCADE
);

CREATE TABLE transaction_local_hold
(
    id UUID NOT NULL,
    patron_home_library VARCHAR(255),
    patron_phone VARCHAR(255),
    title VARCHAR(255),
    author VARCHAR(255),
    call_number VARCHAR(255),
    central_patron_type SMALLINT NOT NULL CHECK(central_patron_type BETWEEN 0 AND 255),
    patron_name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_transaction_local_hold PRIMARY KEY (id),
    CONSTRAINT fk_transaction_local_hold FOREIGN KEY (id)
    REFERENCES transaction_hold (id) ON DELETE CASCADE
);
