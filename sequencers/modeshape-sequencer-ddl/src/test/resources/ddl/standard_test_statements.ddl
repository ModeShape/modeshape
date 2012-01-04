CREATE SCHEMA AUTHORIZATION oe
   CREATE TABLE new_product 
      (color VARCHAR(10)  PRIMARY KEY, quantity NUMERIC) 
   CREATE VIEW new_product_view 
      AS SELECT color, quantity FROM new_product WHERE color = 'RED' 
   GRANT select ON new_product_view TO hr;

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
   
CREATE TABLE MORE_ACTIVITIES (CITY_ID INT NOT NULL,
    SEASON VARCHAR(20), ACTIVITY VARCHAR(32) NOT NULL);

CREATE TABLE PEOPLE
    (PERSON_ID INT NOT NULL CONSTRAINT PEOPLE_PK PRIMARY KEY, PERSON VARCHAR(26));
 
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
    
CREATE TABLE employee (
    empno NUMBER(4) NOT NULL, 
    empname CHAR(10), 
    job CHAR(9), 
    deptno NUMBER(2) NOT NULL,
    CONSTRAINT emp_fk1 FOREIGN KEY (deptno) REFERENCES dept (deptno) INITIALLY IMMEDIATE, 
    CONSTRAINT emp_pk PRIMARY KEY (empno));

CREATE ASSERTION assertNotNull CHECK (value != null) NOT DEFERRABLE;

CREATE ASSERTION assertIsZero CHECK (value != null and value == 0) INITIALLY DEFERRED;

CREATE ASSERTION Avgs CHECK( 3.0 < (SELECT avg(GPA) FROM Student) AND 1000 > (SELECT avg(sizeHS) FROM Student));

CREATE DOMAIN full_domain CHAR DEFAULT null;

CREATE DOMAIN partial_domain AS INTEGER DEFAULT (25);

create domain mydecimal decimal(10) default 1 check (mydecimal>=0) initially immediate not deferrable;
create domain myinteger as integer constraint myintegconstr1 check(myinteger is not null);
create domain mynchar as nchar(100) collate schema1.collation2;
create character set cs1 get WIN1250 collation from desc(collation1);
create character set cs2 as get unicode collate collation2;
create character set cs3 as get WIN1250;
create character set cs4 get WIN1250 collation from external('externcollat2');
create character set cs5 get WIN1250 collation from translation tn1 then collation collation1;
create collation collation1 for cs1 from default no pad;
create collation collation2 for cs2 from translation tn1 then collation collation1;
create collation collation3 for cs3 from external('externcollat2') pad space;
create translation tn1 for cs1 to cs2 from external('externtransl2');
create translation tn2 for cs2 to cs3 from identity;
create translation tn3 for cs3 to cs4 from tn2;
create assertion assertconstr1 check(1=(select count(*) from a)) initially immediate not deferrable;
create assertion assertconstr2 check((select max(b) from c)>0);

create schema schema_1 authorization ADM default character set UNICODE 
    create table table_1 (col1 varchar(20) not null, col2 nchar default current_user)
    create view view_1 (col1, col2) as select*from a with check option
;
create schema schema_2;
create global temporary table table_3 (col1 dec(10,2) default 0) on commit delete rows;
create local temporary table table_4 (col1 dec default null primary key) on commit preserve rows;
create table table_5 (
    col1 integer unique, col2 smallint unique,
    col3 numeric(5,0) references table_3(col1) match full on delete cascade,
    col4 numeric(5,0) references table_4(col1) match partial on delete set default on update set null,
    col5 numeric(5,0) references table_1(col1) on update no action on delete cascade check (col5<col4),
    col6 date not null check (col5<col4 and col6<=date'2003-12-31') unique,
    unique (col1,col4) initially immediate not deferrable,
    constraint pk5 primary key (col1) initially deferred,
    foreign key (col4,col5) references table_7(col2,col3) not deferrable initially immediate,
    constraint ck2 check (col5 is not null)
);
create view view_1 (col1,col2) as values (1,2);
create view view_1 (col1,col2) as values (1,2) with check option;
create view view_1 (col1,col2) as select a,b from c with local check option;
create view view_1 (col1,col2) as table c with cascaded check option;
grant all privileges on table_1 to public with grant option;
grant delete,insert(col1,col2),update, references on table table_1 to user1, user1;
grant usage on domain dom1 to user3;
grant usage on collation col1 to user4, user5;
grant usage on character set cs1 to user6;
grant usage on translation tn1 to user7;

