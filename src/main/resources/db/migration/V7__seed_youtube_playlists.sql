insert into youtube_playlist (
    content_menu_id,
    youtube_playlist_id,
    title,
    sync_enabled
)
values
    ((select id from content_menu where site_key = 'messages'), 'PENDING_MESSAGES_PLAYLIST_ID', '말씀/설교', false),
    ((select id from content_menu where site_key = 'better-devotion'), 'PENDING_BETTER_DEVOTION_PLAYLIST_ID', '더 좋은 묵상', false),
    ((select id from content_menu where site_key = 'its-okay'), 'PENDING_ITS_OKAY_PLAYLIST_ID', '그래도 괜찮아', false);
