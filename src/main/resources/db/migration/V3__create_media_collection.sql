create table media_collection (
    id bigserial primary key,
    collection_key varchar(64) not null unique,
    title varchar(120) not null,
    description text,
    default_path varchar(255) not null unique,
    content_kind varchar(20) not null,
    active boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_media_collection_kind
        check (content_kind in ('LONG_FORM', 'SHORT'))
);

create index idx_media_collection_active_sort
    on media_collection(active, sort_order, id);

create trigger trg_media_collection_updated_at
before update on media_collection
for each row
execute function set_current_timestamp_updated_at();
