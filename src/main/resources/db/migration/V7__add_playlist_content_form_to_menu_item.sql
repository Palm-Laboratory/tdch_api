alter table menu_item
    add column if not exists playlist_content_form varchar(16);

update menu_item
set playlist_content_form = 'LONGFORM'
where type = 'YOUTUBE_PLAYLIST'
  and playlist_content_form is null;

alter table menu_item
    add constraint chk_menu_item_playlist_content_form
        check (
            playlist_content_form is null
            or playlist_content_form in ('LONGFORM', 'SHORTFORM')
        );
