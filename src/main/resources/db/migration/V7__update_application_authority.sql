drop table if exists application_user;
drop table if exists application_authority;

create table if not exists application_authority
(
    id             bigint       not null primary key auto_increment,
    application_id varchar(36)  not null,
    authority      varchar(100) not null,
    level          int          not null
);

CREATE TABLE if not exists application_user
(
    id             bigint primary key auto_increment,
    user_id        varchar(36) NOT NULL,
    application_id varchar(36) not null,
    authority_id   bigint      not null,
    created_at     datetime    NOT NULL,
    FOREIGN KEY (application_id) REFERENCES application (id),
    FOREIGN KEY (user_id) REFERENCES user (id),
    FOREIGN KEY (authority_id) REFERENCES application_authority (id)
);

CREATE INDEX idx_application_user ON application_user (application_id, user_id);

insert into application_authority(application_id, authority, level)
select id, "ROLE_USER", 0
from application;

insert into application_authority(application_id, authority, level)
select id, "ROLE_ADMIN", 2147483647
from application;
