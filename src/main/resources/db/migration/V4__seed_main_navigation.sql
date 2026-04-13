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
        ('newcomer', '제자 양육', '/newcomer', '/newcomer', 'INTERNAL', true, true, true, true, true, 20)
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
)
insert into site_navigation_item (
    navigation_set_id,
    parent_id,
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
        ('about', 'about-greeting', '인사말/비전', '/about/greeting', '/about/greeting', 'INTERNAL', true, true, true, true, true, true, 10),
        ('about', 'about-pastor', '담임목사 소개', '/about/pastor', '/about/pastor', 'INTERNAL', true, true, true, true, true, false, 20),
        ('about', 'about-service-times', '예배 시간 안내', '/about/service-times', '/about/service-times', 'INTERNAL', true, true, true, true, true, false, 30),
        ('about', 'about-location', '오시는 길', '/about/location', '/about/location', 'INTERNAL', true, true, true, true, true, false, 40),
        ('about', 'about-history', '교회연혁', '/about/history', '/about/history', 'INTERNAL', true, true, true, true, true, false, 50),
        ('about', 'about-giving', '헌금 안내', '/about/giving', '/about/giving', 'INTERNAL', true, true, true, true, true, false, 60),
        ('newcomer', 'newcomer-guide', '새가족 안내', '/newcomer/guide', '/newcomer/guide', 'INTERNAL', true, true, true, true, true, true, 10),
        ('newcomer', 'newcomer-care', '새가족 양육', '/newcomer/care', '/newcomer/care', 'INTERNAL', true, true, true, true, true, false, 20),
        ('newcomer', 'newcomer-disciples', '제자 훈련', '/newcomer/disciples', '/newcomer/disciples', 'INTERNAL', true, true, true, true, true, false, 30)
) as data(
    root_key,
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
    default_landing,
    sort_order
) on root_map.menu_key = data.root_key;
