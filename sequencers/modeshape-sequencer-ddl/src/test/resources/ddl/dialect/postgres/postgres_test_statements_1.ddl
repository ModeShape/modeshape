-- Postgres SQL Statements from postgressql-8.4.1-US.pdf
--
-- Extracted 10/5/2009


-- ABORT [ WORK | TRANSACTION ]

ABORT;

ABORT WORK;

ABORT TRANSACTION;


-- ALTER AGGREGATE name ( type [ , ... ] ) RENAME TO new_name
-- ALTER AGGREGATE name ( type [ , ... ] ) OWNER TO new_owner
-- ALTER AGGREGATE name ( type [ , ... ] ) SET SCHEMA new_schema

ALTER AGGREGATE myavg(integer) RENAME TO my_average;

ALTER AGGREGATE myavg(integer) OWNER TO joe;

ALTER AGGREGATE myavg(integer) SET SCHEMA myschema;


-- ALTER CONVERSION name RENAME TO newname
-- ALTER CONVERSION name OWNER TO newowner

ALTER CONVERSION iso_8859_1_to_utf8 RENAME TO latin1_to_unicode;

ALTER CONVERSION iso_8859_1_to_utf8 OWNER TO joe;

-- ALTER DATABASE name [ [ WITH ] option [ ... ] ]
-- where option can be:
--     CONNECTION LIMIT connlimit (integer - -1 means no limit)
-- ALTER DATABASE name RENAME TO newname
-- ALTER DATABASE name OWNER TO new_owner
-- ALTER DATABASE name SET TABLESPACE new_tablespace
-- ALTER DATABASE name SET configuration_parameter { TO | = } { value | DEFAULT }
-- ALTER DATABASE name SET configuration_parameter FROM CURRENT
-- ALTER DATABASE name RESET configuration_parameter    
-- ALTER DATABASE name RESET ALL

ALTER DATABASE test SET enable_indexscan TO off;

--ALTER DOMAIN name
--    { SET DEFAULT expression | DROP DEFAULT }
--ALTER DOMAIN name
--    { SET | DROP } NOT NULL
--ALTER DOMAIN name
--    ADD domain_constraint
--ALTER DOMAIN name
--    DROP CONSTRAINT constraint_name [ RESTRICT | CASCADE ]
--ALTER DOMAIN name
--    OWNER TO new_owner
--ALTER DOMAIN name
--    SET SCHEMA new_schema

ALTER DOMAIN zipcode SET NOT NULL;

-- 10 STATEMENTS *******************************************************

ALTER DOMAIN zipcode DROP NOT NULL;

ALTER DOMAIN zipcode ADD CONSTRAINT zipchk CHECK (char_length(VALUE) = 5);

ALTER DOMAIN zipcode DROP CONSTRAINT zipchk;

ALTER DOMAIN zipcode SET SCHEMA customers;


--ALTER FOREIGN DATA WRAPPER name
--    [ VALIDATOR valfunction | NO VALIDATOR ]
--    [ OPTIONS ( [ ADD | SET | DROP ] option ['value'] [, ... ]) ]
--ALTER FOREIGN DATA WRAPPER name OWNER TO new_owner

ALTER FOREIGN DATA WRAPPER dbi OPTIONS (ADD foo '1', DROP 'bar');

ALTER FOREIGN DATA WRAPPER dbi VALIDATOR bob.myvalidator;

--ALTER FUNCTION name ( [ [ argmode  ] [ argname ] argtype [, ...] ] )
--    action [ ... ] [ RESTRICT ]
--ALTER FUNCTION name ( [ [ argmode  ] [ argname ] argtype [, ...] ] )
--    RENAME TO new_name
--ALTER FUNCTION name ( [ [ argmode  ] [ argname ] argtype [, ...] ] )
--    OWNER TO new_owner
--ALTER FUNCTION name ( [ [ argmode  ] [ argname ] argtype [, ...] ] )
--    SET SCHEMA new_schema
--where action is one of:
--    CALLED ON NULL INPUT | RETURNS NULL ON NULL INPUT | STRICT
--    IMMUTABLE | STABLE | VOLATILE
--    [ EXTERNAL ] SECURITY INVOKER | [ EXTERNAL ] SECURITY DEFINER
--    COST execution_cost
--    ROWS result_rows
--    SET configuration_parameter { TO | = } { value | DEFAULT }
--    SET configuration_parameter FROM CURRENT
--    RESET configuration_parameter
--    RESET ALL

