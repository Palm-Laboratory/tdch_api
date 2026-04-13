create table youtube_playlist_connection (
    id bigserial primary key,
    media_collection_id bigint not null references media_collection(id) on delete cascade,
    youtube_playlist_id varchar(64) not null unique,
    title varchar(255),
    description text,
    channel_id varchar(64),
    channel_title varchar(255),
    thumbnail_url text,
    sync_enabled boolean not null default true,
    created_via varchar(20) not null default 'MANUAL',
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_youtube_playlist_connection_created_via
        check (created_via in ('MANUAL', 'YOUTUBE_API'))
);

create index idx_youtube_playlist_connection_collection_sync
    on youtube_playlist_connection(media_collection_id, sync_enabled, id);

create trigger trg_youtube_playlist_connection_updated_at
before update on youtube_playlist_connection
for each row
execute function set_current_timestamp_updated_at();
