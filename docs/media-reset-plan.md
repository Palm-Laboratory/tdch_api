# Media Reset Plan

## 목표

- 기존 유튜브 미디어 도메인을 완전히 제거하고 새 구조로 다시 시작한다.
- `content_menu` 중심 구조를 폐기하고 `media_collection` 중심 구조로 재설계한다.
- 사이트 네비게이션은 미디어를 소유하지 않고, 미디어 컬렉션을 참조하는 구조로 단순화한다.
- 개발 DB를 초기화하고 Flyway migration 체계를 새 베이스라인으로 다시 잡는다.

## 현재 구조의 문제

- `content_menu`, `youtube_playlist`, `playlist_video`, `video_metadata`가 서로 얽혀 있어서 역할 경계가 흐리다.
- `site_navigation_item.content_site_key`가 `content_menu.site_key`를 참조하지만, 실제 공개 사이트는 `href`, `label`, `default_landing`을 네비게이션 테이블에서 직접 사용한다.
- 결과적으로 미디어 메뉴와 사이트 메뉴가 연결된 것처럼 보이지만, 실질적으로는 이중 관리 구조다.
- 유튜브 연동도 환경변수 bootstrap 기반이라 관리자에서 연결/생성이 불가능하다.

## 리셋 원칙

1. 개발 DB를 비우고 새 migration 세트만 적용한다.
2. 기존 미디어 migration 위에 `alter table`을 이어 붙이지 않는다.
3. 새 구조에서 미디어의 기준 엔티티는 `media_collection`이다.
4. 네비게이션은 `MEDIA_COLLECTION`을 참조할 수 있지만, 미디어 구조의 소유자는 아니다.

## 폐기 대상

### DB 테이블

- `content_menu`
- `youtube_playlist`
- `youtube_video`
- `playlist_video`
- `video_metadata`

### 네비게이션 결합 구조

- `site_navigation_item.content_site_key`
- `content_menu(site_key)` 를 참조하는 FK
- `CONTENT_REF + contentSiteKey` 검증 로직

### 코드

- `kr.or.thejejachurch.api.media` 패키지 전반
- 유튜브 playlist env bootstrap
- 기존 관리자 미디어 페이지/API
- 기존 공개 미디어 API

## 새 도메인 초안

### 1. `media_collection`

사이트 안에서 운영하는 영상 컬렉션의 기준 테이블.

예시:

- 말씀/설교
- 더 좋은 묵상
- 그래도 괜찮아

권장 컬럼:

- `id`
- `collection_key`
- `title`
- `description`
- `default_path`
- `content_kind`
- `active`
- `sort_order`
- `created_at`
- `updated_at`

### 2. `youtube_playlist_connection`

미디어 컬렉션에 연결된 유튜브 플레이리스트.

권장 컬럼:

- `id`
- `media_collection_id`
- `youtube_playlist_id`
- `title`
- `description`
- `channel_id`
- `channel_title`
- `thumbnail_url`
- `sync_enabled`
- `created_via`
- `last_synced_at`
- `created_at`
- `updated_at`

### 3. `media_video`

유튜브 원본 영상 마스터.

권장 컬럼:

- `id`
- `provider`
- `provider_video_id`
- `title`
- `description`
- `published_at`
- `channel_id`
- `channel_title`
- `thumbnail_url`
- `duration_seconds`
- `privacy_status`
- `embeddable`
- `raw_payload`
- `last_synced_at`
- `created_at`
- `updated_at`

### 4. `media_collection_video`

컬렉션과 영상의 연결 테이블.
기존 `playlist_video`의 역할을 일반화한 구조이며, 컬렉션별 표시 제어를 함께 가진다.

권장 컬럼:

- `id`
- `media_collection_id`
- `media_video_id`
- `youtube_playlist_connection_id`
- `source_position`
- `added_to_playlist_at`
- `sync_active`
- `visible`
- `featured`
- `pinned_rank`
- `display_title`
- `display_thumbnail_url`
- `display_published_date`
- `display_kind`
- `sort_order`
- `created_at`
- `updated_at`

### 5. `media_video_meta`

영상 자체 메타데이터.
컬렉션별 표시값과 분리한다.

권장 컬럼:

- `id`
- `media_video_id`
- `preacher`
- `scripture_ref`
- `scripture_text`
- `service_type`
- `summary`
- `tags`
- `created_at`
- `updated_at`

## 네비게이션 구조 개편

기존 `content_site_key` 참조를 제거하고, 네비게이션은 `CONTENT_REF` 타입일 때 `media_collection`을 직접 참조한다.

권장 방향:

- `link_type`
  - `INTERNAL`
  - `EXTERNAL`
  - `ANCHOR`
  - `CONTENT_REF`
- `target_media_collection_id`
- `label_override` 는 선택

의도:

- `CONTENT_REF` 타입일 때는 `media_collection.default_path` 를 기준 링크로 사용한다.
- 컬렉션 제목을 메뉴 라벨로 재사용하거나 필요 시 오버라이드할 수 있다.

## 새 Flyway 베이스라인 제안

개발 DB 초기화를 전제로, migration 번호를 새 구조 기준으로 다시 시작한다.

권장 순서:

1. `V1__create_timestamp_trigger_function.sql`
2. `V2__create_site_navigation_set.sql`
3. `V3__create_media_collection.sql`
4. `V4__create_site_navigation_item.sql`
5. `V5__seed_media_collections.sql`
6. `V6__seed_main_navigation.sql`
7. `V7__create_admin_account.sql`
8. `V8__create_youtube_playlist_connection.sql`
9. `V9__create_media_video.sql`
10. `V10__create_media_collection_video.sql`
11. `V11__create_media_video_meta.sql`

## 관리자 기능 1차 범위

- 미디어 컬렉션 생성/수정
- 기존 유튜브 재생목록 URL 또는 ID 연결
- 연결 후 즉시 sync
- 컬렉션별 영상 노출/대표/고정순위/표시값 관리
- 영상 자체 메타 수정

## 관리자 기능 2차 범위

- 유튜브 OAuth 연결
- 관리자에서 유튜브 재생목록 직접 생성
- 필요 시 플레이리스트 항목 추가/삭제

## 즉시 실행할 작업

1. 기존 migration 세트를 백업 가능한 형태로 정리한다.
2. 새 베이스라인 migration 파일을 작성한다.
3. 새 엔티티/리포지토리/서비스를 최소 단위부터 다시 만든다.
4. 관리자 미디어 기능을 새 구조에 맞게 다시 연결한다.
