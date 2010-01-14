ALTER JAVA CLASS "Agent"
   RESOLVER (("/home/java.101/bin/*" pm)(* public))
   RESOLVE;
   
CREATE SERVER foo FOREIGN DATA WRAPPER "default";

CREATE RULE "_RETURN" AS
    ON SELECT TO t1
    DO INSTEAD
        SELECT * FROM t2;
