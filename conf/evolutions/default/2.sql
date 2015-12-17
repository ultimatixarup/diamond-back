# --- Sample dataset

# --- !Ups

insert into procedure (id,name,address,s3url) values (1,'SN001', 'Mountain View Hospital', 'fjdksfkjdskfl');
insert into procedure (id,name,address,s3url) values (2,'SN007', 'Palo Alto Hospital','ikfjlsfkjalkdjf');
insert into procedure (id,name,address,s3url) values (3,'SN384328', 'Brea Hospital','kdfikjdfkdsjfl');

# --- !Downs

delete from procedure;