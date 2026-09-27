# 푸시 저장소 Neon 전환

## 상태

2026-09-28 로컬 구현. 운영 배포·운영 DB 변경·실제 데이터 이전·실제 알림 발송은 하지 않았다. 기존 배포는 계속 MySQL을 사용한다. `PUSH_STORAGE=neon`은 아래 전환 절차 완료 시에만 적용한다. 기본값 mysql은 배포 전 기존 실행을 보존하기 위한 전환 스위치다.

Neon 연결과 트랜잭션은 기기, 발송, 메시지, 설문 예약, 자동 알림 설정에만 적용된다. 기존 회원/업무 데이터와 내구성 있는 이벤트 전달 기록은 서비스 DB에 둔다. 자동 알림 설정 변경 이력(변경자/시각/값)은 Neon에 기록한다. 이미지는 기존 Vercel 프로젝트의 같은 Neon DB를 사용한다.

## 런타임 설정

- `PUSH_STORAGE=neon`
- `PUSH_DATABASE_URL=jdbc:postgresql://<Neon pooled host>/<database>?sslmode=require`
- `PUSH_DATABASE_USERNAME`, `PUSH_DATABASE_PASSWORD`: Vercel production의 `DATABASE_URL`과 같은 DB의 전용 런타임 계정. PostgreSQL URL을 JDBC URL로 변환하고 사용자/비밀번호는 별도 환경변수로 전달한다. 비밀값을 Git에 저장하지 않는다.
- `PUSH_AUDIENCE=INTERNAL_TEST` (개발 백엔드) 또는 `LIVE` (운영 백엔드). 앱의 빌드 종류와 별개다. INTERNAL_TEST 서버에서는 LIVE 발송 요청을 거절한다.
- 서비스 DB와 Neon 모두 스키마 자동 생성 대신 validate로 실행한다. 서비스 DB에는 outbox 테이블이 먼저 있어야 한다. 푸시 엔티티는 서비스 DB 영속성 관리에서 제외되므로 기존 MySQL 푸시 테이블에 새 분류 컬럼을 추가할 필요가 없다.
- 발송·receipt·복구 워커는 중앙 운영 실행에서만 각각 기존 `PUSH_WORKER_ENABLED`, `PUSH_RECEIPT_WORKER_ENABLED`, `PUSH_RECOVERY_WORKER_ENABLED`를 활성화한다.
- 이벤트 전달기는 각 이벤트 발행 서버의 `PUSH_AUDIENCE`에 해당하는 outbox만 처리한다. 중앙 개발/운영 서버에서 `PUSH_OUTBOX_ENABLED=true`를 설정한다. 동일 DB의 발송 작업은 잠금으로 단일 점유한다.
- 설문 예약 워커는 `PUSH_SURVEY_WORKER_ENABLED=true`를 명시해야 실행한다. 각 audience의 예약/설정을 분리하며 로컬에서는 비활성 상태를 유지한다.
- 푸시 전용 Neon 연결 풀은 기본 최대 5, 최소 유휴 0이다. 운영 동시 인스턴스 수와 실제 지연에 맞춰 배포 시 조정한다.

회원 정보는 기존 DB에서 조회한 userId를 통해 Neon 기기 정보와 연결한다. ADMIN 권한은 미리보기/대상 결정뿐 아니라 작업 점유 시점에도 확인한다. 자동 발송은 명시적 audience 및 idempotency key로 기록된다. 기존 분류 불명 이력은 LEGACY_UNKNOWN으로 유지한다.

## 스키마와 복사

1. `docs/sql/2026-09-28-push-neon.sql`을 Neon에, `docs/sql/2026-09-28-push-outbox-mysql.sql`을 서비스 MySQL에 적용할 변경으로 검토한다. 둘 다 아직 미적용이다. 이미지 테이블은 기존 것을 유지한다. **이전 `2026-09-28-push-audience.sql`은 적용하지 않는다.**
2. 백업/복구 지점과 Neon 계정 권한을 준비한다. 이미지 런타임은 이미지 테이블에만, 백엔드 런타임은 푸시 테이블 및 해당 identity sequence에만 접근하도록 분리한다. DDL은 별도 마이그레이션 계정으로 실행한다.
3. Python 가상환경에 `scripts/migrate-push-neon.requirements.txt`를 설치하고 보호된 환경변수로 `PUSH_SOURCE_MYSQL_URL`, `PUSH_TARGET_POSTGRES_URL`을 제공한다. 필요한 MySQL TLS CA는 `PUSH_SOURCE_MYSQL_SSL_CA`로 지정한다. 연결 문자열이나 토큰을 명령행 인자/로그로 남기지 않는다.
4. `python scripts/migrate-push-neon.py`는 읽기 전용 건수 조사다. Neon baseline을 먼저 적용한 상태에서 실행한다.
5. 최종 전환 창에서 앱 기기 등록/해제, 관리자 쓰기, 업무 이벤트 생산자, 발송/receipt/복구/예약 워커를 일시 중지한다. 기존 SENDING 작업의 진행 여부를 확인하고 새로운 호출이 없는 시점에 복사한다. 출처가 섞인 예약은 먼저 목록과 담당 환경을 확인한다.
6. 비어 있는 대상 테이블에만 `python scripts/migrate-push-neon.py --apply --writers-paused --pending-schedule-audience <확인한 구분>`으로 복사한다. 이 옵션은 분류가 없는 대기 예약에만 적용하며 혼합된 구분이라면 한 값으로 강제하지 말고 개별 매핑을 먼저 준비한다. 전체 복사는 하나의 PostgreSQL 트랜잭션이며 테이블별 건수를 비교하고 identity를 보정한다. 기존 ID·본문·기기 토큰·처리 상태·Expo 티켓·재시도 횟수·시각을 유지한다. 원본 MySQL에는 쓰지 않는다.
7. 기존 Redis 자동 알림 활성화 상태는 전체 서버가 공유했으므로 audience별로 추정 복사하지 않는다. 검토된 값만 내부 테스트/실서비스 각각 설정한다. Neon에 설정이 없으면 기본 꺼짐이다.
8. Dev/Prod 백엔드를 같은 Neon DB에 연결하고 API 조회/등록/해제/닉네임 검색/권한을 확인한다. 관리자 Vercel도 새 버전으로 배포한 후 발송 없는 미리보기부터 확인한다. 중앙 워커를 순서대로 활성화한다. 실제 발송 검증은 사용자 승인 범위에 따라 ADMIN 기기부터 수행한다.
9. 정상 확인 뒤 쓰기를 재개한다. 기존 MySQL 푸시 테이블은 즉시 삭제하지 않는다.

