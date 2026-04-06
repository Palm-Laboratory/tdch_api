create table site_navigation_set (
    id bigserial primary key,
    set_key varchar(50) not null unique,
    label varchar(100) not null,
    description varchar(255),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

insert into site_navigation_set (
    set_key,
    label,
    description,
    active
)
values (
    'main',
    '메인 사이트 메뉴',
    '헤더, 모바일, LNB, 브레드크럼 공용 메뉴',
    true
);

alter table site_navigation_item
    add column navigation_set_id bigint;

update site_navigation_item
set navigation_set_id = (
    select id
    from site_navigation_set
    where set_key = 'main'
);

alter table site_navigation_item
    alter column navigation_set_id set not null;

alter table site_navigation_item
    add constraint fk_site_navigation_item_set
        foreign key (navigation_set_id) references site_navigation_set(id);

alter table site_navigation_item
    add constraint fk_site_navigation_item_content_site_key
        foreign key (content_site_key) references content_menu(site_key);

alter table site_navigation_item
    add constraint chk_site_navigation_root_default_landing
        check (parent_id is not null or default_landing = false);

alter table site_navigation_item
    drop constraint if exists site_navigation_item_menu_key_key;

drop index if exists idx_site_navigation_item_parent_sort;

create unique index uk_site_navigation_item_set_menu_key
    on site_navigation_item(navigation_set_id, menu_key);

create unique index uk_site_navigation_item_default_landing_per_parent
    on site_navigation_item(navigation_set_id, parent_id)
    where default_landing = true and parent_id is not null;

create index idx_site_navigation_item_set_parent_sort
    on site_navigation_item(navigation_set_id, parent_id, sort_order, id);

create index idx_site_navigation_item_set_visible
    on site_navigation_item(navigation_set_id, visible, parent_id, sort_order, id);

create or replace function set_current_timestamp_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_site_navigation_item_updated_at on site_navigation_item;
create trigger trg_site_navigation_item_updated_at
before update on site_navigation_item
for each row
execute function set_current_timestamp_updated_at();

drop trigger if exists trg_site_navigation_set_updated_at on site_navigation_set;
create trigger trg_site_navigation_set_updated_at
before update on site_navigation_set
for each row
execute function set_current_timestamp_updated_at();
