-- Postgres SQL Statements from postgressql-8.4.1-US.pdf
--
-- Extracted 10/5/2009

--CREATE TYPE name AS
--    ( attribute_name data_type [, ... ] )
--CREATE TYPE name AS ENUM
--    ( 'label' [, ... ] )
--CREATE TYPE name (
--    INPUT = input_function,
--    OUTPUT = output_function
--    [ , RECEIVE = receive_function ]
--    [ , SEND = send_function ]
--    [ , TYPMOD_IN = type_modifier_input_function ]
--    [ , TYPMOD_OUT = type_modifier_output_function ]
--    [ , ANALYZE = analyze_function ]
--    [ , INTERNALLENGTH = { internallength | VARIABLE } ]
--    [ , PASSEDBYVALUE ]
--    [ , ALIGNMENT = alignment ]
--    [ , STORAGE = storage ]
--    [ , LIKE = like_type ]
--    [ , CATEGORY = category ]
--    [ , PREFERRED = preferred ]
--    [ , DEFAULT = default ]
--    [ , ELEMENT = element ]
--    [ , DELIMITER = delimiter ]
--)
--CREATE TYPE name

CREATE TYPE compfoo AS (f1 int, f2 text);

CREATE TYPE bug_status AS ENUM ('new', 'open', 'closed');

CREATE TYPE box;

CREATE TYPE box (
    INTERNALLENGTH = 16,
    INPUT = my_box_in_function,
    OUTPUT = my_box_out_function
);

CREATE TYPE box (
    INTERNALLENGTH = 16,
    INPUT = my_box_in_function,
    OUTPUT = my_box_out_function,
    ELEMENT = float4
);

CREATE TYPE bigobj (
    INPUT = lo_filein, OUTPUT = lo_fileout,
    INTERNALLENGTH = VARIABLE
);

--CREATE USER name [ [ WITH ] option [ ... ] ]
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

--CREATE USER MAPPING FOR { username | USER | CURRENT_USER | PUBLIC }
--    SERVER servername
--    [ OPTIONS ( option 'value' [ , ... ] ) ]

CREATE USER MAPPING FOR bob SERVER foo OPTIONS (user 'bob', password 'secret');

--CREATE [ OR REPLACE ] [ TEMP | TEMPORARY ] VIEW name [ ( column_name [, ...] ) ]
--    AS query

CREATE VIEW vista AS SELECT 'Hello World';

CREATE VIEW vista AS SELECT text 'Hello World' AS hello;

CREATE VIEW comedies AS
    SELECT *
    FROM films
    WHERE kind = 'Comedy';
-- 10 STATEMENTS *******************************************************
--DEALLOCATE [ PREPARE ] { name | ALL }

DEALLOCATE name;

DEALLOCATE PREPARE name;

DEALLOCATE ALL;

DEALLOCATE PREPARE ALL;

--DECLARE name [ BINARY ] [ INSENSITIVE ] [ [ NO ] SCROLL ]
--    CURSOR [ { WITH | WITHOUT } HOLD ] FOR query

DECLARE liahona CURSOR FOR SELECT * FROM films;

-- DISCARD { ALL | PLANS | TEMPORARY | TEMP }
DISCARD ALL;

DISCARD PLANS;

DISCARD TEMPORARY;

DISCARD TEMP;

-- DROP AGGREGATE [ IF EXISTS ] name ( type [ , ... ] ) [ CASCADE | RESTRICT ]

DROP AGGREGATE myavg(integer);
-- 20 STATEMENTS *******************************************************
DROP AGGREGATE myavg(integer) RESTRICT;

DROP AGGREGATE IF EXISTS myavg(integer) CASCADE;

-- DROP CAST [ IF EXISTS ] (sourcetype AS targettype) [ CASCADE | RESTRICT ]

DROP CAST (text AS int);

DROP CAST IF EXISTS (text AS int) CASCADE;

DROP CAST IF EXISTS (text AS int);

DROP CAST (text AS int) RESTRICT;

