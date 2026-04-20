alter table post_asset
    add column if not exists detached_at timestamptz;

update post_asset
set detached_at = created_at
where post_id is null
  and detached_at is null;

create index if not exists idx_post_asset_detached_at
    on post_asset(detached_at)
    where post_id is null;
