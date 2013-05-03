CREATE VIEW myView
(
 age integer,
 name varchar
)
AS
 select * from foo;

CREATE TRIGGER ON myView INSTEAD OF INSERT AS FOR EACH ROW 
BEGIN 
 insert into myView (age, name) values (4, 'Lucas');
 insert into myView (age, name) values (6, 'Brady');
 insert into myView (age, name) values (11, 'Joshua');
END;
  
CREATE VIEW HS_VIEW OPTIONS(UPDATABLE TRUE) as select * from Accounts.HEALTHSTATE;  

CREATE TRIGGER ON HS_VIEW INSTEAD OF INSERT AS FOR EACH ROW  
BEGIN ATOMIC  
 SELECT RepHealth(New.HEALTHTIME, New.POLICYKEY, New.OBJKEY, New.HEALTHSTATE) from HS_VIEW;       
END;  

CREATE TRIGGER ON HS_VIEW INSTEAD OF UPDATE AS FOR EACH ROW  
BEGIN ATOMIC  
 SELECT RepHealth(New.HEALTHTIME, New.POLICYKEY, New.OBJKEY, New.HEALTHSTATE) from HS_VIEW;       
END;