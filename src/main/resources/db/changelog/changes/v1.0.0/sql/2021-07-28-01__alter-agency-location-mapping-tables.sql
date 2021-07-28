ALTER TABLE agency_location_lsc_mapping
    ALTER COLUMN library_id DROP NOT NULL;

ALTER TABLE agency_location_lsc_mapping
    ALTER COLUMN location_id DROP NOT NULL;

ALTER TABLE agency_location_ac_mapping
    ALTER COLUMN library_id DROP NOT NULL;

ALTER TABLE agency_location_ac_mapping
    ALTER COLUMN location_id DROP NOT NULL;
