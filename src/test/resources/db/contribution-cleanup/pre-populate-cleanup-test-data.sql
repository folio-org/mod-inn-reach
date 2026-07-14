-- Terminal state records older than retention (3 days) - should be deleted
INSERT INTO job_execution_status (id, instance_id, job_id, type, tenant, status, instance_contributed, retry_attempts, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'PROCESSED', true, 0, now() - interval '10 days', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'FAILED', false, 5, now() - interval '20 days', now() - interval '20 days'),
  ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'DE_CONTRIBUTED', true, 0, now() - interval '15 days', now() - interval '15 days');

INSERT INTO ongoing_contribution_status (id, old_entity, new_entity, domain_event_name, domain_event_type, status, central_server_id, retry_attempts, tenant, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000004', '{}', '{}', 'INSTANCE', 'CREATED', 'PROCESSED', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 0, 'testing', now() - interval '10 days', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000005', '{}', '{}', 'ITEM', 'UPDATED', 'FAILED', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 3, 'testing', now() - interval '20 days', now() - interval '20 days');

-- Non-terminal records older than retention - should NOT be deleted
INSERT INTO job_execution_status (id, instance_id, job_id, type, tenant, status, instance_contributed, retry_attempts, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'IN_PROGRESS', false, 0, now() - interval '30 days', now() - interval '30 days'),
  ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'READY', false, 0, now() - interval '30 days', now() - interval '30 days'),
  ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'RETRY', true, 10, now() - interval '30 days', now() - interval '30 days');

INSERT INTO ongoing_contribution_status (id, old_entity, new_entity, domain_event_name, domain_event_type, status, central_server_id, retry_attempts, tenant, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000009', '{}', '{}', 'HOLDINGS', 'UPDATED', 'IN_PROGRESS', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 0, 'testing', now() - interval '30 days', now() - interval '30 days');

-- Terminal state records within retention - should NOT be deleted
INSERT INTO job_execution_status (id, instance_id, job_id, type, tenant, status, instance_contributed, retry_attempts, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'PROCESSED', true, 0, now(), now());
