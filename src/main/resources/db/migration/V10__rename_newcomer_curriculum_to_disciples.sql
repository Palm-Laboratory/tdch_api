update site_navigation_item
set label = '제자 훈련',
    href = '/newcomer/disciples',
    match_path = '/newcomer/disciples',
    updated_at = current_timestamp
where menu_key = 'newcomer-curriculum';
