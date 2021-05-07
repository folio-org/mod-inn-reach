INSERT INTO central_server(id, name, description, local_server_code, central_server_address, loan_type_id)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'edab6baf', 'local server test description', 'q1w2e',
        'http://centralserver', '5a95f226-d2f3-45d2-9845-16dc68eef48a');

INSERT INTO central_server_credentials(central_server_id, central_server_key, central_server_secret)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', '5ccdb8f4-345e-4659-8f80-3dff01b25929',
        '490888a6-25d3-4dbb-a9ed-a4aa2891664c');

INSERT INTO local_server_credentials(central_server_id, local_server_key, local_server_secret, local_server_secret_salt)
VALUES ('edab6baf-c696-42b1-89bb-1bbb8759b0d2', '0a8eebdb-40e9-49c3-921d-2c753ee3f775',
        'dde50fec-6193-4c62-8480-336d22a0f29e', '7cbb2858-13c2-4d71-b2cd-41acfd573e58');

INSERT INTO local_agency(id, code, central_server_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', 'a1s21', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', 'g5sz2', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2'),
       ('196282fd-f911-48a4-9d5f-17c26d32e547', 'z2vs1', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2');

INSERT INTO folio_library(local_agency_id, folio_library_id)
VALUES ('556a5930-0639-4caa-a66b-f7c99c39972a', '54c0a59d-fca1-414c-9d36-2319094dec7f'),
       ('556a5930-0639-4caa-a66b-f7c99c39972a', 'dd0936d3-4fa8-41e8-9716-6665520d5208'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', 'bb1a3428-dd76-46e1-9fd1-9edbe93553b2'),
       ('cc4aa062-f612-45d7-a1b4-ebba951a54fc', 'c4d0d1f2-0063-4015-a2d6-cee39a660563'),
       ('196282fd-f911-48a4-9d5f-17c26d32e547', '16dd424a-e16b-444a-9848-39477ebdbdcf'),
       ('196282fd-f911-48a4-9d5f-17c26d32e547', '7ebe0cb3-1488-4a6c-8baf-98215ff7ef58');
