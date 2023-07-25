create table IF NOT EXISTS tenant_info
(
    id UUID NOT NULL,
    tenant_id character varying NOT NULL,
    created_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date timestamp without time zone,
    created_by_userid uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'::uuid,
    created_by_username character varying(255) NOT NULL DEFAULT 'SYSTEM'::character varying,
    updated_by_userid uuid,
    updated_by_username character varying(255),
    CONSTRAINT pk_tenant_info PRIMARY KEY (id)
)
