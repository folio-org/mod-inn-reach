INSERT INTO agency_location_mapping(
    central_server_id, library_id, location_id,
    created_by, created_date, last_modified_by, last_modified_date)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        '70cf3473-77f2-4f5c-92c3-6489e65769e4', '99b0d4e2-a5ec-46a1-a5ea-1080e609f969',
        'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO agency_location_lsc_mapping(
    id, local_server_code, central_server_mapping_id,
    library_id, location_id)
VALUES ('c4c29a78-4589-43dc-ae80-004d6e267b6a', '5publ', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        'ef261062-e582-43d0-a1fc-c32dfca1da22', 'aa58c309-4522-4b46-8d1e-0396ee493460');

INSERT INTO agency_location_ac_mapping(
    id, agency_code, local_server_mapping_id,
    library_id, location_id)
VALUES ('dc1176f0-7c78-4016-b400-3cdfb5302085', '5east', 'c4c29a78-4589-43dc-ae80-004d6e267b6a',
        '70cf3473-77f2-4f5c-92c3-6489e65769e4', '99b0d4e2-a5ec-46a1-a5ea-1080e609f969');

INSERT INTO agency_location_ac_mapping(
    id, agency_code, local_server_mapping_id,
    library_id, location_id)
VALUES ('0a2c0cf4-3333-4f11-99d9-f78373d2fdbc', '5main', 'c4c29a78-4589-43dc-ae80-004d6e267b6a',
        '0a9af79b-321b-43b7-908f-f26fb6096e89', 'ae5032a1-fe55-41d1-ab29-b7696b3312a4');
