insert into media_collection (
    collection_key,
    title,
    description,
    default_path,
    content_kind,
    active,
    sort_order
)
values
    ('messages', '말씀/설교', '주일예배 설교 및 예배 영상을 노출하는 컬렉션', '/sermons/messages', 'LONG_FORM', true, 10),
    ('better-devotion', '더 좋은 묵상', '묵상 영상 컬렉션', '/sermons/better-devotion', 'LONG_FORM', true, 20),
    ('its-okay', '그래도 괜찮아', '쇼츠 컬렉션', '/sermons/its-okay', 'SHORT', true, 30);
