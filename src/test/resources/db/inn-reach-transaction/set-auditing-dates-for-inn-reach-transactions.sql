UPDATE inn_reach_transaction
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02'
  WHERE tracking_id = 'tracking1';

UPDATE inn_reach_transaction
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02' - interval '1 month'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02' - interval '1 month'
  WHERE tracking_id = 'tracking2';

UPDATE inn_reach_transaction
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02' + interval '1 month'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02' + interval '1 month'
  WHERE tracking_id = 'tracking3';
