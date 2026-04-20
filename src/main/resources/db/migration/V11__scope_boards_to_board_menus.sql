insert into board (slug, title, type, description)
select
    concat('menu-', menu_item.id),
    menu_item.label,
    coalesce(existing_board.type, 'GENERAL'),
    null
from menu_item
left join board existing_board
  on existing_board.slug = menu_item.board_key
where menu_item.type = 'BOARD'
on conflict (slug) do update
set title = excluded.title,
    type = excluded.type,
    description = excluded.description;

update post
set board_id = scoped_board.id
from menu_item
join board scoped_board
  on scoped_board.slug = concat('menu-', menu_item.id)
where menu_item.type = 'BOARD'
  and post.menu_id = menu_item.id;

update menu_item
set board_key = concat('menu-', id)
where type = 'BOARD';
