-- ==================================================================
--      SQL 92 TEST DDL
--
--      Barry LaFond 9/22/2009
-- ==================================================================

-- ==================================================================
--      SCHEMA STATEMENT
-- ==================================================================
--<SQL schema statement> ::=
--      <SQL schema definition statement>
--    | <SQL schema manipulation statement>
--
--<SQL schema definition statement> ::=
--      <schema definition>
--    | <table definition>
--    | <view definition>
--    | <grant statement>
--    | <domain definition>
--    | <character set definition>
--    | <collation definition>
--    | <translation definition>
--    | <assertion definition>
--

-- ==================================================================
--      SCHEMA DEFINITION
-- ==================================================================
--<schema definition> ::=
--    CREATE SCHEMA <schema name clause>
--      [ <schema character set specification> ]
--      [ <schema element>... ]
--
--<schema name clause> ::=
--      <schema name>
--    | AUTHORIZATION <schema authorization identifier>
--    | <schema name> AUTHORIZATION
--          <schema authorization identifier>
--
--<schema authorization identifier> ::=
--    <authorization identifier>
CREATE SCHEMA schema_name_1;

CREATE SCHEMA AUTHORIZATION user_identifier;

CREATE SCHEMA schema_name_2 AUTHORIZATION user_identifier;

-- ==================================================================
--      SCHEMA DEFINITION
-- ==================================================================

--
--<schema character set specification> ::=
--    DEFAULT CHARACTER
--        SET <character set specification>
--

CREATE SCHEMA schema_name_3 DEFAULT CHARACTER SET char_set_name;

CREATE SCHEMA schema_name_4 DEFAULT CHARACTER SET schema_name_3.char_set_name;

--<schema element> ::=
--      <domain definition>
--    | <table definition>
--    | <view definition>
--    | <grant statement>
--    | <assertion definition>
--    | <character set definition>
--    | <collation definition>
--    | <translation definition>
--

-- ==================================================================
--      DOMAIN DEFINITION
-- ==================================================================

--<domain definition> ::=
--    CREATE DOMAIN <domain name>
--        [ AS ] <data type>
--      [ <default clause> ]
--      [ <domain constraint>... ]
--      [ <collate clause> ]
--
--<domain constraint> ::=
--    [ <constraint name definition> ]
--    <check constraint definition> [ <constraint attributes> ]
--
CREATE DOMAIN domain_name_1 INTEGER;

CREATE DOMAIN domain_name_2 CHAR DEFAULT null;

CREATE DOMAIN domain_name_3 AS INTEGER DEFAULT (25);

CREATE DOMAIN domain_name_4 AS INTEGER DEFAULT (25) 
    CONSTRAINT constraint_name; 
    
CREATE DOMAIN domain_name_5 AS INTEGER DEFAULT (25) 
    CONSTRAINT constraint_name;
    
CREATE DOMAIN us_postal_code AS TEXT
	CHECK(
	   VALUE ~ '^\\d{5}$'
	OR VALUE ~ '^\\d{5}-\\d{4}$'
	);

-- ==================================================================
--      ASSERTION DEFINITION
-- ==================================================================

--<assertion definition> ::=
--    CREATE ASSERTION <constraint name> <assertion check>
--      [ <constraint attributes> ]
--
--<assertion check> ::=
--    CHECK
--        <left paren> <search condition> <right paren>

CREATE ASSERTION assertNotNull CHECK (value != null) NOT DEFERRABLE

CREATE ASSERTION assertIsZero CHECK (value != null and value == 0) INITIALLY DEFERRED
