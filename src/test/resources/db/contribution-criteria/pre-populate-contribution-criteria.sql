INSERT INTO contribution_criteria_configuration (
    id, central_server_id,
    contribute_but_suppress_code_id, contribute_as_system_owned_code_id, do_not_contribute_code_id,
    created_by, created_date, last_modified_by, last_modified_date)
VALUES ('71bd0beb-28cb-40bb-9f40-87463d61a553', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        '8d87682b-0414-4e1a-b810-43df2cda69d1', '7ee055ce-64b3-4e12-9253-f56762412a7e', '5599f23f-d424-4fce-8a51-b7fce690cbda',
        'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO contribution_criteria_excluded_location (
    contribution_criteria_id, location_id)
VALUES ('71bd0beb-28cb-40bb-9f40-87463d61a553', '0defd5b2-06f9-45d0-a607-a242ec689539'),
       ('71bd0beb-28cb-40bb-9f40-87463d61a553', '8c1508bd-b640-408a-8005-d50df392e233'),
       ('71bd0beb-28cb-40bb-9f40-87463d61a553', 'e402b394-2c5d-41ed-8a01-ba7377eb5d80');
