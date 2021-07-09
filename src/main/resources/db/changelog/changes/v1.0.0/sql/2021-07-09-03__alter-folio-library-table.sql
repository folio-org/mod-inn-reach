ALTER TABLE folio_library
    DROP CONSTRAINT folio_library_local_agency_id_fkey;

ALTER TABLE folio_library
    ADD CONSTRAINT folio_library_local_agency_id_fkey FOREIGN KEY (local_agency_id)
        REFERENCES local_agency (id)
        ON DELETE CASCADE;
