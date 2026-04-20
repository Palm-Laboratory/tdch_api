delete from menu_item legacy_child
using menu_item legacy_parent
where legacy_child.parent_id = legacy_parent.id
  and legacy_parent.type = 'FOLDER'
  and legacy_parent.slug = 'legacy-board-posts'
  and legacy_parent.is_auto = true
  and legacy_child.type = 'BOARD'
  and legacy_child.is_auto = true
  and not exists (
      select 1
      from post
      where post.menu_id = legacy_child.id
  );

delete from menu_item legacy_parent
where legacy_parent.type = 'FOLDER'
  and legacy_parent.slug = 'legacy-board-posts'
  and legacy_parent.is_auto = true
  and not exists (
      select 1
      from menu_item child
      where child.parent_id = legacy_parent.id
  );

delete from board legacy_board
where legacy_board.slug in ('notice', 'bulletin', 'album', 'general')
  and not exists (
      select 1
      from post
      where post.board_id = legacy_board.id
  )
  and not exists (
      select 1
      from menu_item
      where menu_item.board_key = legacy_board.slug
  );
