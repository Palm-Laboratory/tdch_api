create table video_metadata (
    id bigserial primary key,
    youtube_video_id bigint not null unique references youtube_video(id),
    preacher varchar(120),
    scripture varchar(255),
    service_type varchar(100),
    summary text,
    tags text[] not null default '{}',
    visible boolean not null default true,
    featured boolean not null default false,
    pinned_rank integer,
    manual_title varchar(255),
    manual_thumbnail_url text,
    manual_published_date date,
    manual_kind varchar(20),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_video_metadata_manual_kind
        check (manual_kind is null or manual_kind in ('LONG_FORM', 'SHORT'))
);
