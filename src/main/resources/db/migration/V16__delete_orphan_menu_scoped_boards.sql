delete from board
where not exists (
      select 1
      from menu_item
      where menu_item.id = board.menu_id
  )
  and not exists (
      select 1
      from menu_item
      where menu_item.type = 'BOARD'
        and menu_item.board_key = board.slug
  );
