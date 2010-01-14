-- ==================================================================
--      ALTER TABLE Statement
-- ==================================================================
--<alter table statement> ::=
--    ALTER TABLE <table name> <alter table action>
--
--<alter table action> ::=
--      <add column definition>
--    | <alter column definition>
--    | <drop column definition>
--    | <add table constraint definition>
--    | <drop table constraint definition>
--
--<add column definition> ::=
--    ADD [ COLUMN ] <column definition>
--
--<column definition> ::=
--    <column name> { <data type> | <domain name> }
--    [ <default clause> ]
--    [ <column constraint definition>... ]
--    [ <collate clause> ]

ALTER TABLE table_name_1 ADD COLUMN column_name VARCHAR(25) NOT NULL;

ALTER TABLE schema_2.table_name_2 ADD schema_2.table_name_2.column_name INTEGER NOT NULL DEFAULT (25);

--<alter column definition> ::=
--    ALTER [ COLUMN ] <column name> <alter column action>
--
--<alter column action> ::=
--      <set column default clause>
--    | <drop column default clause>
--
--<set column default clause> ::=
--    SET <default clause>
--
--<drop column default clause> ::=
--    DROP DEFAULT
--

ALTER TABLE table_name_3 ALTER column_name SET DEFAULT (0);

ALTER TABLE table_name_4 ALTER COLUMN column_name SET DEFAULT (0);

ALTER TABLE table_name_5 ALTER column_name DROP DEFAULT;

--<drop column definition> ::=
--    DROP [ COLUMN ] <column name> <drop behavior>
--
ALTER TABLE table_name_6 DROP COLUMN column_name CASCADE;

ALTER TABLE table_name_7 DROP COLUMN column_name RESTRICT;

ALTER TABLE table_name_8 DROP column_name CASCADE;

ALTER TABLE table_name_9 DROP column_name RESTRICT;

--<add table constraint definition> ::=
--    ADD <table constraint definition>
--

ALTER TABLE table_name_10 ADD CONSTRAINT pk_name PRIMARY KEY (column_name);

ALTER TABLE table_name_11 ADD CONSTRAINT pk_name PRIMARY KEY (column1_name, schema_name_11.table_name_11.column2_name);

ALTER TABLE table_name_12 ADD CONSTRAINT fk_name FOREIGN KEY (ref_col_name) REFERENCES ref_table_name(ref_table_column_name);

--<drop table constraint definition> ::=
--    DROP CONSTRAINT <constraint name> <drop behavior>
--

ALTER TABLE table_name_13 DROP CONSTRAINT fk_name CASCADE;

ALTER TABLE table_name_14 DROP CONSTRAINT fk_name RESTRICT;
