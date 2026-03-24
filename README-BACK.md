# Backend Handoff Notes

이 문서는 제자교회 웹사이트 백엔드 작업을 새 쓰레드나 새 워크스페이스에서 이어갈 때 참고하기 위한 인수인계 메모다.

파일명은 사용자 요청대로 `README-BACK.md` 로 생성했다.

## 현재 상황

- 현재 저장소는 Next.js 프론트엔드 프로젝트다.
- 장기적으로는 프론트와 별도의 `Kotlin + Spring Boot + PostgreSQL` 백엔드 프로젝트를 둘 예정이다.
- 방금 만든 `backend/` 디렉터리는 임시 부트스트랩이다.
- 사용자는 백엔드를 현재 저장소 밖의 상위 폴더로 분리해서 다시 구조를 잡을 예정이다.

즉, `backend/` 안의 내용은 "새 백엔드 프로젝트를 만들 때 참고할 초기 템플릿"으로 보면 된다.

## 아키텍처 결정사항

- 프론트는 지금처럼 별도 Next.js 앱으로 유지한다.
- 백엔드는 Spring Boot API 서버로 분리한다.
- 프론트 코드를 Spring `resources` 로 옮기지 않는다.
- 유튜브는 원본 콘텐츠 저장소로 사용한다.
- 서비스 DB는 유튜브 동기화 결과와 사이트 전용 메타데이터를 저장한다.
- 프론트는 유튜브 API를 직접 호출하지 않고, 항상 우리 백엔드 API만 호출한다.

## 유튜브/콘텐츠 도메인 결정사항

예배 영상 대메뉴 하위 메뉴는 유튜브 채널의 재생목록과 1:1 매핑된다.

- `messages` = `말씀/설교`
- `better-devotion` = `더 좋은 묵상`
- `its-okay` = `그래도 괜찮아`

`그래도 괜찮아` 는 쇼츠 전용 재생목록이다.

핵심 규칙:

- 메뉴명 대신 `playlistId` 를 기준으로 저장한다.
- 재생목록 제목은 바뀔 수 있으므로 식별자로 쓰지 않는다.
- 쇼츠 여부는 우선 메뉴 규칙으로 판별한다.
- 유튜브 제목/썸네일과 사이트 전용 제목/썸네일 override를 분리한다.

## 회원 관리 결론

현재 단계에서는 일반 사용자 회원 관리는 필요하지 않다.

지금 필요한 범위:

- 공개 조회 API
- 유튜브 재생목록 동기화
- 나중에 붙일 최소 관리자 인증

아직 필요 없는 범위:

- 교인 회원가입/로그인
- 개인화 기능
- 커뮤니티 기능
- 일반 사용자 권한 관리

즉 1차 백엔드는 "비회원 공개 서비스 + 내부 관리용 확장 가능 구조" 로 시작하면 된다.

## 문서로 남긴 설계

아래 문서를 먼저 읽으면 된다.

- [`docs/youtube-content-architecture.md`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/docs/youtube-content-architecture.md)
- [`docs/backend-api-bootstrap-plan.md`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/docs/backend-api-bootstrap-plan.md)

문서 요약:

- 유튜브 재생목록 기반 도메인 모델
- Postgres 테이블 스키마 초안
- Spring 패키지 구조 제안
- 동기화 배치 흐름
- API 스펙 초안
- 초기 백엔드 프로젝트 부트스트랩 계획

## 임시로 생성한 backend 부트스트랩

현재 저장소 안에 아래 임시 백엔드 뼈대를 만들어 둔 상태다.

- [`backend/build.gradle.kts`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/build.gradle.kts)
- [`backend/settings.gradle.kts`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/settings.gradle.kts)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/ApiApplication.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/ApiApplication.kt)
- [`backend/src/main/resources/application.yml`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/application.yml)
- [`backend/docker-compose.local.yml`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/docker-compose.local.yml)

포함된 내용:

- Spring Boot 기본 앱
- Kotlin/Gradle 빌드 설정
- 헬스체크 API
- 미디어 조회 API 스텁
- 유튜브 동기화 스케줄러 스텁
- Flyway 마이그레이션
- 로컬 Postgres 실행용 compose

## 임시 backend 안의 주요 파일

### API

