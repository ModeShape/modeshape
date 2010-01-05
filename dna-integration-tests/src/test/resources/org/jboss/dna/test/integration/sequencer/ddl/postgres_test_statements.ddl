-- Postgres SQL Statements from postgressql-8.4.1-US.pdf
--
-- Extracted 10/5/2009

--COMMENT ON
--{
--  TABLE object_name |
--  COLUMN table_name.column_name |
--  AGGREGATE agg_name (agg_type [, ...] ) |
--  CAST (sourcetype AS targettype) |
--  CONSTRAINT constraint_name ON table_name |
--  CONVERSION object_name |
--  DATABASE object_name |
--  DOMAIN object_name |
--  FUNCTION func_name ( [ [ argmode ] [ argname ] argtype [, ...] ] ) |
--  INDEX object_name |
--  LARGE OBJECT large_object_oid |
--  OPERATOR op (leftoperand_type, rightoperand_type) |
--  OPERATOR CLASS object_name USING index_method |
--  OPERATOR FAMILY object_name USING index_method |
--  [ PROCEDURAL ] LANGUAGE object_name |
--  ROLE object_name |
--  RULE rule_name ON table_name |
--  SCHEMA object_name |
--  SEQUENCE object_name |
--  TABLESPACE object_name |
--  TEXT SEARCH CONFIGURATION object_name |
--  TEXT SEARCH DICTIONARY object_name |
--  TEXT SEARCH PARSER object_name |
--  TEXT SEARCH TEMPLATE object_name |
--  TRIGGER trigger_name ON table_name |
--  TYPE object_name |
--  VIEW object_name
--} IS 'text'

COMMENT ON TABLE mytable IS 'This is my table.';
COMMENT ON TABLE mytable IS NULL;
COMMENT ON AGGREGATE my_aggregate (double precision) IS 'Computes sample variance';
COMMENT ON CAST (text AS int4) IS 'Allow casts from text to int4';
COMMENT ON COLUMN my_table.my_column IS 'Employee ID number';
COMMENT ON CONVERSION my_conv IS 'Conversion to UTF8';
COMMENT ON DATABASE my_database IS 'Development Database';
COMMENT ON DOMAIN my_domain IS 'Email Address Domain';
COMMENT ON FUNCTION my_function (timestamp) IS 'Returns Roman Numeral';
COMMENT ON INDEX my_index IS 'Enforces uniqueness on employee ID';
-- 10 STATEMENTS *******************************************************
COMMENT ON LANGUAGE plpython IS 'Python support for stored procedures';
COMMENT ON LARGE OBJECT 346344 IS 'Planning document';
COMMENT ON OPERATOR ^ (text, text) IS 'Performs intersection of two texts';
COMMENT ON OPERATOR - (NONE, text) IS 'This is a prefix operator on text';
COMMENT ON OPERATOR CLASS int4ops USING btree IS '4 byte integer operators for btrees';
COMMENT ON OPERATOR FAMILY integer_ops USING btree IS 'all integer operators for btrees';
COMMENT ON ROLE my_role IS 'Administration group for finance tables';
COMMENT ON RULE my_rule ON my_table IS 'Logs updates of employee records';
COMMENT ON SCHEMA my_schema IS 'Departmental data';
COMMENT ON SEQUENCE my_sequence IS 'Used to generate primary keys';
-- 20 STATEMENTS *******************************************************
COMMENT ON TABLE my_schema.my_table IS 'Employee Information';
COMMENT ON TABLESPACE my_tablespace IS 'Tablespace for indexes';
COMMENT ON TEXT SEARCH CONFIGURATION my_config IS 'Special word filtering';
COMMENT ON TEXT SEARCH DICTIONARY swedish IS 'Snowball stemmer for swedish language';
COMMENT ON TEXT SEARCH PARSER my_parser IS 'Splits text into words';
COMMENT ON TEXT SEARCH TEMPLATE snowball IS 'Snowball stemmer';
COMMENT ON TRIGGER my_trigger ON my_table IS 'Used for RI';
COMMENT ON TYPE complex IS 'Complex number data type';
COMMENT ON VIEW my_view IS 'View of departmental costs';
--COMMIT [ WORK | TRANSACTION ]

