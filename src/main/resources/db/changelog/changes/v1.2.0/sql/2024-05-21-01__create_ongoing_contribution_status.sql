CREATE TABLE IF NOT EXISTS ongoing_contribution_status
(
    id uuid NOT NULL,
    old_entity jsonb,
    new_entity jsonb,
    domain_event_type character varying NOT NULL,
    action_type character varying NOT NULL,
    status character varying NOT NULL,
    central_server_id UUID NOT NULL,
    retry_attempts integer NOT NULL DEFAULT 0,
    created_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp without time zone,
    created_by_userid uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::uuid,
    created_by_username character varying(255) NOT NULL DEFAULT 'SYSTEM'::character varying,
    updated_by_userid uuid,
    updated_by_username character varying(255),
    CONSTRAINT pk_ongoing_contribution_status PRIMARY KEY (id)
)
