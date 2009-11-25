--
-- SAMPLE ORACLE STATEMENTS
--

CREATE OR REPLACE PACKAGE emp_mgmt AS 
	FUNCTION hire (last_name VARCHAR2, job_id VARCHAR2, 
	   manager_id NUMBER, salary NUMBER, 
	   commission_pct NUMBER, department_id NUMBER) 
	   RETURN NUMBER; 
	FUNCTION create_dept(department_id NUMBER, location_id NUMBER) 
	   RETURN NUMBER; 
	PROCEDURE remove_emp(employee_id NUMBER); 
	PROCEDURE remove_dept(department_id NUMBER); 
	PROCEDURE increase_sal(employee_id NUMBER, salary_incr NUMBER); 
	PROCEDURE increase_comm(employee_id NUMBER, comm_incr NUMBER); 
	no_comm EXCEPTION; 
	no_sal EXCEPTION; 
	END emp_mgmt;
/

CREATE PFILE = 'my_init.ora' FROM SPFILE = 's_params.ora';

CREATE PROCEDURE remove_emp (employee_id NUMBER) AS tot_emps NUMBER;
	BEGIN
	   DELETE FROM employees
	   WHERE employees.employee_id = remove_emp.employee_id;
	tot_emps := tot_emps - 1;
	END;
/

CREATE PROCEDURE find_root
   ( x IN REAL ) 
   IS LANGUAGE C
   NAME c_find_root
   LIBRARY c_utils
   PARAMETERS ( x BY REFERENCE );

CREATE PROFILE app_user2 LIMIT
   FAILED_LOGIN_ATTEMPTS 5
   PASSWORD_LIFE_TIME 60
   PASSWORD_REUSE_TIME 60
   PASSWORD_REUSE_MAX 5
   PASSWORD_VERIFY_FUNCTION verify_function
   PASSWORD_LOCK_TIME 1/24
   PASSWORD_GRACE_TIME 10;

CREATE ROLE warehouse_user IDENTIFIED EXTERNALLY;

CREATE PUBLIC ROLLBACK SEGMENT rbs_one
   TABLESPACE rbs_ts;

CREATE ROLLBACK SEGMENT rbs_one
   TABLESPACE rbs_ts
   STORAGE
   ( INITIAL 10K
     NEXT 10K
     MAXEXTENTS UNLIMITED );
     
CREATE SCHEMA AUTHORIZATION oe
   CREATE TABLE new_product 
      (color VARCHAR2(10)  PRIMARY KEY, quantity NUMBER) 
   CREATE VIEW new_product_view 
      AS SELECT color, quantity FROM new_product WHERE color = 'RED' 
   GRANT select ON new_product_view TO hr; 

CREATE SEQUENCE customers_seq
	START WITH     1000
	INCREMENT BY   1
	NOCACHE
	NOCYCLE;

-- 10 Statements so far

CREATE SPFILE = 's_params.ora' 
   FROM PFILE = '$ORACLE_HOME/work/t_init1.ora';

CREATE PUBLIC SYNONYM emp_table 
   FOR hr.employees@remote.us.oracle.com;

CREATE SYNONYM offices 
   FOR hr.locations;

CREATE OR REPLACE SYNONYM offices 
   FOR hr.locations;
   
CREATE OR REPLACE PUBLIC SYNONYM offices 
   FOR hr.locations;

CREATE TABLESPACE rbs_ts
   DATAFILE 'rbs01.dbf' SIZE 10M
   EXTENT MANAGEMENT LOCAL UNIFORM SIZE 100K;

CREATE TABLESPACE tbs_02 
   DATAFILE 'diskb:tbs_f5.dat' SIZE 500K REUSE
   AUTOEXTEND ON NEXT 500K MAXSIZE 100M;

CREATE TRIGGER hr.salary_check
   BEFORE INSERT OR UPDATE OF salary, job_id ON hr.employees
   FOR EACH ROW
   WHEN (new.job_id <> 'AD_VP')
   CALL check_sal(:new.job_id, :new.salary, :new.last_name);
/
   
CREATE OR REPLACE TRIGGER order_info_insert
   INSTEAD OF INSERT ON order_info
	DECLARE
	  duplicate_info EXCEPTION;
	  PRAGMA EXCEPTION_INIT (duplicate_info, -00001);
	BEGIN
	  INSERT INTO customers
	    (customer_id, cust_last_name, cust_first_name) 
	  VALUES (
	  :new.customer_id, 
	  :new.cust_last_name,
	  :new.cust_first_name);
	INSERT INTO orders (order_id, order_date, customer_id)
	VALUES (
	  :new.order_id,
	  :new.order_date,
	  :new.customer_id);
	EXCEPTION
	  WHEN duplicate_info THEN
	    RAISE_APPLICATION_ERROR (
	      num=> -20107,
	      msg=> 'Duplicate customer or order ID');
	END order_info_insert;
