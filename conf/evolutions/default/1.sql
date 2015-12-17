# --- First database schema

# --- !Ups

set ignorecase true;

CREATE TABLE procedure (
  id                        BIGINT NOT NULL AUTO_INCREMENT,
  name                      VARCHAR(255) NOT NULL,
  address                   VARCHAR(1000) NOT NULL,
  s3url               VARCHAR(255) NOT NULL,
  CONSTRAINT pk_procedure PRIMARY KEY (id))
;

# --- !Downs

drop table if exists procedure;