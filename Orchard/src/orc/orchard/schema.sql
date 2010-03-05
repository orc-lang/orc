CREATE TABLE account_type (
	account_type_id SERIAL,
	account_type_name varchar(255) NOT NULL,
	quota int,
	lifespan int,
	can_send_mail boolean NOT NULL DEFAULT FALSE,
	can_import_java boolean NOT NULL DEFAULT FALSE,
	PRIMARY KEY (account_type_id)
);

INSERT INTO account_type (account_type_name, can_send_mail, can_import_java)
	VALUES ('UT Developer', TRUE, TRUE);
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
