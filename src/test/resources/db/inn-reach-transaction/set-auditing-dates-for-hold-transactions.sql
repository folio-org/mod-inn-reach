UPDATE transaction_hold
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02'
  WHERE id = (SELECT transaction_hold_id
                FROM inn_reach_transaction
                WHERE tracking_id = 'tracking1');

UPDATE transaction_hold
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02' - interval '1 month'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02' - interval '1 month'
  WHERE id = (SELECT transaction_hold_id
                FROM inn_reach_transaction
                WHERE tracking_id = 'tracking2');

UPDATE transaction_hold
  SET created_date = timestamp with time zone '2022-02-24 05:00:00+02' + interval '1 month'
    , updated_date = timestamp with time zone '2022-02-24 05:00:00+02' + interval '1 month'
  WHERE id = (SELECT transaction_hold_id
                FROM inn_reach_transaction
                WHERE tracking_id = 'tracking3');
