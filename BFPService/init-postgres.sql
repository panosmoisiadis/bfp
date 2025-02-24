-- bfp.file_table definition

-- Drop table

-- DROP TABLE bfp.file_table;
-- DROP SCHEMA bfp;

CREATE SCHEMA IF NOT EXISTS bfp AUTHORIZATION postgres;

CREATE TABLE IF NOT EXISTS bfp.file_table (
	id uuid NOT NULL,
	owner_id text NOT NULL,
	file_name text NOT NULL,
	file_location text NOT NULL,
	file_size int8 NOT NULL,
	content_type varchar NULL,
	created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
	updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT "FileTable_pkey" PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS file_table_owner_id_idx ON bfp.file_table USING btree (owner_id);