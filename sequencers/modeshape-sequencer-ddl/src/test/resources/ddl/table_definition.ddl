-- ==================================================================
--      SQL 92 TEST DDL
--
--      Barry LaFond 9/22/2009
-- ==================================================================

-- ==================================================================
--      TABLE DEFINITION
-- ==================================================================
--<table definition> ::=
--    CREATE [ { GLOBAL | LOCAL } TEMPORARY ] TABLE
--        <table name>
--      <table element list>
--      [ ON COMMIT { DELETE | PRESERVE } ROWS ]
--

CREATE TABLE table_name_1 (column_name_1 VARCHAR(255));

CREATE GLOBAL TEMPORARY TABLE table_name_2 (column_name_1 VARCHAR(255));

CREATE LOCAL TEMPORARY TABLE table_name_3 (column_name_1 VARCHAR(255));

CREATE TABLE table_name_4 (column_name_1 VARCHAR(255), column_name_2 VARCHAR(255));

CREATE TABLE table_name_5 (
    column_name_1 VARCHAR(255), 
    column_name_2 VARCHAR(255)
    CONSTRAINT pk_1
);

--<table element list> ::=
--      <left paren> <table element> [ { <comma> <table element> }... ] <right paren>
--
--      <table element> ::=
--      <column definition>
--    | <table constraint definition>
--
--<column definition> ::=
--    <column name> { <data type> | <domain name> }
--    [ <default clause> ]
--    [ <column constraint definition>... ]
--    [ <collate clause> ]
--    
--<column constraint definition> ::=
--    [ <constraint name definition> ]
--    <column constraint>
--      [ <constraint attributes> ]
--

-- ==================================================================
--      COLUMN CONSTRAINT DEFINITION
-- ==================================================================

CREATE TABLE table_name_6 (
    column_name_1 VARCHAR(255) NOT NULL DEFAULT NULL
);
    
CREATE TABLE table_name_7 (
    column_name_1 VARCHAR(255) CONSTRAINT pk_name PRIMARY KEY
);
    
CREATE TABLE table_name_8 (
    column_name_1 VARCHAR(255) CONSTRAINT pk_name UNIQUE
);
    
CREATE TABLE table_name_9 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1, ref_column_name_2)
);
    
CREATE TABLE table_name_10 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        MATCH FULL
);    
    
CREATE TABLE table_name_11 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        MATCH PARTIAL
);

CREATE TABLE table_name_12 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON UPDATE CASCADE
);
    
CREATE TABLE table_name_13 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON UPDATE SET NULL
);
    
CREATE TABLE table_name_14 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON UPDATE SET DEFAULT
);
    
CREATE TABLE table_name_15 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON UPDATE NO ACTION
);

CREATE TABLE table_name_16 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON DELETE CASCADE
);
    
CREATE TABLE table_name_17 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON DELETE SET NULL
)
    
CREATE TABLE table_name_18 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON DELETE SET DEFAULT
);
    
CREATE TABLE table_name_19 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON DELETE NO ACTION
);

CREATE TABLE table_name_20 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON UPDATE CASCADE ON DELETE NO ACTION
);
    
CREATE TABLE table_name_21 (
    column_name_1 VARCHAR(255) 
        REFERENCES ref_table_name (ref_column_name_1) 
        ON DELETE CASCADE ON UPDATE SET NULL
)

CREATE TABLE table_name_22 (
    column_name_1 VARCHAR(255) CHECK (XXXXXX)
);

CREATE TABLE table_name_22_A (
    column_name_1 VARCHAR(255) 
        CONSTRAINT pk_name UNIQUE INITIALLY DEFERRED
);

CREATE TABLE table_name_22_B (
    column_name_1 VARCHAR(255) 
        CONSTRAINT pk_name UNIQUE INITIALLY IMMEDIATE
);

CREATE TABLE table_name_22_C (
    column_name_1 VARCHAR(255) 
        CONSTRAINT pk_name UNIQUE INITIALLY DEFERRED DEFERRABLE
)

CREATE TABLE table_name_22_D (
    column_name_1 VARCHAR(255) 
        CONSTRAINT pk_name UNIQUE INITIALLY IMMEDIATE NOT DEFERRABLE
);

CREATE TABLE table_name_22_E (
    column_name_1 VARCHAR(255) 
        CONSTRAINT pk_name UNIQUE DEFERRABLE INITIALLY IMMEDIATE
);