/

CREATE OR REPLACE TRIGGER drop_trigger 
   BEFORE DROP ON hr.SCHEMA 
   BEGIN
      RAISE_APPLICATION_ERROR (
         num => -20000,
         msg => 'Cannot drop object');
   END;
/

-- 20 Statements so far

CREATE TYPE person_t AS OBJECT (name VARCHAR2(100), ssn NUMBER) 
   NOT FINAL;

CREATE TYPE employee_t UNDER person_t 
   (department_id NUMBER, salary NUMBER) NOT FINAL;

CREATE TYPE part_time_emp_t UNDER employee_t (num_hrs NUMBER);

CREATE OR REPLACE TYPE long_address_t
	UNDER address_t
	EXTERNAL NAME 'Examples.LongAddress' LANGUAGE JAVA 
	USING SQLData(
	    street2_attr VARCHAR(250) EXTERNAL NAME 'street2',
	    country_attr VARCHAR (200) EXTERNAL NAME 'country',
	    address_code_attr VARCHAR (50) EXTERNAL NAME 'addrCode',    
	    STATIC FUNCTION create_address RETURN long_address_t 
	      EXTERNAL NAME 'create() return Examples.LongAddress',
	    STATIC FUNCTION  construct (street VARCHAR, city VARCHAR, 
	        state VARCHAR, country VARCHAR, addrs_cd VARCHAR) 
	      RETURN long_address_t 
	      EXTERNAL NAME 
	        'create(java.lang.String, java.lang.String,
	        java.lang.String, java.lang.String, java.lang.String) 
	          return Examples.LongAddress',
	    STATIC FUNCTION construct RETURN long_address_t
	      EXTERNAL NAME 'Examples.LongAddress() 
	        return Examples.LongAddress',
	    STATIC FUNCTION create_longaddress (
	      street VARCHAR, city VARCHAR, state VARCHAR, country VARCHAR, 
	      addrs_cd VARCHAR) return long_address_t
	      EXTERNAL NAME 
	        'Examples.LongAddress (java.lang.String, java.lang.String,
	         java.lang.String, java.lang.String, java.lang.String)
	           return Examples.LongAddress',
	    MEMBER FUNCTION get_country RETURN VARCHAR
	      EXTERNAL NAME 'country_with_code () return java.lang.String'
	  );

CREATE USER ops$external_user
   IDENTIFIED EXTERNALLY
   DEFAULT TABLESPACE example
   QUOTA 5M ON example
   PROFILE app_user;     

CREATE USER global_user
   IDENTIFIED GLOBALLY AS 'CN=analyst, OU=division1, O=oracle, C=US'
   DEFAULT TABLESPACE example
   QUOTA 5M ON example;

CREATE VIEW customer_ro (name, language, credit)
      AS SELECT cust_last_name, nls_language, credit_limit
      FROM customers
      WITH READ ONLY;

CREATE OR REPLACE VIEW oc_inventories OF inventory_typ 
   WITH OBJECT IDENTIFIER (product_id)
   AS SELECT i.product_id, 
     warehouse_typ(w.warehouse_id, w.warehouse_name, w.location_id),
     i.quantity_on_hand
   FROM inventories i, warehouses w
   WHERE i.warehouse_id=w.warehouse_id;

DISASSOCIATE STATISTICS FROM PACKAGES hr.emp_mgmt;

DROP CLUSTER personnel
   INCLUDING TABLES
   CASCADE CONSTRAINTS;

-- 30 Statements so far

DROP CONTEXT hr_context;

DROP DATABASE;

DROP DATABASE LINK remote;

DROP PUBLIC DATABASE LINK remote;

DROP DIMENSION customers_dim;

DROP DIRECTORY bfile_dir;

DROP DISKGROUP dgroup_01 INCLUDING CONTENTS;

DROP FUNCTION oe.SecondMax; 

DROP INDEX ord_customer_ix_demo;

DROP INDEXTYPE textindextype FORCE;

-- 40 Statements so far

DROP JAVA CLASS "MyClass";

DROP LIBRARY ext_lib;

DROP MATERIALIZED VIEW sales_by_month_by_state;

DROP OPERATOR eq_op;

DROP OUTLINE salaries;

DROP PACKAGE emp_mgmt; 

DROP PROCEDURE hr.remove_emp; 

DROP PROFILE app_user CASCADE; 

DROP ROLE dw_manager; 

DROP ROLLBACK SEGMENT rollback_segment; 

-- 50 Statements so far
