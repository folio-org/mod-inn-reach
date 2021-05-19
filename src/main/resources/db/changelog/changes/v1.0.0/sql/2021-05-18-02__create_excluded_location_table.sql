CREATE TABLE excluded_location (
  id UUID not null,
  location_id UUID,
  contribution_criteria_configuration_id UUID,
  CONSTRAINT excluded_locations_pkey PRIMARY KEY (id),
  CONSTRAINT fksictgstixvt233caohriul6ut FOREIGN KEY (contribution_criteria_configuration_id)
  REFERENCES contribution_criteria_configuration (central_server_id) MATCH SIMPLE
);
