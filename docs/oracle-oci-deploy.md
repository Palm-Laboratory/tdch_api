# Oracle OCI Deploy Guide

`tdch_api`를 `Oracle Cloud Infrastructure` 단일 VM에 배포할 때 기준으로 쓰는 운영 절차다.

대상 구조:

- nginx
- Docker Compose
- Spring Boot app
- PostgreSQL 16

## 1. VM 파일 배치

운영 VM에서 기준 디렉토리는 `/opt/tdch`다.

필수 파일:

```text
/opt/tdch/.env
/opt/tdch/docker-compose.prod.yml
```

초기 작성은 아래 예시 파일을 기준으로 한다.

- `.env.production.example`
- `deploy/docker-compose.prod.yml`

## 2. 운영 환경 변수 작성

로컬에서 아래 파일을 기준으로 운영 값을 채운다.

```bash
cp .env.production.example /tmp/tdch.env
```

핵심 값:

- `APP_IMAGE=ghcr.io/<github-owner>/<repo>:latest`
- `POSTGRES_DB=thejejachurch`
- `POSTGRES_USER=postgres`
- `POSTGRES_PASSWORD=<strong-password>`
- `DB_URL=jdbc:postgresql://db:5432/thejejachurch`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=<same-as-postgres-password>`
- `YOUTUBE_*`
- `ADMIN_SYNC_KEY`
- `CORS_ALLOWED_ORIGINS=https://www.tdch.co.kr,https://tdch.co.kr`

완성한 파일을 VM에 업로드한다.

```bash
scp /tmp/tdch.env ubuntu@146.56.45.252:/opt/tdch/.env
scp deploy/docker-compose.prod.yml ubuntu@146.56.45.252:/opt/tdch/docker-compose.prod.yml
```

## 3. PostgreSQL + 앱 기동

VM에서 최초 기동:

```bash
cd /opt/tdch
docker compose -f docker-compose.prod.yml up -d db
docker compose -f docker-compose.prod.yml up -d app
docker compose -f docker-compose.prod.yml ps
curl http://127.0.0.1:8080/api/v1/health
```

DB restore 전에는 먼저 `db`만 올리고, `pg_restore`가 끝난 뒤 `app`을 기동해도 된다.

## 4. nginx 초기 설정

패키지 설치:

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
sudo mkdir -p /var/www/certbot
sudo chown -R www-data:www-data /var/www/certbot
```

인증서 발급 전에는 HTTP-only 설정을 사용한다.

```bash
sudo cp deploy/nginx/api.tdch.co.kr.http.conf /etc/nginx/sites-available/api.tdch.co.kr
sudo ln -sf /etc/nginx/sites-available/api.tdch.co.kr /etc/nginx/sites-enabled/api.tdch.co.kr
sudo nginx -t
sudo systemctl reload nginx
```

이 단계에서 `api.tdch.co.kr` DNS가 VM IP를 가리켜야 한다.

## 5. certbot 발급 후 HTTPS 설정

DNS 전파가 끝났으면 인증서를 발급한다.

```bash
sudo certbot --nginx -d api.tdch.co.kr
```

그 다음 최종 nginx 설정으로 교체한다.

```bash
sudo cp deploy/nginx/api.tdch.co.kr.conf /etc/nginx/sites-available/api.tdch.co.kr
sudo nginx -t
sudo systemctl reload nginx
```

검증:

```bash
curl https://api.tdch.co.kr/api/v1/health
```

## 6. Railway -> Oracle DB 이전 순서

권장 순서:

1. Railway Postgres에서 `pg_dump` 확보
2. Oracle VM의 `db` 컨테이너 기동
3. `pg_restore`로 운영 데이터 복원
4. `app` 컨테이너 기동
5. `api/v1/health`, `api/v1/media/home`, `api/v1/navigation` 검증

예시:

```bash
docker exec -i tdch-postgres psql -U postgres -d thejejachurch -c "select 1;"
```

## 7. GitHub Actions 배포

워크플로 파일:

```text
.github/workflows/deploy-oracle.yml
```

필수 GitHub Secrets:

- `OCI_HOST`
- `OCI_USERNAME`
- `OCI_SSH_PRIVATE_KEY`
- `OCI_DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

권장 값:

- `OCI_HOST=146.56.45.252`
- `OCI_USERNAME=ubuntu`
- `OCI_DEPLOY_PATH=/opt/tdch`

`GHCR_TOKEN`은 최소 `read:packages` 권한이 있는 토큰을 사용한다.

## 8. 최종 전환 체크리스트

- `api.tdch.co.kr` A 레코드가 Oracle Reserved IP를 가리킴
- `https://api.tdch.co.kr/api/v1/health` 응답 정상
- `tdch_web`의 `MEDIA_API_BASE_URL`, `NEXT_PUBLIC_MEDIA_API_BASE_URL`을 `https://api.tdch.co.kr`로 교체
- Railway 종료 전 최종 `pg_dump` 백업 별도 보관
