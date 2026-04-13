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

create trigger trg_media_collection_video_updated_at
before update on media_collection_video
for each row
execute function set_current_timestamp_updated_at();
