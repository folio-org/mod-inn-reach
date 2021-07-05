ALTER TABLE location_mapping
    ADD COLUMN library_id UUID NOT NULL;

ALTER TABLE location_mapping
  DROP CONSTRAINT unq_location_mapping_server_loc_irloc;

ALTER TABLE location_mapping
    ADD CONSTRAINT unq_location_mapping_server_loc UNIQUE (central_server_id, location_id);
