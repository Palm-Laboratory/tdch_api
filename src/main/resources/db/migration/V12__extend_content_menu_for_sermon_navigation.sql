alter table content_menu
    add column status varchar(20) not null default 'DRAFT',
    add column navigation_visible boolean not null default true,
    add column sort_order integer not null default 0,
    add column description text,
    add column discovered_at timestamptz,
    add column published_at timestamptz,
    add column last_modified_by bigint;

alter table content_menu
    add constraint chk_content_menu_status
        check (status in ('DRAFT', 'PUBLISHED', 'INACTIVE'));

create index idx_content_menu_status_sort
    on content_menu(status, sort_order, id);

create index idx_content_menu_navigation_visible
    on content_menu(navigation_visible, sort_order, id);