- [`backend/src/main/kotlin/kr/or/thejejachurch/api/health/HealthController.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/health/HealthController.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/interfaces/api/MediaController.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/interfaces/api/MediaController.kt)

현재 노출 엔드포인트:

- `GET /api/v1/health`
- `GET /api/v1/media/menus`
- `GET /api/v1/media/home`
- `GET /api/v1/media/menus/{siteKey}/videos`
- `GET /api/v1/media/videos/{youtubeVideoId}`

주의:

- 아직 DB 조회가 아니라 샘플 데이터 응답이다.
- `MediaQueryService` 가 임시 하드코딩 데이터를 반환한다.

관련 파일:

- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/application/MediaQueryService.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/application/MediaQueryService.kt)

### 도메인 엔티티

- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/ContentMenu.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/ContentMenu.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/YoutubePlaylist.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/YoutubePlaylist.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/YoutubeVideo.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/YoutubeVideo.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/PlaylistVideo.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/PlaylistVideo.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/VideoMetadata.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/domain/VideoMetadata.kt)

### 배치/설정

- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/application/YoutubeSyncService.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/application/YoutubeSyncService.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/media/infrastructure/scheduler/YoutubeSyncScheduler.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/media/infrastructure/scheduler/YoutubeSyncScheduler.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/common/config/YoutubeProperties.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/common/config/YoutubeProperties.kt)

주의:

- 아직 실제 YouTube API 클라이언트는 구현하지 않았다.
- 현재 sync 서비스는 스텁이다.

### 예외 처리

- [`backend/src/main/kotlin/kr/or/thejejachurch/api/common/error/GlobalExceptionHandler.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/common/error/GlobalExceptionHandler.kt)
- [`backend/src/main/kotlin/kr/or/thejejachurch/api/common/error/NotFoundException.kt`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/kotlin/kr/or/thejejachurch/api/common/error/NotFoundException.kt)

## Flyway 마이그레이션 상태

아래 마이그레이션 파일을 추가해 두었다.

- [`backend/src/main/resources/db/migration/V1__create_content_menu.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V1__create_content_menu.sql)
- [`backend/src/main/resources/db/migration/V2__create_youtube_playlist.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V2__create_youtube_playlist.sql)
- [`backend/src/main/resources/db/migration/V3__create_youtube_video.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V3__create_youtube_video.sql)
- [`backend/src/main/resources/db/migration/V4__create_playlist_video.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V4__create_playlist_video.sql)
- [`backend/src/main/resources/db/migration/V5__create_video_metadata.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V5__create_video_metadata.sql)
- [`backend/src/main/resources/db/migration/V6__seed_content_menus.sql`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/backend/src/main/resources/db/migration/V6__seed_content_menus.sql)

주의:

- `content_menu`만 Flyway seed로 생성한다.
- 실제 `youtube_playlist` 연결은 환경변수 기반 bootstrap으로 애플리케이션 시작 시 upsert 하는 전략으로 정리했다.

## 기술 결정사항

- 프론트와 백엔드는 분리 배포한다.
- 백엔드는 `Kotlin + Spring Boot + PostgreSQL + Flyway` 로 간다.
- Java 21 기준으로 간다.

## 현재 환경에서 확인된 제약

- 현재 프로젝트에 Gradle wrapper를 추가했다.
- 다만 이 샌드박스에서는 Gradle daemon/socket 제약과 Java 21 미설치 상태 때문에 실제 부팅 검증은 아직 끝내지 못했다.

즉 새 백엔드 프로젝트에서 가장 먼저 해야 할 일은 Java 21 환경에서 wrapper 기반 부팅 검증을 끝내는 것이다.

## 새 백엔드 프로젝트에서 이어서 해야 할 우선순위

1. Java 21 환경에서 wrapper 기반 부팅 검증을 끝낸다.
2. Postgres 로컬 컨테이너를 올리고 Flyway 마이그레이션을 검증한다.
3. `content_menu` seed + `youtube_playlist` env bootstrap 을 반영한다.
4. JPA Repository 를 추가한다.
5. `MediaQueryService` 의 샘플 응답을 실제 DB 조회로 바꾼다.
6. YouTube API 클라이언트를 구현한다.
7. `YoutubeSyncService` 를 실제 동기화 로직으로 바꾼다.
8. 프론트에서 더미 데이터를 백엔드 API로 점진 교체한다.

## 프론트 연동 방향

현재 프론트는 계속 별도 Next.js 앱으로 유지한다.

나중에 프론트에서 연결할 대상:

- 홈 영상 섹션
- `/sermons/messages`
- `/sermons/better-devotion`
- `/sermons/its-okay`

현재 프론트의 더미 데이터 위치:

- [`src/lib/site-data.ts`](/Users/hanwool/ground/Palm%20Lab/thejejachurch_web/src/lib/site-data.ts)

## 다음 쓰레드에서 바로 말하면 좋은 문장

새 쓰레드에서 아래처럼 말하면 맥락 연결이 빠르다.

`README-BACKE.md 와 docs/youtube-content-architecture.md, docs/backend-api-bootstrap-plan.md 를 기준으로 새 상위 폴더의 Spring Boot 백엔드 프로젝트를 이어서 구성해줘.`

## 정리

현재까지의 결론은 단순하다.

- 백엔드는 별도 프로젝트로 분리
- 회원 관리는 아직 불필요
- 우선은 공개 조회 API와 유튜브 동기화에 집중
- 관리자 인증은 2차 단계에서 추가
- 현재 저장소 안 `backend/` 는 참고용 임시 부트스트랩
