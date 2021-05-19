CREATE TABLE statistical_code_behavior (
  id UUID NOT NULL,
  contribution_behavior VARCHAR(255),
  statistical_code_id UUID,
  contribution_criteria_configuration_id UUID,
  CONSTRAINT statistical_code_behavior_pkey PRIMARY KEY (id),
  CONSTRAINT fkm0k88mk8es24ns3v3g4xa6ar FOREIGN KEY (contribution_criteria_configuration_id)
  REFERENCES contribution_criteria_configuration (central_server_id) MATCH SIMPLE
);