-- DROP CONVERSION [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP CONVERSION conversion_name;

DROP CONVERSION IF EXISTS conversion_name CASCADE;

DROP CONVERSION IF EXISTS conversion_name;

DROP CONVERSION conversion_name RESTRICT;
-- 30 STATEMENTS *******************************************************
-- DROP DATABASE [ IF EXISTS ] name

DROP DATABASE db_name;

DROP DATABASE IF EXISTS db_name;

-- DROP DOMAIN [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP DOMAIN domain_name;

DROP DOMAIN IF EXISTS domain_name CASCADE;

DROP DOMAIN IF EXISTS domain_name;

DROP DOMAIN domain_name RESTRICT;

-- DROP FOREIGN DATA WRAPPER [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP FOREIGN DATA WRAPPER fdr_name;

DROP FOREIGN DATA WRAPPER IF EXISTS fdr_name CASCADE;

DROP FOREIGN DATA WRAPPER IF EXISTS fdr_name;

DROP FOREIGN DATA WRAPPER fdr_name RESTRICT;
-- 40 STATEMENTS *******************************************************
-- DROP FUNCTION [ IF EXISTS ] name ( [ [ argmode ] [ argname ] argtype [, ...] ] ) [ CASCADE | RESTRICT ]

DROP FUNCTION sqrt(integer);

DROP FUNCTION sqrt(integer) RESTRICT;

DROP FUNCTION IF EXISTS sqrt(integer) CASCADE;

-- DROP GROUP [ IF EXISTS ] name [, ...]

DROP GROUP group_name;

DROP GROUP IF EXISTS group_name;

-- DROP INDEX [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP INDEX index_name;

DROP INDEX IF EXISTS index_name CASCADE;

DROP INDEX IF EXISTS index_name;

DROP INDEX index_name RESTRICT;

-- DROP [ PROCEDURAL ] LANGUAGE [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP LANGUAGE domain_name;
-- 50 STATEMENTS *******************************************************
DROP LANGUAGE IF EXISTS domain_name CASCADE;

DROP LANGUAGE IF EXISTS domain_name;

DROP LANGUAGE domain_name RESTRICT;

DROP PROCEDURAL LANGUAGE domain_name;

DROP PROCEDURAL LANGUAGE IF EXISTS domain_name CASCADE;

DROP PROCEDURAL LANGUAGE IF EXISTS domain_name;

DROP PROCEDURAL LANGUAGE domain_name RESTRICT;

-- DROP OPERATOR [ IF EXISTS ] name ( { lefttype | NONE } , { righttype | NONE } ) [ CASCADE | RESTRICT ]

DROP OPERATOR ^ (integer, integer);

DROP OPERATOR ~ (none, bit);

DROP OPERATOR ! (bigint, none);
-- 60 STATEMENTS *******************************************************
DROP OPERATOR eq_op;

DROP OPERATOR eq_op CASCADE;

DROP OPERATOR eq_op RESTRICT;

-- DROP OPERATOR CLASS [ IF EXISTS ] name USING index_method [ CASCADE | RESTRICT ]

DROP OPERATOR CLASS widget_ops USING btree;

DROP OPERATOR CLASS widget_ops USING btree CASCADE;

DROP OPERATOR CLASS IF EXISTS widget_ops USING btree;

DROP OPERATOR CLASS IF EXISTS widget_ops USING btree RESTRICT;

-- DROP OPERATOR FAMILY [ IF EXISTS ] name USING index_method [ CASCADE | RESTRICT ]

DROP OPERATOR FAMILY float_ops USING btree;

DROP OPERATOR FAMILY float_ops USING btree CASCADE;

DROP OPERATOR FAMILY IF EXISTS float_ops USING btree;
-- 70 STATEMENTS *******************************************************
DROP OPERATOR FAMILY IF EXISTS float_ops USING btree RESTRICT;

-- DROP OWNED BY name [, ...] [ CASCADE | RESTRICT ]

DROP OWNED BY owned_role_name;

DROP OWNED BY IF EXISTS owned_role_name CASCADE;

DROP OWNED BY IF EXISTS owned_role_name;

DROP OWNED BY owned_role_name RESTRICT;

-- DROP ROLE [ IF EXISTS ] name [, ...]

DROP ROLE role_name;

DROP ROLE IF EXISTS role_name CASCADE;

DROP ROLE IF EXISTS role_name;

DROP ROLE role_name RESTRICT;

-- DROP RULE [ IF EXISTS ] name ON relation [ CASCADE | RESTRICT ]

DROP RULE newrule ON mytable;
-- 80 STATEMENTS *******************************************************
DROP RULE newrule ON mytable CASCADE;

DROP RULE IF EXISTS newrule ON mytable;

DROP RULE IF EXISTS newrule ON mytable RESTRICT;

-- DROP SCHEMA [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP SCHEMA schema_name;

DROP SCHEMA schema_name CASCADE;

DROP SCHEMA IF EXISTS schema_name;

DROP SCHEMA IF EXISTS schema_name RESTRICT;

-- DROP SEQUENCE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP SEQUENCE sequence_name;

DROP SEQUENCE sequence_name CASCADE;

DROP SEQUENCE IF EXISTS sequence_name;
-- 90 STATEMENTS *******************************************************
DROP SEQUENCE IF EXISTS sequence_name RESTRICT;

-- DROP SERVER [ IF EXISTS ] servername [ CASCADE | RESTRICT ]

DROP SERVER server_name;

DROP SERVER server_name CASCADE;

DROP SERVER IF EXISTS server_name;

DROP SERVER IF EXISTS server_name RESTRICT;

-- DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP TABLE films, distributors;

DROP TABLE films, distributors CASCADE;

DROP TABLE IF EXISTS films, distributors;

DROP TABLE IF EXISTS films, distributors RESTRICT;

-- DROP TABLESPACE [ IF EXISTS ] tablespacename

DROP TABLESPACE tablespace_name;
-- 100 STATEMENTS *******************************************************
DROP TABLESPACE IF EXISTS tablespace_name;

-- DROP TEXT SEARCH CONFIGURATION [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP TEXT SEARCH CONFIGURATION text_search_name;

DROP TEXT SEARCH CONFIGURATION text_search_name CASCADE;

DROP TEXT SEARCH CONFIGURATION IF EXISTS text_search_name;

DROP TEXT SEARCH CONFIGURATION IF EXISTS text_search_name RESTRICT;

-- DROP TEXT SEARCH DICTIONARY [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP TEXT SEARCH DICTIONARY text_search_name;

DROP TEXT SEARCH DICTIONARY text_search_name CASCADE;

DROP TEXT SEARCH DICTIONARY IF EXISTS text_search_name;

DROP TEXT SEARCH DICTIONARY IF EXISTS text_search_name RESTRICT;

-- DROP TEXT SEARCH PARSER [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP TEXT SEARCH PARSER text_search_name;
-- 110 STATEMENTS *******************************************************
DROP TEXT SEARCH PARSER text_search_name CASCADE;

DROP TEXT SEARCH PARSER IF EXISTS text_search_name;

DROP TEXT SEARCH PARSER IF EXISTS text_search_name RESTRICT;

-- DROP TEXT SEARCH TEMPLATE [ IF EXISTS ] name [ CASCADE | RESTRICT ]

DROP TEXT SEARCH TEMPLATE text_search_name;

DROP TEXT SEARCH TEMPLATE text_search_name CASCADE;

DROP TEXT SEARCH TEMPLATE IF EXISTS text_search_name;

DROP TEXT SEARCH TEMPLATE IF EXISTS text_search_name RESTRICT;

-- DROP TRIGGER [ IF EXISTS ] name ON table [ CASCADE | RESTRICT ]

DROP TRIGGER trigger_name ON mytable;

DROP TRIGGER trigger_name ON mytable CASCADE;

DROP TRIGGER IF EXISTS trigger_name ON mytable;
-- 120 STATEMENTS *******************************************************
DROP TRIGGER IF EXISTS trigger_name ON mytable RESTRICT;

-- DROP TYPE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP TYPE type_name;

DROP TYPE type_name CASCADE;

DROP TYPE IF EXISTS type_name;

DROP TYPE IF EXISTS type_name RESTRICT;

-- DROP USER [ IF EXISTS ] name [, ...]

DROP USER user_name_1, user_name_2;

DROP USER IF EXISTS user_name_1, user_name_2;

-- DROP USER MAPPING [ IF EXISTS ] FOR { username | USER | CURRENT_USER | PUBLIC } SERVER server_name

DROP USER MAPPING FOR bob SERVER foo;

DROP USER MAPPING IF EXISTS FOR bob SERVER foo;

DROP USER MAPPING FOR USER SERVER foo;
-- 130 STATEMENTS *******************************************************
DROP USER MAPPING FOR CURRENT SERVER foo;

DROP USER MAPPING FOR PUBLIC SERVER foo;

-- DROP VIEW [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]

DROP VIEW view_name;

DROP VIEW view_name CASCADE;

DROP VIEW IF EXISTS view_name_1, view_name_2;

DROP VIEW IF EXISTS view_name RESTRICT;
-- 143 STATEMENTS  *** INCLUDES 7 DROP statements with multiple names *
