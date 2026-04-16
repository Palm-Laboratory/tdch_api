create table if not exists youtube_channel (
    id bigserial primary key,
    channel_id varchar(64) not null unique,
    title varchar(200) not null,
    is_active boolean not null default true,
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

drop trigger if exists trg_youtube_channel_updated_at on youtube_channel;
create trigger trg_youtube_channel_updated_at
before update on youtube_channel
for each row
execute function set_current_timestamp_updated_at();

create table if not exists youtube_playlist (
    id bigserial primary key,
    channel_id bigint not null references youtube_channel(id) on delete cascade,
    playlist_id varchar(64) not null unique,
    title varchar(200) not null,
    description text,
    thumbnail_url text,
    item_count int not null default 0,
    published_at timestamptz,
    sync_status varchar(16) not null default 'ACTIVE',
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_youtube_playlist_sync_status
        check (sync_status in ('ACTIVE', 'REMOVED'))
);

create index if not exists idx_youtube_playlist_channel on youtube_playlist(channel_id);
create index if not exists idx_youtube_playlist_sync_status on youtube_playlist(sync_status);

drop trigger if exists trg_youtube_playlist_updated_at on youtube_playlist;
create trigger trg_youtube_playlist_updated_at
before update on youtube_playlist
for each row
execute function set_current_timestamp_updated_at();

create table if not exists menu_item (
    id bigserial primary key,
    parent_id bigint references menu_item(id) on delete cascade,
    type varchar(32) not null,
    status varchar(16) not null default 'PUBLISHED',
    label varchar(100) not null,
    label_customized boolean not null default false,
    slug varchar(100) not null unique,
    static_page_key varchar(100),
    board_key varchar(100),
    playlist_id bigint unique references youtube_playlist(id) on delete cascade,
    external_url text,
    open_in_new_tab boolean not null default false,
    depth int not null default 0,
    path text not null default '',
    sort_order int not null default 0,
    is_auto boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_menu_item_type
        check (type in (
            'STATIC',
            'BOARD',
            'FOLDER',
            'YOUTUBE_PLAYLIST_GROUP',
            'YOUTUBE_PLAYLIST',
            'EXTERNAL_LINK'
        )),
    constraint chk_menu_item_status
        check (status in ('DRAFT', 'PUBLISHED', 'HIDDEN', 'ARCHIVED'))
);

create index if not exists idx_menu_item_parent_sort on menu_item(parent_id, sort_order);
create index if not exists idx_menu_item_status on menu_item(status);
create index if not exists idx_menu_item_type on menu_item(type);
create index if not exists idx_menu_item_playlist on menu_item(playlist_id);

drop trigger if exists trg_menu_item_updated_at on menu_item;
create trigger trg_menu_item_updated_at
before update on menu_item
for each row
execute function set_current_timestamp_updated_at();

create table if not exists menu_revision (
    id bigserial primary key,
    snapshot jsonb not null,
    summary varchar(200),
    created_by bigint references admin_account(id),
    created_at timestamptz not null default now()
);

create index if not exists idx_menu_revision_created_at on menu_revision(created_at desc);
