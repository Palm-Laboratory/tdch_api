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

create index idx_media_video_channel_published
    on media_video(channel_id, published_at desc, id);

create trigger trg_media_video_updated_at
before update on media_video
for each row
execute function set_current_timestamp_updated_at();
