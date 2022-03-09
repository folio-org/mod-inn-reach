ALTER TABLE folio_library ADD COLUMN central_server_id UUID;

UPDATE folio_library SET central_server_id = local_agencies.central_server_id
FROM (SELECT id, central_server_id FROM local_agency) AS local_agencies
WHERE local_agency_id = local_agencies.id;

ALTER TABLE folio_library
ADD CONSTRAINT unq_central_server_folio_library UNIQUE (central_server_id, folio_library_id);
ALTER TABLE folio_library ALTER COLUMN central_server_id SET NOT NULL;
