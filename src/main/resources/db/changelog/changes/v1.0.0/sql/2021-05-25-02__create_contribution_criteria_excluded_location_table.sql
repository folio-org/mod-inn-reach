CREATE TABLE public.contribution_criteria_excluded_location
(
    id uuid NOT NULL,
    excluded_location_id uuid,
    criteria_configuration_id uuid,
    CONSTRAINT contribution_criteria_excluded_location_pkey PRIMARY KEY (id),
    CONSTRAINT fkoi7sg96sfu88or7jcs99po0ca FOREIGN KEY (criteria_configuration_id)
        REFERENCES public.contribution_criteria_configuration (central_server_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
