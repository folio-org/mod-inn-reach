INSERT INTO central_server(id, name, description, local_server_code, central_server_address, loan_type_id)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'name', 'description', 'fli01',
        'https://rssandbox-api.iii.com', '6dae9cd4-ae7c-11eb-8529-0242ac130003');

INSERT INTO central_server_credentials(central_server_id, central_server_key, central_server_secret)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'b55f2568-e03a-4cc2-8f30-5fb69aa14f5f',
        '0c3ae7f3-4e70-4d5d-b94d-5a6605166494');

INSERT INTO local_server_credentials(central_server_id, local_server_key, local_server_secret)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'b55f2568-e03a-4cc2-8f30-5fb69aa14f5f',
        '$2a$10$bJY.mnChIj0DCF0345nB/.FflxflGhPDaa6I5OvHhhJci5TRRf3Be');

INSERT INTO local_agency(id, code, central_server_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', 'q1w2e', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', 'w2e3r', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2');

INSERT INTO folio_library(local_agency_id, folio_library_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', '7c244444-ae7c-11eb-8529-0242ac130004'),
       ('556a5930-0639-4caa-a66b-f7c99c39972a', '7f58859e-ae7c-11eb-8529-0242ac130004'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', '71fb3252-ae7c-11eb-8529-0242ac130004'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', '761451d4-ae7c-11eb-8529-0242ac130004');
