# thejejachurch-api

제자교회 웹사이트용 Kotlin + Spring Boot API 서버 부트스트랩입니다.

## 현재 범위

- 헬스체크 API
- 관리자 인증/계정 관리 API
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
ADMIN_SYNC_KEY=your-admin-key
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

## 초기 엔드포인트

- `GET /api/v1/health`
- `POST /api/v1/admin/auth/login`
- `POST /api/v1/admin/auth/logout`
- `GET /api/v1/admin/accounts`
- `POST /api/v1/admin/accounts`

운영 CORS 예시:

```text
CORS_ALLOWED_ORIGINS=https://your-project.vercel.app,https://your-domain.com,https://www.your-domain.com
```

## Flyway checksum mismatch 대응

현재는 `admin_account` 기준의 단일 baseline migration만 유지합니다.

- 기본 복구 전략: 관리자 계정 외 테이블 제거 후 Flyway history 초기화
- 예외 전략: 현재 DB 구조가 baseline과 동일하다는 것을 확인한 경우에만 `repair` 검토

자세한 기준과 복구 절차는 [../docs/flyway-migration-hygiene.md](/Users/hanwool/ground/Palm%20Lab/TDCH/docs/flyway-migration-hygiene.md)를 따릅니다.

## Oracle VM 운영 배포

Oracle Cloud Infrastructure VM 기준 운영 파일을 함께 관리합니다.

- `deploy/docker-compose.prod.yml`: 운영용 `app + postgres` compose
- `deploy/nginx/api.tdch.co.kr.http.conf`: 인증서 발급 전 HTTP-only nginx 설정
- `deploy/nginx/api.tdch.co.kr.conf`: 인증서 발급 후 HTTPS reverse proxy 설정
- `.env.production.example`: `/opt/tdch/.env` 작성용 예시
- `.github/workflows/deploy-oracle.yml`: `main` 브랜치용 GHCR + SSH 배포 워크플로
- `docs/oracle-oci-operations-manual.md`: 최초 서버 세팅부터 GHCR, 배포, 점검, 복구까지 포함한 운영 매뉴얼
- `docs/oracle-oci-deploy.md`: 빠른 배포 요약과 핵심 체크리스트

운영 VM의 최종 파일 배치는 아래를 전제로 합니다.

```text
/opt/tdch/.env
/opt/tdch/docker-compose.prod.yml
```

배포 워크플로는 `GHCR`에 이미지를 올린 뒤, Oracle VM에 SSH로 접속해 해당 배포의 `sha-<commit>` 이미지를 직접 pull하고 `docker compose up -d`를 실행합니다.
