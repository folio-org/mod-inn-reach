CREATE TABLE public.statistical_code_behavior
(
    id UUID NOT NULL,
    contribution_behavior CHARACTER VARYING(50),
    statistical_code_id UUID,
    criteria_configuration_id UUID,
    CONSTRAINT statistical_code_behavior_pkey PRIMARY KEY (id),
    CONSTRAINT fktc5rm9nccoo6u25cirqtk60hc FOREIGN KEY (criteria_configuration_id)
        REFERENCES public.contribution_criteria_configuration (central_server_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
