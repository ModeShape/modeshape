-- Postgres SQL Statements from postgressql-8.4.1-US.pdf
--
-- Extracted 10/5/2009

-- EXPLAIN [ ANALYZE ] [ VERBOSE ] statement

EXPLAIN SELECT * FROM foo;

EXPLAIN SELECT * FROM foo WHERE i = 4;

EXPLAIN SELECT sum(i) FROM foo WHERE i < 10;

EXPLAIN ANALYZE EXECUTE query(100, 200);

-- FETCH [ direction { FROM | IN } ] cursorname
--  RIOR
--  FIRST
--  LAST
--  ABSOLUTE count
--  RELATIVE count
--  count
--  ALL
--  FORWARD
--  FORWARD count
--  FORWARD ALL
--  BACKWARD
--  BACKWARD count
--  BACKWARD ALL

FETCH FORWARD 5 FROM liahona;

FETCH PRIOR FROM liahona;

-- GRANT { { SELECT | INSERT | UPDATE | DELETE | TRUNCATE | REFERENCES | TRIGGER }
--    [,...] | ALL [ PRIVILEGES ] }
--    ON [ TABLE ] tablename [, ...]
--    TO { [ GROUP ] rolename | PUBLIC } [, ...] [ WITH GRANT OPTION ]


GRANT SELECT ON TABLE t TO maria,harry; 

GRANT UPDATE, TRIGGER ON TABLE t TO anita,zhi;

GRANT SELECT ON TABLE s.v to PUBLIC;

GRANT EXECUTE ON FUNCTION p(a int, b TEXT) TO george;
-- 10 STATEMENTS *******************************************************
GRANT purchases_reader_role TO george,maria;

GRANT SELECT ON TABLE t TO purchases_reader_role;

GRANT SELECT ON mytable TO PUBLIC;
GRANT SELECT, UPDATE, INSERT ON mytable TO admin;
GRANT SELECT (col1), UPDATE (col1) ON mytable TO miriam_rw;

GRANT INSERT ON films TO PUBLIC;
GRANT ALL PRIVILEGES ON kinds TO manuel;
GRANT admins TO joe;

-- INSERT INTO table [ ( column [, ...] ) ]
--    { DEFAULT VALUES | VALUES ( { expression | DEFAULT } [, ...] ) [, ...] | query }
--    [ RETURNING * | output_expression [ [ AS ] output_name ] [, ...] ]

INSERT INTO films VALUES
    ('UA502', 'Bananas', 105, '1971-07-13', 'Comedy', '82 minutes');

INSERT INTO films (code, title, did, date_prod, kind)
    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');
-- 20 STATEMENTS *******************************************************
INSERT INTO distributors (did, dname) VALUES (DEFAULT, 'XYZ Widgets')
   RETURNING did;

-- LISTEN name

LISTEN virtual;

LOAD 'filename';

--LOCK [ TABLE ] [ ONLY ] name [, ...] [ IN lockmode MODE ] [ NOWAIT ]
--    where lockmode is one of:
--        ACCESS SHARE | ROW SHARE | ROW EXCLUSIVE | SHARE UPDATE EXCLUSIVE
--        | SHARE | SHARE ROW EXCLUSIVE | EXCLUSIVE | ACCESS EXCLUSIVE

LOCK TABLE films IN SHARE MODE;

LOCK TABLE films IN SHARE ROW EXCLUSIVE MODE;

-- MOVE [ direction { FROM | IN } ] cursorname

MOVE FORWARD 5 IN liahona;
MOVE 5;

-- NOTIFY name

NOTIFY virtual;

-- PREPARE name [ ( datatype [, ...] ) ] AS statement

PREPARE fooplan (int, text, bool, numeric) AS
    INSERT INTO foo VALUES($1, $2, $3, $4);

PREPARE usrrptplan (int) AS
    SELECT * FROM users u, logs l WHERE u.usrid=$1 AND u.usrid=l.usrid
    AND l.date = $2;
-- 30 STATEMENTS *******************************************************
-- REINDEX { INDEX | TABLE | DATABASE | SYSTEM } name [ FORCE ]
    
REINDEX INDEX distributors;

REINDEX TABLE my_table;

REINDEX DATABASE my_db FORCE;

REINDEX SYSTEM my_system;
-- 34 STATEMENTS *******************************************************
