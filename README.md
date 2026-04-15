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
YOUTUBE_CHANNEL_ID=your-channel-id
ADMIN_SYNC_KEY=your-admin-key
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

메뉴는 역할별로 나뉩니다.

- `content_menu`: 설교/영상 콘텐츠 카테고리
- `site_navigation`: 헤더/모바일 메뉴/브레드크럼/LNB용 사이트 메뉴

`content_menu` 는 Flyway/관리자 기능을 통해 관리하고, 실제 `youtube_playlist` 연결은 관리자 생성/발견 플로우로 운영합니다.

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

운영 CORS 예시:

```text
CORS_ALLOWED_ORIGINS=https://your-project.vercel.app,https://your-domain.com,https://www.your-domain.com
```

## Flyway checksum mismatch 대응

이미 적용된 migration 파일은 수정하지 않는 것이 원칙입니다. 과거 migration을 수정하면 로컬 DB에서 Flyway validation이 실패할 수 있습니다.

- 기본 복구 전략: 로컬 DB reset 후 재기동
- 예외 전략: 현재 DB 구조가 migration 결과와 동일하다는 것을 확인한 경우에만 `repair` 검토

자세한 기준과 복구 절차는 [../docs/flyway-migration-hygiene.md](/Users/hanwool/ground/Palm%20Lab/TDCH/docs/flyway-migration-hygiene.md)를 따릅니다.

## Oracle VM 운영 배포

Oracle Cloud Infrastructure VM 기준 운영 파일을 함께 관리합니다.

- `deploy/docker-compose.prod.yml`: 운영용 `app + postgres` compose
- `deploy/nginx/api.tdch.co.kr.http.conf`: 인증서 발급 전 HTTP-only nginx 설정
- `deploy/nginx/api.tdch.co.kr.conf`: 인증서 발급 후 HTTPS reverse proxy 설정
- `.env.production.example`: `/opt/tdch/.env` 작성용 예시
- `.github/workflows/deploy-oracle.yml`: `main` 브랜치용 GHCR + SSH 배포 워크플로
- `docs/oracle-oci-deploy.md`: VM 반영 절차와 GitHub Secrets 정리

운영 VM의 최종 파일 배치는 아래를 전제로 합니다.

```text
/opt/tdch/.env
/opt/tdch/docker-compose.prod.yml
```

배포 워크플로는 `GHCR`에 이미지를 올린 뒤, Oracle VM에 SSH로 접속해 `docker compose pull && up -d`를 실행합니다.
