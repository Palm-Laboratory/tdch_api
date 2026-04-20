create table if not exists board_type (
    id bigserial primary key,
    key varchar(32) not null unique,
    label varchar(100) not null,
    description text,
    sort_order integer not null default 0
);

insert into board_type (key, label, description, sort_order)
values
    ('NOTICE', '공지사항', '공지와 안내 게시판', 0),
    ('BULLETIN', '주보', '주보 게시판', 1),
    ('ALBUM', '행사 앨범', '사진 중심 행사 앨범 게시판', 2),
    ('GENERAL', '자유게시판', '일반 게시판', 3)
on conflict (key) do update
set label = excluded.label,
    description = excluded.description,
    sort_order = excluded.sort_order;

alter table board
    add column if not exists menu_id bigint;

alter table board
    add column if not exists board_type_id bigint;

update board
set board_type_id = board_type.id
from board_type
where board_type.key = board.type
  and board.board_type_id is null;

update board
set menu_id = menu_item.id
from menu_item
where menu_item.type = 'BOARD'
  and menu_item.board_key = board.slug
  and board.menu_id is null;

with desired_board_slug as (
    select
        board.id as board_id,
        menu_item.id as menu_id,
        menu_item.label as menu_label,
        case
            when parent.slug is not null and parent.slug <> ''
                then concat(parent.slug, '-', menu_item.slug)
            else menu_item.slug
        end as base_slug
    from board
    join menu_item
      on menu_item.id = board.menu_id
     and menu_item.type = 'BOARD'
    left join menu_item parent
      on parent.id = menu_item.parent_id
),
ranked_board_slug as (
    select
        board_id,
        menu_id,
        menu_label,
        base_slug,
        row_number() over (partition by base_slug order by menu_id) as duplicate_index
    from desired_board_slug
)
update board
set slug = case
        when ranked_board_slug.duplicate_index = 1 then ranked_board_slug.base_slug
        else concat(ranked_board_slug.base_slug, '-', ranked_board_slug.menu_id)
    end,
    title = ranked_board_slug.menu_label
from ranked_board_slug
where board.id = ranked_board_slug.board_id;

update menu_item
set board_key = board.slug
from board
where board.menu_id = menu_item.id
  and menu_item.type = 'BOARD';

alter table board
    alter column board_type_id set not null;

alter table board
    drop constraint if exists fk_board_board_type;

alter table board
    add constraint fk_board_board_type
        foreign key (board_type_id) references board_type(id);

alter table board
    drop constraint if exists fk_board_menu_item;

alter table board
    add constraint fk_board_menu_item
        foreign key (menu_id) references menu_item(id)
        on delete set null;

create unique index if not exists uq_board_menu_id
    on board(menu_id)
    where menu_id is not null;
