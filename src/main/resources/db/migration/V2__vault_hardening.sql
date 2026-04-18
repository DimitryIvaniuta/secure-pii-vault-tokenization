alter table if exists pii_vault_record
    add column if not exists subject_ref_fingerprint varchar(128),
    add column if not exists wrapped_dek_iv bytea,
    add column if not exists wrapped_dek_ciphertext bytea,
    add column if not exists wrapped_dek_tag_length integer;

create index if not exists idx_pii_vault_record_subject_ref_fingerprint on pii_vault_record(subject_ref_fingerprint);

alter table if exists audit_event
    add column if not exists signature_version varchar(64),
    add column if not exists event_signature varchar(256);

create table if not exists vault_idempotency_request (
    id uuid primary key,
    actor varchar(128) not null,
    idempotency_key varchar(256) not null,
    request_hash varchar(64) not null,
    token varchar(128) not null,
    created_at timestamptz not null,
    constraint uq_vault_idempotency_actor_key unique (actor, idempotency_key)
);

create index if not exists idx_vault_idempotency_token on vault_idempotency_request(token);
