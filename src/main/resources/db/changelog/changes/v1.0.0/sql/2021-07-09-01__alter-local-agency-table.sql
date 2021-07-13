ALTER TABLE local_agency
    DROP CONSTRAINT local_agency_central_server_id_fkey;

ALTER TABLE local_agency
    ADD CONSTRAINT local_agency_central_server_id_fkey FOREIGN KEY (central_server_id)
        REFERENCES central_server (id)
        ON DELETE CASCADE;
