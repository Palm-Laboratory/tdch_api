update site_navigation_item
set href = '/newcomer/guide',
    match_path = '/newcomer/guide',
    updated_at = current_timestamp
where menu_key = 'newcomer-main';
