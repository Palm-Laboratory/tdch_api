create table content_menu (
    id bigserial primary key,
    site_key varchar(64) not null unique,
    menu_name varchar(100) not null,
    slug varchar(120) not null unique,
    content_kind varchar(20) not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_content_menu_kind
        check (content_kind in ('LONG_FORM', 'SHORT'))
);

alter table site_navigation
    add constraint fk_site_navigation_content_site_key
        foreign key (content_site_key) references content_menu(site_key);
