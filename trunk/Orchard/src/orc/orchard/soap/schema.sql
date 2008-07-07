-- Create the "orchard" user
-- NB: you can connect on the command line using:
--     psql -h localhost orchard orchard
CREATE ROLE orchard ENCRYPTED PASSWORD 'md51bfd88fd04c8d388ca73ecfc24044423' NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT LOGIN;
-- Create the "orchard" database
CREATE DATABASE orchard OWNER orchard WITH ENCODING 'UTF8';

\connect orchard

CREATE TABLE account_type (
	account_type_id SERIAL,
	account_type_name varchar(255) NOT NULL,
	quota_running int,
	quota_all int,
	age_all interval,
	PRIMARY KEY (account_type_id)
);

INSERT INTO account_type (account_type_name, age_all)
	VALUES ('guest', '1 day');
INSERT INTO account_type (account_type_name, quota_all)
	VALUES ('developer', 10);

CREATE TABLE account (
	account_id SERIAL,
	account_type_id int NOT NULL REFERENCES account_type,
	username varchar(255) NOT NULL,
	salt char(16) NOT NULL,
	password_md5 char(32) NOT NULL,
	email varchar(255) NOT NULL,
	developer_key uuid,
	UNIQUE (username),
	PRIMARY KEY (account_id)
);

INSERT INTO account (account_type_id, username, salt, password_md5, email, developer_key)
	VALUES (2, '', '', '', '', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');