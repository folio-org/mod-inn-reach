CREATE OR REPLACE FUNCTION modify_created(p_table varchar) RETURNS void LANGUAGE plpgsql
AS '
BEGIN
  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  ADD COLUMN created_by_userid UUID NOT NULL DEFAULT ''''00000000-0000-0000-0000-000000000000''''::uuid'';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  ADD COLUMN created_by_username VARCHAR(255) NOT NULL DEFAULT ''''SYSTEM'''''';

  EXECUTE ''UPDATE '' || p_table ||
          ''  SET created_by_userid = ''''00000000-0000-0000-0000-000000000000''''::uuid,'' ||
          ''      created_by_username = ''''SYSTEM'''''';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  DROP COLUMN created_by'';

  EXECUTE ''ALTER TABLE '' || p_table ||
          ''  ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP'';

  RETURN;
END;';

SELECT modify_created(table_name::varchar)
FROM information_schema.columns
WHERE table_schema = '${database.defaultSchemaName}'
  AND column_name = 'created_by';

DROP FUNCTION modify_created;
