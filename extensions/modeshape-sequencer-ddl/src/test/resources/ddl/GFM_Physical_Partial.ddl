-- Build Script
--     RDBMS           : Oracle 8.1.6
--     Generated With  :  
--     Generated On    : 2006-01-06 10:13:24
--     Generation Options
--         Generate Comments             : true
--         Generate Drop Statements      : true
--

-- Uncomment the following line for use of the logging facility (see last line also)
--spool .log

--  ----------------------------------------------------------------------------------------------------------------
--  Generate From
--    Model       : /foo/GFM.xmi
--    Model Type  : Physical
--    Metamodel   : Relational (http://www.metamatrix.com/metamodels/Relational)
--    Model UUID  : mmuuid:518a6900-6bbb-1fc5-9f13-9fdb85d04baa
--  ----------------------------------------------------------------------------------------------------------------

DROP SCHEMA GLOBALFORCEMGMT CASCADE;

-- ** NOTE: Replace "<USERID>" with the appropriate ID of the user **
-- CREATE SCHEMA GLOBALFORCEMGMT AUTHORIZATION <USERID>
CREATE SCHEMA GLOBALFORCEMGMT AUTHORIZATION GBLFORCE

-- (generated from GLOBALFORCEMGMT/ACFT_TYPE)

CREATE TABLE ACFT_TYPE
(
  ACFT_TYPE_ID      NUMERIC(20) NOT NULL,
  CAT_CODE          CHAR(6) NOT NULL,
  SUBCAT_CODE       CHAR(7),
  OWNER_ID          NUMERIC(11) NOT NULL,
  UPDATE_SEQNR      NUMERIC(15) NOT NULL,
  ENGINE_IND_CODE   CHAR(3)
)

-- (generated from GLOBALFORCEMGMT/ADDR)

CREATE TABLE ADDR
(
  ADDR_ID          NUMERIC(20) NOT NULL,
  PLACE_NAME_TXT   VARCHAR(100),
  CAT_CODE         CHAR(7) NOT NULL,
  OWNER_ID         NUMERIC(11) NOT NULL,
  UPDATE_SEQNR     NUMERIC(15) NOT NULL
)



ALTER TABLE ACFT_TYPE
  ADD CONSTRAINT PRIMARY_1
    PRIMARY KEY (ACFT_TYPE_ID)

ALTER TABLE OBJT_ESTAB_OBJT_DET_ROLE_ASSOC
  ADD CONSTRAINT DET_ROLE_ASSOC_IBFK_2
    FOREIGN KEY (ESTABD_OBJ_TYPE_ID,OBJ_TYPE_ESTAB_IX,OBJ_TYPE_ESTAB_OBJT_DET_IX)
    REFERENCES OBJ_TYPE_ESTAB_OBJT_DET(ESTABD_OBJ_TYPE_ID,OBJ_TYPE_ESTAB_IX,OBJ_TYPE_ESTAB_OBJT_DET_IX)

;


-- Uncomment the following line for use of the logging facility
--spool off

commit;
-- ==  167 STATEMENTS ============================================
