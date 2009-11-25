--
-- SAMPLE ORACLE STATEMENTS
--

DROP SEQUENCE oe.customers_seq; 

DROP SYNONYM customers; 

DROP PUBLIC SYNONYM customers;

DROP TABLE list_customers PURGE; 

DROP TABLESPACE tbs_01 
    INCLUDING CONTENTS 
        CASCADE CONSTRAINTS; 

DROP TRIGGER hr.salary_check; 

DROP TYPE person_t;

DROP TYPE BODY data_typ;

DROP USER sidney CASCADE; 

DROP VIEW emp_view; 

-- 10 Statements so far

EXPLAIN PLAN 
    SET STATEMENT_ID = 'Raise in Tokyo' 
    INTO plan_table 
    FOR UPDATE employees 
        SET salary = salary * 1.10 
        WHERE department_id =  
           (SELECT department_id FROM departments
               WHERE location_id = 1200); 

FLASHBACK DATABASE TO TIMESTAMP SYSDATE-1;

FLASHBACK STANDBY DATABASE TO TIMESTAMP SYSDATE-1;

FLASHBACK TABLE employees_demo
  TO TIMESTAMP (SYSTIMESTAMP - INTERVAL '1' minute);

GRANT CREATE SESSION 
   TO hr; 

GRANT
     CREATE ANY MATERIALIZED VIEW
   , ALTER ANY MATERIALIZED VIEW
   , DROP ANY MATERIALIZED VIEW
   , QUERY REWRITE
   , GLOBAL QUERY REWRITE
   TO dw_manager
   WITH ADMIN OPTION;

GRANT dw_manager 
   TO sh 
   WITH ADMIN OPTION; 

GRANT SELECT ON sh.sales TO warehouse_user;

GRANT READ ON DIRECTORY bfile_dir TO hr
   WITH GRANT OPTION;

GRANT ALL ON bonuses TO hr 
   WITH GRANT OPTION;

-- 20 Statements so far

GRANT SELECT, UPDATE 
   ON emp_view TO PUBLIC; 

GRANT SELECT 
   ON oe.customers_seq TO hr; 

GRANT REFERENCES (employee_id), 
      UPDATE (employee_id, salary, commission_pct) 
   ON hr.employees
   TO oe; 

CREATE TABLE dependent 
   (dependno   NUMBER, 
    dependname VARCHAR2(10), 
    employee   NUMBER 
   CONSTRAINT in_emp REFERENCES hr.employees(employee_id) );

LOCK TABLE employees
   IN EXCLUSIVE MODE 
   NOWAIT; 

MERGE INTO bonuses D
   USING (SELECT employee_id, salary, department_id FROM employees
   WHERE department_id = 80) S
   ON (D.employee_id = S.employee_id)
   WHEN MATCHED THEN UPDATE SET D.bonus = D.bonus + S.salary*.01
     DELETE WHERE (S.salary > 8000)
   WHEN NOT MATCHED THEN INSERT (D.employee_id, D.bonus)
     VALUES (S.employee_id, S.salary*0.1)
     WHERE (S.salary <= 8000);

NOAUDIT DELETE ANY TABLE;

NOAUDIT SELECT TABLE BY hr; 

PURGE TABLE RB$$33750$TABLE$0;

PURGE RECYCLEBIN;

-- 30 Statements so far

RENAME temporary TO job_history; 

RENAME departments_new TO emp_departments;

REVOKE UPDATE ON hr.employees FROM oe;

REVOKE READ ON DIRECTORY bfile_dir FROM hr;

ROLLBACK;

ROLLBACK TO SAVEPOINT banda_sal; 

ROLLBACK WORK 
    FORCE '25.32.87';

SAVEPOINT banda_sal;

SET CONSTRAINT ALL IMMEDIATE;

SET CONSTRAINTS ALL IMMEDIATE;

-- 40 Statements so far

SET CONSTRAINTS emp_job_nn, emp_salary_min ,
   hr.jhist_dept_fk@remote DEFERRED;

SET ROLE dw_manager IDENTIFIED BY warehouse; 

SET ROLE ALL; 

SET ROLE NONE; 

SET TRANSACTION READ ONLY NAME 'Toronto'; 

TRUNCATE TABLE sales_demo PRESERVE MATERIALIZED VIEW LOG; 

TRUNCATE TABLE orders_demo;

TRUNCATE CLUSTER personnel REUSE STORAGE;
-- 48 Statements

