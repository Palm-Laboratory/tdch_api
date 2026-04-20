update board
set menu_id = menu_item.id,
    title = menu_item.label
from menu_item
where menu_item.type = 'BOARD'
  and menu_item.board_key = board.slug
  and board.menu_id is null
  and not exists (
      select 1
      from board existing_board
      where existing_board.menu_id = menu_item.id
  );

with default_board_type as (
    select id, key
    from board_type
    where key = 'GENERAL'
),
missing_board_menu as (
    select
        menu_item.id as menu_id,
        menu_item.label as menu_label,
        coalesce(
            nullif(menu_item.board_key, ''),
            case
                when parent.slug is not null and parent.slug <> ''
                    then concat(parent.slug, '-', menu_item.slug)
                else menu_item.slug
            end,
            concat('menu-', menu_item.id)
        ) as base_slug
    from menu_item
    left join menu_item parent
      on parent.id = menu_item.parent_id
    left join board board_by_menu
      on board_by_menu.menu_id = menu_item.id
    left join board board_by_key
      on board_by_key.slug = menu_item.board_key
    where menu_item.type = 'BOARD'
      and board_by_menu.id is null
      and board_by_key.id is null
),
slugged_missing_board_menu as (
    select
        missing_board_menu.menu_id,
        missing_board_menu.menu_label,
        case
            when exists (
                select 1
                from board existing_board
                where existing_board.slug = missing_board_menu.base_slug
            )
                then concat(missing_board_menu.base_slug, '-', missing_board_menu.menu_id)
            else missing_board_menu.base_slug
        end as slug
    from missing_board_menu
)
insert into board (slug, title, type, description, menu_id, board_type_id)
select
    slugged_missing_board_menu.slug,
    slugged_missing_board_menu.menu_label,
    default_board_type.key,
    null,
    slugged_missing_board_menu.menu_id,
    default_board_type.id
from slugged_missing_board_menu
cross join default_board_type
on conflict (slug) do nothing;

update menu_item
set board_key = board.slug
from board
where board.menu_id = menu_item.id
  and menu_item.type = 'BOARD';

update post
set board_id = board.id
from board
where post.menu_id = board.menu_id
  and board.menu_id is not null;
