insert into contribution(
     id, central_server_id, job_id,
     status, complete_date,
     records_total, records_processed, records_contributed, records_updated, records_decontributed)
VALUES ('ae274737-c398-4cf6-8dd3-d228e5b1f608', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'a193f510-b178-4ce6-ab70-d8e09f646a2d',
        0, NULL,
        1000, 42, 40, 40, 0),
       ('b414ad15-cf4e-40ca-a6be-7e0380dbe96e', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', '6f844c2f-d0a6-4e32-8aec-e9d3aaed88cf',
        1, CURRENT_TIMESTAMP,
        1500, 1500, 40, 40, 0),
       ('9a344fb9-61bb-49ca-95bd-ad329593671d', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 'ddbbeef6-b2b6-4bc3-a8e9-55091937e84b',
        1, CURRENT_TIMESTAMP,
        1500, 1500, 40, 40, 0);
