create table if not exists youtube_video (
    id bigserial primary key,
    channel_id bigint not null references youtube_channel(id) on delete cascade,
    video_id varchar(64) not null unique,
    title varchar(300) not null,
    description text,
    thumbnail_url text,
    published_at timestamptz,
    duration_seconds int,
    content_form varchar(16) not null default 'LONGFORM',
    privacy_status varchar(16) not null default 'PUBLIC',
    sync_status varchar(16) not null default 'ACTIVE',
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_youtube_video_content_form
        check (content_form in ('LONGFORM', 'SHORTFORM')),
    constraint chk_youtube_video_privacy_status
        check (privacy_status in ('PUBLIC', 'UNLISTED', 'PRIVATE')),
    constraint chk_youtube_video_sync_status
        check (sync_status in ('ACTIVE', 'REMOVED'))
);

create index if not exists idx_youtube_video_channel on youtube_video(channel_id);
create index if not exists idx_youtube_video_published_at on youtube_video(published_at desc);
create index if not exists idx_youtube_video_content_form on youtube_video(content_form);
create index if not exists idx_youtube_video_sync_status on youtube_video(sync_status);

drop trigger if exists trg_youtube_video_updated_at on youtube_video;
create trigger trg_youtube_video_updated_at
before update on youtube_video
for each row
execute function set_current_timestamp_updated_at();

create table if not exists youtube_playlist_item (
    id bigserial primary key,
    playlist_id bigint not null references youtube_playlist(id) on delete cascade,
    video_id bigint not null references youtube_video(id) on delete cascade,
    position int not null,
    added_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_youtube_playlist_item_playlist_video unique (playlist_id, video_id),
    constraint uk_youtube_playlist_item_playlist_position unique (playlist_id, position)
);

create index if not exists idx_youtube_playlist_item_video on youtube_playlist_item(video_id);
create index if not exists idx_youtube_playlist_item_playlist on youtube_playlist_item(playlist_id, position);

drop trigger if exists trg_youtube_playlist_item_updated_at on youtube_playlist_item;
create trigger trg_youtube_playlist_item_updated_at
before update on youtube_playlist_item
for each row
execute function set_current_timestamp_updated_at();

create table if not exists sermon_video_meta (
    id bigserial primary key,
    video_id bigint not null unique references youtube_video(id) on delete cascade,
    display_title varchar(300),
    preacher_name varchar(120),
    display_published_at timestamptz,
    hidden boolean not null default false,
    scripture_reference varchar(200),
    scripture_body text,
    message_body text,
    summary text,
    thumbnail_override_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_sermon_video_meta_hidden on sermon_video_meta(hidden);
create index if not exists idx_sermon_video_meta_display_published_at on sermon_video_meta(display_published_at desc);

drop trigger if exists trg_sermon_video_meta_updated_at on sermon_video_meta;
create trigger trg_sermon_video_meta_updated_at
before update on sermon_video_meta
for each row
execute function set_current_timestamp_updated_at();
