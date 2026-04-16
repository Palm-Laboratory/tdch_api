with root_about as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    values (null, 'FOLDER', 'PUBLISHED', '교회 소개', 'about', null, null, null, false, 0, '', 0, false)
    returning id
),
root_newcomer as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    values (null, 'FOLDER', 'PUBLISHED', '제자 양육', 'newcomer', null, null, null, false, 0, '', 1, false)
    returning id
),
root_videos as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    values (null, 'YOUTUBE_PLAYLIST_GROUP', 'PUBLISHED', '예배 영상', 'videos', null, null, null, false, 0, '', 2, false)
    returning id
),
child_greeting as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '인사말/비전', 'greeting', 'about.greeting', null, null, false, 1, '', 0, false
    from root_about
    returning id
),
child_pastor as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '담임목사 소개', 'pastor', 'about.pastor', null, null, false, 1, '', 1, false
    from root_about
    returning id
),
child_service_times as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '예배 시간 안내', 'service-times', 'about.service-times', null, null, false, 1, '', 2, false
    from root_about
    returning id
),
child_location as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '오시는 길', 'location', 'about.location', null, null, false, 1, '', 3, false
    from root_about
    returning id
),
child_history as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '교회 연혁', 'history', 'about.history', null, null, false, 1, '', 4, false
    from root_about
    returning id
),
child_giving as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '헌금 안내', 'giving', 'about.giving', null, null, false, 1, '', 5, false
    from root_about
    returning id
),
child_newcomer_guide as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '새가족 안내', 'guide', 'newcomer.guide', null, null, false, 1, '', 0, false
    from root_newcomer
    returning id
),
child_newcomer_care as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '새가족 양육', 'care', 'newcomer.care', null, null, false, 1, '', 1, false
    from root_newcomer
    returning id
),
child_newcomer_disciples as (
    insert into menu_item (parent_id, type, status, label, slug, static_page_key, board_key, external_url, open_in_new_tab, depth, path, sort_order, is_auto)
    select id, 'STATIC', 'PUBLISHED', '제자 훈련', 'disciples', 'newcomer.disciples', null, null, false, 1, '', 2, false
    from root_newcomer
    returning id
)
select 1;

with recursive menu_paths as (
    select id, parent_id, concat('/', id, '/') as computed_path, 0 as computed_depth
    from menu_item
    where parent_id is null
    union all
    select child.id, child.parent_id, concat(parent.computed_path, child.id, '/'), parent.computed_depth + 1
    from menu_item child
    join menu_paths parent on child.parent_id = parent.id
)
update menu_item menu
set path = menu_paths.computed_path,
    depth = menu_paths.computed_depth
from menu_paths
where menu.id = menu_paths.id;
