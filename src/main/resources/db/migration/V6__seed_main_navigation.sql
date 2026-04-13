with nav_set as (
    select id
    from site_navigation_set
    where set_key = 'main'
)
insert into site_navigation_item (
    navigation_set_id,
    parent_id,
    menu_key,
    label,
    href,
    match_path,
    link_type,
    target_media_collection_id,
    visible,
    header_visible,
    mobile_visible,
    lnb_visible,
    breadcrumb_visible,
    default_landing,
    sort_order
)
select
    nav_set.id,
    null,
    data.menu_key,
    data.label,
    data.href,
    data.match_path,
    data.link_type,
    null,
    data.visible,
    data.header_visible,
    data.mobile_visible,
    data.lnb_visible,
    data.breadcrumb_visible,
    false,
    data.sort_order
from nav_set
cross join (
    values
        ('about', '교회 소개', '/about', '/about', 'INTERNAL', true, true, true, true, true, 10),
        ('sermons', '예배 영상', '/sermons', '/sermons', 'INTERNAL', true, true, true, true, true, 20),
        ('newcomer', '제자 양육', '/newcomer', '/newcomer', 'INTERNAL', true, true, true, true, true, 30)
) as data(
    menu_key,
    label,
    href,
    match_path,
    link_type,
    visible,
    header_visible,
    mobile_visible,
    lnb_visible,
    breadcrumb_visible,
    sort_order
);

with nav_set as (
    select id
    from site_navigation_set
    where set_key = 'main'
),
root_map as (
    select id, menu_key
    from site_navigation_item
    where navigation_set_id = (select id from nav_set)
      and parent_id is null
),
collection_map as (
    select id, collection_key
    from media_collection
)
insert into site_navigation_item (
    navigation_set_id,
    parent_id,
    menu_key,
    label,
    href,
    match_path,
    link_type,
    target_media_collection_id,
    visible,
    header_visible,
    mobile_visible,
    lnb_visible,
    breadcrumb_visible,
    default_landing,
    sort_order
)
select
    (select id from nav_set),
    root_map.id,
    data.menu_key,
    data.label,
    data.href,
    data.match_path,
    data.link_type,
    collection_map.id,
    data.visible,
    data.header_visible,
    data.mobile_visible,
    data.lnb_visible,
    data.breadcrumb_visible,
    data.default_landing,
    data.sort_order
from root_map
join (
    values
        ('about', 'about-greeting', '인사말/비전', '/about/greeting', '/about/greeting', 'INTERNAL', null, true, true, true, true, true, true, 10),
        ('about', 'about-pastor', '담임목사 소개', '/about/pastor', '/about/pastor', 'INTERNAL', null, true, true, true, true, true, false, 20),
        ('about', 'about-service-times', '예배 시간 안내', '/about/service-times', '/about/service-times', 'INTERNAL', null, true, true, true, true, true, false, 30),
        ('about', 'about-location', '오시는 길', '/about/location', '/about/location', 'INTERNAL', null, true, true, true, true, true, false, 40),
        ('about', 'about-history', '교회연혁', '/about/history', '/about/history', 'INTERNAL', null, true, true, true, true, true, false, 50),
        ('about', 'about-giving', '헌금 안내', '/about/giving', '/about/giving', 'INTERNAL', null, true, true, true, true, true, false, 60),
        ('sermons', 'sermons-messages', '말씀/설교', '/sermons/messages', '/sermons/messages', 'CONTENT_REF', 'messages', true, true, true, true, true, true, 10),
        ('sermons', 'sermons-better-devotion', '더 좋은 묵상', '/sermons/better-devotion', '/sermons/better-devotion', 'CONTENT_REF', 'better-devotion', true, true, true, true, true, false, 20),
        ('sermons', 'sermons-its-okay', '그래도 괜찮아', '/sermons/its-okay', '/sermons/its-okay', 'CONTENT_REF', 'its-okay', true, true, true, true, true, false, 30),
        ('newcomer', 'newcomer-guide', '새가족 안내', '/newcomer/guide', '/newcomer/guide', 'INTERNAL', null, true, true, true, true, true, true, 10),
        ('newcomer', 'newcomer-care', '새가족 양육', '/newcomer/care', '/newcomer/care', 'INTERNAL', null, true, true, true, true, true, false, 20),
        ('newcomer', 'newcomer-disciples', '제자 훈련', '/newcomer/disciples', '/newcomer/disciples', 'INTERNAL', null, true, true, true, true, true, false, 30)
) as data(
    root_key,
    menu_key,
    label,
    href,
    match_path,
    link_type,
    collection_key,
    visible,
    header_visible,
    mobile_visible,
    lnb_visible,
    breadcrumb_visible,
    default_landing,
    sort_order
) on root_map.menu_key = data.root_key
left join collection_map on collection_map.collection_key = data.collection_key;
