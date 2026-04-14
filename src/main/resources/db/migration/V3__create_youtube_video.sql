create table youtube_video (
    id bigserial primary key,
    youtube_video_id varchar(32) not null unique,
    title varchar(255) not null,
    description text,
    published_at timestamptz not null,
    channel_id varchar(64),
    channel_title varchar(255),
    thumbnail_url text,
    duration_seconds integer,
    privacy_status varchar(20),
    upload_status varchar(20),
    embeddable boolean not null default true,
    made_for_kids boolean,
    detected_kind varchar(20) not null,
    youtube_watch_url text not null,
    youtube_embed_url text not null,
    raw_payload jsonb,
    last_synced_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_youtube_video_kind
        check (detected_kind in ('LONG_FORM', 'SHORT'))
);
