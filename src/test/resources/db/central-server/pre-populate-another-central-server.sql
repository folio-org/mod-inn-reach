INSERT INTO central_server(id, name, description, local_server_code, central_server_code, central_server_address, loan_type_id)
VALUES ('cfae4887-f8fb-4e4c-a5cc-34f74e353cf8', 'other', 'other description', 'def12', 'd2ir1',
        'https://centralserver', 'be248906-34e7-4b6e-a09b-a2d31025ae09');

INSERT INTO central_server_credentials(central_server_id, central_server_key, central_server_secret)
VALUES ('cfae4887-f8fb-4e4c-a5cc-34f74e353cf8', '338983f7-4de2-4de1-bcf5-02d2cc6f51cd',
        'bd8b6145-1251-4b9c-8f43-0743008590f7');

INSERT INTO local_server_credentials(central_server_id, local_server_key, local_server_secret)
VALUES ('cfae4887-f8fb-4e4c-a5cc-34f74e353cf8', '338983f7-4de2-4de1-bcf5-02d2cc6f51cd',
        '$2a$10$e5GgJPr5xsT48irZBZzJwON8pBJ7rCdH5Wk/PhP1cqmEpR7H8JBqa');

INSERT INTO local_agency(id, code, central_server_id)
VALUES ('33cf57fe-df39-4cc1-9a00-d45af089db82', 'g91ub', 'cfae4887-f8fb-4e4c-a5cc-34f74e353cf8'),
       ('3b3034af-85ee-451e-85b9-946b399b93be', 'i21z1', 'cfae4887-f8fb-4e4c-a5cc-34f74e353cf8');

INSERT INTO folio_library(local_agency_id, folio_library_id)
VALUES ('33cf57fe-df39-4cc1-9a00-d45af089db82', '152327cf-e28a-4aff-afc6-5c47c97ffa93'),
       ('33cf57fe-df39-4cc1-9a00-d45af089db82', '6a6bc1bb-0696-4e9d-8241-add7424ce544'),
       ('3b3034af-85ee-451e-85b9-946b399b93be', '2969bf01-d80a-4806-b42d-e3c0fc059daf'),
       ('3b3034af-85ee-451e-85b9-946b399b93be', '4ba8931b-a322-4dba-95f4-ea7e75361d8e');
