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
   
CREATE FUNCTION SecondMax (input NUMBER) RETURN NUMBER
    PARALLEL_ENABLE AGGREGATE USING SecondMaxImpl;

CREATE OR REPLACE FUNCTION text_length(a CLOB) 
   RETURN NUMBER DETERMINISTIC IS
    BEGIN 
      RETURN DBMS_LOB.GETLENGTH(a);
    END;
/
