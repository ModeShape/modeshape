--
-- SAMPLE ORACLE STATEMENTS
--

ALTER TABLE employees 
   PCTFREE 30
   PCTUSED 60; 

ALTER TABLE countries 
   ADD (duty_pct     NUMBER(2,2)  CHECK (duty_pct < 10.5),
        visa_needed  VARCHAR2(3)); 

ALTER TABLESPACE tbs_01 
    BEGIN BACKUP; 

ALTER TABLESPACE omf_ts1 ADD DATAFILE; 

ALTER TABLESPACE undots1
  RETENTION NOGUARANTEE;

ALTER TRIGGER update_job_history DISABLE;

ALTER TYPE data_typ 
   ADD MEMBER FUNCTION qtr(der_qtr DATE) 
   RETURN CHAR CASCADE;

ALTER TYPE cust_address_typ
   ADD ATTRIBUTE (phone phone_list_typ) CASCADE;

ALTER TYPE phone_list_typ
  MODIFY ELEMENT TYPE VARCHAR(64) CASCADE;

ALTER USER app_user1 
   GRANT CONNECT THROUGH sh
   WITH ROLE warehouse_user;

-- 10 Statements

ALTER USER app_user1 IDENTIFIED GLOBALLY AS 'CN=tom,O=oracle,C=US';

ALTER USER sidney 
    IDENTIFIED BY second_2nd_pwd
    DEFAULT TABLESPACE example; 

ALTER VIEW customer_ro
    COMPILE; 

ANALYZE TABLE customers VALIDATE STRUCTURE ONLINE;

ANALYZE TABLE employees VALIDATE STRUCTURE CASCADE; 

ANALYZE TABLE orders DELETE STATISTICS; 

ASSOCIATE STATISTICS WITH PACKAGES emp_mgmt DEFAULT SELECTIVITY 10;

AUDIT SELECT 
    ON hr.employees
    WHENEVER SUCCESSFUL; 

AUDIT INSERT, UPDATE
    ON oe.customers; 

AUDIT DELETE ANY TABLE; 

-- 20 Statements

AUDIT ROLE
    WHENEVER SUCCESSFUL; 

COMMENT ON COLUMN employees.job_id 
   IS 'abbreviated job title';

COMMIT WORK; 

COMMIT COMMENT 'In-doubt transaction Code 36, Call (415) 555-2637'; 

CREATE CLUSTER personnel
   (department NUMBER(4))
SIZE 512 
STORAGE (initial 100K next 50K);

CREATE CLUSTER address
   (postal_code NUMBER, country_id CHAR(2))
   HASHKEYS 20
   HASH IS MOD(postal_code + country_id, 101);

CREATE CLUSTER cust_orders (customer_id NUMBER(6))
   SIZE 512 SINGLE TABLE HASHKEYS 100;

CREATE CONTEXT hr_context USING emp_mgmt;

CREATE CONTROLFILE REUSE DATABASE "demo" NORESETLOGS NOARCHIVELOG
	    MAXLOGFILES 32
	    MAXLOGMEMBERS 2
	    MAXDATAFILES 32
	    MAXINSTANCES 1
	    MAXLOGHISTORY 449
	LOGFILE
	  GROUP 1 '/path/oracle/dbs/t_log1.f'  SIZE 500K,
	  GROUP 2 '/path/oracle/dbs/t_log2.f'  SIZE 500K
	# STANDBY LOGFILE
	DATAFILE
	  '/path/oracle/dbs/t_db1.f',
	  '/path/oracle/dbs/dbu19i.dbf',
	  '/path/oracle/dbs/tbs_11.f',
	  '/path/oracle/dbs/smundo.dbf',
	  '/path/oracle/dbs/demo.dbf'
	CHARACTER SET WE8DEC
	;

CREATE DATABASE sample
   CONTROLFILE REUSE 
   LOGFILE
      GROUP 1 ('diskx:log1.log', 'disky:log1.log') SIZE 50K, 
      GROUP 2 ('diskx:log2.log', 'disky:log2.log') SIZE 50K 
   MAXLOGFILES 5 
   MAXLOGHISTORY 100 
   MAXDATAFILES 10 
   MAXINSTANCES 2 
   ARCHIVELOG 
   CHARACTER SET AL32UTF8
   NATIONAL CHARACTER SET AL16UTF16
   DATAFILE  
      'disk1:df1.dbf' AUTOEXTEND ON,
      'disk2:df2.dbf' AUTOEXTEND ON NEXT 10M MAXSIZE UNLIMITED
   DEFAULT TEMPORARY TABLESPACE temp_ts
   UNDO TABLESPACE undo_ts 
   SET TIME_ZONE = '+02:00';

