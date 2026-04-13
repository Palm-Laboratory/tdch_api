create table site_navigation_item (
    id bigserial primary key,
    navigation_set_id bigint not null references site_navigation_set(id),
    parent_id bigint references site_navigation_item(id),
    menu_key varchar(64) not null,
    label varchar(100) not null,
    href varchar(255) not null,
    match_path varchar(255),
    link_type varchar(20) not null default 'INTERNAL',
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
        check (link_type in ('INTERNAL', 'ANCHOR', 'EXTERNAL')),
    constraint chk_site_navigation_root_default_landing
        check (parent_id is not null or default_landing = false),
    constraint uk_site_navigation_item_set_menu_key
        unique (navigation_set_id, menu_key)
);

create unique index uk_site_navigation_item_default_landing_per_parent
    on site_navigation_item(navigation_set_id, parent_id)
    where default_landing = true and parent_id is not null;

create index idx_site_navigation_item_set_parent_sort
    on site_navigation_item(navigation_set_id, parent_id, sort_order, id);

create index idx_site_navigation_item_set_visible
    on site_navigation_item(navigation_set_id, visible, parent_id, sort_order, id);

create trigger trg_site_navigation_item_updated_at
before update on site_navigation_item
for each row
execute function set_current_timestamp_updated_at();
