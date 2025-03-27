CREATE TABLE if not exists application_user
(
    id             bigint      NOT NULL PRIMARY KEY auto_increment,
    user_id        varchar(36) not null,
    application_id varchar(36) not null,
    role           varchar(10) not null,
    created_at     timestamp   NOT NULL
);

CREATE TABLE if not exists organization
(
    id         varchar(36) NOT NULL PRIMARY KEY,
    name       varchar     not null,
    manager_id varchar(36),
    created_at timestamp   NOT NULL
);

CREATE TABLE if not exists organization_application
(
    id              bigint      NOT NULL PRIMARY KEY auto_increment,
    organization_id varchar(36) not null,
    application_id  varchar(36) not null,
    created_at      timestamp   NOT NULL
);

CREATE TABLE if not exists user_oauth_token
(
    user_id       varchar(36) NOT NULL PRIMARY KEY,
    access_token  varchar     not null,
    refresh_token varchar     null
);
