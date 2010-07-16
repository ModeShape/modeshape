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

DROP SCHEMA schema_name CASCADE

DROP SCHEMA schema_name RESTRICT


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

ALTER TABLE table_name ADD COLUMN column_name VARCHAR(25) NOT NULL

ALTER TABLE table_name ADD column_name INTEGER NOT NULL DEFAref_table_nameULT (25)

-- Adding multiple columns is not valid SQL-92
--ALTER TABLE table_name ADD (COLUMN column1_name INTEGER NOT NULL, COLUMN column2_name VARCHAR(25))
--ALTER TABLE table_name ADD (column1_name INTEGER NOT NULL DEFAULT (25), column2_name VARCHAR(25))

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

ALTER TABLE table_name ALTER column_name SET DEFAULT (0)

ALTER TABLE table_name ALTER column_name DROP DEFAULT

--<drop column definition> ::=
--    DROP [ COLUMN ] <column name> <drop behavior>
--
ALTER TABLE table_name DROP COLUMN column_name CASCADE

ALTER TABLE table_name DROP COLUMN column_name RESTRICT

ALTER TABLE table_name DROP column_name CASCADE

ALTER TABLE table_name DROP column_name RESTRICT

--<add table constraint definition> ::=
--    ADD <table constraint definition>
--

ALTER TABLE table_name ADD CONSTRAINT pk_name PRIMARY KEY (column_name)

ALTER TABLE table_name ADD CONSTRAINT pk_name PRIMARY KEY (column1_name, column2_name)

ALTER TABLE table_name ADD CONSTRAINT fk_name FOREIGN KEY (ref_col_name) REFERENCES ref_table_name(ref_table_column_name)

--<drop table constraint definition> ::=
--    DROP CONSTRAINT <constraint name> <drop behavior>
--

ALTER TABLE table_name DROP CONSTRAINT fk_name CASCADE

ALTER TABLE table_name DROP CONSTRAINT fk_name RESTRICT


-- ==================================================================
--      DROP TABLE Statement
-- ==================================================================
--<drop table statement> ::=
--    DROP TABLE <table name> <drop behavior>
--

DROP TABLE table_name CASCADE

DROP TABLE table_name RESTRICT

-- ==================================================================
--      DROP VIEW Statement
-- ==================================================================
--<drop view statement> ::=
--    DROP VIEW <table name> <drop behavior>
--

DROP TABLE view_name CASCADE

DROP TABLE view_name RESTRICT

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

ALTER DOMAIN domain.name SET DEFAULT NULL 

ALTER DOMAIN domain.name SET DEFAULT USER 

--<drop domain default clause> ::= DROP DEFAULT
--

ALTER DOMAIN domain.name DROP DEFAULT

--<add domain constraint definition> ::=
--    ADD <domain constraint>
--

ALTER DOMAIN domain.name ADD CONSTRAINT constraint_name

--<drop domain constraint definition> ::=
--    DROP CONSTRAINT <constraint name>
--

ALTER DOMAIN domain.name DROP CONSTRAINT constraint_name

--<drop domain statement> ::=
--    DROP DOMAIN <domain name> <drop behavior>
--

-- ==================================================================
--      DROP XXXXXX Statements
-- ==================================================================
DROP DOMAIN domain.name CASCADE

DROP DOMAIN domain.name RESTRICT

--<drop character set statement> ::=
--    DROP CHARACTER SET <character set name>
--

DROP CHARACTER SET character_set_name

--<drop collation statement> ::=
--    DROP COLLATION <collation name>
--

DROP COLLATION collation_name

--<drop translation statement> ::=
--    DROP TRANSLATION <translation name>
--

DROP TRANSLATION translation_name

--<drop assertion statement> ::=
--    DROP ASSERTION <constraint name>
--
DROP ASSERTION constraint_name
