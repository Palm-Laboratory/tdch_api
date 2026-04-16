do $$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'sermon_video_meta'
    ) and not exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'media_video_meta'
    ) then
        alter table sermon_video_meta rename to media_video_meta;
    end if;
end $$;

alter index if exists idx_sermon_video_meta_hidden
    rename to idx_media_video_meta_hidden;

alter index if exists idx_sermon_video_meta_display_published_at
    rename to idx_media_video_meta_display_published_at;

drop trigger if exists trg_sermon_video_meta_updated_at on media_video_meta;
drop trigger if exists trg_media_video_meta_updated_at on media_video_meta;

create trigger trg_media_video_meta_updated_at
before update on media_video_meta
for each row
execute function set_current_timestamp_updated_at();
