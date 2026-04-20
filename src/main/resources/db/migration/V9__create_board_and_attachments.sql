create table board (
    id bigserial primary key,
    slug varchar(100) not null unique,
    title varchar(200) not null,
    type varchar(32) not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_board_type
        check (type in ('NOTICE', 'BULLETIN', 'ALBUM', 'GENERAL'))
);

insert into board (slug, title, type, description)
values
    ('notice', '공지사항', 'NOTICE', '교회 공지사항 게시판'),
    ('bulletin', '주보', 'BULLETIN', '주보 게시판'),
    ('album', '행사 앨범', 'ALBUM', '행사 사진 게시판'),
    ('general', '자유게시판', 'GENERAL', '일반 게시판')
on conflict (slug) do update
set title = excluded.title,
    type = excluded.type,
    description = excluded.description;

drop trigger if exists trg_board_updated_at on board;
create trigger trg_board_updated_at
before update on board
for each row
execute function set_current_timestamp_updated_at();

create table post (
    id bigserial primary key,
    board_id bigint not null references board(id) on delete cascade,
    title varchar(200) not null,
    content_json jsonb not null,
    content_html text,
    author_id bigint not null references admin_account(id),
    published_at timestamptz,
    is_public boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_post_board_id on post(board_id);
create index if not exists idx_post_author_id on post(author_id);
create index if not exists idx_post_board_public_published_at on post(board_id, is_public, published_at desc);

drop trigger if exists trg_post_updated_at on post;
create trigger trg_post_updated_at
before update on post
for each row
execute function set_current_timestamp_updated_at();

create table post_asset (
    id bigserial primary key,
    post_id bigint references post(id) on delete cascade,
    uploaded_by_actor_id bigint not null references admin_account(id),
    kind varchar(32) not null,
    original_filename varchar(255) not null,
    stored_path text not null,
    mime_type varchar(120),
    byte_size bigint not null,
    width integer,
    height integer,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_post_asset_stored_path unique (stored_path),
    constraint chk_post_asset_kind
        check (kind in ('INLINE_IMAGE', 'FILE_ATTACHMENT'))
);

create index if not exists idx_post_asset_post_id on post_asset(post_id);
create index if not exists idx_post_asset_uploaded_by_actor_id on post_asset(uploaded_by_actor_id);

drop trigger if exists trg_post_asset_updated_at on post_asset;
create trigger trg_post_asset_updated_at
before update on post_asset
for each row
execute function set_current_timestamp_updated_at();

create table upload_token (
    id bigserial primary key,
    board_id bigint references board(id) on delete cascade,
    actor_id bigint not null references admin_account(id),
    max_byte_size bigint not null,
    token_hash varchar(128) not null unique,
    asset_kind varchar(32) not null,
    allowed_mime_types jsonb not null default '[]'::jsonb,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_upload_token_asset_kind
        check (asset_kind in ('INLINE_IMAGE', 'FILE_ATTACHMENT'))
);

create index if not exists idx_upload_token_board_id on upload_token(board_id);
create index if not exists idx_upload_token_expires_at on upload_token(expires_at);

drop trigger if exists trg_upload_token_updated_at on upload_token;
create trigger trg_upload_token_updated_at
before update on upload_token
for each row
execute function set_current_timestamp_updated_at();
