-- Partial index for the ongoing contribution claim query.
-- Only rows with status IN ('READY','RETRY') enter the index, so it stays
-- small (proportional to the backlog, not the full table).
-- created_date is the column because it's the ORDER BY key in the claim query.
CREATE INDEX IF NOT EXISTS idx_ocs_ready_retry
  ON ongoing_contribution_status (created_date)
  WHERE status IN ('READY', 'RETRY');

-- Partial index for getInProgressRecordsCount() and
-- updateInProgressToReady() queries.
-- Covering index on (id) so COUNT(*) can be answered from the index alone.
-- Also benefits updateInProgressToReady() which filters WHERE status = 'IN_PROGRESS'.
CREATE INDEX IF NOT EXISTS idx_ocs_in_progress
  ON ongoing_contribution_status (id)
  WHERE status = 'IN_PROGRESS';