ALTER FUNCTION sqrt(integer) RENAME TO square_root;

ALTER FUNCTION sqrt(integer) OWNER TO joe;

ALTER FUNCTION sqrt(integer) SET SCHEMA maths;

ALTER FUNCTION check_password(text) SET search_path = admin, pg_temp;

-- 20 STATEMENTS *******************************************************

ALTER FUNCTION check_password(text) RESET search_path;

--ALTER GROUP groupname ADD USER username [, ... ]
--ALTER GROUP groupname DROP USER username [, ... ]
--ALTER GROUP groupname RENAME TO newname

ALTER GROUP staff ADD USER karl, john;

ALTER GROUP workers DROP USER beth;

ALTER GROUP workers RENAME TO beth;

--ALTER INDEX name     RENAME TO new_name
--ALTER INDEX name     SET TABLESPACE tablespace_name
--ALTER INDEX name     SET ( storage_parameter = value [, ... ] )
--ALTER INDEX name     RESET ( storage_parameter [, ... ] )

ALTER INDEX distributors RENAME TO suppliers;

ALTER INDEX distributors SET TABLESPACE fasttablespace;

ALTER INDEX distributors SET (fillfactor = 75);

--ALTER [ PROCEDURAL ] LANGUAGE name RENAME TO newname
--ALTER [ PROCEDURAL ] LANGUAGE name OWNER TO new_owner

ALTER LANGUAGE swahili RENAME TO new_swahili;

ALTER PROCEDURAL LANGUAGE swahili RENAME TO new_swahili;

ALTER PROCEDURAL LANGUAGE swahili OWNER TO president_dd;

-- 30 STATEMENTS *******************************************************

-- ALTER OPERATOR name ( { lefttype | NONE } , { righttype | NONE } ) OWNER TO newowner

ALTER OPERATOR @@ (text, text) OWNER TO joe;

--ALTER OPERATOR CLASS name USING index_method RENAME TO newname
--ALTER OPERATOR CLASS name USING index_method OWNER TO newowner

ALTER OPERATOR CLASS some_name USING index_method RENAME TO newname;
ALTER OPERATOR CLASS some_name USING index_method OWNER TO newowner;

--ALTER OPERATOR FAMILY name USING index_method ADD
--  { OPERATOR strategy_number operator_name ( op_type, op_type )
--   | FUNCTION support_number [ ( op_type [ , op_type ] ) ] funcname ( argument_type [, ...] )
--  } [, ... ]
--ALTER OPERATOR FAMILY name USING index_method DROP
--  { OPERATOR strategy_number ( op_type [ , op_type ] )
--   | FUNCTION support_number ( op_type [ , op_type ] )
--  } [, ... ]
--ALTER OPERATOR FAMILY name USING index_method RENAME TO newname
--ALTER OPERATOR FAMILY name USING index_method OWNER TO newowner

ALTER OPERATOR FAMILY integer_ops USING btree ADD
  -- int4 vs int2
             < (int4, int2) ,
  OPERATOR 1
             <= (int4, int2) ,
  OPERATOR 2
  OPERATOR 3 = (int4, int2) ,
             >= (int4, int2) ,
  OPERATOR 4
             > (int4, int2) ,
  OPERATOR 5
  FUNCTION 1 btint42cmp(int4, int2) ,
  -- int2 vs int4
             < (int2, int4) ,
  OPERATOR 1
             <= (int2, int4) ,
  OPERATOR 2
  OPERATOR 3 = (int2, int4) ,
             >= (int2, int4) ,
  OPERATOR 4
             > (int2, int4) ,
  OPERATOR 5
  FUNCTION 1 btint24cmp(int2, int4) ;