--<constraint name definition> ::= CONSTRAINT <constraint name>
--
--<constraint name> ::= <qualified name>
--
--<column constraint> ::=
--      NOT NULL
--    | <unique specification>
--    | <references specification>
--    | <check constraint definition>
--
--<unique specification> ::=
--    UNIQUE | PRIMARY KEY
--
--<references specification> ::=
--    REFERENCES <referenced table and columns>
--      [ MATCH <match type> ]
--      [ <referential triggered action> ]
--
--<referenced table and columns> ::=
--     <table name> [ <left paren> <reference column list> <right paren> ]
--
--<table name> ::=
--      <qualified name>
--    | <qualified local table name>
--
--<reference column list> ::= <column name list>
--
--<column name list> ::=
--    <column name> [ { <comma> <column name> }... ]
--
--<match type> ::=
--      FULL
--    | PARTIAL
--
--<referential triggered action> ::=
--      <update rule> [ <delete rule> ]
--    | <delete rule> [ <update rule> ]
--
--<update rule> ::= ON UPDATE <referential action>
--
--<referential action> ::=
--      CASCADE
--    | SET NULL
--    | SET DEFAULT
--    | NO ACTION
--
--<delete rule> ::= ON DELETE <referential action>
--
--<check constraint definition> ::=
--    CHECK
--        <left paren> <search condition> <right paren>

-- ==================================================================
--      TABLE CONSTRAINT DEFINITION
-- ==================================================================

--<table constraint definition> ::=
--    [ <constraint name definition> ]
--    <table constraint> [ <constraint attributes> ]
--
--<table constraint> ::=
--      <unique constraint definition>
--    | <referential constraint definition>
--    | <check constraint definition>
--
--<unique constraint definition> ::=
--            <unique specification> even in SQL3)
--    <unique specification>
--      <left paren> <unique column list> <right paren>
--
--<unique column list> ::= <column name list>
--
--<referential constraint definition> ::=
--    FOREIGN KEY
--        <left paren> <referencing columns> <right paren>
--      <references specification>
--
--<referencing columns> ::=
--    <reference column list>
--
--<constraint attributes> ::=
--      <constraint check time> [ [ NOT ] DEFERRABLE ]
--    | [ NOT ] DEFERRABLE [ <constraint check time> ]
--
--<constraint check time> ::=
--      INITIALLY DEFERRED
--    | INITIALLY IMMEDIATE
    
CREATE TABLE table_name23 (
    column_name_1 VARCHAR(255),
    CONSTRAINT pk_name PRIMARY KEY (ref_column_name_1)
);

CREATE TABLE table_name24 (
    column_name_1 VARCHAR(255),
    PRIMARY KEY (ref_column_name_1)
);

CREATE TABLE table_name25 (
    column_name_1 VARCHAR(255),
    CONSTRAINT unique_constraint_name UNIQUE (ref_column_name_1)
);

CREATE TABLE table_name26 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1)
);

CREATE TABLE table_name27 (
    column_name_1 VARCHAR(255),
    CONSTRAINT fk_name FOREIGN KEY (ref_column_name_1, ref_column_name_2)
        REFERENCES ref_table_name (ref_column_name_1) 
);

CREATE TABLE table_name28 (
    column_name_1 VARCHAR(255),
    CONSTRAINT fk_name FOREIGN KEY (ref_column_name_1, ref_column_name_2)
        REFERENCES ref_table_name (ref_column_name_1) 
);

CREATE TABLE table_name29 (
    column_name_1 VARCHAR(255),
    CONSTRAINT fk_name FOREIGN KEY (ref_column_name_1, ref_column_name_2)
        REFERENCES ref_table_name (ref_column_name_1)
        ON DELETE CASCADE ON UPDATE SET NULL
        MATCH FULL
);

CREATE TABLE table_name30 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) INITIALLY DEFERRED
);

CREATE TABLE table_name31 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) INITIALLY IMMEDIATE
);

CREATE TABLE table_name32 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) INITIALLY IMMEDIATE DEFERRABLE
);

CREATE TABLE table_name33 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) INITIALLY IMMEDIATE NOT DEFERRABLE
);

CREATE TABLE table_name34 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) NOT DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE table_name35 (
    column_name_1 VARCHAR(255),
    UNIQUE (ref_column_name_1) DEFERRABLE INITIALLY DEFERRED
);
