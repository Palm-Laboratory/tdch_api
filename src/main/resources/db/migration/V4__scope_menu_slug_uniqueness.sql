alter table menu_item
    drop constraint if exists menu_item_slug_key;

create unique index if not exists uq_menu_item_root_slug
    on menu_item (slug)
    where parent_id is null;

create unique index if not exists uq_menu_item_sibling_slug
    on menu_item (parent_id, slug)
    where parent_id is not null;