ALTER OPERATOR FAMILY integer_ops USING btree DROP
  -- int4 vs int2
  OPERATOR 1 (int4, int2) ,
  OPERATOR 2 (int4, int2) ,
  OPERATOR 3 (int4, int2) ,
  OPERATOR 4 (int4, int2) ,
  OPERATOR 5 (int4, int2) ,
  FUNCTION 1 (int4, int2) ,
  -- int2 vs int4
  OPERATOR 1 (int2, int4) ,
  OPERATOR 2 (int2, int4) ,
  PERATOR 3 (int2, int4) ,
  OPERATOR 4 (int2, int4) ,
  OPERATOR 5 (int2, int4) ,
  FUNCTION 1 (int2, int4) ;

--ALTER ROLE name [ [ WITH ] option [ ... ] ]
--	where option can be:
--	      SUPERUSER | NOSUPERUSER
--	    | CREATEDB | NOCREATEDB
--	    | CREATEROLE | NOCREATEROLE
--	    | CREATEUSER | NOCREATEUSER
--	    | INHERIT | NOINHERIT
--	    | LOGIN | NOLOGIN
--	    | CONNECTION LIMIT connlimit
--	    | [ ENCRYPTED | UNENCRYPTED ] PASSWORD 'password '
--	    | VALID UNTIL 'timestamp'
--
--ALTER ROLE name RENAME TO newname
--ALTER ROLE name      SET configuration_parameter { TO | = } { value | DEFAULT }
--ALTER ROLE name      SET configuration_parameter FROM CURRENT
--ALTER ROLE name      RESET configuration_parameter
--ALTER ROLE name      RESET ALL

ALTER ROLE davide WITH PASSWORD 'hu8jmn3';

ALTER ROLE davide WITH PASSWORD NULL;

ALTER ROLE chris VALID UNTIL 'May 4 12:00:00 2015 +1';

ALTER ROLE fred VALID UNTIL 'infinity';

ALTER ROLE miriam CREATEROLE CREATEDB;
-- 40 STATEMENTS *******************************************************
ALTER ROLE worker_bee SET maintenance_work_mem = 100000;

--ALTER SCHEMA name RENAME TO newname
--ALTER SCHEMA name OWNER TO newowner

ALTER SCHEMA name RENAME TO newname;
ALTER SCHEMA name OWNER TO newowner;

--ALTER SEQUENCE name [ INCREMENT [ BY ] increment ]
--    [ MINVALUE minvalue | NO MINVALUE ] [ MAXVALUE maxvalue | NO MAXVALUE ]
--    [ START [ WITH ] start ]
--    [ RESTART [ [ WITH ] restart ] ]
--    [ CACHE cache ] [ [ NO ] CYCLE ]
--    [ OWNED BY { table.column | NONE } ]
--ALTER SEQUENCE name OWNER TO new_owner
--ALTER SEQUENCE name RENAME TO new_name
--ALTER SEQUENCE name SET SCHEMA new_schema

ALTER SEQUENCE serial RESTART WITH 105;

--ALTER SERVER servername [ VERSION 'newversion' ]
--    [ OPTIONS ( [ ADD | SET | DROP ] option ['value'] [, ... ] ) ]
--ALTER SERVER servername OWNER TO new_owner

ALTER SERVER foo OPTIONS (host 'foo', dbname 'foodb');

ALTER SERVER foo VERSION '8.4' OPTIONS (SET host 'baz');

