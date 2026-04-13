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

create trigger trg_site_navigation_set_updated_at
before update on site_navigation_set
for each row
execute function set_current_timestamp_updated_at();
