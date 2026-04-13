-- Draft only.
-- This file is not part of Flyway execution yet.

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

create table media_video (
    id bigserial primary key,
    provider varchar(20) not null default 'YOUTUBE',
    provider_video_id varchar(64) not null unique,
    title varchar(255) not null,
    description text,
    published_at timestamptz not null,
    channel_id varchar(64),
    channel_title varchar(255),
    thumbnail_url text,
    duration_seconds integer,
    privacy_status varchar(30),
    embeddable boolean not null default true,
    raw_payload jsonb,
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_media_video_provider
        check (provider in ('YOUTUBE'))
);

create table media_collection_video (
    id bigserial primary key,
    media_collection_id bigint not null references media_collection(id) on delete cascade,
    media_video_id bigint not null references media_video(id) on delete cascade,
    youtube_playlist_connection_id bigint references youtube_playlist_connection(id) on delete set null,
    source_position integer,
    added_to_playlist_at timestamptz,
    sync_active boolean not null default true,
    visible boolean not null default true,
    featured boolean not null default false,
    pinned_rank integer,
    display_title varchar(255),
    display_thumbnail_url text,
    display_published_date date,
    display_kind varchar(20),
    sort_order integer,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (media_collection_id, media_video_id),
    constraint chk_media_collection_video_display_kind
        check (display_kind is null or display_kind in ('LONG_FORM', 'SHORT'))
);

create index idx_media_collection_video_collection_order
    on media_collection_video(
        media_collection_id,
        coalesce(pinned_rank, 999999),
        coalesce(sort_order, source_position, 999999),
        id
    );

create table media_video_meta (
    id bigserial primary key,
    media_video_id bigint not null unique references media_video(id) on delete cascade,
    preacher varchar(120),
    scripture_ref text,
    scripture_text text,
    service_type varchar(100),
    summary text,
    tags text[] not null default '{}',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table site_navigation_item
    drop constraint if exists fk_site_navigation_item_content_site_key;

alter table site_navigation_item
    drop column if exists content_site_key;

alter table site_navigation_item
    add column target_media_collection_id bigint references media_collection(id);
