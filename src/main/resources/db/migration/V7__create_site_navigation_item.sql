create table site_navigation_item (
    id bigserial primary key,
    parent_id bigint references site_navigation_item(id),
    menu_key varchar(64) not null unique,
    label varchar(100) not null,
    href varchar(255) not null,
    match_path varchar(255),
    link_type varchar(20) not null default 'INTERNAL',
    content_site_key varchar(64),
    visible boolean not null default true,
    header_visible boolean not null default true,
    mobile_visible boolean not null default true,
    lnb_visible boolean not null default true,
    breadcrumb_visible boolean not null default true,
    default_landing boolean not null default false,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_site_navigation_link_type
        check (link_type in ('INTERNAL', 'ANCHOR', 'EXTERNAL', 'CONTENT_REF'))
);

create index idx_site_navigation_item_parent_sort
    on site_navigation_item(parent_id, sort_order, id);
