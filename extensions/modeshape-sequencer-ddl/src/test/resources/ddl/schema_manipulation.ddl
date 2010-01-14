-- ==================================================================
--      SQL 92 TEST DDL
--
--      Barry LaFond 9/22/2009
-- ==================================================================

-- ==================================================================
--      SCHEMA MANIPULATION STATEMENTS
-- ==================================================================
--<SQL schema manipulation statement> ::=
--      <drop schema statement>
--    | <alter table statement>
--    | <drop table statement>
--    | <drop view statement>
--    | <revoke statement>
--    | <alter domain statement>
--    | <drop domain statement>
--    | <drop character set statement>
--    | <drop collation statement>
--    | <drop translation statement>
--    | <drop assertion statement>
--

-- ==================================================================
--      DROP SCHEMA Statement
-- ==================================================================
--<drop schema statement> ::=
--    DROP SCHEMA <schema name> <drop behavior>
--
--<drop behavior> ::= CASCADE | RESTRICT
--

DROP SCHEMA schema_name_1 CASCADE;

DROP SCHEMA schema_name_2 RESTRICT;


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

-- ALTER TABLE schema_1.table_name_2 ADD (COLUMN column1_name INTEGER NOT NULL, COLUMN column2_name VARCHAR(25))

ALTER TABLE schema_1.table_name_3 ADD schema_1.table_name_3.column_name INTEGER NOT NULL DEFAULT (25);

-- ALTER TABLE table_name_4 ADD (column1_name INTEGER NOT NULL DEFAULT (25), column2_name VARCHAR(25))

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

ALTER TABLE table_name_5 ALTER column_name SET DEFAULT (0);

ALTER TABLE table_name_6 ALTER COLUMN column_name SET DEFAULT (0);

ALTER TABLE table_name_7 ALTER column_name DROP DEFAULT;

--<drop column definition> ::=
--    DROP [ COLUMN ] <column name> <drop behavior>
--
ALTER TABLE table_name_8 DROP COLUMN column_name CASCADE;

ALTER TABLE table_name_9 DROP COLUMN column_name RESTRICT;

ALTER TABLE table_name_10 DROP column_name CASCADE;

ALTER TABLE table_name_11 DROP column_name RESTRICT;

--<add table constraint definition> ::=
--    ADD <table constraint definition>
--

ALTER TABLE table_name_12 ADD CONSTRAINT pk_name PRIMARY KEY (column_name);

ALTER TABLE table_name_13 ADD CONSTRAINT pk_name PRIMARY KEY (column1_name, schema_name_13.table_name_13.column2_name);

ALTER TABLE table_name_14 ADD CONSTRAINT fk_name FOREIGN KEY (ref_col_name) REFERENCES ref_table_name(ref_table_column_name);

--<drop table constraint definition> ::=
--    DROP CONSTRAINT <constraint name> <drop behavior>
--

ALTER TABLE table_name_15 DROP CONSTRAINT fk_name CASCADE;

ALTER TABLE table_name_16 DROP CONSTRAINT fk_name RESTRICT;


-- ==================================================================
--      DROP TABLE Statement
-- ==================================================================
--<drop table statement> ::=
--    DROP TABLE <table name> <drop behavior>
--

DROP TABLE table_name_17 CASCADE;

DROP TABLE table_name_18 RESTRICT;

-- ==================================================================
--      DROP VIEW Statement
-- ==================================================================
--<drop view statement> ::=
--    DROP VIEW <table name> <drop behavior>
--

DROP VIEW view_name_19 CASCADE;

DROP VIEW view_name_20 RESTRICT;

-- ======================= INGNORABLE STATEMENT ======================
--<revoke statement> ::=
--    REVOKE [ GRANT OPTION FOR ]
--        <privileges>
--        ON <object name>
--      FROM <grantee> [ { <comma> <grantee> }... ] <drop behavior>
-- ===================================================================

-- ==================================================================
--      ALTER DOMAIN Statement
-- ==================================================================
--<alter domain statement> ::=
--    ALTER DOMAIN <domain name> <alter domain action>
--
--<alter domain action> ::=
--      <set domain default clause>
--    | <drop domain default clause>
--    | <add domain constraint definition>
--    | <drop domain constraint definition>
--
--<set domain default clause> ::= SET <default clause>
--

ALTER DOMAIN some_domain.name_1 SET DEFAULT NULL;

ALTER DOMAIN some_domain.name_2 SET DEFAULT USER;

--<drop domain default clause> ::= DROP DEFAULT
--

ALTER DOMAIN some_domain.name_3 DROP DEFAULT;

--<add domain constraint definition> ::=
--    ADD <domain constraint>
--

ALTER DOMAIN some_domain.name_4 ADD CONSTRAINT constraint_name;

--<drop domain constraint definition> ::=
--    DROP CONSTRAINT <constraint name>
--

ALTER DOMAIN some_domain.name_5 DROP CONSTRAINT constraint_name;

-- ==================================================================
--      DROP XXXXXX Statements
-- ==================================================================

--<drop domain statement> ::=
--    DROP DOMAIN <domain name> <drop behavior>
--

DROP DOMAIN some_domain.name_6 CASCADE;

DROP DOMAIN some_domain.name_7 RESTRICT;

--<drop character set statement> ::=
--    DROP CHARACTER SET <character set name>
--

DROP CHARACTER SET character_set_name;

--<drop collation statement> ::=
--    DROP COLLATION <collation name>
--

DROP COLLATION collation_name;

--<drop translation statement> ::=
--    DROP TRANSLATION <translation name>
--

DROP TRANSLATION translation_name;

--<drop assertion statement> ::=
--    DROP ASSERTION <constraint name>
--
DROP ASSERTION assertion_name;
