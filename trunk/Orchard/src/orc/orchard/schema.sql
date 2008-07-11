CREATE TABLE account_type (
	account_type_id SERIAL,
	account_type_name varchar(255) NOT NULL,
	quota int,
	lifespan interval,
	event_buffer_size int,
	PRIMARY KEY (account_type_id)
);

INSERT INTO account_type (account_type_name)
	VALUES ('UT Developer');
INSERT INTO account_type (account_type_name, quota)
	VALUES ('Public Developer', 10);

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
	VALUES (1, 'quark', '', '', 'quark@cs.utexas.edu', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');
INSERT INTO account (account_type_id, username, salt, password_md5, email, developer_key)
	VALUES (2, 'test', '', '', '', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12');
