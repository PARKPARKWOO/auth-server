# application_user V8 운영 절차

이 문서는 `V8__unique_application_user.sql` 적용과 실패 복구를 위한 승인 게이트다. V8은
`application_user` 원본 행을 자동 삭제하거나 수정하지 않는다.

## 사전 조건

- 유지보수 시간과 복구 계획을 승인받는다. `LOCK TABLES application_user WRITE` 동안 다른
  세션의 해당 테이블 접근이 차단될 수 있다.
- Flyway 계정에는 기존 읽기·인덱스 DDL 권한 외에 `LOCK TABLES privilege`와 별도의
  `CREATE TEMPORARY TABLES` 권한이 모두 필요하다. 임시 테이블은 원본 식별자를 오류에
  포함하지 않는 generic failure sentinel에 사용한다.
- Flyway가 V8 전체를 같은 DB connection에서 실행하는지 확인한다. table lock과 user variable은
  session 범위이므로 connection이 바뀌면 안전 계약이 깨진다.
- 예상하지 못한 SQL 오류나 connection 장애로 V8이 중단되면 재시도 전에 해당 Flyway session이
  종료됐고 `application_user` table lock이 해제됐는지 확인한다. 중복/WRONG의 의도적 실패는
  V8이 `UNLOCK TABLES`를 먼저 실행한 뒤 발생한다.
- `CREATE TEMPORARY TABLES` 권한이 없으면 unique index DDL은 성공했지만 sentinel 생성과
  schema history 기록은 실패한 부분 성공 상태가 될 수 있다. 적용 전에 권한을 확인하고,
  이 상태가 발생하면 아래의 "인덱스는 존재하지만 schema history 누락" 절차만 따른다.

읽기 전용 중복 preflight:

```sql
SELECT application_id, user_id, COUNT(*)
FROM application_user
GROUP BY application_id, user_id
HAVING COUNT(*) > 1;
```

결과가 한 행이라도 있으면 배포를 중단한다. 어떤 membership과 authority를 보존할지는 자동으로
결정하지 않으며, 데이터 소유자 승인을 받은 별도 작업에서만 중복 정리를 수행한다.

named index exact 상태 확인:

```sql
SELECT index_name, non_unique, seq_in_index, column_name, sub_part, index_type, collation, is_visible
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'application_user'
  AND index_name = 'uq_application_user_application_user'
ORDER BY seq_in_index;
```

정확한 상태는 결과가 두 행이고 다음 조건을 모두 만족하는 경우뿐이다.

1. 두 행 모두 `non_unique = 0`, `sub_part IS NULL`, `index_type = BTREE`, `collation = A`,
   `is_visible = YES`
2. `seq_in_index = 1`은 `application_id`
3. `seq_in_index = 2`는 `user_id`

이름만 같고 위 조건이 다른 index는 `WRONG`이다. 운영자가 임의로 schema history를 추가하거나
잘못된 index를 자동 교체하지 않는다.

## 중복으로 V8 실패

1. V8의 generic sentinel 오류를 확인하고 추가 migration 시도를 중단한다.
2. 데이터 소유자 승인을 기록한다.
3. 승인 범위에 따라 중복 정리를 수행한다. V8 자체에는 자동 삭제 로직이 없다.
4. 읽기 전용 preflight 결과가 0행인지 다시 확인한다.
5. Flyway failed V8 기록을 `flyway repair`로 정리한다.
6. V8을 멱등 재실행한다.

`flyway repair`는 중복 데이터를 고치지 않는다. 중복 정리와 0행 검증보다 먼저 실행하지 않는다.

## 인덱스는 존재하지만 schema history 누락

이는 unique index DDL 성공 후 Flyway schema history 기록 전에 프로세스나 connection이 실패한
경우에 발생할 수 있다.

1. `information_schema.statistics` 조회로 named index의 정확한 상태를 먼저 확인한다.
2. 정확한 경우에만 failed V8 기록을 `flyway repair`로 정리한다.
3. V8을 멱등 재실행한다. V8은 exact `CORRECT` index를 확인하고 DDL을 no-op 한 뒤 history를 기록한다.
4. index가 `WRONG`이면 repair나 재실행을 중단하고, 별도 승인 후 index 교정 계획을 수행한다.

복구 중에도 원본 membership을 자동 삭제하거나 authority를 임의로 선택하지 않는다.
