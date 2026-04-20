alter table post
    add column if not exists is_pinned boolean not null default false;

create index if not exists idx_post_board_public_pinned_created_at
    on post(board_id, is_public, is_pinned desc, created_at desc, id desc);

create index if not exists idx_post_menu_public_pinned_created_at
    on post(menu_id, is_public, is_pinned desc, created_at desc, id desc);
