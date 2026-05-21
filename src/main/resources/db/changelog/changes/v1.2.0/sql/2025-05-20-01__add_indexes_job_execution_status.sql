-- Partial composite index for the scheduler "claim" query.
-- Only rows with status IN ('READY','RETRY') enter the index, so it stays
-- small (proportional to the backlog, not the full table).
-- job_id is the leading column to enable an index nested-loop join to
-- the contribution table and avoid the seq-scan / hash-join that spills
-- to pgsql_tmp.
CREATE INDEX IF NOT EXISTS idx_jes_ready_retry
  ON job_execution_status (job_id, instance_contributed, updated_date)
  WHERE status IN ('READY', 'RETRY');

-- Partial index on contribution(job_id) for active contributions only.
-- The query always filters c.status = 0 (active), so status belongs in the
-- WHERE clause (not as a column) — same reasoning as idx_jes_ready_retry.
-- This keeps the index small: only active contributions are indexed.
CREATE INDEX IF NOT EXISTS idx_contribution_job_id_active
  ON contribution (job_id)
  WHERE status = 0;

-- Partial index for the getInProgressRecordsCount() query.
CREATE INDEX IF NOT EXISTS idx_jes_in_progress
  ON job_execution_status (job_id)
  WHERE status = 'IN_PROGRESS';


