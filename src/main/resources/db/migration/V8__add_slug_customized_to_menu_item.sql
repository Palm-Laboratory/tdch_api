alter table menu_item
    add column if not exists slug_customized boolean not null default false;
