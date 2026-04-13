create table admin_account (
    id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(100) not null,
    display_name varchar(100) not null,
    role varchar(20) not null,
    active boolean not null default true,
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_admin_account_role
        check (role in ('SUPER_ADMIN', 'ADMIN'))
);

create index idx_admin_account_active_username
    on admin_account(active, username);

create trigger trg_admin_account_updated_at
before update on admin_account
for each row
execute function set_current_timestamp_updated_at();
