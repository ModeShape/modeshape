ALTER TABLE PersonsUnnamed ADD CHECK (P_Id>0)

ALTER TABLE PersonsNamed ADD CONSTRAINT chk_Person CHECK (P_Id>0 AND City='Sandnes')