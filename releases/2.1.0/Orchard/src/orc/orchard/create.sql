-- Create the "orchard" user
-- NB: you can connect on the command line using:
--     psql -h localhost orchard orchard
CREATE ROLE orchard ENCRYPTED PASSWORD 'md51bfd88fd04c8d388ca73ecfc24044423' NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT LOGIN;
-- Create the "orchard" database
CREATE DATABASE orchard WITH ENCODING 'UTF8' OWNER orchard;
