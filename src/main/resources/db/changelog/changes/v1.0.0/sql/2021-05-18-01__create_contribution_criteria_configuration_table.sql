CREATE TABLE contribution_criteria_configuration (
  central_server_id UUID not null,
  created_by_user_id UUID,
  created_date TIMESTAMP,
  updated_by_user_id UUID,
  updated_date TIMESTAMP,
  CONSTRAINT contribution_criteria_configuration_pkey PRIMARY KEY (central_server_id),
  CONSTRAINT fkrrckhrrkkqh6cs4y1ka51xd6o FOREIGN KEY (central_server_id)
  REFERENCES central_server (id) MATCH SIMPLE
);
