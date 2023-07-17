CREATE TABLE IF NOT EXISTS job_execution_status
(
    id uuid NOT NULL,
    instance_id uuid NOT NULL,
    job_id uuid NOT NULL,
    type character varying NOT NULL,
    tenant character varying NOT NULL,
    status character varying NOT NULL,
    created_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp without time zone,
    created_by_userid uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::uuid,
    created_by_username character varying(255) NOT NULL DEFAULT 'SYSTEM'::character varying,
    updated_by_userid uuid,
    updated_by_username character varying(255),
    CONSTRAINT pk_job_execution_status PRIMARY KEY (id)
)

