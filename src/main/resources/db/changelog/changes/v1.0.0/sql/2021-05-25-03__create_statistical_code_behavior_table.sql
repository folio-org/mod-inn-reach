CREATE TABLE public.statistical_code_behavior
(
    id UUID NOT NULL,
    contribution_behavior VARCHAR(30),
    statistical_code_id UUID,
    criteria_configuration_id UUID,
    CONSTRAINT statistical_code_behavior_pkey PRIMARY KEY (id),
    CONSTRAINT fk_statistical_code_behavior__criteria_configuration FOREIGN KEY (criteria_configuration_id)
        REFERENCES contribution_criteria_configuration (central_server_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
