# thejejachurch-api

제자교회 웹사이트용 Kotlin + Spring Boot API 서버 부트스트랩입니다.

## 현재 범위

- 헬스체크 API
- 미디어 조회 API 뼈대
- 유튜브 재생목록 기반 DB 스키마
- Flyway 마이그레이션

## 로컬 준비

권장 런타임:

- Java 17+
- PostgreSQL 16+

현재 워크스페이스에는 Gradle이 설치되어 있지 않아 래퍼를 아직 생성하지 않았습니다.
Gradle이 준비된 환경에서 아래를 한 번 실행하면 됩니다.

```bash
cd backend
gradle wrapper
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
```

## 초기 엔드포인트

- `GET /api/v1/health`
- `GET /api/v1/media/menus`
- `GET /api/v1/media/home`
- `GET /api/v1/media/menus/{siteKey}/videos`
- `GET /api/v1/media/videos/{youtubeVideoId}`
