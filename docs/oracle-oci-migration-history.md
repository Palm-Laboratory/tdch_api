# Oracle OCI Migration History

이 문서는 `tdch_api` 운영 환경을 다른 플랫폼에서 Oracle OCI VM으로 옮길 때 사용했던 이관 메모를 보관한다.

현재 운영 절차는 [`oracle-oci-deploy.md`](./oracle-oci-deploy.md)를 기준으로 본다.

## Railway -> Oracle DB 이전 순서

과거 운영 전환 시 사용했던 순서:

1. 기존 Postgres에서 `pg_dump` 확보
2. Oracle VM의 `db` 컨테이너 기동
3. `pg_restore`로 운영 데이터 복원
4. `app` 컨테이너 기동
5. `api/v1/health`, `api/v1/media/home`, `api/v1/navigation` 검증

예시:

```bash
cd <deploy-path>
docker compose -f docker-compose.prod.yml up -d db
docker exec -i tdch-postgres psql -U postgres -d thejejachurch -c "select 1;"
```

## 전환 시 확인했던 항목

- 운영 데이터 백업 별도 보관
- DB restore 완료 전에는 앱 컨테이너를 올리지 않음
- API 헬스체크와 주요 조회 API를 직접 확인
- 웹 프론트의 API base URL이 새 운영 도메인을 가리키는지 확인

이 문서는 historical reference 용도다.
