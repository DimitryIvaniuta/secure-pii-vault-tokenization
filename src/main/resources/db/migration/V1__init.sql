create table if not exists pii_vault_record (
    id uuid primary key,
    token varchar(128) not null unique,
    status varchar(32) not null,
    classification varchar(64) not null,
    subject_ref varchar(128) not null,
    payload_ciphertext bytea,
    payload_iv bytea,
    payload_tag_length integer,
    encryption_key_id varchar(64),
    created_at timestamptz not null,
    created_by varchar(128) not null,
    last_accessed_at timestamptz,
    deleted_at timestamptz
);

create index if not exists idx_pii_vault_record_token on pii_vault_record(token);
create index if not exists idx_pii_vault_record_status on pii_vault_record(status);

create table if not exists customer_profile (
    id uuid primary key,
    external_ref varchar(128) not null unique,
    pii_token varchar(128) not null,
    customer_status varchar(32) not null,
    created_at timestamptz not null,
    created_by varchar(128) not null
);

create index if not exists idx_customer_profile_token on customer_profile(pii_token);

create table if not exists audit_event (
    id uuid primary key,
    event_type varchar(64) not null,
    outcome varchar(32) not null,
    token varchar(128),
    actor varchar(128) not null,
    actor_roles varchar(512) not null,
    purpose varchar(256),
    correlation_id varchar(128) not null,
    details_json text not null,
    created_at timestamptz not null
);

create index if not exists idx_audit_event_token on audit_event(token);
create index if not exists idx_audit_event_created_at on audit_event(created_at desc);
