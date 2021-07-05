ALTER TABLE library_mapping
  DROP CONSTRAINT unq_library_mapping_server_lib_irloc;

ALTER TABLE library_mapping
    ADD CONSTRAINT unq_library_mapping_server_lib UNIQUE (central_server_id, library_id);
