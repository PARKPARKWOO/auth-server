create table user
(
    `id`       varchar(36)  not null primary key,
    email      varchar(255) null,
    password   varchar(255) null,
    social_id  varchar(255) null,
    provider   varchar(255) null,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    deleted_at DATETIME     NULL
)