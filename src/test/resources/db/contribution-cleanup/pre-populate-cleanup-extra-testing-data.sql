-- Terminal state records older than retention for default testing tenant
INSERT INTO job_execution_status (id, instance_id, job_id, type, tenant, status, instance_contributed, retry_attempts, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'PROCESSED', true, 0, now() - interval '10 days', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000100', 'test', 'testing', 'FAILED', false, 5, now() - interval '20 days', now() - interval '20 days');

INSERT INTO ongoing_contribution_status (id, old_entity, new_entity, domain_event_name, domain_event_type, status, central_server_id, retry_attempts, tenant, created_date, updated_date)
VALUES
  ('00000000-0000-0000-0000-000000000103', '{}', '{}', 'INSTANCE', 'CREATED', 'PROCESSED', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 0, 'testing', now() - interval '10 days', now() - interval '10 days'),
  ('00000000-0000-0000-0000-000000000104', '{}', '{}', 'ITEM', 'UPDATED', 'FAILED', 'edab6baf-c696-42b1-89bb-1bbb8759b0d2', 3, 'testing', now() - interval '20 days', now() - interval '20 days');
