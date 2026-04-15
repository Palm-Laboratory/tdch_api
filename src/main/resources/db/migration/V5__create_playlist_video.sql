create table playlist_video (
    id bigserial primary key,
    youtube_playlist_id bigint not null references youtube_playlist(id),
    youtube_video_id bigint not null references youtube_video(id),
    position integer not null,
    added_to_playlist_at timestamptz,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (youtube_playlist_id, youtube_video_id),
    unique (youtube_playlist_id, position)
);
