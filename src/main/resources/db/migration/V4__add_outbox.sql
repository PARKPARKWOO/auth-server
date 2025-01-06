CREATE TABLE outbox
(
    id         bigint       NOT NULL PRIMARY KEY auto_increment,
    payload    TEXT         NOT NULL,
    event_type varchar(255) NOT NULL,
    record_operation  varchar(20)  NOT NULL,
    created_at timestamp    NOT NULL
);
