alter table youtube_playlist
    add column last_discovered_at timestamptz,
    add column last_sync_succeeded_at timestamptz,
    add column last_sync_failed_at timestamptz,
    add column last_sync_error_message text,
    add column discovery_source varchar(20);

create index idx_youtube_playlist_sync_enabled
    on youtube_playlist(sync_enabled);

create index idx_youtube_playlist_content_menu_id
    on youtube_playlist(content_menu_id);