--ALTER TABLE [ ONLY ] name [ * ]
--    action [, ... ]
--ALTER TABLE [ ONLY ] name [ * ]
--    RENAME [ COLUMN ] column TO new_column
--ALTER TABLE name
--    RENAME TO new_name
--ALTER TABLE name
--    SET SCHEMA new_schema
--where action is one of:
--    ADD [ COLUMN ] column type [ column_constraint [ ... ] ]
--    DROP [ COLUMN ] column [ RESTRICT | CASCADE ]
--    ALTER [ COLUMN ] column [ SET DATA ] TYPE type [ USING expression ]
--    ALTER [ COLUMN ] column SET DEFAULT expression
--    ALTER [ COLUMN ] column DROP DEFAULT
--    ALTER [ COLUMN ] column { SET | DROP } NOT NULL
--    ALTER [ COLUMN ] column SET STATISTICS integer
--    ALTER [ COLUMN ] column SET STORAGE { PLAIN | EXTERNAL | EXTENDED | MAIN }
--    ADD table_constraint
--    DROP CONSTRAINT constraint_name [ RESTRICT | CASCADE ]
--    DISABLE TRIGGER [ trigger_name | ALL | USER ]
--    ENABLE TRIGGER [ trigger_name | ALL | USER ]
--    ENABLE REPLICA TRIGGER trigger_name
--    ENABLE ALWAYS TRIGGER trigger_name
--    DISABLE RULE rewrite_rule_name
--    ENABLE RULE rewrite_rule_name
--    ENABLE REPLICA RULE rewrite_rule_name
--    ENABLE ALWAYS RULE rewrite_rule_name
--    CLUSTER ON index_name
--    SET WITHOUT CLUSTER
--    SET WITH OIDS
--    SET WITHOUT OIDS
--    SET ( storage_parameter = value [, ... ] )
--    RESET ( storage_parameter [, ... ] )
--    INHERIT parent_table
--    NO INHERIT parent_table
--    OWNER TO new_owner
--    SET TABLESPACE new_tablespace

ALTER TABLE distributors 
    ADD COLUMN nick_name varchar(30),
    ADD COLUMN address varchar(30);

ALTER TABLE distributors DROP COLUMN address RESTRICT;

ALTER TABLE distributors
    ALTER COLUMN address TYPE varchar(80),
    ALTER COLUMN name TYPE varchar(100);

ALTER TABLE foo
    ALTER COLUMN foo_timestamp SET DATA TYPE timestamp with time zone
    USING
        timestamp with time zone 'epoch' + foo_timestamp * interval '1 second';
-- 50 STATEMENTS *******************************************************
ALTER TABLE foo
    ALTER COLUMN foo_timestamp DROP DEFAULT,
    ALTER COLUMN foo_timestamp TYPE timestamp with time zone
    USING
        timestamp with time zone 'epoch' + foo_timestamp *GO interval '1 second',
    ALTER COLUMN foo_timestamp SET DEFAULT now();

ALTER TABLE distributors RENAME COLUMN address TO city;

ALTER TABLE distributors RENAME TO suppliers;

ALTER TABLE distributors ALTER COLUMN street SET NOT NULL;

ALTER TABLE distributors ALTER COLUMN street DROP NOT NULL;

ALTER TABLE distributors ADD CONSTRAINT zipchk CHECK (char_length(zipcode) = 5);

ALTER TABLE distributors DROP CONSTRAINT zipchk;

ALTER TABLE ONLY distributors DROP CONSTRAINT zipchk;

ALTER TABLE distributors ADD CONSTRAINT dist_id_zipcode_key UNIQUE (dist_id, zipcode);

ALTER TABLE distributors ADD PRIMARY KEY (dist_id);
-- 60 STATEMENTS *******************************************************
ALTER TABLE distributors SET TABLESPACE fasttablespace;

ALTER TABLE myschema.distributors SET SCHEMA yourschema;

--ALTER TABLESPACE name RENAME TO newname
--ALTER TABLESPACE name OWNER TO newowner

ALTER TABLESPACE index_space RENAME TO fast_raid;

ALTER TABLESPACE index_space OWNER TO mary;

