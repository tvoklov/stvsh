create table users (
	id varchar(36) primary key,
	username varchar
);
create table folder (
	id varchar(36) primary key,
	name varchar,
	owner_id varchar(36),
	schema varchar
);
create table folder_access(
	folder_id varchar(36),
	user_id varchar(36),
	access_type varchar(100)
);
create table sheet(
	id varchar(36) primary key,
	folder_id varchar(36),
	value varchar
)