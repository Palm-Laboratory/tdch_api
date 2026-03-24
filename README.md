# thejejachurch-api

제자교회 웹사이트용 Kotlin + Spring Boot API 서버 부트스트랩입니다.

## 현재 범위

- 헬스체크 API
- 미디어 조회 API 뼈대
- 사이트 메뉴 조회 API
- 유튜브 재생목록 기반 DB 스키마
- Flyway 마이그레이션

## 로컬 준비

권장 런타임:

- Java 21+
- PostgreSQL 16+

이 프로젝트에는 Gradle wrapper가 포함되어 있습니다.
Java 21이 준비된 환경에서 아래를 실행하면 됩니다.

```bash
cd tdch_api
cp .env.example .env
./gradlew bootRun
```

## 환경 변수

```text
DB_URL=jdbc:postgresql://localhost:5432/thejejachurch
DB_USERNAME=postgres
DB_PASSWORD=postgres
YOUTUBE_API_KEY=your-key
YOUTUBE_MESSAGES_PLAYLIST_ID=your-playlist-id
YOUTUBE_BETTER_DEVOTION_PLAYLIST_ID=your-playlist-id
YOUTUBE_ITS_OKAY_PLAYLIST_ID=your-playlist-id
ADMIN_SYNC_KEY=your-admin-key
```

메뉴는 역할별로 나뉩니다.

- `content_menu`: 설교/영상 콘텐츠 카테고리
- `site_navigation_item`: 헤더/모바일 메뉴/브레드크럼/LNB용 사이트 메뉴

`content_menu` 는 Flyway seed로 생성하고, 실제 `youtube_playlist` 연결은 위 환경변수를 읽어 앱 시작 시 bootstrap 합니다.

## 초기 엔드포인트

- `GET /api/v1/health`
- `GET /api/v1/navigation`
- `GET /api/v1/media/menus`
- `GET /api/v1/media/home`
- `GET /api/v1/media/menus/{siteKey}/videos`
- `GET /api/v1/media/videos/{youtubeVideoId}`
- `POST /api/v1/admin/media/sync`

수동 sync 실행 예시:

```bash
curl -X POST http://localhost:8080/api/v1/admin/media/sync \
  -H "X-Admin-Key: your-admin-key"
```