초기 복사 이후 계속 쓰기가 발생하는 무중단 동기화 도구는 이 스크립트의 범위가 아니다. 이 절차는 짧은 쓰기 중지 창을 사용하는 최종 복사 방식이다. 스크립트가 중간 실패하면 대상 INSERT는 롤백한다. identity sequence의 증가 자체는 롤백되지 않을 수 있지만 재실행 시 다시 보정된다. 대상에 기존 행이 있으면 덮어쓰지 않고 중단한다.

## 이벤트 유실·중복 방지

캐릭터 해제/신고 처리/혼잡도 이벤트는 MySQL 업무 트랜잭션의 BEFORE_COMMIT에서 outbox를 기록한다. Neon 장애 중에도 기록이 남는다. 전달기는 실패 시 지수 지연으로 재시도하며, 오류 로그는 이벤트 ID/시도 횟수/예외 종류만 기록한다. Neon의 발송 요청은 audience별 중복 방지 키와 PostgreSQL 트랜잭션 잠금으로 중복 생성을 막는다.

회원 탈퇴는 토큰 비활성화가 성공한 뒤 기존 회원 삭제가 진행된다. Neon 비활성화 실패 시 탈퇴 요청 전체가 실패하므로 재시도해야 한다. 서로 다른 DB의 원자적 커밋을 보장하는 것으로 설명하지 않는다.

오래 대기하는 outbox와 반복 실패는 `completed_at IS NULL` 및 attempts/next_attempt_at으로 모니터링한다. 완료 이벤트의 보존/정리 정책은 배포 전 운영 기준에 맞춰 확정한다. 과거 발송 자체를 다시 수행하는 복구는 금지한다.

## 롤백

Neon 쓰기 시작 전에는 신규 워커를 멈추고 기존 설정을 유지한다. Neon에 등록/발송이 생긴 이후에는 단순히 PUSH_STORAGE를 mysql로 되돌리지 않는다. 모든 쓰기와 워커를 중지하고 새 토큰/해제/발송/티켓/receipt/예약 변경분을 기존 DB에 반영한 뒤 중복 발송 여부를 검증해야 한다. 역방향 자동 이전은 제공하지 않는다. 장애 중에는 양쪽 워커를 동시에 켜는 대신 쓰기를 중지한 채 데이터 대조 또는 Neon 복구를 우선한다.

## 검증

- 기존 notification 테스트 및 실제 PostgreSQL 16 별도 DB 통합 테스트.
- 회원 영속성에서 푸시 엔티티 제외, 푸시 영속성에서 회원 엔티티 제외.
- DB를 넘는 닉네임/이메일 검색 및 ADMIN 조회.
- PostgreSQL SKIP LOCKED, 동시 동일 키 요청, audience 제한, 예약/설정 분리.
- 업무 rollback 시 outbox 미생성, commit 시 기록 생성, 실패 재시도 후 완료.
- 이전 행 변환의 ID/상태 보존 및 분류 미상 예약 차단.

PG 통합 테스트는 전용 로컬 PostgreSQL 테스트 DB가 필요하다. 이미 테스트 컨테이너가 있으면 `docker start kodaero-push-neon-test-0928`로 재사용한다. `KODAERO_PUSH_PG_TEST=true`를 지정한 경우에만 실행하며 테스트 DB를 초기화한다. 운영/Neon URL을 연결하지 않는다.

```sh
docker run --detach --name kodaero-push-neon-test-0928 --publish 127.0.0.1:55439:5432 \
  --env POSTGRES_USER=push_test --env POSTGRES_PASSWORD=local-test-only --env POSTGRES_DB=push_test postgres:16-alpine
KODAERO_PUSH_PG_TEST=true ./gradlew test --tests '*notification*'
python3 scripts/test_migrate_push_neon.py
```

실제 운영 MySQL→Neon 복사 및 운영 PostgreSQL validate 기동은 배포 전 별도 검증 대상이다. 로컬 테스트 성공을 실제 운영 이전 완료로 간주하지 않는다.