COMMIT WORK;
-- 30 STATEMENTS *******************************************************
COMMIT TRANSACTION;

COMMIT;

--COMMIT PREPARED transaction_id;

COMMIT PREPARED 'foobar';

--COPY tablename [ ( column [, ...] ) ]
--    FROM { 'filename' | STDIN }
--    [ [ WITH ]
--           [ BINARY ]
--           [ OIDS ]
--           [ DELIMITER [ AS ] 'delimiter ' ]
--           [ NULL [ AS ] 'null string ' ]
--           [ CSV [ HEADER ]
--                 [ QUOTE [ AS ] 'quote' ]
--                 [ ESCAPE [ AS ] 'escape' ]
--                 [ FORCE NOT NULL column [, ...] ]
--COPY { tablename [ ( column [, ...] ) ] | ( query ) }
--    TO { 'filename' | STDOUT }
--    [ [ WITH ]
--           [ BINARY ]
--           [ OIDS ]
--           [ DELIMITER [ AS ] 'delimiter ' ]
--           [ NULL [ AS ] 'null string ' ]
--           [ CSV [ HEADER ]
--                 [ QUOTE [ AS ] 'quote' ]
--                 [ ESCAPE [ AS ] 'escape' ]
--                 [ FORCE QUOTE column [, ...] ]

COPY country TO STDOUT WITH DELIMITER '|';

COPY country FROM '/usr1/proj/bray/sql/country_data';

COPY (SELECT * FROM country WHERE country_name LIKE 'A%') TO '/usr1/proj/bray/sql/a_list_co';

--CREATE AGGREGATE name ( input_data_type [ , ... ] ) (
--    SFUNC = sfunc,
--    STYPE = state_data_type
--    [ , FINALFUNC = ffunc ]
--    [ , INITCOND = initial_condition ]
--    [ , SORTOP = sort_operator ]
--)
--or the old syntax
--CREATE AGGREGATE name (
--    BASETYPE = base_type,
--    SFUNC = sfunc,
--    STYPE = state_data_type
--    [ , FINALFUNC = ffunc ]
--    [ , INITCOND = initial_condition ]
--    [ , SORTOP = sort_operator ]
--)


--CREATE CAST (sourcetype AS targettype)
--    WITH FUNCTION funcname (argtypes)
--    [ AS ASSIGNMENT | AS IMPLICIT ]
--CREATE CAST (sourcetype AS targettype)
--    WITHOUT FUNCTION
--    [ AS ASSIGNMENT | AS IMPLICIT ]
--CREATE CAST (sourcetype AS targettype)
--    WITH INOUT
--    [ AS ASSIGNMENT | AS IMPLICIT ]

CREATE CAST (bigint AS int4) WITH FUNCTION int4(bigint) AS ASSIGNMENT;

--CREATE CONSTRAINT TRIGGER name
--    AFTER event [ OR ... ]
--    ON table_name
--    [ FROM referenced_table_name ]
--    { NOT DEFERRABLE | [ DEFERRABLE ] { INITIALLY IMMEDIATE | INITIALLY DEFERRED } }
--    FOR EACH ROW
--    EXECUTE PROCEDURE funcname ( arguments )

--CREATE [ DEFAULT ] CONVERSION name
--    FOR source_encoding TO dest_encoding FROM funcname

CREATE CONVERSION myconv FOR 'UTF8' TO 'LATIN1' FROM myfunc;

--CREATE DATABASE name
--    [ [ WITH ] [ OWNER [=] dbowner ]
--           [ TEMPLATE [=] template ]
--           [ ENCODING [=] encoding ]
--           [ LC_COLLATE [=] lc_collate ]
--           [ LC_CTYPE [=] lc_ctype ]
--           [ TABLESPACE [=] tablespace ]
--           [ CONNECTION LIMIT [=] connlimit ] ]

CREATE DATABASE lusiadas;

CREATE DATABASE sales OWNER salesapp TABLESPACE salesspace;
-- 40 STATEMENTS *******************************************************
CREATE DATABASE music ENCODING 'LATIN1' TEMPLATE template0;


--CREATE DOMAIN name [ AS ] data_type
--    [ DEFAULT expression ]
--    [ constraint [ ... ] ]
--where constraint is:
--[ CONSTRAINT constraint_name ]
--{ NOT NULL | NULL | CHECK (expression) }

CREATE DOMAIN us_postal_code AS TEXT
	CHECK(
	   VALUE ~ '^\\d{5}$'
	OR VALUE ~ '^\\d{5}-\\d{4}$'
	);

--CREATE FOREIGN DATA WRAPPER name
--    [ VALIDATOR valfunction | NO VALIDATOR ]
--    [ OPTIONS ( option 'value' [, ... ] ) ]

CREATE FOREIGN DATA WRAPPER dummy;

CREATE FOREIGN DATA WRAPPER postgresql VALIDATOR postgresql_fdw_validator;

CREATE FOREIGN DATA WRAPPER mywrapper
    OPTIONS (debug 'true');

--CREATE [ OR REPLACE ] FUNCTION
--    name ( [ [ argmode ] [ argname ] argtype [ { DEFAULT | = } defexpr ] [, ...] ] )
--    [ RETURNS rettype
--      | RETURNS TABLE ( colname coltype [, ...] ) ]
--  { LANGUAGE langname
--    | WINDOW
--    | IMMUTABLE | STABLE | VOLATILE
--    | CALLED ON NULL INPUT | RETURNS NULL ON NULL INPUT | STRICT
--    | [ EXTERNAL ] SECURITY INVOKER | [ EXTERNAL ] SECURITY DEFINER
--    | COST execution_cost
--    | ROWS result_rows
--    | SET configuration_parameter { TO value | = value | FROM CURRENT }
--    | AS 'definition'
--    | AS 'obj_file', 'link_symbol'
--  } ...
--    [ WITH ( attribute [, ...] ) ]

CREATE FUNCTION add(integer, integer) RETURNS integer
    AS 'select $1 + $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION increment(i integer) RETURNS integer AS $$
        BEGIN
                RETURN i + 1;
        END;

CREATE FUNCTION dup(in int, out f1 int, out f2 text)
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;

CREATE FUNCTION dup(int) RETURNS dup_result
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;

CREATE FUNCTION dup(int) RETURNS TABLE(f1 int, f2 text)
    AS $$ SELECT $1, CAST($1 AS text) || ' is text' $$
    LANGUAGE SQL;
-- 50 STATEMENTS *******************************************************
--CREATE GROUP name [ [ WITH ] option [ ... ] ]
--where option can be:
--      SUPERUSER | NOSUPERUSER
--    | CREATEDB | NOCREATEDB
--    | CREATEROLE | NOCREATEROLE
--    | CREATEUSER | NOCREATEUSER
--    | INHERIT | NOINHERIT
--    | LOGIN | NOLOGIN
--    | [ ENCRYPTED | UNENCRYPTED ] PASSWORD 'password '
--    | VALID UNTIL 'timestamp'
--    | IN ROLE rolename [, ...]
--    | IN GROUP rolename [, ...]
--    | ROLE rolename [, ...]
--    | ADMIN rolename [, ...]
--    | USER rolename [, ...]
--    | SYSID uid

--CREATE [ UNIQUE ] INDEX [ CONCURRENTLY ] name ON table [ USING method ]
--    ( { column | ( expression ) } [ opclass ] [ ASC | DESC ] [ NULLS { FIRST | LAST } ] [, ..
--    [ WITH ( storage_parameter = value [, ... ] ) ]
--    [ TABLESPACE tablespace ]
--    [ WHERE predicate ]

CREATE UNIQUE INDEX title_idx ON films (title);

CREATE INDEX lower_title_idx ON films ((lower(title)));

CREATE INDEX title_idx_nulls_low ON films (title NULLS FIRST);

CREATE UNIQUE INDEX title_idx ON films (title) WITH (fillfactor = 70);

CREATE INDEX gin_idx ON documents_table (locations) WITH (fastupdate = off);

CREATE INDEX code_idx ON films(code) TABLESPACE indexspace;

CREATE INDEX CONCURRENTLY sales_quantity_index ON sales_table (quantity);

--CREATE [ PROCEDURAL ] LANGUAGE name
--CREATE [ TRUSTED ] [ PROCEDURAL ] LANGUAGE name
--    HANDLER call_handler [ VALIDATOR valfunction ]

CREATE LANGUAGE plpgsql;

CREATE PROCEDURAL LANGUAGE plpgsql;

CREATE TRUSTED PROCEDURAL LANGUAGE plpgsql;
-- 60 STATEMENTS *******************************************************
CREATE LANGUAGE plsample
    HANDLER plsample_call_handler;

--CREATE OPERATOR name (
--    PROCEDURE = funcname
--    [, LEFTARG = lefttype ] [, RIGHTARG = righttype ]
--    [, COMMUTATOR = com_op ] [, NEGATOR = neg_op ]
--    [, RESTRICT = res_proc ] [, JOIN = join_proc ]
--    [, HASHES ] [, MERGES ]
--)

CREATE OPERATOR === (
    LEFTARG = box,
    RIGHTARG = box,
    PROCEDURE = area_equal_procedure,
    COMMUTATOR = ===,
    NEGATOR = !==,
    RESTRICT = area_restriction_procedure,
    JOIN = area_join_procedure,
    HASHES, MERGES
);

--CREATE OPERATOR CLASS name [ DEFAULT ] FOR TYPE data_type
--  USING index_method [ FAMILY family_name ] AS
--  { OPERATOR strategy_number operator_name [ ( op_type, op_type ) ]
--   | FUNCTION support_number [ ( op_type [ , op_type ] ) ] funcname ( argument_type [, ...] )
--   | STORAGE storage_type
--  } [, ... ]

CREATE OPERATOR CLASS gist__int_ops
    DEFAULT FOR TYPE _int4 USING gist AS
        OPERATOR        3       &&,
        OPERATOR        6       = (anyarray, anyarray),
        OPERATOR        7       @>,
                                <@,
        OPERATOR        8
        OPERATOR        20      @@ (_int4, query_int),
        FUNCTION        1       g_int_consistent (internal, _int4, int, oid, internal),
        FUNCTION        2       g_int_union (internal, internal),
        FUNCTION        3       g_int_compress (internal),
        FUNCTION        4       g_int_decompress (internal),
        FUNCTION        5       g_int_penalty (internal, internal, internal),
        FUNCTION        6       g_int_picksplit (internal, internal),
        FUNCTION        7       g_int_same (_int4, _int4, internal);

--CREATE OPERATOR FAMILY name USING index_method

CREATE OPERATOR FAMILY name USING index_method;

--CREATE ROLE name [ [ WITH ] option [ ... ] ]
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
--    | IN ROLE rolename [, ...]
--    | IN GROUP rolename [, ...]
--    | ROLE rolename [, ...]
--    | ADMIN rolename [, ...]
--    | USER rolename [, ...]
--    | SYSID uid

CREATE ROLE jonathan LOGIN;

CREATE USER davide WITH PASSWORD 'jw8s0F4';

CREATE ROLE miriam WITH LOGIN PASSWORD 'jw8s0F4' VALID UNTIL '2005-01-01';

CREATE ROLE admin WITH CREATEDB CREATEROLE;

--CREATE [ OR REPLACE ] RULE name AS ON event
--    TO table [ WHERE condition ]
--    DO [ ALSO | INSTEAD ] { NOTHING | command | ( command ; command ... ) }

CREATE RULE "_RETURN" AS
    ON SELECT TO t1
    DO INSTEAD
        SELECT * FROM t2;
    
CREATE RULE "_RETURN" AS
    ON SELECT TO t2
    DO INSTEAD
        SELECT * FROM t1;
-- 70 STATEMENTS *******************************************************
CREATE RULE notify_me AS ON UPDATE TO mytable DO ALSO NOTIFY mytable;

--CREATE SCHEMA schemaname [ AUTHORIZATION username ] [ schema_element [ ... ] ]
--CREATE SCHEMA AUTHORIZATION username [ schema_element [ ... ] ]

CREATE SCHEMA myschema;

CREATE SCHEMA AUTHORIZATION joe;

CREATE SCHEMA hollywood
    CREATE TABLE films (title text, release date, awards text[])
    CREATE VIEW winners AS
        SELECT title, release FROM films WHERE awards IS NOT NULL;

--CREATE [ TEMPORARY | TEMP ] SEQUENCE name [ INCREMENT [ BY ] increment ]
--    [ MINVALUE minvalue | NO MINVALUE ] [ MAXVALUE maxvalue | NO MAXVALUE ]
--    [ START [ WITH ] start ] [ CACHE cache ] [ [ NO ] CYCLE ]
--    [ OWNED BY { table.column | NONE } ]

CREATE SEQUENCE serial START 101;

--CREATE SERVER servername [ TYPE 'servertype' ] [ VERSION 'serverversion' ]
--    FOREIGN DATA WRAPPER fdwname
--    [ OPTIONS ( option 'value' [, ... ] ) ]

CREATE SERVER foo FOREIGN DATA WRAPPER "default";

CREATE SERVER myserver FOREIGN DATA WRAPPER pgsql OPTIONS (host 'foo', dbname 'foodb', port);
       
--CREATE [ [ GLOBAL | LOCAL ] { TEMPORARY | TEMP } ] TABLE table_name 
--  ( [
--      { column_name data_type [ DEFAULT default_expr ] [ column_constraint [ ... ] ] 
--          | table_constraint 
--          | LIKE parent_table [ { INCLUDING | EXCLUDING } { DEFAULTS | CONSTRAINTS | INDEXES } ] .
--      [, ... ] 
--  ] )
--[ INHERITS ( parent_table [, ... ] ) ]
--[ WITH ( storage_parameter [= value] [, ... ] ) | WITH OIDS | WITHOUT OIDS ]
--[ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
--[ TABLESPACE tablespace ]
--where column_constraint is:
--[ CONSTRAINT constraint_name ]
--{ NOT NULL |
--  NULL |
--  UNIQUE index_parameters |
--  PRIMARY KEY index_parameters |
--  CHECK ( expression ) |
--  REFERENCES reftable [ ( refcolumn ) ] [ MATCH FULL | MATCH PARTIAL | MATCH SIMPLE ]
--    [ ON DELETE action ] [ ON UPDATE action ] }
--[ DEFERRABLE | NOT DEFERRABLE ] [ INITIALLY DEFERRED | INITIALLY IMMEDIATE ]
--and table_constraint is:
--[ CONSTRAINT constraint_name ]
--{ UNIQUE ( column_name [, ... ] ) index_parameters |
--  PRIMARY KEY ( column_name [, ... ] ) index_parameters |
--  CHECK ( expression ) |
--  FOREIGN KEY ( column_name [, ... ] ) REFERENCES reftable [ ( refcolumn [, ... ] ) ]
--    [ MATCH FULL | MATCH PARTIAL | MATCH SIMPLE ] [ ON DELETE action ] [ ON UPDATE action ] 
--[ DEFERRABLE | NOT DEFERRABLE ] [ INITIALLY DEFERRED | INITIALLY IMMEDIATE ]
--index_parameters in UNIQUE and PRIMARY KEY constraints are:
--[ WITH ( storage_parameter [= value] [, ... ] ) ]
--[ USING INDEX TABLESPACE tablespace ]

CREATE TABLE films (
    code        char(5) CONSTRAINT firstkey PRIMARY KEY,
    title       varchar(40) NOT NULL,
    did         integer NOT NULL,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute
);

CREATE TABLE distributors (
     did    integer PRIMARY KEY DEFAULT nextval('serial'),
     name   varchar(40) NOT NULL CHECK (name <> ”)
     
);

CREATE TABLE array_int (
    vector int[][]
);
-- 80 STATEMENTS *******************************************************
CREATE TABLE films (
    code        char(5),
    title       varchar(40),
    did         integer,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute,
    CONSTRAINT production UNIQUE(date_prod)
);

CREATE TABLE distributors (
    did        integer CHECK (did > 100),
    name    varchar(40)
);

CREATE TABLE distributors (
    did     integer,
    name    varchar(40)
    CONSTRAINT con1 CHECK (did > 100 AND name <> ”)
);

CREATE TABLE films (
    code        char(5),
    title       varchar(40),
    did         integer,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute,
    CONSTRAINT code_title PRIMARY KEY(code,title)
);

CREATE TABLE films (
    code        char(5),
    title       varchar(40),
    did         integer,
    date_prod   date,
    kind        varchar(10),
    len         interval hour to minute,
    CONSTRAINT code_title PRIMARY KEY(code,title)
);

CREATE TABLE distributors (
    name      varchar(40) DEFAULT 'Luso Films',
    did       integer DEFAULT nextval('distributors_serial'),
    modtime   timestamp DEFAULT current_timestamp
);

CREATE TABLE distributors (
    did     integer CONSTRAINT no_null NOT NULL,
    name    varchar(40) NOT NULL
);

CREATE TABLE distributors (
    did     integer,
    name    varchar(40) UNIQUE
);

CREATE TABLE distributors (
    did     integer,
    name    varchar(40),
    UNIQUE(name)
);

CREATE TABLE distributors (
    did     integer,
    name    varchar(40),
    UNIQUE(name) WITH (fillfactor=70)
)
WITH (fillfactor=70);
-- 90 STATEMENTS *******************************************************
CREATE TABLE cinemas (
        id serial,
        name text,
        location text
) TABLESPACE diskvol1;

--CREATE [ [ GLOBAL | LOCAL ] { TEMPORARY | TEMP } ] TABLE table_name
--    [ (column_name [, ...] ) ]
--    [ WITH ( storage_parameter [= value] [, ... ] ) | WITH OIDS | WITHOUT OIDS ]
--    [ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
--    [ TABLESPACE tablespace ]
--    AS query
--          [ WITH [ NO ] DATA ]

CREATE TABLE films_recent AS
  SELECT * FROM films WHERE date_prod >= '2002-01-01';

CREATE TABLE films2 AS
  TABLE films;

CREATE TEMP TABLE films_recent WITH (OIDS) ON COMMIT DROP AS
  EXECUTE recentfilms('2002-01-01');

--CREATE TABLESPACE tablespacename [ OWNER username ] LOCATION 'directory '

CREATE TABLESPACE dbspace LOCATION '/data/dbs';

CREATE TABLESPACE indexspace OWNER genevieve LOCATION '/data/indexes';

--CREATE TEXT SEARCH CONFIGURATION name (
--    PARSER = parser_name |
--    COPY = source_config
--)

CREATE TEXT SEARCH CONFIGURATION my_search_config (
    PARSER = my_parser
);

--CREATE TEXT SEARCH DICTIONARY name (
--    TEMPLATE = template
--    [, option = value [, ... ]]
--)

CREATE TEXT SEARCH DICTIONARY my_russian (
    template = snowball,
    language = russian,
    stopwords = myrussian
);

--CREATE TEXT SEARCH PARSER name (
--    START = start_function ,
--    GETTOKEN = gettoken_function ,
--    END = end_function ,
--    LEXTYPES = lextypes_function
--    [, HEADLINE = headline_function ]
--)

CREATE TEXT SEARCH PARSER my_search_parser (
    START = startNow(),
    GETTOKEN = getToken(),
    END = end(),
    LEXTYPES = getLexTypes()
);

--CREATE TEXT SEARCH TEMPLATE name (
--    [ INIT = init_function , ]
--    LEXIZE = lexize_function
--)

CREATE TEXT SEARCH TEMPLATE my_search_template (
    LEXIZE = lexizeNow()
);

--CREATE TRIGGER name { BEFORE | AFTER } { event [ OR ... ] }
--    ON table [ FOR [ EACH ] { ROW | STATEMENT } ]
--    EXECUTE PROCEDURE funcname ( arguments )
-- 100 STATEMENTS *******************************************************
CREATE TRIGGER trigger_name BEFORE dawn
    ON table
    EXECUTE PROCEDURE funcname ( 'arg1', 'arg2' );

ALTER TABLE foreign_companies RENAME COLUMN address TO city;

ALTER TABLE us_companies RENAME TO suppliers;

ALTER TABLE old_addresses ALTER COLUMN street SET NOT NULL;

ALTER TABLE new_addresses ALTER COLUMN street DROP NOT NULL;

GRANT EXECUTE ON FUNCTION divideByTwo(numerator int, IN demoninator int) TO george;

-- 106 STATEMENTS *******************************************************