--ALTER TEXT SEARCH CONFIGURATION name
--    ADD MAPPING FOR token_type [, ... ] WITH dictionary_name [, ... ]
--ALTER TEXT SEARCH CONFIGURATION name
--    ALTER MAPPING FOR token_type [, ... ] WITH dictionary_name [, ... ]
--ALTER TEXT SEARCH CONFIGURATION name
--    ALTER MAPPING REPLACE old_dictionary WITH new_dictionary
--ALTER TEXT SEARCH CONFIGURATION name
--    ALTER MAPPING FOR token_type [, ... ] REPLACE old_dictionary WITH new_dictionary
--ALTER TEXT SEARCH CONFIGURATION name
--    DROP MAPPING [ IF EXISTS ] FOR token_type [, ... ]
--ALTER TEXT SEARCH CONFIGURATION name RENAME TO newname
--ALTER TEXT SEARCH CONFIGURATION name OWNER TO newowner

ALTER TEXT SEARCH CONFIGURATION my_config
  ALTER MAPPING REPLACE english WITH swedish;

ALTER TEXT SEARCH DICTIONARY  name (
    option [ = value ] [, ... ]
);

--ALTER TEXT SEARCH DICTIONARY  name RENAME TO newname
--ALTER TEXT SEARCH DICTIONARY  name OWNER TO newowner

ALTER TEXT SEARCH DICTIONARY my_dict ( StopWords = newrussian );

ALTER TEXT SEARCH DICTIONARY my_dict ( language = dutch, StopWords );

ALTER TEXT SEARCH DICTIONARY my_dict ( dummy );

--ALTER TEXT SEARCH PARSER name RENAME TO newname

ALTER TEXT SEARCH PARSER name RENAME TO newname;
-- 70 STATEMENTS *******************************************************
--ALTER TEXT SEARCH TEMPLATE name RENAME TO newname

ALTER TEXT SEARCH TEMPLATE name RENAME TO newname;

--ALTER TRIGGER name ON table RENAME TO newname

ALTER TRIGGER emp_stamp ON emp RENAME TO emp_track_chgs;

--ALTER TYPE name RENAME TO new_name
--ALTER TYPE name OWNER TO new_owner
--ALTER TYPE name SET SCHEMA new_schema

ALTER TYPE electronic_mail RENAME TO email;

ALTER TYPE email OWNER TO joe;

ALTER TYPE email SET SCHEMA customers;

--ALTER USER name [ [ WITH ] option [ ... ] ]
--where option can be:
--      SUPERUSER | NOSUPERUSER
--    | CREATEDB | NOCREATEDB
--    | CREATEROLE | NOCREATEROLE
--    | CREATEUSER | NOCREATEUSER
--    | INHERIT | NOINHERIT
--    | LOGIN | NOLOGIN
--    | CONNECTION LIMIT connlimit
--    | [ ENCRYPTED | UNENCRYPTED ] PASSWORD 'password '
--    | VALID UNTIL 'timestamp'
--ALTER USER name RENAME TO newname
--ALTER USER name      SET configuration_parameter { TO | = } { value | DEFAULT }
--ALTER USER name      SET configuration_parameter FROM CURRENT
--ALTER USER name      RESET configuration_parameter
--ALTER USER name      RESET ALL

--ALTER USER MAPPING FOR { username | USER | CURRENT_USER | PUBLIC }
--    SERVER servername
--    OPTIONS ( [ ADD | SET | DROP ] option ['value'] [, ... ] )

ALTER USER MAPPING FOR bob SERVER foo OPTIONS (user 'bob', password 'public');

--ALTER VIEW name     ALTER [ COLUMN ] column SET DEFAULT expression
--ALTER VIEW name     ALTER [ COLUMN ] column DROP DEFAULT
--ALTER VIEW name     OWNER TO new_owner
--ALTER VIEW name     RENAME TO new_name
--ALTER VIEW name     SET SCHEMA new_schema

ALTER VIEW foo RENAME TO bar;

ANALYZE [ VERBOSE ] [ table [ ( column [, ...] ) ] ];

--CLUSTER [VERBOSE] tablename [ USING indexname ]
--CLUSTER [VERBOSE]

CLUSTER employees USING employees_ind;

CLUSTER employees;
-- 80 STATEMENTS *******************************************************
CLUSTER;

CLUSTER indexname ON tablename
-- 82 Statements *******************************************************
