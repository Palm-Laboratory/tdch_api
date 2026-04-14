# Oracle OCI Deploy Guide

`tdch_api`를 `Oracle Cloud Infrastructure` 단일 VM에 배포하고 운영할 때 사용하는 현재 기준 문서다.

대상 구조:

- nginx
- Docker Compose
- Spring Boot app
- PostgreSQL 16

과거 데이터 이관 절차는 별도 문서 [`oracle-oci-migration-history.md`](./oracle-oci-migration-history.md)로 분리했다.

## 1. 운영 디렉토리 배치

운영 VM에는 애플리케이션 파일을 둘 배포 디렉토리가 필요하다.
이 문서에서는 예시로 `<deploy-path>`를 사용한다.

일반적인 예시:

```text
<deploy-path>=/opt/tdch
```

필수 파일:

```text
<deploy-path>/.env
<deploy-path>/docker-compose.prod.yml
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

- `APP_IMAGE=ghcr.io/<github-owner>/<github-repository>:latest`
- `POSTGRES_DB=thejejachurch`
- `POSTGRES_USER=postgres`
- `POSTGRES_PASSWORD=<strong-password>`
- `DB_URL=jdbc:postgresql://db:5432/thejejachurch`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=<same-as-postgres-password>`
- `YOUTUBE_*`
- `ADMIN_SYNC_KEY`
- `CORS_ALLOWED_ORIGINS=https://<web-domain>,https://www.<web-domain>`

완성한 파일을 VM에 업로드한다.

```bash
scp /tmp/tdch.env <oci-user>@<oci-host>:<deploy-path>/.env
scp deploy/docker-compose.prod.yml <oci-user>@<oci-host>:<deploy-path>/docker-compose.prod.yml
```

## 3. PostgreSQL + 앱 기동

VM에서 최초 기동:

```bash
cd <deploy-path>
docker compose -f docker-compose.prod.yml up -d db
docker compose -f docker-compose.prod.yml up -d app
docker compose -f docker-compose.prod.yml ps
curl http://127.0.0.1:8080/api/v1/health
```

DB 복원이나 점검이 필요하면 먼저 `db`만 올리고, 작업이 끝난 뒤 `app`을 기동해도 된다.

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
sudo cp deploy/nginx/api.tdch.co.kr.http.conf /etc/nginx/sites-available/<api-domain>
sudo ln -sf /etc/nginx/sites-available/<api-domain> /etc/nginx/sites-enabled/<api-domain>
sudo nginx -t
sudo systemctl reload nginx
```

주의:

- 현재 저장소의 nginx 템플릿 파일명은 `api.tdch.co.kr` 기준이다.
- 실제 운영 도메인이 다르면 템플릿 내부 `server_name`과 대상 파일명을 같이 맞춰야 한다.
- 이 단계에서는 `<api-domain>` DNS가 `<oci-host>`를 가리켜야 한다.

## 5. certbot 발급 후 HTTPS 설정

DNS 전파가 끝났으면 인증서를 발급한다.

```bash
sudo certbot --nginx -d <api-domain>
```

그 다음 최종 nginx 설정으로 교체한다.

```bash
sudo cp deploy/nginx/api.tdch.co.kr.conf /etc/nginx/sites-available/<api-domain>
sudo nginx -t
sudo systemctl reload nginx
```

검증:

```bash
curl https://<api-domain>/api/v1/health
```

## 6. GitHub Actions 배포

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

예시:

- `OCI_HOST=<oci-host>`
- `OCI_USERNAME=<oci-user>`
- `OCI_DEPLOY_PATH=<deploy-path>`

`GHCR_TOKEN`은 최소 `read:packages` 권한이 있는 토큰을 사용한다.

배포 워크플로는 다음 순서로 동작한다.

1. 테스트 실행
2. GHCR 이미지 빌드 및 push
3. 운영 VM에 `docker-compose.prod.yml` 업로드
4. SSH 접속 후 `docker compose pull && up -d --remove-orphans`

## 7. 운영 검증 체크리스트

- `<api-domain>` DNS가 운영 VM을 가리킴
- `https://<api-domain>/api/v1/health` 응답 정상
- `https://<api-domain>/api/v1/media/home` 응답 정상
- `https://<api-domain>/api/v1/navigation` 응답 정상
- `tdch_web`의 API base URL이 `https://<api-domain>`을 가리킴
- 운영 VM의 `<deploy-path>/.env`와 GitHub Actions secrets가 서로 일관됨
