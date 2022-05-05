INSERT INTO central_server(id, name, description, local_server_code, central_server_code, central_server_address, loan_type_id, inn_reach_recall_user_id)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'name', 'description', 'test1', 'd2ir',
        'https://centralserver', '6dae9cd4-ae7c-11eb-8529-0242ac130003', 'ef58f191-ec62-44bb-a571-d59c536bcf4a');

INSERT INTO central_server_credentials(central_server_id, central_server_key, central_server_secret)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', '0550d8a5-73c6-466c-96ab-e5c65959e0f4',
        '668a9d83-74b2-402d-8472-c424ff1c0320');

INSERT INTO local_server_credentials(central_server_id, local_server_key, local_server_secret)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', '0550d8a5-73c6-466c-96ab-e5c65959e0f4',
        '$2a$10$e5GgJPr5xsT48irZBZzJwON8pBJ7rCdH5Wk/PhP1cqmEpR7H8JBqa');

INSERT INTO local_agency(id, code, central_server_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', 'q1w2e', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', 'w2e3r', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2');

INSERT INTO folio_library(local_agency_id, folio_library_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', '7c244444-ae7c-11eb-8529-0242ac130004'),
       ('556a5930-0639-4caa-a66b-f7c99c39972a', '7f58859e-ae7c-11eb-8529-0242ac130004'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', '71fb3252-ae7c-11eb-8529-0242ac130004'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', '761451d4-ae7c-11eb-8529-0242ac130004');
