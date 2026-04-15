alter table site_navigation
    add column menu_type varchar(20) not null default 'STATIC_PAGE';

alter table site_navigation
    add constraint chk_site_navigation_menu_type
        check (menu_type in ('STATIC_PAGE', 'BOARD_PAGE', 'VIDEO_PAGE'));

create index idx_site_navigation_menu_type
    on site_navigation(menu_type, parent_id, sort_order, id);

create table site_navigation_static_page (
    site_navigation_id bigint primary key references site_navigation(id) on delete cascade,
    page_key varchar(100),
    page_path varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table site_navigation_board_page (
    site_navigation_id bigint primary key references site_navigation(id) on delete cascade,
    board_key varchar(100) not null,
    list_path varchar(255) not null,
    category_key varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table site_navigation_video_page (
    site_navigation_id bigint primary key references site_navigation(id) on delete cascade,
    video_root_key varchar(64) not null,
    landing_mode varchar(20),
    content_kind_filter varchar(20),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_site_navigation_video_page_content_kind_filter
        check (content_kind_filter is null or content_kind_filter in ('LONG_FORM', 'SHORT'))
);

alter table content_menu
    add column video_root_key varchar(64) not null default 'sermons';

create index idx_content_menu_video_root_key
    on content_menu(video_root_key, status, sort_order, id);

update site_navigation
set menu_type = 'VIDEO_PAGE'
where href = '/sermons';

insert into site_navigation_video_page (
    site_navigation_id,
    video_root_key,
    landing_mode,
    content_kind_filter
)
select
    id,
    'sermons',
    'ROOT',
    null
from site_navigation
where href = '/sermons';
