CREATE TABLE application
(
    id   VARCHAR(36)  NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

create table user
(
    `id`       varchar(36)  not null primary key,
    email      varchar(50)  null,
    password   varchar(50)  null,
    social_id  varchar(255) null,
    provider   varchar(10)  null,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    deleted_at DATETIME     NULL
);

CREATE TABLE application_oauth_provider
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(36)  NOT NULL,
    provider       VARCHAR(10)  NOT NULL,
    redirect_uri   VARCHAR(255) NOT NULL,
    client_id      VARCHAR(255) NOT NULL,
    client_secret  VARCHAR(255),
    FOREIGN KEY (application_id) REFERENCES application (id)
);

CREATE TABLE application_domain
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(36)  NOT NULL,
    domain         varchar(255) not null,
    FOREIGN KEY (application_id) REFERENCES application (id)
);