-- 30 Statements

CREATE PUBLIC DATABASE LINK remote 
   USING 'remote'; 

CREATE DATABASE LINK local 
   CONNECT TO hr IDENTIFIED BY hr
   USING 'local';

CREATE DIMENSION customers_dim 
   LEVEL customer   IS (customers.cust_id)
   LEVEL city       IS (customers.cust_city) 
   LEVEL state      IS (customers.cust_state_province) 
   LEVEL country    IS (countries.country_id) 
   LEVEL subregion  IS (countries.country_subregion) 
   LEVEL region     IS (countries.country_region) 
   HIERARCHY geog_rollup (
      customer      CHILD OF
      city          CHILD OF 
      state         CHILD OF 
      country       CHILD OF 
      subregion     CHILD OF 
      region 
   JOIN KEY (customers.country_id) REFERENCES country
   )
   ATTRIBUTE customer DETERMINES
   (cust_first_name, cust_last_name, cust_gender, 
    cust_marital_status, cust_year_of_birth, 
    cust_income_level, cust_credit_limit) 
   ATTRIBUTE country DETERMINES (countries.country_name)
;

CREATE DIRECTORY admin AS 'oracle/admin';

CREATE OR REPLACE DIRECTORY bfile_dir AS '/private1/LOB/files';

CREATE DISKGROUP dgroup_01
  EXTERNAL REDUNDANCY
  DISK '$ORACLE_HOME/disks/c*';
  
CREATE FUNCTION SecondMax (input NUMBER) RETURN NUMBER
    PARALLEL_ENABLE AGGREGATE USING SecondMaxImpl;

CREATE OR REPLACE FUNCTION text_length(a CLOB) 
   RETURN NUMBER DETERMINISTIC IS
	BEGIN 
	  RETURN DBMS_LOB.GETLENGTH(a);
	END;
/

CREATE INDEXTYPE position_indextype
   FOR position_between(NUMBER, NUMBER, NUMBER)
   USING position_im;

CREATE JAVA SOURCE NAMED "Hello" AS
   public class Hello {
      public static String hello() {
         return \"Hello World\";   } };

-- 40 Statements

CREATE JAVA RESOURCE NAMED "appText" 
   USING BFILE (bfile_dir, 'textBundle.dat');

CREATE LIBRARY ext_lib AS '/OR/lib/ext_lib.so';
/

CREATE OR REPLACE LIBRARY ext_lib IS '/OR/newlib/ext_lib.so';
/

CREATE LIBRARY app_lib as '${ORACLE_HOME}/lib/app_lib.so'
   AGENT 'sales.hq.acme.com';
/
   
CREATE MATERIALIZED VIEW LOG ON employees
   WITH PRIMARY KEY
   INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW all_customers
   PCTFREE 5 PCTUSED 60 
   TABLESPACE example 
   STORAGE (INITIAL 50K NEXT 50K) 
   USING INDEX STORAGE (INITIAL 25K NEXT 25K)
   REFRESH START WITH ROUND(SYSDATE + 1) + 11/24 
   NEXT NEXT_DAY(TRUNC(SYSDATE), 'MONDAY') + 15/24 
   AS SELECT * FROM sh.customers@remote 
         UNION
      SELECT * FROM sh.customers@local; 

CREATE MATERIALIZED VIEW LOG ON product_information 
   WITH ROWID, SEQUENCE (list_price, min_price, category_id) 
   INCLUDING NEW VALUES;

CREATE OPERATOR eq_op
   BINDING (VARCHAR2, VARCHAR2) 
   RETURN NUMBER 
   USING eq_f; 

CREATE OUTLINE salaries FOR CATEGORY special
   ON SELECT last_name, salary FROM employees;

CREATE OR REPLACE OUTLINE public_salaries 
   FROM PRIVATE my_salaries;

-- 50 Statements so far
