-- application_user membership uniqueness migration.
-- Flyway must execute every statement below on the same connection because
-- LOCK TABLES and user variables are session-scoped.
-- The migration account requires the LOCK TABLES privilege and the separate
-- CREATE TEMPORARY TABLES privilege in addition to its normal read/index DDL privileges. A WRITE lock
-- closes the preflight/DDL TOCTOU window; the temporary table is the generic,
-- identifier-free failure sentinel used after UNLOCK TABLES.
-- Keep every intentional duplicate/wrong-index failure after UNLOCK TABLES. For an
-- unexpected SQL/connection failure, operators must verify that the Flyway session
-- ended and its table lock was released before any retry.
--
-- 운영 복구 (Flyway schema history 포함):
-- 1) 중복 또는 잘못된 named index로 V8이 실패하면 자동 정리하지 않는다. 승인된
--    중복 정리와 named index 상태의 정확한 확인을 마친 뒤 `flyway repair`를 실행하고
--    V8을 멱등 재실행한다.
-- 2) unique index DDL은 성공했지만 schema history 기록 전에 장애가 난 경우,
--    information_schema.statistics에서 named index가 정확한지 먼저 확인한다. 정확하면
--    `flyway repair` 후 V8을 멱등 재실행하며, V8은 CORRECT 상태에서 no-op 한다.
--    CREATE TEMPORARY TABLES 권한 누락도 이 부분 성공 상태를 만들 수 있으므로 사전에 확인한다.

LOCK TABLES application_user WRITE;

SELECT COUNT(*)
INTO @application_user_duplicate_count
FROM (
    SELECT 1
    FROM application_user
    GROUP BY application_id, user_id
    HAVING COUNT(*) > 1
) AS duplicate_memberships;

SELECT
    COUNT(*),
    COALESCE(
        SUM(
            CASE
                WHEN non_unique = 0
                    AND sub_part IS NULL
                    AND index_type = 'BTREE'
                    AND collation = 'A'
                    AND is_visible = 'YES'
                    AND (
                        (seq_in_index = 1 AND column_name = 'application_id')
                        OR (seq_in_index = 2 AND column_name = 'user_id')
                    )
                    THEN 1
                ELSE 0
            END
        ),
        0
    )
INTO @application_user_index_column_count, @application_user_exact_column_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'application_user'
  AND index_name = 'uq_application_user_application_user';

SET @application_user_index_state = CASE
    WHEN @application_user_index_column_count = 0 THEN 'MISSING'
    WHEN @application_user_index_column_count = 2
        AND @application_user_exact_column_count = 2 THEN 'CORRECT'
    ELSE 'WRONG'
END;

SET @application_user_migration_should_fail = IF(
    @application_user_duplicate_count > 0 OR @application_user_index_state = 'WRONG',
    1,
    0
);

SET @application_user_index_sql = IF(
    @application_user_duplicate_count = 0 AND @application_user_index_state = 'MISSING',
    'CREATE UNIQUE INDEX uq_application_user_application_user ON application_user (application_id, user_id) ALGORITHM=INPLACE LOCK=EXCLUSIVE',
    'SELECT 1'
);

PREPARE application_user_index_statement FROM @application_user_index_sql;
EXECUTE application_user_index_statement;
DEALLOCATE PREPARE application_user_index_statement;

UNLOCK TABLES;

DROP TEMPORARY TABLE IF EXISTS tmp_v8_failure_guard;

CREATE TEMPORARY TABLE tmp_v8_failure_guard
(
    failure_sentinel TINYINT NOT NULL,
    CONSTRAINT uq_v8_failure_guard UNIQUE (failure_sentinel)
);

-- The only intentional error contains generic sentinel 1 and the guard name.
-- It never includes application_id, user_id, or a raw composite membership key.
INSERT INTO tmp_v8_failure_guard (failure_sentinel)
SELECT failure_sentinel
FROM (
    SELECT 1 AS failure_sentinel
    UNION ALL
    SELECT 1
) AS generic_failure_rows
WHERE @application_user_migration_should_fail = 1;

DROP TEMPORARY TABLE tmp_v8_failure_guard;
