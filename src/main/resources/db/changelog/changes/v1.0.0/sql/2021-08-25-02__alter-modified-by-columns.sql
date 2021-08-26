CREATE OR REPLACE FUNCTION modify_last_modified(p_table varchar) RETURNS void LANGUAGE plpgsql
AS '
BEGIN
  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  ADD COLUMN updated_by_userid UUID'';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  ADD COLUMN updated_by_username VARCHAR(255)'';

  EXECUTE ''UPDATE '' || p_table ||
          ''  SET updated_by_userid = ''''00000000-0000-0000-0000-000000000000''''::uuid,'' ||
          ''      updated_by_username = ''''SYSTEM'''''' ||
          ''  WHERE last_modified_by IS NOT NULL'';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  DROP COLUMN last_modified_by'';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  RENAME COLUMN last_modified_date TO updated_date'';

  RETURN;
END;';

SELECT modify_last_modified(table_name)
FROM information_schema.columns
WHERE table_schema = CURRENT_SCHEMA()
  AND column_name = 'last_modified_by';

DROP FUNCTION modify_last_modified;
