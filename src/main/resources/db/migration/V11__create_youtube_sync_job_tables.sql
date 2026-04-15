create table youtube_sync_job (
    id bigserial primary key,
    trigger_type varchar(20) not null,
    started_at timestamptz not null,
    finished_at timestamptz,
    status varchar(20) not null,
    total_playlists integer not null default 0,
    succeeded_playlists integer not null default 0,
    failed_playlists integer not null default 0,
    error_summary text,
    created_by bigint,
    created_at timestamptz not null default now(),
    constraint chk_youtube_sync_job_trigger_type
        check (trigger_type in ('SCHEDULED', 'MANUAL')),
    constraint chk_youtube_sync_job_status
        check (status in ('RUNNING', 'SUCCEEDED', 'PARTIAL_FAILED', 'FAILED'))
);

create index idx_youtube_sync_job_started_at
    on youtube_sync_job(started_at desc);

create index idx_youtube_sync_job_status
    on youtube_sync_job(status, started_at desc);

create table youtube_sync_job_item (
    id bigserial primary key,
    job_id bigint not null references youtube_sync_job(id) on delete cascade,
    content_menu_id bigint references content_menu(id),
    youtube_playlist_id bigint references youtube_playlist(id),
    status varchar(20) not null,
    processed_items integer not null default 0,
    inserted_videos integer not null default 0,
    updated_videos integer not null default 0,
    deactivated_playlist_videos integer not null default 0,
    error_message text,
    started_at timestamptz not null,
    finished_at timestamptz,
    created_at timestamptz not null default now(),
    constraint chk_youtube_sync_job_item_status
        check (status in ('SUCCEEDED', 'FAILED'))
);

create index idx_youtube_sync_job_item_job_id
    on youtube_sync_job_item(job_id);

create index idx_youtube_sync_job_item_playlist
    on youtube_sync_job_item(youtube_playlist_id, started_at desc);
