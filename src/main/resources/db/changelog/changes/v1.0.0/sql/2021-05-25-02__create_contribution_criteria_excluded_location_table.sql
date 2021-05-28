CREATE TABLE public.contribution_criteria_excluded_location
(
    id uuid NOT NULL,
    excluded_location_id uuid,
    criteria_configuration_id uuid,
    CONSTRAINT contribution_criteria_excluded_location_pkey PRIMARY KEY (id),
    CONSTRAINT criteria_configuration_el_fk0 FOREIGN KEY (criteria_configuration_id)
        REFERENCES public.contribution_criteria_configuration (central_server_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
