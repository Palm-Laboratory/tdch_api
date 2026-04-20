update board
set menu_id = menu_item.id
from menu_item
where menu_item.type = 'BOARD'
  and (
      menu_item.board_key = board.slug
      or board.slug = concat('menu-', menu_item.id)
  )
  and board.menu_id is null;

with desired_board_slug as (
    select
        board.id as board_id,
        menu_item.id as menu_id,
        menu_item.label as menu_label,
        case
            when parent.slug is not null and parent.slug <> ''
                then concat(parent.slug, '-', menu_item.slug)
            else menu_item.slug
        end as base_slug
    from board
    join menu_item
      on menu_item.id = board.menu_id
     and menu_item.type = 'BOARD'
    left join menu_item parent
      on parent.id = menu_item.parent_id
),
ranked_board_slug as (
    select
        board_id,
        menu_id,
        menu_label,
        base_slug,
        row_number() over (partition by base_slug order by menu_id) as duplicate_index
    from desired_board_slug
)
update board
set slug = case
        when ranked_board_slug.duplicate_index = 1 then ranked_board_slug.base_slug
        else concat(ranked_board_slug.base_slug, '-', ranked_board_slug.menu_id)
    end,
    title = ranked_board_slug.menu_label
from ranked_board_slug
where board.id = ranked_board_slug.board_id;

update menu_item
set board_key = board.slug
from board
where board.menu_id = menu_item.id
  and menu_item.type = 'BOARD';

delete from board
where menu_id is null
  and slug like 'menu-%'
  and not exists (
      select 1
      from post
      where post.board_id = board.id
  )
  and not exists (
      select 1
      from upload_token
      where upload_token.board_id = board.id
  );
