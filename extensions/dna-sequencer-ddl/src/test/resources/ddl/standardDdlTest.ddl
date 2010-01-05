CREATE TABLE MORE_ACTIVITIES (CITY_ID INT NOT NULL,
    SEASON VARCHAR(20), ACTIVITY VARCHAR(32) NOT NULL);

CREATE SCHEMA AUTHORIZATION oe
   CREATE TABLE new_product 
      (color VARCHAR(10)  PRIMARY KEY, quantity NUMERIC) 
   CREATE VIEW new_product_view 
      AS SELECT color, quantity FROM new_product WHERE color = 'RED' 
   GRANT select ON new_product_view TO hr;
    
CREATE TABLE PEOPLE
    (PERSON_ID INT NOT NULL CONSTRAINT PEOPLE_PK PRIMARY KEY, PERSON VARCHAR(26));

CREATE SCHEMA schema_name_1
	CREATE TABLE table_name_15 (
	    column_name_1 VARCHAR(255) 
	        REFERENCES ref_table_name (ref_column_name_1) 
	        ON UPDATE NO ACTION )
	CREATE VIEW SAMP.V1 (COL_SUM, COL_DIFF)
	    AS SELECT COMM + BONUS, COMM - BONUS
	       FROM SAMP.EMPLOYEE
	CREATE TABLE table_name26 (
	    column_name_1 VARCHAR(255),
	    UNIQUE (ref_column_name_1));
      
CREATE TABLE table_name29 (
    column_name_1 VARCHAR(255),
    CONSTRAINT fk_name FOREIGN KEY (ref_column_name_1, ref_column_name_2)
        REFERENCES ref_table_name (ref_column_name_1)
        ON DELETE CASCADE ON UPDATE SET NULL
        MATCH FULL);
        
CREATE TABLE ACTIVITIES (CITY_ID INT NOT NULL,
    SEASON VARCHAR(20), ACTIVITY VARCHAR(32) NOT NULL)  

CREATE TABLE HOTELAVAILABILITY
     (HOTEL_ID INT NOT NULL, BOOKING_DATE DATE NOT NULL,
    ROOMS_TAKEN INT DEFAULT 0, PRIMARY KEY (HOTEL_ID, BOOKING_DATE));

GRANT SELECT ON TABLE purchaseOrders TO maria,harry;

GRANT UPDATE, USAGE ON TABLE purchaseOrders TO anita,zhi;

GRANT SELECT ON TABLE orders.bills to PUBLIC;

GRANT INSERT(a, b, c) ON TABLE purchaseOrders TO purchases_reader_role;



