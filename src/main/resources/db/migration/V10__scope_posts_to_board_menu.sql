alter table post
    add column if not exists menu_id bigint;

insert into menu_item (
    parent_id,
    type,
    status,
    label,
    label_customized,
    slug,
    slug_customized,
    static_page_key,
    board_key,
    playlist_id,
    external_url,
    open_in_new_tab,
    depth,
    path,
    sort_order,
    is_auto
)
select
    null,
    'FOLDER',
    'HIDDEN',
    '기존 게시판',
    false,
    'legacy-board-posts',
    true,
    null,
    null,
    null,
    null,
    false,
    0,
    'legacy-board-posts',
    100000,
    true
where exists (
    select 1
    from board
    where not exists (
        select 1
        from menu_item
        where menu_item.type = 'BOARD'
          and menu_item.board_key = board.slug
    )
)
and not exists (
    select 1
    from menu_item
    where menu_item.type = 'FOLDER'
      and menu_item.slug = 'legacy-board-posts'
      and menu_item.parent_id is null
);

insert into menu_item (
    parent_id,
    type,
    status,
    label,
    label_customized,
    slug,
    slug_customized,
    static_page_key,
    board_key,
    playlist_id,
    external_url,
    open_in_new_tab,
    depth,
    path,
    sort_order,
    is_auto
)
select
    parent.id,
    'BOARD',
    'HIDDEN',
    board.title,
    false,
    concat('board-', board.slug, '-', board.id),
    true,
    null,
    board.slug,
    null,
    null,
    false,
    1,
    concat(parent.slug, '/', 'board-', board.slug, '-', board.id),
    100000 + board.id::int,
    true
from board
join menu_item parent
  on parent.type = 'FOLDER'
 and parent.slug = 'legacy-board-posts'
 and parent.parent_id is null
where not exists (
    select 1
    from menu_item
    where menu_item.type = 'BOARD'
      and menu_item.board_key = board.slug
);

update post
set menu_id = matched_menu.id
from board
join lateral (
    select menu_item.id
    from menu_item
    where menu_item.type = 'BOARD'
      and menu_item.board_key = board.slug
    order by menu_item.sort_order asc, menu_item.id asc
    limit 1
) matched_menu on true
where post.board_id = board.id
  and post.menu_id is null;

alter table post
    alter column menu_id set not null;

alter table post
    drop constraint if exists fk_post_menu_item;

alter table post
    add constraint fk_post_menu_item
        foreign key (menu_id) references menu_item(id);

create index if not exists idx_post_menu_id_created_at
    on post(menu_id, created_at desc, id desc);

create index if not exists idx_post_menu_id_public_created_at
    on post(menu_id, is_public, created_at desc, id desc);
