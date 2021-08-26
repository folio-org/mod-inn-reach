insert into contribution(
     id, central_server_id,
     status, complete_date,
     records_total, records_processed, records_contributed, records_updated, records_decontributed)
VALUES ('ae274737-c398-4cf6-8dd3-d228e5b1f608', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        0, NULL,
        1000, 42, 40, 40, 0),
       ('b414ad15-cf4e-40ca-a6be-7e0380dbe96e', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        1, CURRENT_TIMESTAMP,
        1500, 1500, 40, 40, 0),
       ('9a344fb9-61bb-49ca-95bd-ad329593671d', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2',
        1, CURRENT_TIMESTAMP,
        1500, 1500, 40, 40, 0);
