create table youtube_playlist (
    id bigserial primary key,
    content_menu_id bigint not null references content_menu(id),
    youtube_playlist_id varchar(64) not null unique,
    title varchar(255) not null,
    description text,
    channel_id varchar(64),
    channel_title varchar(255),
    thumbnail_url text,
    item_count integer,
    sync_enabled boolean not null default true,
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
