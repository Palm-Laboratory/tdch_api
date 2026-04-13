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

create trigger trg_media_video_meta_updated_at
before update on media_video_meta
for each row
execute function set_current_timestamp_updated_at();
