create table site_navigation (
    id bigserial primary key,
    parent_id bigint references site_navigation(id),
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
        check (link_type in ('INTERNAL', 'ANCHOR', 'EXTERNAL', 'CONTENT_REF')),
    constraint chk_site_navigation_root_default_landing
        check (parent_id is not null or default_landing = false)
);

create unique index uk_site_navigation_default_landing_per_parent
    on site_navigation(parent_id)
    where default_landing = true and parent_id is not null;

create index idx_site_navigation_parent_sort
    on site_navigation(parent_id, sort_order, id);

create index idx_site_navigation_visible
    on site_navigation(visible, parent_id, sort_order, id);

create or replace function set_current_timestamp_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_site_navigation_updated_at on site_navigation;
create trigger trg_site_navigation_updated_at
before update on site_navigation
for each row
execute function set_current_timestamp_updated_at();
