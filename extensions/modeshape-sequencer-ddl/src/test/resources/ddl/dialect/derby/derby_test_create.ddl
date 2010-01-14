CREATE TABLE AUDITENTRIES
(
   TIMESTAMP            VARCHAR(50)            NOT NULL,
   CONTEXT              VARCHAR(64)            NOT NULL,
   ACTIVITY             VARCHAR(64)            NOT NULL,
   RESOURCES            VARCHAR(4000)          NOT NULL,
   PRINCIPAL            VARCHAR(255)            NOT NULL,
   HOSTNAME             VARCHAR(64)            NOT NULL,
   VMID                 VARCHAR(64)            NOT NULL
);
CREATE TABLE AUTHPERMISSIONS
(
   PERMISSIONUID        NUMERIC(20)            NOT NULL,
   RESOURCENAME         VARCHAR(250)           NOT NULL,
   ACTIONS              NUMERIC(10)            NOT NULL,
   CONTENTMODIFIER      VARCHAR(250),
   PERMTYPEUID          NUMERIC(10)            NOT NULL,
   REALMUID             NUMERIC(20)            NOT NULL,
   POLICYUID            NUMERIC(20)            NOT NULL,
	CONSTRAINT P_KEY_2a PRIMARY KEY (PERMISSIONUID)
);
CREATE TABLE AUTHPERMTYPES
(
   PERMTYPEUID          NUMERIC(10)            NOT NULL,
   DISPLAYNAME          VARCHAR(250)           NOT NULL,
   FACTORYCLASSNAME     VARCHAR(80)            NOT NULL,
   CONSTRAINT P_KEY_2b PRIMARY KEY (PERMTYPEUID)
);
CREATE TABLE AUTHPOLICIES
(
   POLICYUID            NUMERIC(20)            NOT NULL,
   DESCRIPTION          VARCHAR(250),
   POLICYNAME           VARCHAR(250)           NOT NULL,
   CONSTRAINT P_KEY_2c PRIMARY KEY (POLICYUID)
);
CREATE TABLE AUTHPRINCIPALS
(
   PRINCIPALTYPE        NUMERIC(10)            NOT NULL,
   PRINCIPALNAME        VARCHAR(255)           NOT NULL,
   POLICYUID            NUMERIC(20)            NOT NULL,
   GRANTOR              VARCHAR(255)           NOT NULL
);
CREATE TABLE AUTHREALMS
(
   REALMUID             NUMERIC(20)            NOT NULL,
   REALMNAME            VARCHAR(250)           NOT NULL,
   DESCRIPTION          VARCHAR(550),
   CONSTRAINT P_KEY_3a PRIMARY KEY (REALMUID)
);
CREATE TABLE CFG_STARTUP_STATE
(
   STATE                NUMERIC,
   LASTCHANGED          VARCHAR(50)
);
CREATE TABLE IDTABLE
(
   IDCONTEXT            VARCHAR(20)            NOT NULL,
   NEXTID               NUMERIC(20),
   CONSTRAINT SYS_C0083745 PRIMARY KEY (IDCONTEXT)
);
CREATE TABLE LOGENTRIES
(
   TIMESTAMP            VARCHAR(50)            NOT NULL,
   CONTEXT              VARCHAR(64)            NOT NULL,
   MSGLEVEL             NUMERIC(10)            NOT NULL,
   "EXCEPTION"          VARCHAR(4000),
   MESSAGE              VARCHAR(2000)          NOT NULL,
   HOSTNAME             VARCHAR(64)            NOT NULL,
   VMID                 VARCHAR(64)            NOT NULL,
   THREAModeShapeME           VARCHAR(64)            NOT NULL,
   VMSEQNUM             NUMERIC(7)             NOT NULL
);
CREATE TABLE LOGMESSAGETYPES
(
   MESSAGELEVEL         NUMERIC(10)            NOT NULL,
   "NAME"               VARCHAR(64)            NOT NULL,
   DISPLAYNAME          VARCHAR(64),
   CONSTRAINT P_KEY_2d PRIMARY KEY (MESSAGELEVEL)
);
CREATE TABLE MM_PRODUCTS
(
   PRODUCT_UID          NUMERIC(20)                NOT NULL,
   PRODUCT_NAME         VARCHAR(50)            NOT NULL,
   PRODUCT_DISPLAY_NM   VARCHAR(100),
   CONSTRAINT MM_PROD_UID PRIMARY KEY (PRODUCT_UID)
);
CREATE TABLE PRINCIPALTYPES
(
   PRINCIPALTYPEUID     NUMERIC(10)            NOT NULL,
   PRINCIPALTYPE        VARCHAR(60)            NOT NULL,
   DISPLAYNAME          VARCHAR(80)            NOT NULL,
   LASTCHANGEDBY        VARCHAR(255)            NOT NULL,
   LASTCHANGED          VARCHAR(50),
   CONSTRAINT P_KEY_2f PRIMARY KEY (PRINCIPALTYPEUID)
);
CREATE TABLE PRODUCTSSESSIONS
(
   PRODUCT_UID          NUMERIC(20)                NOT NULL,
   SESSION_UID          NUMERIC(20)                NOT NULL,
   CONSTRAINT SYS_C0084027 PRIMARY KEY (PRODUCT_UID, SESSION_UID)
);
CREATE TABLE RT_MDLS
(
   MDL_UID              NUMERIC(20)            NOT NULL,
   MDL_UUID             VARCHAR(64)            NOT NULL,
   MDL_NM               VARCHAR(255)           NOT NULL,
   MDL_VERSION          VARCHAR(50),
   DESCRIPTION          VARCHAR(255),
   MDL_URI              VARCHAR(255),
   MDL_TYPE             NUMERIC(3),
   IS_PHYSICAL          CHAR(1)                NOT NULL,
   MULTI_SOURCED        CHAR(1)    WITH DEFAULT '0',  
   VISIBILITY           NUMERIC(3) 
   
);
CREATE TABLE RT_MDL_PRP_NMS
(
   PRP_UID              NUMERIC(20)            NOT NULL,
   MDL_UID              NUMERIC(20)            NOT NULL,
   PRP_NM               VARCHAR(255)           NOT NULL
);
CREATE TABLE RT_MDL_PRP_VLS
(
   PRP_UID              NUMERIC(20)            NOT NULL,
   PART_ID              NUMERIC(20)            NOT NULL,
   PRP_VL               VARCHAR(255)           NOT NULL
);
CREATE TABLE RT_VDB_MDLS
(
   VDB_UID              NUMERIC(20)            NOT NULL,
   MDL_UID              NUMERIC(20)            NOT NULL,
   CNCTR_BNDNG_NM       VARCHAR(255)
);
CREATE TABLE RT_VIRTUAL_DBS
(
   VDB_UID              NUMERIC(20)            NOT NULL,
   VDB_VERSION          VARCHAR(50)            NOT NULL,
   VDB_NM               VARCHAR(255)           NOT NULL,
   DESCRIPTION          VARCHAR(255),
   PROJECT_GUID         VARCHAR(64),
   VDB_STATUS           NUMERIC                NOT NULL,
   WSDL_DEFINED         CHAR(1)  WITH DEFAULT '0',   
   VERSION_BY           VARCHAR(100),
   VERSION_DATE         VARCHAR(50)            NOT NULL,
   CREATED_BY           VARCHAR(100),
   CREATION_DATE        VARCHAR(50),
   UPDATED_BY           VARCHAR(100),
   UPDATED_DATE         VARCHAR(50),
   VDB_FILE_NM VARCHAR(2048)
);
CREATE TABLE SERVICESESSIONS
(
   SESSIONUID           NUMERIC(20)            NOT NULL,
   PRINCIPAL            VARCHAR(255)            NOT NULL,
   APPLICATION          VARCHAR(128)           NOT NULL,
   CREATIONTIME         VARCHAR(50),
   CLIENTCOUNT          NUMERIC(10)            NOT NULL,
   STATE                NUMERIC(10)            NOT NULL,
   STATETIME            VARCHAR(50),
   USESSUBSCRIBER       CHAR(1)               NOT NULL,
   PRODUCTINFO1         VARCHAR(255),
   PRODUCTINFO2         VARCHAR(255),
   PRODUCTINFO3         VARCHAR(255),
   PRODUCTINFO4         VARCHAR(255),
   CONSTRAINT P_KEY_2g PRIMARY KEY (SESSIONUID)
);

ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHPERM FOREIGN KEY (PERMTYPEUID)
      REFERENCES AUTHPERMTYPES (PERMTYPEUID)
;
ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHPLCY FOREIGN KEY (POLICYUID)
      REFERENCES AUTHPOLICIES (POLICYUID)
;
ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHRLMS FOREIGN KEY (REALMUID)
      REFERENCES AUTHREALMS (REALMUID)
;
ALTER TABLE AUTHPRINCIPALS
   ADD CONSTRAINT FK_ATHPLCY_PLCYUID FOREIGN KEY (POLICYUID)
      REFERENCES AUTHPOLICIES (POLICYUID)
;
CREATE UNIQUE INDEX AUTHPERM_UIX
    ON AUTHPERMISSIONS(POLICYUID, RESOURCENAME)
;
CREATE UNIQUE INDEX AUTHPOLICIES_NAM_U
    ON AUTHPOLICIES(POLICYNAME)
;
CREATE INDEX LOGNTRIS_MSGLVL_IX
    ON LOGENTRIES(MSGLEVEL)
;

CREATE UNIQUE INDEX PRNCIPALTYP_UIX
    ON PRINCIPALTYPES(PRINCIPALTYPE)
;
CREATE UNIQUE INDEX MDL_PRP_NMS_UIX
    ON RT_MDL_PRP_NMS(MDL_UID,PRP_NM)
;
CREATE INDEX RTMDLS_MDLNAME_IX
    ON RT_MDLS(MDL_NM)
;
CREATE INDEX RTVIRTUALDBS_NM_IX
    ON RT_VIRTUAL_DBS(VDB_NM)
;
CREATE INDEX RTVIRTULDBSVRSN_IX
    ON RT_VIRTUAL_DBS(VDB_VERSION)
;
CREATE TABLE CS_EXT_FILES  (
   FILE_UID             DECIMAL(19)                          NOT NULL,
   CHKSUM               DECIMAL(20),
   FILE_NAME            VARCHAR(255)		NOT NULL,
   FILE_CONTENTS        BLOB(1000M),
   CONFIG_CONTENTS	    CLOB(20M),
   SEARCH_POS           DECIMAL(10),
   IS_ENABLED           VARCHAR(1),
   FILE_DESC            VARCHAR(4000),
   CREATED_BY           VARCHAR(100),
   CREATION_DATE        VARCHAR(50),
   UPDATED_BY           VARCHAR(100),
   UPDATE_DATE          VARCHAR(50),
   FILE_TYPE            VARCHAR(30))
;
ALTER TABLE CS_EXT_FILES
       ADD   PRIMARY KEY (FILE_UID);
ALTER TABLE CS_EXT_FILES ADD CONSTRAINT CSEXFILS_FIL_NA_UK UNIQUE (FILE_NAME)
;


CREATE TABLE CS_SYSTEM_PROPS (
	PROPERTY_NAME VARCHAR(255),
	PROPERTY_VALUE VARCHAR(255)
)
;

CREATE UNIQUE INDEX SYSPROPS_KEY ON CS_SYSTEM_PROPS (PROPERTY_NAME);


CREATE FUNCTION SYSDATE () 
RETURNS TIMESTAMP
PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
EXTERNAL NAME 'java.lang.System.currentTimeMillis'
;
-- CREATE FUNCTION SYSDATE ()
-- RETURNS TIMESTAMP
-- LANGUAGE SQL
-- SPECIFIC SYSDATEORACLE
-- NOT DETERMINISTIC
-- CONTAINS SQL
-- NO EXTERNAL ACTION
-- RETURN
-- CURRENT TIMESTAMP
-- ;
CREATE TABLE MMSCHEMAINFO_CA
(
    SCRIPTNAME        VARCHAR(50),
    SCRIPTEXECUTEDBY  VARCHAR(50),
    SCRIPTREV         VARCHAR(50),
    RELEASEDATE       VARCHAR(50),
    DATECREATED       TIMESTAMP,
    DATEUPDATED       TIMESTAMP,
    UPDATEID          VARCHAR(50),
    METAMATRIXSERVERURL  VARCHAR(100)
)
;
CREATE TABLE CFG_LOCK (
  USER_NAME       VARCHAR(50) NOT NULL,
  DATETIME_ACQUIRED VARCHAR(50) NOT NULL,
  DATETIME_EXPIRE VARCHAR(50) NOT NULL,
  HOST       VARCHAR(100),
  LOCK_TYPE NUMERIC (1) )
;

CREATE TABLE TX_MMXCMDLOG (
REQUESTID  VARCHAR(255)  NOT NULL,
TXNUID  VARCHAR(50)  ,
CMDPOINT  NUMERIC(10)  NOT NULL,
SESSIONUID  VARCHAR(255)  NOT NULL,
APP_NAME  VARCHAR(255) ,
PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
VDBNAME  VARCHAR(255)  NOT NULL,
VDBVERSION  VARCHAR(50)  NOT NULL,
CREATED_TS  VARCHAR(50)  ,
ENDED_TS  VARCHAR(50)  ,
CMD_STATUS  NUMERIC(10)  NOT NULL,
SQL_ID  NUMERIC(10) ,
FINL_ROWCNT NUMERIC(10)
)
;
CREATE TABLE TX_SRCCMDLOG (
REQUESTID  VARCHAR(255)  NOT NULL,
NODEID  NUMERIC(10)  NOT NULL,
SUBTXNUID  VARCHAR(50)  ,
CMD_STATUS  NUMERIC(10)  NOT NULL,
MDL_NM  VARCHAR(255)  NOT NULL,
CNCTRNAME  VARCHAR(255)  NOT NULL,
CMDPOINT  NUMERIC(10)  NOT NULL,
SESSIONUID  VARCHAR(255)  NOT NULL,
PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
CREATED_TS  VARCHAR(50)  ,
ENDED_TS  VARCHAR(50)  ,
SQL_ID  NUMERIC(10)  ,
FINL_ROWCNT  NUMERIC(10)  
)
;
CREATE VIEW DUAL(C1) AS VALUES 1
;
INSERT INTO MMSCHEMAINFO_CA (SCRIPTNAME,SCRIPTEXECUTEDBY,SCRIPTREV,
RELEASEDATE, DATECREATED,DATEUPDATED, UPDATEID,METAMATRIXSERVERURL)
SELECT 'MM_CREATE.SQL',USER,'Seneca.3117', '10/03/2008 12:01 AM', CURRENT TIMESTAMP,CURRENT TIMESTAMP,'','' 
FROM DUAL;
-- COMMIT;
CREATE TABLE TX_SQL ( SQL_ID  NUMERIC(10)    NOT NULL,
    SQL_VL  CLOB(1000000) )
;
ALTER TABLE TX_SQL 
    ADD CONSTRAINT TX_SQL_PK
PRIMARY KEY (SQL_ID)
;
-- Build Script
--     RDBMS           : IBM DB2 7.x UDB
--     Generated With  : MetaMatrix MetaBase Modeler Release 3.1 SP6(Build 1672)
--     Generated On    : 2003-12-12 13:23:32
--     Generate From
--         Model       : RepositorySchema.xml
--         Model Type  : PhysicalModel
--         Metamodel   : Relational (http://www.metamatrix.com/metabase/3.0/metamodels/Relational.xml)
--         Model UUID  : mmuuid:eb6f4180-dbd4-1eab-890d-8f6b86baf96d
--     Generation Options
--         Generate Comments             : true
--         Generate Drop Statements      : true
--
--
-- The ITEMS table stores the raw, structure-independent information about the items contained by the Repository. This table is capable of persisting multiple versions of an item.
--
CREATE TABLE MBR_ITEMS
(
  ITEM_ID_P1        BIGINT NOT NULL,
  ITEM_ID_P2        BIGINT NOT NULL,
  ITEM_VERSION      VARCHAR(80) NOT NULL,
  ITEM_NAME         VARCHAR(255) NOT NULL,
  UPPER_ITEM_NAME   VARCHAR(255) NOT NULL,
  COMMENT_FLD       VARCHAR(2000),
  LOCK_HOLDER       VARCHAR(100),
  LOCK_DATE         VARCHAR(50),
  CREATED_BY        VARCHAR(100) NOT NULL,
  CREATION_DATE     VARCHAR(50) NOT NULL,
  ITEM_TYPE         NUMERIC(10) NOT NULL
);
--
-- The ITEM_CONTENTS table stores the contents for items (files) stored in the repository. This table is capable of persisting multiple versions of the contents for an item.
--
CREATE TABLE MBR_ITEM_CONTENTS
(
  ITEM_ID_P1     BIGINT NOT NULL,
  ITEM_ID_P2     BIGINT NOT NULL,
  ITEM_VERSION   VARCHAR(80) NOT NULL,
  ITEM_CONTENT   BLOB(20M) NOT NULL
);
--
-- The ENTRIES table stores the structure information for all the objects stored in the Repository. This includes both folders and items.
--
CREATE TABLE MBR_ENTRIES
(
  ENTRY_ID_P1          BIGINT NOT NULL,
  ENTRY_ID_P2          BIGINT NOT NULL,
  ENTRY_NAME           VARCHAR(255) NOT NULL,
  UPPER_ENTRY_NAME     VARCHAR(255) NOT NULL,
  ITEM_ID_P1           BIGINT,
  ITEM_ID_P2           BIGINT,
  ITEM_VERSION         VARCHAR(80),
  PARENT_ENTRY_ID_P1   BIGINT,
  PARENT_ENTRY_ID_P2   BIGINT,
  DELETED              CHAR(1) NOT NULL
);
CREATE UNIQUE INDEX MBR_ENT_NM_PNT_IX ON MBR_ENTRIES(UPPER_ENTRY_NAME,PARENT_ENTRY_ID_P1,PARENT_ENTRY_ID_P2);
CREATE INDEX MBR_ENT_PARNT_IX ON MBR_ENTRIES(PARENT_ENTRY_ID_P1);
CREATE INDEX MBR_ENT_NM_IX ON MBR_ENTRIES(UPPER_ENTRY_NAME);
CREATE INDEX MBR_ITEMS_ID_IX ON MBR_ENTRIES(ITEM_ID_P1,ITEM_ID_P2);
--
-- The LABELS table stores the various labels that have been defined.
--
CREATE TABLE MBR_LABELS
(
  LABEL_ID_P1     BIGINT NOT NULL,
  LABEL_ID_P2     BIGINT NOT NULL,
  LABEL_FLD       VARCHAR(255) NOT NULL,
  COMMENT_FLD     VARCHAR(2000),
  CREATED_BY      VARCHAR(100) NOT NULL,
  CREATION_DATE   VARCHAR(50) NOT NULL
);
--
-- The ITEM_LABELS table maintains the relationships between the ITEMS and the LABELs; that is, the labels that have been applied to each of the item versions. (This is a simple intersect table.)
--
CREATE TABLE MBR_ITEM_LABELS
(
  ITEM_ID_P1     BIGINT NOT NULL,
  ITEM_ID_P2     BIGINT NOT NULL,
  ITEM_VERSION   VARCHAR(80) NOT NULL,
  LABEL_ID_P1    BIGINT NOT NULL,
  LABEL_ID_P2    BIGINT NOT NULL
);
--
-- The ITEM_LABELS table maintains the relationships between the ITEMS and the LABELs; that is, the labels that have been applied to each of the item versions. (This is a simple intersect table.)
--
CREATE TABLE MBR_FOLDER_LABELS
(
  ENTRY_ID_P1   BIGINT NOT NULL,
  ENTRY_ID_P2   BIGINT NOT NULL,
  LABEL_ID_P1   BIGINT NOT NULL,
  LABEL_ID_P2   BIGINT NOT NULL
);
CREATE TABLE MBR_ITEM_TYPES
(
  ITEM_TYPE_CODE   NUMERIC(10) NOT NULL,
  ITEM_TYPE_NM     VARCHAR(20) NOT NULL
);
CREATE TABLE MBR_POLICIES
(
  POLICY_NAME     VARCHAR(250) NOT NULL,
  CREATION_DATE   VARCHAR(50),
  CHANGE_DATE     VARCHAR(50),
  GRANTOR         VARCHAR(32)
);
CREATE TABLE MBR_POL_PERMS
(
  ENTRY_ID_P1   BIGINT NOT NULL,
  ENTRY_ID_P2   BIGINT NOT NULL,
  POLICY_NAME   VARCHAR(250) NOT NULL,
  CREATE_BIT    CHAR(1) NOT NULL,
  READ_BIT      CHAR(1) NOT NULL,
  UPDATE_BIT    CHAR(1) NOT NULL,
  DELETE_BIT    CHAR(1) NOT NULL
);
CREATE TABLE MBR_POL_USERS
(
  POLICY_NAME   VARCHAR(250) NOT NULL,
  USER_NAME     VARCHAR(80) NOT NULL
);
ALTER TABLE MBR_ITEMS
  ADD CONSTRAINT PK_ITEMS
    PRIMARY KEY (ITEM_ID_P1,ITEM_ID_P2,ITEM_VERSION)
;
ALTER TABLE MBR_ITEM_CONTENTS
  ADD CONSTRAINT PK_ITEM_CONTENTS
    PRIMARY KEY (ITEM_ID_P1,ITEM_ID_P2,ITEM_VERSION)
;
ALTER TABLE MBR_ENTRIES
  ADD CONSTRAINT PK_ENTRIES
    PRIMARY KEY (ENTRY_ID_P1,ENTRY_ID_P2)
;
ALTER TABLE MBR_LABELS
  ADD CONSTRAINT PK_LABELS
    PRIMARY KEY (LABEL_ID_P1,LABEL_ID_P2)
;
ALTER TABLE MBR_ITEM_LABELS
  ADD CONSTRAINT PK_ITEM_LABELS
    PRIMARY KEY (ITEM_ID_P1,ITEM_ID_P2,ITEM_VERSION,LABEL_ID_P1,LABEL_ID_P2)
;
ALTER TABLE MBR_FOLDER_LABELS
  ADD CONSTRAINT PK_FOLDER_LABELS
    PRIMARY KEY (ENTRY_ID_P1,ENTRY_ID_P2,LABEL_ID_P1,LABEL_ID_P2)
;
ALTER TABLE MBR_POLICIES
  ADD CONSTRAINT PK_POLICIES
    PRIMARY KEY (POLICY_NAME)
;
ALTER TABLE MBR_POL_PERMS
  ADD CONSTRAINT PK_POL_PERMS
    PRIMARY KEY (ENTRY_ID_P1,ENTRY_ID_P2,POLICY_NAME)
;
ALTER TABLE MBR_POL_USERS
  ADD CONSTRAINT PK_POL_USERS
    PRIMARY KEY (POLICY_NAME,USER_NAME)
;
-- (generated from DtcBase/ObjectIndex)
CREATE TABLE DD_INDEX
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  NME                  VARCHAR(1000),
  DETAIL_TBLE_NME      VARCHAR(20) NOT NULL,
  VIRT_DETL_TBLE_NME   VARCHAR(128) NOT NULL,
  MDL_LGCL_ID          VARCHAR(1000),
  MDL_UUID1            BIGINT NOT NULL,
  MDL_UUID2            BIGINT NOT NULL,
  MDL_UUID_STRING      VARCHAR(44) NOT NULL,
  MTACLS_TYPE_ID       BIGINT NOT NULL,
  PARENT_LGCL_ID       VARCHAR(1000),
  PARENT_UUID1         BIGINT,
  PARENT_UUID2         BIGINT,
  PARENT_UUID_STRING   VARCHAR(44),
  TXN_ID               BIGINT NOT NULL
);
-- (generated from DtcBase/Models)
CREATE TABLE DD_MDL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NME           VARCHAR(128) NOT NULL,
  TXN_ID        BIGINT NOT NULL,
  VRSION        VARCHAR(20) NOT NULL
);
-- (generated from DtcBase/Metaclasses)
CREATE TABLE DD_MTACLS_TYPE
(
  ID           BIGINT NOT NULL,
  MTACLS_URI   VARCHAR(300) NOT NULL,
  MTAMDL_ID    BIGINT NOT NULL,
  DSPLY_NME    VARCHAR(128) NOT NULL
);
-- (generated from DtcBase/Metamodels)
CREATE TABLE DD_MTAMDL
(
  ID          BIGINT NOT NULL,
  URI         VARCHAR(256) NOT NULL,
  DSPLY_NME   VARCHAR(128) NOT NULL
);
-- (generated from DtcBase/TransactionLog)
CREATE TABLE DD_TXN_LOG
(
  ID          BIGINT NOT NULL,
  USER_NME    VARCHAR(128),
  BEGIN_TXN   VARCHAR(50),
  END_TXN     VARCHAR(50),
  ACTION      VARCHAR(128),
  TXN_STATE   INTEGER
);
-- (generated from DtcBase/Relationships)
CREATE TABLE DD_RELATIONSHIPS
(
  REFERRER_LGCL_ID       VARCHAR(1000),
  REFERRER_UUID1         BIGINT NOT NULL,
  REFERRER_UUID2         BIGINT NOT NULL,
  REFERRER_UUID_STRING   VARCHAR(44) NOT NULL,
  REFEREE_LGCL_ID        VARCHAR(1000),
  REFEREE_UUID1          BIGINT NOT NULL,
  REFEREE_UUID2          BIGINT NOT NULL,
  REFEREE_UUID_STRING    VARCHAR(44) NOT NULL,
  REFEREE_POSITION       INTEGER NOT NULL,
  REL_FTRE_ID            BIGINT NOT NULL,
  TXN_ID                 BIGINT NOT NULL
);
-- (generated from DtcBase/ModelMtamdlIntersect)
CREATE TABLE DD_MDL_MTAMDL
(
  MDL_LGCL_ID       VARCHAR(1000),
  MDL_UUID1         BIGINT NOT NULL,
  MDL_UUID2         BIGINT NOT NULL,
  MDL_UUID_STRING   VARCHAR(44) NOT NULL,
  MTAMDL_ID         BIGINT NOT NULL,
  TXN_ID            BIGINT NOT NULL
);
-- (generated from DtcBase/MetaclassFeatures)
CREATE TABLE DD_FTRE
(
  ID                BIGINT NOT NULL,
  MTACLS_FTRE_URI   VARCHAR(300) NOT NULL,
  MTACLS_TYPE_ID    BIGINT NOT NULL,
  MTAMDL_ID         BIGINT NOT NULL
);
-- (generated from DtcBase/TransactionStates)
CREATE TABLE DD_TXN_STATES
(
  ID      INTEGER NOT NULL,
  STATE   VARCHAR(128) NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: PushDownType
-- (generated from Function/PushDownType)
CREATE TABLE DD_METM_PUSHDWNTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ScalarFunction
-- (generated from Function/ScalarFunction)
CREATE TABLE DD_METM_SCALRFNCTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  CATGRY        VARCHAR(256),
  PUSHDWN       BIGINT,
  INVCTNCLSS    VARCHAR(256),
  INVCTNMTHD    VARCHAR(256),
  DETRMNSTC     CHAR(1),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FunctionParameter
-- (generated from Function/FunctionParameter)
CREATE TABLE DD_METM_FUNCTNPRMT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  TYP           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ReturnParameter
-- (generated from Function/ReturnParameter)
CREATE TABLE DD_METM_RETRNPRMTR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TYP           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: Severity
-- (generated from Manifest/Severity)
CREATE TABLE DD_VIRT_SEVRTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: ModelAccessibility
-- (generated from Manifest/ModelAccessibility)
CREATE TABLE DD_VIRT_MODLCCSSBL
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: VirtualDatabase
-- (generated from Manifest/VirtualDatabase)
CREATE TABLE DD_VIRT_VIRTLDTBS
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  SEVRTY           BIGINT,
  NAM              VARCHAR(256),
  IDENTFR          VARCHAR(256),
  UUID             VARCHAR(256),
  DESCRPTN         CLOB(1000000),
  VERSN            VARCHAR(256),
  PROVDR           VARCHAR(256),
  TIMLSTCHNGD      VARCHAR(256),
  TIMLSTPRDCD      VARCHAR(256),
  TIMLSTCHNGDSDT   VARCHAR(128),
  TIMLSTPRDCDSDT   VARCHAR(128),
  PRODCRNM         VARCHAR(256),
  PRODCRVRSN       VARCHAR(256),
  INCLDMDLFLS      CHAR(1),
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ModelReference
-- (generated from Manifest/ModelReference)
CREATE TABLE DD_VIRT_MODLRFRNC
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  NAM                  VARCHAR(256),
  PATH                 VARCHAR(256),
  MODLLCTN             VARCHAR(256),
  UUID                 VARCHAR(256),
  MODLTYP              BIGINT,
  PRIMRYMTMDLR         VARCHAR(256),
  SEVRTY               BIGINT,
  VERSN                VARCHAR(256),
  URI                  VARCHAR(512),
  VISBL                CHAR(1),
  ACCSSBLTY            BIGINT,
  TIMLSTSYNCHRNZD      VARCHAR(256),
  TIMLSTSYNCHRNZDSDT   VARCHAR(256),
  CHECKSM              BIGINT,
  USES                 BIGINT,
  USEDBY               BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProblemMarker
-- (generated from Manifest/ProblemMarker)
CREATE TABLE DD_VIRT_PROBLMMRKR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  SEVRTY        BIGINT,
  MESSG         VARCHAR(256),
  TARGT         VARCHAR(256),
  TARGTR        VARCHAR(256),  
  COD           BIGINT,
  STACKTRC      CLOB(1000000),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ModelSource
-- (generated from Manifest/ModelSource)
CREATE TABLE DD_VIRT_MODLSRC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ModelSourceProperty
-- (generated from Manifest/ModelSourceProperty)
CREATE TABLE DD_VIRT_MODLSRCPRP
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  VAL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: WsdlOptions
-- (generated from Manifest/WsdlOptions)
CREATE TABLE DD_VIRT_WSDLPTNS
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TARGTNMSPCR   VARCHAR(256),
  DEFLTNMSPCR   VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: NonModelReference
-- (generated from Manifest/NonModelReference)
CREATE TABLE DD_VIRT_NONMDLRFRN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  PATH          VARCHAR(256),
  CHECKSM       BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: SoapEncoding
-- (generated from Xml/SoapEncoding)
CREATE TABLE DD_XMLD_SOAPNCDNG
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: ChoiceErrorMode
-- (generated from Xml/ChoiceErrorMode)
CREATE TABLE DD_XMLD_CHOCRRRMD
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: ValueType
-- (generated from Xml/ValueType)
CREATE TABLE DD_XMLD_VALTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: BuildStatus
-- (generated from Xml/BuildStatus)
CREATE TABLE DD_XMLD_BUILDSTTS
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: NormalizationType
-- (generated from Xml/NormalizationType)
CREATE TABLE DD_XMLD_NORMLZTNTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlFragment
-- (generated from Xml/XmlFragment)
CREATE TABLE DD_XMLD_XMLFRGMNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlDocument
-- (generated from Xml/XmlDocument)
CREATE TABLE DD_XMLD_XMLDCMNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ENCDNG        VARCHAR(256),
  FORMTTD       CHAR(1),
  VERSN         VARCHAR(256),
  STANDLN       CHAR(1),
  SOAPNCDNG     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlElement
-- (generated from Xml/XmlElement)
CREATE TABLE DD_XMLD_XMLLMNT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  BUILDSTT        BIGINT,
  NAM             VARCHAR(256),
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  NAMSPC          BIGINT,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  VAL             VARCHAR(256),
  VALTYP          BIGINT,
  RECRSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlAttribute
-- (generated from Xml/XmlAttribute)
CREATE TABLE DD_XMLD_XMLTTRBT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  BUILDSTT        BIGINT,
  NAM             VARCHAR(256),
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  NAMSPC          BIGINT,
  VAL             VARCHAR(256),
  VALTYP          BIGINT,
  USE1            BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlRoot
-- (generated from Xml/XmlRoot)
CREATE TABLE DD_XMLD_XMLRT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  BUILDSTT        BIGINT,
  NAM             VARCHAR(256),
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  NAMSPC          BIGINT,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  VAL             VARCHAR(256),
  VALTYP          BIGINT,
  RECRSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlComment
-- (generated from Xml/XmlComment)
CREATE TABLE DD_XMLD_XMLCMMNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TEXT          VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlNamespace
-- (generated from Xml/XmlNamespace)
CREATE TABLE DD_XMLD_XMLNMSPC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  PREFX         VARCHAR(256),
  URI           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlSequence
-- (generated from Xml/XmlSequence)
CREATE TABLE DD_XMLD_XMLSQNC
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  BUILDSTT        BIGINT,
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlAll
-- (generated from Xml/XmlAll)
CREATE TABLE DD_XMLD_XMLLL
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  BUILDSTT        BIGINT,
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlChoice
-- (generated from Xml/XmlChoice)
CREATE TABLE DD_XMLD_XMLCHC
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  BUILDSTT        BIGINT,
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  DEFLTRRRMD      BIGINT,
  DEFLTPTN        BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProcessingInstruction
-- (generated from Xml/ProcessingInstruction)
CREATE TABLE DD_XMLD_PROCSSNGNS
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  RAWTXT        VARCHAR(256),
  TARGT         VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XmlFragmentUse
-- (generated from Xml/XmlFragmentUse)
CREATE TABLE DD_XMLD_XMLFRGMNTS
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  BUILDSTT        BIGINT,
  NAM             VARCHAR(256),
  EXCLDFRMDCMNT   CHAR(1),
  MINCCRS         BIGINT,
  MAXCCRS         BIGINT,
  XSDCMPNNT       BIGINT,
  NAMSPC          BIGINT,
  CHOCCRTR        VARCHAR(256),
  CHOCRDR         BIGINT,
  DEFLTFR         BIGINT,
  FRAGMNT         BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EAttribute
-- (generated from Ecore/EAttribute)
CREATE TABLE DD_ECOR_EATTRBT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  MANY          CHAR(1),
  REQRD         CHAR(1),
  ETYP          BIGINT,
  CHANGBL       CHAR(1),
  VOLTL         CHAR(1),
  TRANSNT       CHAR(1),
  DEFLTVLLTRL   VARCHAR(256),
  DEFLTVL       VARCHAR(256),
  UNSTTBL       CHAR(1),
  DERVD         CHAR(1),
  ID            CHAR(1),
  EATTRBTTYP    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EAnnotation
-- (generated from Ecore/EAnnotation)
CREATE TABLE DD_ECOR_EANNTTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  SOURC         VARCHAR(256),
  REFRNCS       BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EClass
-- (generated from Ecore/EClass)
CREATE TABLE DD_ECOR_ECLSS
(
  LGCL_ID           VARCHAR(1000),
  UUID1             BIGINT NOT NULL,
  UUID2             BIGINT NOT NULL,
  UUID_STRING       VARCHAR(44) NOT NULL,
  NAM               VARCHAR(256),
  INSTNCCLSSNM      VARCHAR(256),
  INSTNCCLSS        VARCHAR(256),
  DEFLTVL           VARCHAR(256),
  ABSTRCT           CHAR(1),
  INTRFC            CHAR(1),
  ESUPRTYPS         BIGINT,
  EALLTTRBTS        BIGINT,
  EALLRFRNCS        BIGINT,
  EREFRNCS          BIGINT,
  EATTRBTS          BIGINT,
  EALLCNTNMNTS      BIGINT,
  EALLPRTNS         BIGINT,
  EALLSTRCTRLFTRS   BIGINT,
  EALLSPRTYPS       BIGINT,
  EIDTTRBT          BIGINT,
  TXN_ID            BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EDataType
-- (generated from Ecore/EDataType)
CREATE TABLE DD_ECOR_EDATTYP
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  INSTNCCLSSNM   VARCHAR(256),
  INSTNCCLSS     VARCHAR(256),
  DEFLTVL        VARCHAR(256),
  SERLZBL        CHAR(1),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EEnum
-- (generated from Ecore/EEnum)
CREATE TABLE DD_ECOR_EENM
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  INSTNCCLSSNM   VARCHAR(256),
  INSTNCCLSS     VARCHAR(256),
  DEFLTVL        VARCHAR(256),
  SERLZBL        CHAR(1),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EEnumLiteral
-- (generated from Ecore/EEnumLiteral)
CREATE TABLE DD_ECOR_EENMLTRL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  VAL           BIGINT,
  INSTNC        VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EFactory
-- (generated from Ecore/EFactory)
CREATE TABLE DD_ECOR_EFACTRY
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  EPACKG        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EObject
-- (generated from Ecore/EObject)
CREATE TABLE DD_ECOR_EOBJCT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EOperation
-- (generated from Ecore/EOperation)
CREATE TABLE DD_ECOR_EOPRTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  MANY          CHAR(1),
  REQRD         CHAR(1),
  ETYP          BIGINT,
  EEXCPTNS      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EPackage
-- (generated from Ecore/EPackage)
CREATE TABLE DD_ECOR_EPACKG
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  NSUR           VARCHAR(256),
  NSPRFX         VARCHAR(256),
  EFACTRYNSTNC   BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EParameter
-- (generated from Ecore/EParameter)
CREATE TABLE DD_ECOR_EPARMTR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  MANY          CHAR(1),
  REQRD         CHAR(1),
  ETYP          BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EReference
-- (generated from Ecore/EReference)
CREATE TABLE DD_ECOR_EREFRNC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  MANY          CHAR(1),
  REQRD         CHAR(1),
  ETYP          BIGINT,
  CHANGBL       CHAR(1),
  VOLTL         CHAR(1),
  TRANSNT       CHAR(1),
  DEFLTVLLTRL   VARCHAR(256),
  DEFLTVL       VARCHAR(256),
  UNSTTBL       CHAR(1),
  DERVD         CHAR(1),
  CONTNMNT      CHAR(1),
  CONTNR        CHAR(1),
  RESLVPRXS     CHAR(1),
  EOPPST        BIGINT,
  EREFRNCTYP    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EStringToStringMapEntry
-- (generated from Ecore/EStringToStringMapEntry)
CREATE TABLE DD_ECOR_ESTRNGTSTR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  KEY1          VARCHAR(256),
  VAL           CLOB(1000000),
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: VisibilityKind
-- (generated from Uml2/VisibilityKind)
CREATE TABLE DD_UML_VISBLTYKND
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: ParameterDirectionKind
-- (generated from Uml2/ParameterDirectionKind)
CREATE TABLE DD_UML_PARMTRDRCTN
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: AggregationKind
-- (generated from Uml2/AggregationKind)
CREATE TABLE DD_UML_AGGRGTNKND
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: CallConcurrencyKind
-- (generated from Uml2/CallConcurrencyKind)
CREATE TABLE DD_UML_CALLCNCRRNC
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: ParameterEffectKind
-- (generated from Uml2/ParameterEffectKind)
CREATE TABLE DD_UML_PARMTRFFCTK
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Comment
-- (generated from Uml2/Comment)
CREATE TABLE DD_UML_COMMNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  BODY          VARCHAR(256),
  ANNTTDLMNT    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Class
-- (generated from Uml2/Class)
CREATE TABLE DD_UML_CLASS
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  CLASSFRBHVR          BIGINT,
  PART                 BIGINT,
  ROL                  BIGINT,
  SUPRCLSS             BIGINT,
  EXTNSN               BIGINT,
  ISACTV               VARCHAR(256),
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Property
-- (generated from Uml2/Property)
CREATE TABLE DD_UML_PROPRTY
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  OWNDLMNT        BIGINT,
  OWNR            BIGINT,
  NAM             VARCHAR(256),
  QUALFDNM        VARCHAR(256),
  VISBLTY         BIGINT,
  CLINTDPNDNCY    BIGINT,
  REDFNTNCNTXT    BIGINT,
  ISLF            VARCHAR(256),
  FEATRNGCLSSFR   BIGINT,
  ISSTTC          VARCHAR(256),
  TYP             BIGINT,
  ISORDRD         VARCHAR(256),
  ISUNQ           VARCHAR(256),
  LOWR            VARCHAR(256),
  UPPR            VARCHAR(256),
  ISRDNLY         VARCHAR(256),
  TEMPLTPRMTR     BIGINT,
  END1            BIGINT,
  DEPLYDLMNT      BIGINT,
  DEFLT           VARCHAR(256),
  ISCMPST         VARCHAR(256),
  ISDRVD          VARCHAR(256),
  CLASS_          BIGINT,
  OPPST           BIGINT,
  ISDRVDNN        VARCHAR(256),
  REDFNDPRPRTY    BIGINT,
  SUBSTTDPRPRTY   BIGINT,
  ASSCTN          BIGINT,
  AGGRGTN         BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Operation
-- (generated from Uml2/Operation)
CREATE TABLE DD_UML_OPERTN
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  OWNDLMNT        BIGINT,
  OWNR            BIGINT,
  NAM             VARCHAR(256),
  QUALFDNM        VARCHAR(256),
  VISBLTY         BIGINT,
  CLINTDPNDNCY    BIGINT,
  MEMBR           BIGINT,
  IMPRTDMMBR      BIGINT,
  REDFNTNCNTXT    BIGINT,
  ISLF            VARCHAR(256),
  FEATRNGCLSSFR   BIGINT,
  ISSTTC          VARCHAR(256),
  PARMTR          BIGINT,
  RAISDXCPTN      BIGINT,
  ISABSTRCT       VARCHAR(256),
  METHD           BIGINT,
  CONCRRNCY       BIGINT,
  TYP             BIGINT,
  ISORDRD         VARCHAR(256),
  ISUNQ           VARCHAR(256),
  LOWR            VARCHAR(256),
  UPPR            VARCHAR(256),
  TEMPLTPRMTR     BIGINT,
  ISQRY           VARCHAR(256),
  PRECNDTN        BIGINT,
  POSTCNDTN       BIGINT,
  REDFNDPRTN      BIGINT,
  BODYCNDTN       BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Parameter
-- (generated from Uml2/Parameter)
CREATE TABLE DD_UML_PARMTR
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TEMPLTPRMTR    BIGINT,
  END1           BIGINT,
  TYP            BIGINT,
  ISORDRD        VARCHAR(256),
  ISUNQ          VARCHAR(256),
  LOWR           VARCHAR(256),
  UPPR           VARCHAR(256),
  DEFLT          VARCHAR(256),
  DIRCTN         BIGINT,
  ISEXCPTN       VARCHAR(256),
  ISSTRM         VARCHAR(256),
  EFFCT          BIGINT,
  PARMTRST       BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Package
-- (generated from Uml2/Package)
CREATE TABLE DD_UML_PACKG
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  NESTDPCKG            BIGINT,
  NESTNGPCKG           BIGINT,
  OWNDTYP              BIGINT,
  APPLDPRFL            BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Enumeration
-- (generated from Uml2/Enumeration)
CREATE TABLE DD_UML_ENUMRTN
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DataType
-- (generated from Uml2/DataType)
CREATE TABLE DD_UML_DATTYP
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: EnumerationLiteral
-- (generated from Uml2/EnumerationLiteral)
CREATE TABLE DD_UML_ENUMRTNLTRL
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  DEPLYDLMNT           BIGINT,
  CLASSFR              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PrimitiveType
-- (generated from Uml2/PrimitiveType)

CREATE TABLE DD_UML_PRIMTVTYP
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Constraint
-- (generated from Uml2/Constraint)
CREATE TABLE DD_UML_CONSTRNT
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  CONTXT               BIGINT,
  CONSTRNDLMNT         BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LiteralBoolean
-- (generated from Uml2/LiteralBoolean)
CREATE TABLE DD_UML_LITRLBLN
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  VAL            VARCHAR(256),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LiteralString
-- (generated from Uml2/LiteralString)
CREATE TABLE DD_UML_LITRLSTRNG
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  VAL            VARCHAR(256),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LiteralNull
-- (generated from Uml2/LiteralNull)
CREATE TABLE DD_UML_LITRLNLL
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LiteralInteger
-- (generated from Uml2/LiteralInteger)
CREATE TABLE DD_UML_LITRLNTGR
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  VAL            VARCHAR(256),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LiteralUnlimitedNatural
-- (generated from Uml2/LiteralUnlimitedNatural)
CREATE TABLE DD_UML_LITRLNLMTDN
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  VAL            VARCHAR(256),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: InstanceSpecification
-- (generated from Uml2/InstanceSpecification)
CREATE TABLE DD_UML_INSTNCSPCFC
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  DEPLYDLMNT           BIGINT,
  CLASSFR              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Slot
-- (generated from Uml2/Slot)
CREATE TABLE DD_UML_SLOT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  DEFNNGFTR     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Generalization
-- (generated from Uml2/Generalization)
CREATE TABLE DD_UML_GENRLZTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  RELTDLMNT     BIGINT,
  SOURC         BIGINT,
  TARGT         BIGINT,
  GENRL         BIGINT,
  ISSBSTTTBL    VARCHAR(256),
  GENRLZTNST    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ElementImport
-- (generated from Uml2/ElementImport)
CREATE TABLE DD_UML_ELEMNTMPRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  RELTDLMNT     BIGINT,
  SOURC         BIGINT,
  TARGT         BIGINT,
  VISBLTY       BIGINT,
  ALIS          VARCHAR(256),
  IMPRTDLMNT    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PackageImport
-- (generated from Uml2/PackageImport)
CREATE TABLE DD_UML_PACKGMPRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  RELTDLMNT     BIGINT,
  SOURC         BIGINT,
  TARGT         BIGINT,
  VISBLTY       BIGINT,
  IMPRTDPCKG    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Association
-- (generated from Uml2/Association)
CREATE TABLE DD_UML_ASSCTN
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  RELTDLMNT            BIGINT,
  ISDRVD               VARCHAR(256),
  ENDTYP               BIGINT,
  MEMBRND              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PackageMerge
-- (generated from Uml2/PackageMerge)
CREATE TABLE DD_UML_PACKGMRG
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  RELTDLMNT     BIGINT,
  SOURC         BIGINT,
  TARGT         BIGINT,
  MERGDPCKG     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Stereotype
-- (generated from Uml2/Stereotype)
CREATE TABLE DD_UML_STERTYP
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  CLASSFRBHVR          BIGINT,
  PART                 BIGINT,
  ROL                  BIGINT,
  SUPRCLSS             BIGINT,
  EXTNSN               BIGINT,
  ISACTV               VARCHAR(256),
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Profile
-- (generated from Uml2/Profile)
CREATE TABLE DD_UML_PROFL
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  NESTDPCKG            BIGINT,
  NESTNGPCKG           BIGINT,
  OWNDTYP              BIGINT,
  APPLDPRFL            BIGINT,
  OWNDSTRTYP           BIGINT,
  METCLSSRFRNC         BIGINT,
  METMDLRFRNC          BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProfileApplication
-- (generated from Uml2/ProfileApplication)
CREATE TABLE DD_UML_PROFLPPLCTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  RELTDLMNT     BIGINT,
  SOURC         BIGINT,
  TARGT         BIGINT,
  VISBLTY       BIGINT,
  IMPRTDPCKG    BIGINT,
  IMPRTDPRFL    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Extension
-- (generated from Uml2/Extension)
CREATE TABLE DD_UML_EXTNSN
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  RELTDLMNT            BIGINT,
  ISDRVD               VARCHAR(256),
  ENDTYP               BIGINT,
  MEMBRND              BIGINT,
  ISRQRD               VARCHAR(256),
  METCLSS              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ExtensionEnd
-- (generated from Uml2/ExtensionEnd)
CREATE TABLE DD_UML_EXTNSNND
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  OWNDLMNT        BIGINT,
  OWNR            BIGINT,
  NAM             VARCHAR(256),
  QUALFDNM        VARCHAR(256),
  VISBLTY         BIGINT,
  CLINTDPNDNCY    BIGINT,
  REDFNTNCNTXT    BIGINT,
  ISLF            VARCHAR(256),
  FEATRNGCLSSFR   BIGINT,
  ISSTTC          VARCHAR(256),
  TYP             BIGINT,
  ISORDRD         VARCHAR(256),
  ISUNQ           VARCHAR(256),
  LOWR            VARCHAR(256),
  UPPR            VARCHAR(256),
  ISRDNLY         VARCHAR(256),
  TEMPLTPRMTR     BIGINT,
  END1            BIGINT,
  DEPLYDLMNT      BIGINT,
  DEFLT           VARCHAR(256),
  ISCMPST         VARCHAR(256),
  ISDRVD          VARCHAR(256),
  CLASS_          BIGINT,
  OPPST           BIGINT,
  ISDRVDNN        VARCHAR(256),
  REDFNDPRPRTY    BIGINT,
  SUBSTTDPRPRTY   BIGINT,
  ASSCTN          BIGINT,
  AGGRGTN         BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Dependency
-- (generated from Uml2/Dependency)
CREATE TABLE DD_UML_DEPNDNCY
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  RELTDLMNT            BIGINT,
  SOURC                BIGINT,
  TARGT                BIGINT,
  CLINT                BIGINT,
  SUPPLR               BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: GeneralizationSet
-- (generated from Uml2/GeneralizationSet)
CREATE TABLE DD_UML_GENRLZTNST
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  ISCVRNG              VARCHAR(256),
  ISDSJNT              VARCHAR(256),
  POWRTYP              BIGINT,
  GENRLZTN             BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: AssociationClass
-- (generated from Uml2/AssociationClass)
CREATE TABLE DD_UML_ASSCTNCLSS
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  CLASSFRBHVR          BIGINT,
  PART                 BIGINT,
  ROL                  BIGINT,
  SUPRCLSS             BIGINT,
  EXTNSN               BIGINT,
  ISACTV               VARCHAR(256),
  RELTDLMNT            BIGINT,
  ISDRVD               VARCHAR(256),
  ENDTYP               BIGINT,
  MEMBRND              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Model
-- (generated from Uml2/Model)
CREATE TABLE DD_UML_MODL
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  NESTDPCKG            BIGINT,
  NESTNGPCKG           BIGINT,
  OWNDTYP              BIGINT,
  APPLDPRFL            BIGINT,
  VIEWPNT              VARCHAR(256),
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Interface
-- (generated from Uml2/Interface)
CREATE TABLE DD_UML_INTRFC
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  OWNDLMNT             BIGINT,
  OWNR                 BIGINT,
  NAM                  VARCHAR(256),
  QUALFDNM             VARCHAR(256),
  VISBLTY              BIGINT,
  CLINTDPNDNCY         BIGINT,
  MEMBR                BIGINT,
  IMPRTDMMBR           BIGINT,
  TEMPLTPRMTR          BIGINT,
  PACKGBLLMNT_VSBLTY   BIGINT,
  PACKG                BIGINT,
  REDFNTNCNTXT         BIGINT,
  ISLF                 VARCHAR(256),
  FEATR                BIGINT,
  ISABSTRCT            VARCHAR(256),
  INHRTDMMBR           BIGINT,
  GENRL                BIGINT,
  ATTRBT               BIGINT,
  REDFNDCLSSFR         BIGINT,
  POWRTYPXTNT          BIGINT,
  USECS                BIGINT,
  REPRSNTTN            BIGINT,
  REDFNDNTRFC          BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: StringExpression
-- (generated from Uml2/StringExpression)
CREATE TABLE DD_UML_STRNGXPRSSN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OWNDLMNT      BIGINT,
  OWNR          BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DurationInterval
-- (generated from Uml2/DurationInterval)
CREATE TABLE DD_UML_DURTNNTRVL
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  "MIN"          BIGINT,
  "MAX"          BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TimeInterval
-- (generated from Uml2/TimeInterval)
CREATE TABLE DD_UML_TIMNTRVL
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  TYP            BIGINT,
  TEMPLTPRMTR    BIGINT,
  "MIN"          BIGINT,
  "MAX"          BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ParameterSet
-- (generated from Uml2/ParameterSet)
CREATE TABLE DD_UML_PARMTRST
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  OWNDLMNT       BIGINT,
  OWNR           BIGINT,
  NAM            VARCHAR(256),
  QUALFDNM       VARCHAR(256),
  VISBLTY        BIGINT,
  CLINTDPNDNCY   BIGINT,
  PARMTR         BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: CaseConversion
-- (generated from Jdbc/CaseConversion)
CREATE TABLE DD_JDBC_CASCNVRSN
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: SourceNames
-- (generated from Jdbc/SourceNames)
CREATE TABLE DD_JDBC_SOURCNMS
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcSourceProperty
-- (generated from Jdbc/JdbcSourceProperty)
CREATE TABLE DD_JDBC_JDBCSRCPRP
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  VAL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcDriver
-- (generated from Jdbc/JdbcDriver)
CREATE TABLE DD_JDBC_JDBCDRVR
(
  LGCL_ID             VARCHAR(1000),
  UUID1               BIGINT NOT NULL,
  UUID2               BIGINT NOT NULL,
  UUID_STRING         VARCHAR(44) NOT NULL,
  NAM                 VARCHAR(256),
  URLSYNTX            VARCHAR(256),
  JARFLRS             VARCHAR(256),
  AVALBLDRVRCLSSNMS   VARCHAR(256),
  PREFRRDDRVRCLSSNM   VARCHAR(256),
  TXN_ID              BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcSource
-- (generated from Jdbc/JdbcSource)
CREATE TABLE DD_JDBC_JDBCSRC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  JDBCDRVR      BIGINT,
  NAM           VARCHAR(256),
  DRIVRNM       VARCHAR(256),
  DRIVRCLSS     VARCHAR(256),
  USERNM        VARCHAR(256),
  URL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcDriverContainer
-- (generated from Jdbc/JdbcDriverContainer)
CREATE TABLE DD_JDBC_JDBCDRVRCN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcSourceContainer
-- (generated from Jdbc/JdbcSourceContainer)
CREATE TABLE DD_JDBC_JDBCSRCCNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcImportSettings
-- (generated from Jdbc/JdbcImportSettings)
CREATE TABLE DD_JDBC_JDBCMPRTST
(
  LGCL_ID           VARCHAR(1000),
  UUID1             BIGINT NOT NULL,
  UUID2             BIGINT NOT NULL,
  UUID_STRING       VARCHAR(44) NOT NULL,
  CRETCTLGSNMDL     CHAR(1),
  CRETSCHMSNMDL     CHAR(1),
  CONVRTCSNMDL      BIGINT,
  GENRTSRCNMSNMDL   BIGINT,
  INCLDDCTLGPTHS    VARCHAR(256),
  INCLDDSCHMPTHS    VARCHAR(256),
  EXCLDDBJCTPTHS    VARCHAR(256),
  INCLDFRGNKYS      CHAR(1),
  INCLDNDXS         CHAR(1),
  INCLDPRCDRS       CHAR(1),
  INCLDPPRXMTNDXS   CHAR(1),
  INCLDNQNDXS       CHAR(1),
  INCLDDTBLTYPS     VARCHAR(256),
  TXN_ID            BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JdbcImportOptions
-- (generated from Jdbc/JdbcImportOptions)
CREATE TABLE DD_JDBC_JDBCMPRTPT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  VAL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: ModelType
-- (generated from Core/ModelType)
CREATE TABLE DD_COR_MODLTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Annotation
-- (generated from Core/Annotation)
CREATE TABLE DD_COR_ANNTTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  DESCRPTN      CLOB(1000000),
  KEYWRDS       VARCHAR(256),
  ANNTTDBJCT    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: AnnotationContainer
-- (generated from Core/AnnotationContainer)
CREATE TABLE DD_COR_ANNTTNCNTNR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ModelAnnotation
-- (generated from Core/ModelAnnotation)
CREATE TABLE DD_COR_MODLNNTTN
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  DESCRPTN        CLOB(1000000),
  NAMNSRC         VARCHAR(256),
  PRIMRYMTMDLR    VARCHAR(256),
  MODLTYP         BIGINT,
  MAXSTSZ         BIGINT,
  VISBL           CHAR(1),
  SUPPRTSDSTNCT   CHAR(1),
  SUPPRTSJN       CHAR(1),
  SUPPRTSRDRBY    CHAR(1),
  SUPPRTSTRJN     CHAR(1),
  SUPPRTSWHRLL    CHAR(1),
  NAMSPCR         VARCHAR(256),
  PRODCRNM        VARCHAR(256),
  PRODCRVRSN      VARCHAR(256),
  EXTNSNPCKG      BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Link
-- (generated from Core/Link)
CREATE TABLE DD_COR_LINK
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  DESCRPTN      CLOB(1000000),
  REFRNCS       VARCHAR(256),
  LINKDBJCTS    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LinkContainer
-- (generated from Core/LinkContainer)
CREATE TABLE DD_COR_LINKCNTNR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ModelImport
-- (generated from Core/ModelImport)
CREATE TABLE DD_COR_MODLMPRT
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  PATH           VARCHAR(256),
  MODLLCTN       VARCHAR(256),
  UUID           VARCHAR(256),
  MODLTYP        BIGINT,
  PRIMRYMTMDLR   VARCHAR(256),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XClass
-- (generated from Extension/XClass)
CREATE TABLE DD_EXTN_XCLSS
(
  LGCL_ID           VARCHAR(1000),
  UUID1             BIGINT NOT NULL,
  UUID2             BIGINT NOT NULL,
  UUID_STRING       VARCHAR(44) NOT NULL,
  NAM               VARCHAR(256),
  INSTNCCLSSNM      VARCHAR(256),
  INSTNCCLSS        VARCHAR(256),
  DEFLTVL           VARCHAR(256),
  ABSTRCT           CHAR(1),
  INTRFC            CHAR(1),
  ESUPRTYPS         BIGINT,
  EALLTTRBTS        BIGINT,
  EALLRFRNCS        BIGINT,
  EREFRNCS          BIGINT,
  EATTRBTS          BIGINT,
  EALLCNTNMNTS      BIGINT,
  EALLPRTNS         BIGINT,
  EALLSTRCTRLFTRS   BIGINT,
  EALLSPRTYPS       BIGINT,
  EIDTTRBT          BIGINT,
  EXTNDDCLSS        BIGINT,
  TXN_ID            BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XPackage
-- (generated from Extension/XPackage)
CREATE TABLE DD_EXTN_XPACKG
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  NSUR           VARCHAR(256),
  NSPRFX         VARCHAR(256),
  EFACTRYNSTNC   BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XAttribute
-- (generated from Extension/XAttribute)
CREATE TABLE DD_EXTN_XATTRBT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  MANY          CHAR(1),
  REQRD         CHAR(1),
  ETYP          BIGINT,
  CHANGBL       CHAR(1),
  VOLTL         CHAR(1),
  TRANSNT       CHAR(1),
  DEFLTVLLTRL   VARCHAR(256),
  DEFLTVL       VARCHAR(256),
  UNSTTBL       CHAR(1),
  DERVD         CHAR(1),
  ID            CHAR(1),
  EATTRBTTYP    BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XEnum
-- (generated from Extension/XEnum)
CREATE TABLE DD_EXTN_XENM
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  INSTNCCLSSNM   VARCHAR(256),
  INSTNCCLSS     VARCHAR(256),
  DEFLTVL        VARCHAR(256),
  SERLZBL        CHAR(1),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XEnumLiteral
-- (generated from Extension/XEnumLiteral)
CREATE TABLE DD_EXTN_XENMLTRL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  VAL           BIGINT,
  INSTNC        VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: XSDAttributeUseCategory
-- (generated from Xsd/XSDAttributeUseCategory)
CREATE TABLE DD_XSD_XSDTTRBTSCT
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDCardinality
-- (generated from Xsd/XSDCardinality)
CREATE TABLE DD_XSD_XSDCRDNLTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDComplexFinal
-- (generated from Xsd/XSDComplexFinal)
CREATE TABLE DD_XSD_XSDCMPLXFNL
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDCompositor
-- (generated from Xsd/XSDCompositor)
CREATE TABLE DD_XSD_XSDCMPSTR
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDConstraint
-- (generated from Xsd/XSDConstraint)
CREATE TABLE DD_XSD_XSDCNSTRNT
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDContentTypeCategory
-- (generated from Xsd/XSDContentTypeCategory)
CREATE TABLE DD_XSD_XSDCNTNTTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDDerivationMethod
-- (generated from Xsd/XSDDerivationMethod)
CREATE TABLE DD_XSD_XSDDRVTNMTH
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDDiagnosticSeverity
-- (generated from Xsd/XSDDiagnosticSeverity)
CREATE TABLE DD_XSD_XSDDGNSTCSV
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDDisallowedSubstitutions
-- (generated from Xsd/XSDDisallowedSubstitutions)
CREATE TABLE DD_XSD_XSDDSLLWDSB
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDForm
-- (generated from Xsd/XSDForm)
CREATE TABLE DD_XSD_XSDFRM
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDIdentityConstraintCategory
-- (generated from Xsd/XSDIdentityConstraintCategory)
CREATE TABLE DD_XSD_XSDDNTTYCNS
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDNamespaceConstraintCategory
-- (generated from Xsd/XSDNamespaceConstraintCategory)
CREATE TABLE DD_XSD_XSDNMSPCCNS
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDOrdered
-- (generated from Xsd/XSDOrdered)
CREATE TABLE DD_XSD_XSDRDRD
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDProcessContents
-- (generated from Xsd/XSDProcessContents)
CREATE TABLE DD_XSD_XSDPRCSSCNT
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDProhibitedSubstitutions
-- (generated from Xsd/XSDProhibitedSubstitutions)
CREATE TABLE DD_XSD_XSDPRHBTDSB
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDSimpleFinal
-- (generated from Xsd/XSDSimpleFinal)
CREATE TABLE DD_XSD_XSDSMPLFNL
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDSubstitutionGroupExclusions
-- (generated from Xsd/XSDSubstitutionGroupExclusions)
CREATE TABLE DD_XSD_XSDSBSTTTNG
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDVariety
-- (generated from Xsd/XSDVariety)
CREATE TABLE DD_XSD_XSDVRTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDWhiteSpace
-- (generated from Xsd/XSDWhiteSpace)
CREATE TABLE DD_XSD_XSDWHTSPC
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: XSDXPathVariety
-- (generated from Xsd/XSDXPathVariety)
CREATE TABLE DD_XSD_XSDXPTHVRTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDAnnotation
-- (generated from Xsd/XSDAnnotation)
CREATE TABLE DD_XSD_XSDNNTTN
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  APPLCTNNFRMTN   CLOB(1000000),
  USERNFRMTN      CLOB(1000000),
  ATTRBTS         CLOB(1000000),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDAttributeDeclaration
-- (generated from Xsd/XSDAttributeDeclaration)
CREATE TABLE DD_XSD_XSDTTRBTDCL
(
  LGCL_ID             VARCHAR(1000),
  UUID1               BIGINT NOT NULL,
  UUID2               BIGINT NOT NULL,
  UUID_STRING         VARCHAR(44) NOT NULL,
  ELEMNT              CLOB(1000000),
  CONTNR              BIGINT,
  ROOTCNTNR           BIGINT,
  SCHM                BIGINT,
  NAM                 VARCHAR(256),
  TARGTNMSPC          VARCHAR(256),
  ALISNM              VARCHAR(256),
  URI                 VARCHAR(256),
  ALISR               VARCHAR(256),
  QNAM                VARCHAR(256),
  VAL                 CLOB(1000000),
  CONSTRNT            BIGINT,
  FORM                BIGINT,
  LEXCLVL             VARCHAR(256),
  GLOBL               CHAR(1),
  FEATRRFRNC          CHAR(1),
  SCOP                BIGINT,
  RESLVDFTR           BIGINT,
  TYP                 BIGINT,
  ATTRBTDCLRTNRFRNC   CHAR(1),
  TYPDFNTN            BIGINT,
  RESLVDTTRBTDCLRTN   BIGINT,
  TXN_ID              BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDAttributeGroupDefinition
-- (generated from Xsd/XSDAttributeGroupDefinition)
CREATE TABLE DD_XSD_XSDTTRBTGRP
(
  LGCL_ID               VARCHAR(1000),
  UUID1                 BIGINT NOT NULL,
  UUID2                 BIGINT NOT NULL,
  UUID_STRING           VARCHAR(44) NOT NULL,
  ELEMNT                CLOB(1000000),
  CONTNR                BIGINT,
  ROOTCNTNR             BIGINT,
  SCHM                  BIGINT,
  NAM                   VARCHAR(256),
  TARGTNMSPC            VARCHAR(256),
  ALISNM                VARCHAR(256),
  URI                   VARCHAR(256),
  ALISR                 VARCHAR(256),
  QNAM                  VARCHAR(256),
  CIRCLR                CHAR(1),
  ATTRBTGRPDFNTNRFRNC   CHAR(1),
  ATTRBTSS              BIGINT,
  ATTRBTWLDCRD          BIGINT,
  RESLVDTTRBTGRPDFNTN   BIGINT,
  TXN_ID                BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDAttributeUse
-- (generated from Xsd/XSDAttributeUse)
CREATE TABLE DD_XSD_XSDTTRBTS
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  ELEMNT         CLOB(1000000),
  CONTNR         BIGINT,
  ROOTCNTNR      BIGINT,
  SCHM           BIGINT,
  REQRD          CHAR(1),
  VAL            CLOB(1000000),
  CONSTRNT       BIGINT,
  USE1           BIGINT,
  LEXCLVL        VARCHAR(256),
  ATTRBTDCLRTN   BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDBoundedFacet
-- (generated from Xsd/XSDBoundedFacet)
CREATE TABLE DD_XSD_XSDBNDDFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  VAL             CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDCardinalityFacet
-- (generated from Xsd/XSDCardinalityFacet)
CREATE TABLE DD_XSD_XSDCRDNLTYF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDComplexTypeDefinition
-- (generated from Xsd/XSDComplexTypeDefinition)

CREATE TABLE DD_XSD_XSDCMPLXTYP
(
  LGCL_ID           VARCHAR(1000),
  UUID1             BIGINT NOT NULL,
  UUID2             BIGINT NOT NULL,
  UUID_STRING       VARCHAR(44) NOT NULL,
  ELEMNT            CLOB(1000000),
  CONTNR            BIGINT,
  ROOTCNTNR         BIGINT,
  SCHM              BIGINT,
  NAM               VARCHAR(256),
  TARGTNMSPC        VARCHAR(256),
  ALISNM            VARCHAR(256),
  URI               VARCHAR(256),
  ALISR             VARCHAR(256),
  QNAM              VARCHAR(256),
  CIRCLR            CHAR(1),
  ANNTTNS           BIGINT,
  ROOTTYP           BIGINT,
  BASTYP            BIGINT,
  SIMPLTYP          BIGINT,
  COMPLXTYP         BIGINT,
  DERVTNMTHD        BIGINT,
  FINL              BIGINT,
  ABSTRCT           CHAR(1),
  CONTNTTYPCTGRY    BIGINT,
  PROHBTDSBSTTTNS   BIGINT,
  LEXCLFNL          BIGINT,
  BLOCK             BIGINT,
  MIXD              CHAR(1),
  BASTYPDFNTN       BIGINT,
  CONTNTTYP         BIGINT,
  ATTRBTSS          BIGINT,
  ATTRBTWLDCRD      BIGINT,
  ROOTTYPDFNTN      BIGINT,
  TXN_ID            BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDDiagnostic
-- (generated from Xsd/XSDDiagnostic)
CREATE TABLE DD_XSD_XSDDGNSTC
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  ELEMNT         CLOB(1000000),
  CONTNR         BIGINT,
  ROOTCNTNR      BIGINT,
  SCHM           BIGINT,
  SEVRTY         BIGINT,
  MESSG          VARCHAR(256),
  LOCTNR         VARCHAR(256),
  LIN            BIGINT,
  COLMN          BIGINT,
  NOD            CLOB(1000000),
  ANNTTNR        VARCHAR(256),
  COMPNNTS       BIGINT,
  PRIMRYCMPNNT   BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDElementDeclaration
-- (generated from Xsd/XSDElementDeclaration)
CREATE TABLE DD_XSD_XSDLMNTDCLR
(
  LGCL_ID             VARCHAR(1000),
  UUID1               BIGINT NOT NULL,
  UUID2               BIGINT NOT NULL,
  UUID_STRING         VARCHAR(44) NOT NULL,
  ELEMNT              CLOB(1000000),
  CONTNR              BIGINT,
  ROOTCNTNR           BIGINT,
  SCHM                BIGINT,
  NAM                 VARCHAR(256),
  TARGTNMSPC          VARCHAR(256),
  ALISNM              VARCHAR(256),
  URI                 VARCHAR(256),
  ALISR               VARCHAR(256),
  QNAM                VARCHAR(256),
  VAL                 CLOB(1000000),
  CONSTRNT            BIGINT,
  FORM                BIGINT,
  LEXCLVL             VARCHAR(256),
  GLOBL               CHAR(1),
  FEATRRFRNC          CHAR(1),
  SCOP                BIGINT,
  RESLVDFTR           BIGINT,
  TYP                 BIGINT,
  NILLBL              CHAR(1),
  DISLLWDSBSTTTNS     BIGINT,
  SUBSTTTNGRPXCLSNS   BIGINT,
  ABSTRCT             CHAR(1),
  LEXCLFNL            BIGINT,
  BLOCK               BIGINT,
  ELEMNTDCLRTNRFRNC   CHAR(1),
  CIRCLR              CHAR(1),
  TYPDFNTN            BIGINT,
  RESLVDLMNTDCLRTN    BIGINT,
  SUBSTTTNGRPFFLTN    BIGINT,
  SUBSTTTNGRP         BIGINT,
  TXN_ID              BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDEnumerationFacet
-- (generated from Xsd/XSDEnumerationFacet)
CREATE TABLE DD_XSD_XSDNMRTNFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  ANNTTNS         BIGINT,
  VAL             CLOB(1000000),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDFractionDigitsFacet
-- (generated from Xsd/XSDFractionDigitsFacet)
CREATE TABLE DD_XSD_XSDFRCTNDGT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDIdentityConstraintDefinition
-- (generated from Xsd/XSDIdentityConstraintDefinition)
CREATE TABLE DD_XSD_XSDDNTTYC_1
(
  LGCL_ID               VARCHAR(1000),
  UUID1                 BIGINT NOT NULL,
  UUID2                 BIGINT NOT NULL,
  UUID_STRING           VARCHAR(44) NOT NULL,
  ELEMNT                CLOB(1000000),
  CONTNR                BIGINT,
  ROOTCNTNR             BIGINT,
  SCHM                  BIGINT,
  NAM                   VARCHAR(256),
  TARGTNMSPC            VARCHAR(256),
  ALISNM                VARCHAR(256),
  URI                   VARCHAR(256),
  ALISR                 VARCHAR(256),
  QNAM                  VARCHAR(256),
  IDENTTYCNSTRNTCTGRY   BIGINT,
  REFRNCDKY             BIGINT,
  TXN_ID                BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDImport
-- (generated from Xsd/XSDImport)
CREATE TABLE DD_XSD_XSDMPRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ELEMNT        CLOB(1000000),
  CONTNR        BIGINT,
  ROOTCNTNR     BIGINT,
  SCHM          BIGINT,
  SCHMLCTN      VARCHAR(256),
  RESLVDSCHM    BIGINT,
  NAMSPC        VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDInclude
-- (generated from Xsd/XSDInclude)
CREATE TABLE DD_XSD_XSDNCLD
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  ELEMNT         CLOB(1000000),
  CONTNR         BIGINT,
  ROOTCNTNR      BIGINT,
  SCHM           BIGINT,
  SCHMLCTN       VARCHAR(256),
  RESLVDSCHM     BIGINT,
  INCRPRTDSCHM   BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDLengthFacet
-- (generated from Xsd/XSDLengthFacet)
CREATE TABLE DD_XSD_XSDLNGTHFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMaxExclusiveFacet
-- (generated from Xsd/XSDMaxExclusiveFacet)
CREATE TABLE DD_XSD_XSDMXXCLSVF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             CLOB(1000000),
  INCLSV          CHAR(1),
  EXCLSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMaxInclusiveFacet
-- (generated from Xsd/XSDMaxInclusiveFacet)
CREATE TABLE DD_XSD_XSDMXNCLSVF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             CLOB(1000000),
  INCLSV          CHAR(1),
  EXCLSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMaxLengthFacet
-- (generated from Xsd/XSDMaxLengthFacet)
CREATE TABLE DD_XSD_XSDMXLNGTHF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMinExclusiveFacet
-- (generated from Xsd/XSDMinExclusiveFacet)
CREATE TABLE DD_XSD_XSDMNXCLSVF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             CLOB(1000000),
  INCLSV          CHAR(1),
  EXCLSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMinInclusiveFacet
-- (generated from Xsd/XSDMinInclusiveFacet)
CREATE TABLE DD_XSD_XSDMNNCLSVF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             CLOB(1000000),
  INCLSV          CHAR(1),
  EXCLSV          CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDMinLengthFacet
-- (generated from Xsd/XSDMinLengthFacet)
CREATE TABLE DD_XSD_XSDMNLNGTHF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDModelGroup
-- (generated from Xsd/XSDModelGroup)
CREATE TABLE DD_XSD_XSDMDLGRP
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ELEMNT        CLOB(1000000),
  CONTNR        BIGINT,
  ROOTCNTNR     BIGINT,
  SCHM          BIGINT,
  COMPSTR       BIGINT,
  PARTCLS       BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDModelGroupDefinition
-- (generated from Xsd/XSDModelGroupDefinition)
CREATE TABLE DD_XSD_XSDMDLGRPDF
(
  LGCL_ID             VARCHAR(1000),
  UUID1               BIGINT NOT NULL,
  UUID2               BIGINT NOT NULL,
  UUID_STRING         VARCHAR(44) NOT NULL,
  ELEMNT              CLOB(1000000),
  CONTNR              BIGINT,
  ROOTCNTNR           BIGINT,
  SCHM                BIGINT,
  NAM                 VARCHAR(256),
  TARGTNMSPC          VARCHAR(256),
  ALISNM              VARCHAR(256),
  URI                 VARCHAR(256),
  ALISR               VARCHAR(256),
  QNAM                VARCHAR(256),
  CIRCLR              CHAR(1),
  MODLGRPDFNTNRFRNC   CHAR(1),
  RESLVDMDLGRPDFNTN   BIGINT,
  TXN_ID              BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDNotationDeclaration
-- (generated from Xsd/XSDNotationDeclaration)
CREATE TABLE DD_XSD_XSDNTTNDCLR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ELEMNT        CLOB(1000000),
  CONTNR        BIGINT,
  ROOTCNTNR     BIGINT,
  SCHM          BIGINT,
  NAM           VARCHAR(256),
  TARGTNMSPC    VARCHAR(256),
  ALISNM        VARCHAR(256),
  URI           VARCHAR(256),
  ALISR         VARCHAR(256),
  QNAM          VARCHAR(256),
  SYSTMDNTFR    VARCHAR(256),
  PUBLCDNTFR    VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDNumericFacet
-- (generated from Xsd/XSDNumericFacet)
CREATE TABLE DD_XSD_XSDNMRCFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  VAL             CHAR(1),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDOrderedFacet
-- (generated from Xsd/XSDOrderedFacet)
CREATE TABLE DD_XSD_XSDRDRDFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDParticle
-- (generated from Xsd/XSDParticle)
CREATE TABLE DD_XSD_XSDPRTCL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ELEMNT        CLOB(1000000),
  CONTNR        BIGINT,
  ROOTCNTNR     BIGINT,
  SCHM          BIGINT,
  MINCCRS       BIGINT,
  MAXCCRS       BIGINT,
  TERM          BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDPatternFacet
-- (generated from Xsd/XSDPatternFacet)
CREATE TABLE DD_XSD_XSDPTTRNFCT
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  ANNTTNS         BIGINT,
  VAL             CLOB(1000000),
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDRedefine
-- (generated from Xsd/XSDRedefine)
CREATE TABLE DD_XSD_XSDRDFN
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  ELEMNT         CLOB(1000000),
  CONTNR         BIGINT,
  ROOTCNTNR      BIGINT,
  SCHM           BIGINT,
  SCHMLCTN       VARCHAR(256),
  RESLVDSCHM     BIGINT,
  INCRPRTDSCHM   BIGINT,
  ANNTTNS        BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDSchema
-- (generated from Xsd/XSDSchema)
CREATE TABLE DD_XSD_XSDSCHM
(
  LGCL_ID                VARCHAR(1000),
  UUID1                  BIGINT NOT NULL,
  UUID2                  BIGINT NOT NULL,
  UUID_STRING            VARCHAR(44) NOT NULL,
  ELEMNT                 CLOB(1000000),
  CONTNR                 BIGINT,
  ROOTCNTNR              BIGINT,
  SCHM                   BIGINT,
  DOCMNT                 CLOB(1000000),
  SCHMLCTN               VARCHAR(256),
  TARGTNMSPC             VARCHAR(256),
  ATTRBTFRMDFLT          BIGINT,
  ELEMNTFRMDFLT          BIGINT,
  FINLDFLT               BIGINT,
  BLOCKDFLT              BIGINT,
  VERSN                  VARCHAR(256),
  ELEMNTDCLRTNS          BIGINT,
  ATTRBTDCLRTNS          BIGINT,
  ATTRBTGRPDFNTNS        BIGINT,
  TYPDFNTNS              BIGINT,
  MODLGRPDFNTNS          BIGINT,
  IDENTTYCNSTRNTDFNTNS   BIGINT,
  NOTTNDCLRTNS           BIGINT,
  ANNTTNS                BIGINT,
  ALLDGNSTCS             BIGINT,
  REFRNCNGDRCTVS         BIGINT,
  ROOTVRSN               BIGINT,
  ORIGNLVRSN             BIGINT,
  SCHMFRSCHM             BIGINT,
  TXN_ID                 BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDSimpleTypeDefinition
-- (generated from Xsd/XSDSimpleTypeDefinition)
CREATE TABLE DD_XSD_XSDSMPLTYPD
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  ELEMNT               CLOB(1000000),
  CONTNR               BIGINT,
  ROOTCNTNR            BIGINT,
  SCHM                 BIGINT,
  NAM                  VARCHAR(256),
  TARGTNMSPC           VARCHAR(256),
  ALISNM               VARCHAR(256),
  URI                  VARCHAR(256),
  ALISR                VARCHAR(256),
  QNAM                 VARCHAR(256),
  CIRCLR               CHAR(1),
  ANNTTNS              BIGINT,
  ROOTTYP              BIGINT,
  BASTYP               BIGINT,
  SIMPLTYP             BIGINT,
  COMPLXTYP            BIGINT,
  VARTY                BIGINT,
  FINL                 BIGINT,
  LEXCLFNL             BIGINT,
  VALDFCTS             VARCHAR(256),
  FACTS                BIGINT,
  MEMBRTYPDFNTNS       BIGINT,
  BASTYPDFNTN          BIGINT,
  PRIMTVTYPDFNTN       BIGINT,
  ITEMTYPDFNTN         BIGINT,
  ROOTTYPDFNTN         BIGINT,
  MINFCT               BIGINT,
  MAXFCT               BIGINT,
  MAXNCLSVFCT          BIGINT,
  MINNCLSVFCT          BIGINT,
  MINXCLSVFCT          BIGINT,
  MAXXCLSVFCT          BIGINT,
  LENGTHFCT            BIGINT,
  WHITSPCFCT           BIGINT,
  ENUMRTNFCTS          BIGINT,
  PATTRNFCTS           BIGINT,
  CARDNLTYFCT          BIGINT,
  NUMRCFCT             BIGINT,
  MAXLNGTHFCT          BIGINT,
  MINLNGTHFCT          BIGINT,
  TOTLDGTSFCT          BIGINT,
  FRACTNDGTSFCT        BIGINT,
  ORDRDFCT             BIGINT,
  BOUNDDFCT            BIGINT,
  EFFCTVMXFCT          BIGINT,
  EFFCTVWHTSPCFCT      BIGINT,
  EFFCTVMXLNGTHFCT     BIGINT,
  EFFCTVFRCTNDGTSFCT   BIGINT,
  EFFCTVPTTRNFCT       BIGINT,
  EFFCTVNMRTNFCT       BIGINT,
  EFFCTVTTLDGTSFCT     BIGINT,
  EFFCTVMNLNGTHFCT     BIGINT,
  EFFCTVLNGTHFCT       BIGINT,
  EFFCTVMNFCT          BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDTotalDigitsFacet
-- (generated from Xsd/XSDTotalDigitsFacet)
CREATE TABLE DD_XSD_XSDTTLDGTSF
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDWhiteSpaceFacet
-- (generated from Xsd/XSDWhiteSpaceFacet)
CREATE TABLE DD_XSD_XSDWHTSPCFC
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  ELEMNT          CLOB(1000000),
  CONTNR          BIGINT,
  ROOTCNTNR       BIGINT,
  SCHM            BIGINT,
  LEXCLVL         CLOB(1000000),
  FACTNM          VARCHAR(256),
  EFFCTVVL        CLOB(1000000),
  SIMPLTYPDFNTN   BIGINT,
  FIXD            CHAR(1),
  VAL             BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDWildcard
-- (generated from Xsd/XSDWildcard)
CREATE TABLE DD_XSD_XSDWLDCRD
(
  LGCL_ID              VARCHAR(1000),
  UUID1                BIGINT NOT NULL,
  UUID2                BIGINT NOT NULL,
  UUID_STRING          VARCHAR(44) NOT NULL,
  ELEMNT               CLOB(1000000),
  CONTNR               BIGINT,
  ROOTCNTNR            BIGINT,
  SCHM                 BIGINT,
  NAMSPCCNSTRNTCTGRY   BIGINT,
  NAMSPCCNSTRNT        VARCHAR(256),
  PROCSSCNTNTS         BIGINT,
  LEXCLNMSPCCNSTRNT    VARCHAR(256),
  ANNTTNS              BIGINT,
  TXN_ID               BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: XSDXPathDefinition
-- (generated from Xsd/XSDXPathDefinition)
CREATE TABLE DD_XSD_XSDXPTHDFNT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ELEMNT        CLOB(1000000),
  CONTNR        BIGINT,
  ROOTCNTNR     BIGINT,
  SCHM          BIGINT,
  VARTY         BIGINT,
  VAL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: RecursionErrorMode
-- (generated from Transformation/RecursionErrorMode)
CREATE TABLE DD_TRAN_RECRSNRRRM
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: JoinType
-- (generated from Transformation/JoinType)
CREATE TABLE DD_TRAN_JOINTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: SortDirection
-- (generated from Transformation/SortDirection)
CREATE TABLE DD_TRAN_SORTDRCTN
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TransformationContainer
-- (generated from Transformation/TransformationContainer)
CREATE TABLE DD_TRAN_TRANSFRMTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SqlTransformation
-- (generated from Transformation/SqlTransformation)
CREATE TABLE DD_TRAN_SQLTRNSFRM
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  HELPDBJCT      BIGINT,  
  SELCTSQL       CLOB(1000000),
  INSRTSQL       CLOB(1000000),
  UPDTSQL        CLOB(1000000),
  DELTSQL        CLOB(1000000),
  INSRTLLWD      CHAR(1),
  UPDTLLWD       CHAR(1),
  DELTLLWD       CHAR(1),
  OUTPTLCKD      CHAR(1),
  INSRTSQLDFLT   CHAR(1),
  UPDTSQLDFLT    CHAR(1),
  DELTSQLDFLT    CHAR(1),
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TransformationMapping
-- (generated from Transformation/TransformationMapping)
CREATE TABLE DD_TRAN_TRANSFRM_1
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SqlAlias
-- (generated from Transformation/SqlAlias)
CREATE TABLE DD_TRAN_SQLLS
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  ALIS          VARCHAR(256),
  ALISDBJCT     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SqlTransformationMappingRoot
-- (generated from Transformation/SqlTransformationMappingRoot)
CREATE TABLE DD_TRAN_SQLTRNSF_1
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  OUTPTRDNLY    CHAR(1),
  TOPTBTTM      CHAR(1),
  COMMNDSTCK    VARCHAR(256),
  TARGT         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FragmentMappingRoot
-- (generated from Transformation/FragmentMappingRoot)
CREATE TABLE DD_TRAN_FRAGMNTMPP
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  OUTPTRDNLY    CHAR(1),
  TOPTBTTM      CHAR(1),
  COMMNDSTCK    VARCHAR(256),
  TARGT         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TreeMappingRoot
-- (generated from Transformation/TreeMappingRoot)
CREATE TABLE DD_TRAN_TREMPPNGRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  OUTPTRDNLY    CHAR(1),
  TOPTBTTM      CHAR(1),
  COMMNDSTCK    VARCHAR(256),
  TARGT         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingClass
-- (generated from Transformation/MappingClass)
CREATE TABLE DD_TRAN_MAPPNGCLSS
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  NAM              VARCHAR(256),
  RECRSV           CHAR(1),
  RECRSNLLWD       CHAR(1),
  RECRSNCRTR       VARCHAR(256),
  RECRSNLMT        BIGINT,
  RECRSNLMTRRRMD   BIGINT,
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingClassColumn
-- (generated from Transformation/MappingClassColumn)
CREATE TABLE DD_TRAN_MAPPNGCL_1
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: StagingTable
-- (generated from Transformation/StagingTable)
CREATE TABLE DD_TRAN_STAGNGTBL
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  NAM              VARCHAR(256),
  RECRSV           CHAR(1),
  RECRSNLLWD       CHAR(1),
  RECRSNCRTR       VARCHAR(256),
  RECRSNLMT        BIGINT,
  RECRSNLMTRRRMD   BIGINT,
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingClassSet
-- (generated from Transformation/MappingClassSet)
CREATE TABLE DD_TRAN_MAPPNGCL_2
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TARGT         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingClassSetContainer
-- (generated from Transformation/MappingClassSetContainer)
CREATE TABLE DD_TRAN_MAPPNGCL_3
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: InputParameter
-- (generated from Transformation/InputParameter)
CREATE TABLE DD_TRAN_INPTPRMTR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: InputSet
-- (generated from Transformation/InputSet)
CREATE TABLE DD_TRAN_INPTST
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: InputBinding
-- (generated from Transformation/InputBinding)
CREATE TABLE DD_TRAN_INPTBNDNG
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  INPTPRMTR        BIGINT,
  MAPPNGCLSSCLMN   BIGINT,
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DataFlowMappingRoot
-- (generated from Transformation/DataFlowMappingRoot)
CREATE TABLE DD_TRAN_DATFLWMPPN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  OUTPTRDNLY    CHAR(1),
  TOPTBTTM      CHAR(1),
  COMMNDSTCK    VARCHAR(256),
  TARGT         BIGINT,
  ALLWSPTMZTN   CHAR(1),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DataFlowNode
-- (generated from Transformation/DataFlowNode)
CREATE TABLE DD_TRAN_DATFLWND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DataFlowLink
-- (generated from Transformation/DataFlowLink)
CREATE TABLE DD_TRAN_DATFLWLNK
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  OUTPTND       BIGINT,
  INPTND        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Expression
-- (generated from Transformation/Expression)
CREATE TABLE DD_TRAN_EXPRSSN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  VAL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TargetNode
-- (generated from Transformation/TargetNode)
CREATE TABLE DD_TRAN_TARGTND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TARGT         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SourceNode
-- (generated from Transformation/SourceNode)
CREATE TABLE DD_TRAN_SOURCND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  SOURC         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: OperationNodeGroup
-- (generated from Transformation/OperationNodeGroup)
CREATE TABLE DD_TRAN_OPERTNNDGR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: OperationNode
-- (generated from Transformation/OperationNode)
CREATE TABLE DD_TRAN_OPERTNND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: JoinNode
-- (generated from Transformation/JoinNode)
CREATE TABLE DD_TRAN_JOINND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: UnionNode
-- (generated from Transformation/UnionNode)
CREATE TABLE DD_TRAN_UNINND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProjectionNode
-- (generated from Transformation/ProjectionNode)
CREATE TABLE DD_TRAN_PROJCTNND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FilterNode
-- (generated from Transformation/FilterNode)
CREATE TABLE DD_TRAN_FILTRND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: GroupingNode
-- (generated from Transformation/GroupingNode)
CREATE TABLE DD_TRAN_GROPNGND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: DupRemovalNode
-- (generated from Transformation/DupRemovalNode)
CREATE TABLE DD_TRAN_DUPRMVLND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SortNode
-- (generated from Transformation/SortNode)
CREATE TABLE DD_TRAN_SORTND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SqlNode
-- (generated from Transformation/SqlNode)
CREATE TABLE DD_TRAN_SQLND
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  INPTLNKS      BIGINT,
  OUTPTLNKS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: NullableType
-- (generated from Relational/NullableType)
CREATE TABLE DD_RELT_NULLBLTYP
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: DirectionKind
-- (generated from Relational/DirectionKind)
CREATE TABLE DD_RELT_DIRCTNKND
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: MultiplicityKind
-- (generated from Relational/MultiplicityKind)
CREATE TABLE DD_RELT_MULTPLCTYK
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The model class name for the set enumeration values held in this table is: SearchabilityType
-- (generated from Relational/SearchabilityType)
CREATE TABLE DD_RELT_SEARCHBLTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Column
-- (generated from Relational/Column)
CREATE TABLE DD_RELT_COLMN
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  NAMNSRC        VARCHAR(256),
  NATVTYP        VARCHAR(256),
  LENGTH         BIGINT,
  FIXDLNGTH      CHAR(1),
  PRECSN         BIGINT,
  SCAL           BIGINT,
  NULLBL         BIGINT,
  AUTNCRMNTD     CHAR(1),
  DEFLTVL        VARCHAR(256),
  MINMMVL        VARCHAR(256),
  MAXMMVL        VARCHAR(256),
  FORMT          VARCHAR(256),
  CHARCTRSTNM    VARCHAR(256),
  COLLTNNM       VARCHAR(256),
  SELCTBL        CHAR(1),
  UPDTBL         CHAR(1),
  CASSNSTV       CHAR(1),
  SEARCHBLTY     BIGINT,
  CURRNCY        CHAR(1),
  RADX           BIGINT,
  SIGND          CHAR(1),
  DISTNCTVLCNT   BIGINT,
  NULLVLCNT      BIGINT,
  UNIQKYS        BIGINT,
  INDXS          BIGINT,
  FORGNKYS       BIGINT,
  ACCSSPTTRNS    BIGINT,
  TYP            BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Schema
-- (generated from Relational/Schema)
CREATE TABLE DD_RELT_SCHM
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PrimaryKey
-- (generated from Relational/PrimaryKey)
CREATE TABLE DD_RELT_PRIMRYKY
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  FORGNKYS      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ForeignKey
-- (generated from Relational/ForeignKey)
CREATE TABLE DD_RELT_FORGNKY
(
  LGCL_ID            VARCHAR(1000),
  UUID1              BIGINT NOT NULL,
  UUID2              BIGINT NOT NULL,
  UUID_STRING        VARCHAR(44) NOT NULL,
  NAM                VARCHAR(256),
  NAMNSRC            VARCHAR(256),
  FORGNKYMLTPLCTY    BIGINT,
  PRIMRYKYMLTPLCTY   BIGINT,
  COLMNS             BIGINT,
  UNIQKY             BIGINT,
  TXN_ID             BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: View
-- (generated from Relational/View)
CREATE TABLE DD_RELT_VIEW
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  NAM             VARCHAR(256),
  NAMNSRC         VARCHAR(256),
  SYSTM           CHAR(1),
  CARDNLTY        BIGINT,
  SUPPRTSPDT      CHAR(1),
  MATRLZD         CHAR(1),
  LOGCLRLTNSHPS   BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Catalog
-- (generated from Relational/Catalog)
CREATE TABLE DD_RELT_CATLG
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Procedure
-- (generated from Relational/Procedure)
CREATE TABLE DD_RELT_PROCDR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  FUNCTN        CHAR(1),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Index
-- (generated from Relational/Index)
CREATE TABLE DD_RELT_INDX
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  FILTRCNDTN    VARCHAR(256),
  NULLBL        CHAR(1),
  AUTPDT        CHAR(1),
  UNIQ          CHAR(1),
  COLMNS        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProcedureParameter
-- (generated from Relational/ProcedureParameter)
CREATE TABLE DD_RELT_PROCDRPRMT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  DIRCTN        BIGINT,
  DEFLTVL       VARCHAR(256),
  NATVTYP       VARCHAR(256),
  LENGTH        BIGINT,
  PRECSN        BIGINT,
  SCAL          BIGINT,
  NULLBL        BIGINT,
  RADX          BIGINT,
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: UniqueConstraint
-- (generated from Relational/UniqueConstraint)
CREATE TABLE DD_RELT_UNIQCNSTRN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  FORGNKYS      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: AccessPattern
-- (generated from Relational/AccessPattern)
CREATE TABLE DD_RELT_ACCSSPTTRN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LogicalRelationship
-- (generated from Relational/LogicalRelationship)
CREATE TABLE DD_RELT_LOGCLRLTNS
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: LogicalRelationshipEnd
-- (generated from Relational/LogicalRelationshipEnd)
CREATE TABLE DD_RELT_LOGCLRLT_1
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  MULTPLCTY     BIGINT,
  TABL          BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: BaseTable
-- (generated from Relational/BaseTable)
CREATE TABLE DD_RELT_BASTBL
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  NAM             VARCHAR(256),
  NAMNSRC         VARCHAR(256),
  SYSTM           CHAR(1),
  CARDNLTY        BIGINT,
  SUPPRTSPDT      CHAR(1),
  MATRLZD         CHAR(1),
  LOGCLRLTNSHPS   BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProcedureResult
-- (generated from Relational/ProcedureResult)
CREATE TABLE DD_RELT_PROCDRRSLT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingHelper
-- (generated from Mapping/MappingHelper)
CREATE TABLE DD_MAPP_MAPPNGHLPR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Mapping
-- (generated from Mapping/Mapping)
CREATE TABLE DD_MAPP_MAPPNG
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: TypeConverter
-- (generated from Mapping/TypeConverter)
CREATE TABLE DD_MAPP_TYPCNVRTR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FunctionPair
-- (generated from Mapping/FunctionPair)
CREATE TABLE DD_MAPP_FUNCTNPR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  IN2T          BIGINT,
  OUT2N         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FunctionNamePair
-- (generated from Mapping/FunctionNamePair)
CREATE TABLE DD_MAPP_FUNCTNNMPR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  IN2T          VARCHAR(256),
  OUT2N         VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingStrategy
-- (generated from Mapping/MappingStrategy)
CREATE TABLE DD_MAPP_MAPPNGSTRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: MappingRoot
-- (generated from Mapping/MappingRoot)
CREATE TABLE DD_MAPP_MAPPNGRT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  INPTS         BIGINT,
  OUTPTS        BIGINT,
  TYPMPPNG      BIGINT,
  OUTPTRDNLY    CHAR(1),
  TOPTBTTM      CHAR(1),
  COMMNDSTCK    VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ComplexTypeConverter
-- (generated from Mapping/ComplexTypeConverter)
CREATE TABLE DD_MAPP_COMPLXTYPC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  HELPDBJCT     BIGINT,
  IN2T          BIGINT,
  OUT2N         BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Category
-- (generated from Dataaccess/Category)
CREATE TABLE DD_DATC_CATGRY
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Group
-- (generated from Dataaccess/Group)
CREATE TABLE DD_DATC_GROP
(
  LGCL_ID         VARCHAR(1000),
  UUID1           BIGINT NOT NULL,
  UUID2           BIGINT NOT NULL,
  UUID_STRING     VARCHAR(44) NOT NULL,
  NAM             VARCHAR(256),
  NAMNSRC         VARCHAR(256),
  SYSTM           CHAR(1),
  CARDNLTY        BIGINT,
  SUPPRTSPDT      CHAR(1),
  MATRLZD         CHAR(1),
  LOGCLRLTNSHPS   BIGINT,
  TXN_ID          BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Element
-- (generated from Dataaccess/Element)
CREATE TABLE DD_DATC_ELEMNT
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            VARCHAR(256),
  NAMNSRC        VARCHAR(256),
  NATVTYP        VARCHAR(256),
  LENGTH         BIGINT,
  FIXDLNGTH      CHAR(1),
  PRECSN         BIGINT,
  SCAL           BIGINT,
  NULLBL         BIGINT,
  AUTNCRMNTD     CHAR(1),
  DEFLTVL        VARCHAR(256),
  MINMMVL        VARCHAR(256),
  MAXMMVL        VARCHAR(256),
  FORMT          VARCHAR(256),
  CHARCTRSTNM    VARCHAR(256),
  COLLTNNM       VARCHAR(256),
  SELCTBL        CHAR(1),
  UPDTBL         CHAR(1),
  CASSNSTV       CHAR(1),
  SEARCHBLTY     BIGINT,
  CURRNCY        CHAR(1),
  RADX           BIGINT,
  SIGND          CHAR(1),
  DISTNCTVLCNT   BIGINT,
  NULLVLCNT      BIGINT,
  UNIQKYS        BIGINT,
  INDXS          BIGINT,
  FORGNKYS       BIGINT,
  ACCSSPTTRNS    BIGINT,
  TYP            BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Procedure
-- (generated from Dataaccess/Procedure_1)
CREATE TABLE DD_DATC_PROCDR
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  FUNCTN        CHAR(1),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Index
-- (generated from Dataaccess/Index_1)
CREATE TABLE DD_DATC_INDX
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  FILTRCNDTN    VARCHAR(256),
  NULLBL        CHAR(1),
  AUTPDT        CHAR(1),
  UNIQ          CHAR(1),
  COLMNS        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PrimaryKey
-- (generated from Dataaccess/PrimaryKey_1)
CREATE TABLE DD_DATC_PRIMRYKY
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  FORGNKYS      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: UniqueConstraint
-- (generated from Dataaccess/UniqueConstraint_1)
CREATE TABLE DD_DATC_UNIQCNSTRN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  FORGNKYS      BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProcedureParameter
-- (generated from Dataaccess/ProcedureParameter_1)
CREATE TABLE DD_DATC_PROCDRPRMT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  DIRCTN        BIGINT,
  DEFLTVL       VARCHAR(256),
  NATVTYP       VARCHAR(256),
  LENGTH        BIGINT,
  PRECSN        BIGINT,
  SCAL          BIGINT,
  NULLBL        BIGINT,
  RADX          BIGINT,
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ForeignKey
-- (generated from Dataaccess/ForeignKey_1)
CREATE TABLE DD_DATC_FORGNKY
(
  LGCL_ID            VARCHAR(1000),
  UUID1              BIGINT NOT NULL,
  UUID2              BIGINT NOT NULL,
  UUID_STRING        VARCHAR(44) NOT NULL,
  NAM                VARCHAR(256),
  NAMNSRC            VARCHAR(256),
  FORGNKYMLTPLCTY    BIGINT,
  PRIMRYKYMLTPLCTY   BIGINT,
  COLMNS             BIGINT,
  UNIQKY             BIGINT,
  TXN_ID             BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: ProcedureResult
-- (generated from Dataaccess/ProcedureResult_1)
CREATE TABLE DD_DATC_PROCDRRSLT
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: AccessPattern
-- (generated from Dataaccess/AccessPattern_1)
CREATE TABLE DD_DATC_ACCSSPTTRN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  NAMNSRC       VARCHAR(256),
  COLMNS        BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Operation
-- (generated from Webservice/Operation_1)
CREATE TABLE DD_WEBS_OPERTN
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  PATTRN        VARCHAR(256),
  SAF           CHAR(1),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Input
-- (generated from Webservice/Input)
CREATE TABLE DD_WEBS_INPT
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  NAM              VARCHAR(256),
  CONTNTLMNT       BIGINT,
  CONTNTCMPLXTYP   BIGINT,
  CONTNTSMPLTYP    BIGINT,
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Output
-- (generated from Webservice/Output)
CREATE TABLE DD_WEBS_OUTPT
(
  LGCL_ID          VARCHAR(1000),
  UUID1            BIGINT NOT NULL,
  UUID2            BIGINT NOT NULL,
  UUID_STRING      VARCHAR(44) NOT NULL,
  NAM              VARCHAR(256),
  CONTNTLMNT       BIGINT,
  CONTNTCMPLXTYP   BIGINT,
  CONTNTSMPLTYP    BIGINT,
  XMLDCMNT         BIGINT,
  TXN_ID           BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Interface
-- (generated from Webservice/Interface_1)
CREATE TABLE DD_WEBS_INTRFC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SampleMessages
-- (generated from Webservice/SampleMessages)
CREATE TABLE DD_WEBS_SAMPLMSSGS
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SampleFile
-- (generated from Webservice/SampleFile)
CREATE TABLE DD_WEBS_SAMPLFL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  URL           VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: SampleFromXsd
-- (generated from Webservice/SampleFromXsd)
CREATE TABLE DD_WEBS_SAMPLFRMXS
(
  LGCL_ID            VARCHAR(1000),
  UUID1              BIGINT NOT NULL,
  UUID2              BIGINT NOT NULL,
  UUID_STRING        VARCHAR(44) NOT NULL,
  MAXNMBRFLVLSTBLD   BIGINT,
  TXN_ID             BIGINT NOT NULL
);
--
-- The model class name for the set enumeration values held in this table is: RelationshipTypeStatus
-- (generated from Relationship/RelationshipTypeStatus)
CREATE TABLE DD_RELT_RELTNSHPTY
(
  ID      BIGINT NOT NULL,
  VALUE   VARCHAR(500)
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: RelationshipType
-- (generated from Relationship/RelationshipType)
CREATE TABLE DD_RELT_RELTNSHP_1
(
  LGCL_ID        VARCHAR(1000),
  UUID1          BIGINT NOT NULL,
  UUID2          BIGINT NOT NULL,
  UUID_STRING    VARCHAR(44) NOT NULL,
  NAM            CLOB(1000000),
  DIRCTD         CHAR(1),
  EXCLSV         CHAR(1),
  CROSSMDL       CHAR(1),
  ABSTRCT        CHAR(1),
  USERDFND       CHAR(1),
  STATS          BIGINT,
  STERTYP        VARCHAR(256),
  CONSTRNT       VARCHAR(256),
  LABL           VARCHAR(256),
  OPPSTLBL       VARCHAR(256),
  RELTNSHPFTRS   BIGINT,
  SUPRTYP        BIGINT,
  SUBTYP         BIGINT,
  TXN_ID         BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: Relationship
-- (generated from Relationship/Relationship)
CREATE TABLE DD_RELT_RELTNSHP
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           CLOB(1000000),
  TARGTS        BIGINT,
  SOURCS        BIGINT,
  TYP           BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: FileReference
-- (generated from Relationship/FileReference)
CREATE TABLE DD_RELT_FILRFRNC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  URI           VARCHAR(256),
  RESLVBL       CHAR(1),
  ENCDNG        VARCHAR(256),
  ABSTRCT       VARCHAR(256),
  KEYWRDS       VARCHAR(256),
  RELTDRS       VARCHAR(256),
  TOOLNM        VARCHAR(256),
  TOOLVRSN      VARCHAR(256),
  FORMTNM       VARCHAR(256),
  FORMTVRSN     VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: RelationshipRole
-- (generated from Relationship/RelationshipRole)
CREATE TABLE DD_RELT_RELTNSHPRL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           CLOB(1000000),
  STERTYP       VARCHAR(256),
  ORDRD         CHAR(1),
  UNIQ          CHAR(1),
  NAVGBL        CHAR(1),
  LOWRBND       BIGINT,
  UPPRBND       BIGINT,
  CONSTRNT      VARCHAR(256),
  OPPSTRL       BIGINT,
  INCLDTYPS     BIGINT,
  EXCLDTYPS     BIGINT,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: PlaceholderReferenceContainer
-- (generated from Relationship/PlaceholderReferenceContainer)
CREATE TABLE DD_RELT_PLACHLDRRF
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: UriReference
-- (generated from Relationship/UriReference)
CREATE TABLE DD_RELT_URIRFRNC
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           VARCHAR(256),
  URI           VARCHAR(256),
  RESLVBL       CHAR(1),
  ENCDNG        VARCHAR(256),
  ABSTRCT       VARCHAR(256),
  KEYWRDS       VARCHAR(256),
  RELTDRS       VARCHAR(256),
  TXN_ID        BIGINT NOT NULL
);
--
-- The metamodel type of the metadata model objects that can be stored in this table is: RelationshipFolder
-- (generated from Relationship/RelationshipFolder)
CREATE TABLE DD_RELT_RELTNSHPFL
(
  LGCL_ID       VARCHAR(1000),
  UUID1         BIGINT NOT NULL,
  UUID2         BIGINT NOT NULL,
  UUID_STRING   VARCHAR(44) NOT NULL,
  NAM           CLOB(1000000),
  TXN_ID        BIGINT NOT NULL
);
CREATE INDEX DD_INDEX_IX1 ON DD_INDEX (LGCL_ID);
CREATE INDEX DD_INDEX_IX4 ON DD_INDEX (UUID_STRING);
CREATE INDEX DD_MDL_IX1 ON DD_MDL (LGCL_ID);
CREATE INDEX DD_MDL_IX4 ON DD_MDL (UUID_STRING);
CREATE INDEX DD_RLTNSHPS_IX1 ON DD_RELATIONSHIPS (REFERRER_LGCL_ID);
CREATE INDEX DD_RLTNSHPS_IX2 ON DD_RELATIONSHIPS (REFERRER_UUID1);
CREATE INDEX DD_RLTNSHPS_IX3 ON DD_RELATIONSHIPS (REFERRER_UUID2);
CREATE INDEX DD_RLTNSHPS_IX4 ON DD_RELATIONSHIPS (REFERRER_UUID_STRING);
CREATE INDEX DD_RLTNSHPS_IX5 ON DD_RELATIONSHIPS (REFEREE_LGCL_ID);
CREATE INDEX DD_RLTNSHPS_IX6 ON DD_RELATIONSHIPS (REFEREE_UUID1);
CREATE INDEX DD_RLTNSHPS_IX7 ON DD_RELATIONSHIPS (REFEREE_UUID2);
CREATE INDEX DD_RLTNSHPS_IX8 ON DD_RELATIONSHIPS (REFEREE_UUID_STRING);
CREATE INDEX DD_MDL_MTAMDL_IX1 ON DD_MDL_MTAMDL (MDL_LGCL_ID);
CREATE INDEX DD_MDL_MTAMDL_IX2 ON DD_MDL_MTAMDL (MDL_UUID1);
CREATE INDEX DD_MDL_MTAMDL_IX3 ON DD_MDL_MTAMDL (MDL_UUID2);
CREATE INDEX DD_MDL_MTAMDL_IX4 ON DD_MDL_MTAMDL (MDL_UUID_STRING);
CREATE INDEX DD_INDEX_UUIDTBLNM ON DD_INDEX (MDL_UUID_STRING,DETAIL_TBLE_NME);
CREATE INDEX DD_MTMDL_NM ON DD_MTAMDL (DSPLY_NME);
CREATE INDEX DD_INDEX_VDETAILNM ON DD_INDEX (VIRT_DETL_TBLE_NME);
CREATE INDEX DD_INDEX_PRENTUUID ON DD_INDEX (PARENT_UUID_STRING);
CREATE INDEX DD_METM_SCALRF_IX1 ON DD_METM_SCALRFNCTN (LGCL_ID);
CREATE INDEX DD_METM_SCALRF_IX2 ON DD_METM_SCALRFNCTN (UUID_STRING);
CREATE INDEX DD_METM_FUNCTN_IX1 ON DD_METM_FUNCTNPRMT (LGCL_ID);
CREATE INDEX DD_METM_FUNCTN_IX2 ON DD_METM_FUNCTNPRMT (UUID_STRING);
CREATE INDEX DD_METM_RETRNP_IX1 ON DD_METM_RETRNPRMTR (LGCL_ID);
CREATE INDEX DD_METM_RETRNP_IX2 ON DD_METM_RETRNPRMTR (UUID_STRING);
CREATE INDEX DD_VIRT_VIRTLD_IX1 ON DD_VIRT_VIRTLDTBS (LGCL_ID);
CREATE INDEX DD_VIRT_VIRTLD_IX2 ON DD_VIRT_VIRTLDTBS (UUID_STRING);
CREATE INDEX DD_VIRT_MODLRF_IX1 ON DD_VIRT_MODLRFRNC (LGCL_ID);
CREATE INDEX DD_VIRT_MODLRF_IX2 ON DD_VIRT_MODLRFRNC (UUID_STRING);
CREATE INDEX DD_VIRT_PROBLM_IX1 ON DD_VIRT_PROBLMMRKR (LGCL_ID);
CREATE INDEX DD_VIRT_PROBLM_IX2 ON DD_VIRT_PROBLMMRKR (UUID_STRING);
CREATE INDEX DD_VIRT_MODLSR_IX1 ON DD_VIRT_MODLSRC (LGCL_ID);
CREATE INDEX DD_VIRT_MODLSR_IX2 ON DD_VIRT_MODLSRC (UUID_STRING);
CREATE INDEX DD_VIRT_MODLSR_I_1 ON DD_VIRT_MODLSRCPRP (LGCL_ID);
CREATE INDEX DD_VIRT_MODLSR_I_2 ON DD_VIRT_MODLSRCPRP (UUID_STRING);
CREATE INDEX DD_VIRT_WSDLPT_IX1 ON DD_VIRT_WSDLPTNS (LGCL_ID);
CREATE INDEX DD_VIRT_WSDLPT_IX2 ON DD_VIRT_WSDLPTNS (UUID_STRING);
CREATE INDEX DD_VIRT_NONMDL_IX1 ON DD_VIRT_NONMDLRFRN (LGCL_ID);
CREATE INDEX DD_VIRT_NONMDL_IX2 ON DD_VIRT_NONMDLRFRN (UUID_STRING);
CREATE INDEX DD_XMLD_XMLFRG_IX1 ON DD_XMLD_XMLFRGMNT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLFRG_IX2 ON DD_XMLD_XMLFRGMNT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLDCM_IX1 ON DD_XMLD_XMLDCMNT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLDCM_IX2 ON DD_XMLD_XMLDCMNT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLLMN_IX1 ON DD_XMLD_XMLLMNT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLLMN_IX2 ON DD_XMLD_XMLLMNT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLTTR_IX1 ON DD_XMLD_XMLTTRBT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLTTR_IX2 ON DD_XMLD_XMLTTRBT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLRT_IX1 ON DD_XMLD_XMLRT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLRT_IX2 ON DD_XMLD_XMLRT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLCMM_IX1 ON DD_XMLD_XMLCMMNT (LGCL_ID);
CREATE INDEX DD_XMLD_XMLCMM_IX2 ON DD_XMLD_XMLCMMNT (UUID_STRING);
CREATE INDEX DD_XMLD_XMLNMS_IX1 ON DD_XMLD_XMLNMSPC (LGCL_ID);
CREATE INDEX DD_XMLD_XMLNMS_IX2 ON DD_XMLD_XMLNMSPC (UUID_STRING);
CREATE INDEX DD_XMLD_XMLSQN_IX1 ON DD_XMLD_XMLSQNC (LGCL_ID);
CREATE INDEX DD_XMLD_XMLSQN_IX2 ON DD_XMLD_XMLSQNC (UUID_STRING);
CREATE INDEX DD_XMLD_XMLLL_IX1 ON DD_XMLD_XMLLL (LGCL_ID);
CREATE INDEX DD_XMLD_XMLLL_IX2 ON DD_XMLD_XMLLL (UUID_STRING);
CREATE INDEX DD_XMLD_XMLCHC_IX1 ON DD_XMLD_XMLCHC (LGCL_ID);
CREATE INDEX DD_XMLD_XMLCHC_IX2 ON DD_XMLD_XMLCHC (UUID_STRING);
CREATE INDEX DD_XMLD_PROCSS_IX1 ON DD_XMLD_PROCSSNGNS (LGCL_ID);
CREATE INDEX DD_XMLD_PROCSS_IX2 ON DD_XMLD_PROCSSNGNS (UUID_STRING);
CREATE INDEX DD_XMLD_XMLFRG_I_1 ON DD_XMLD_XMLFRGMNTS (LGCL_ID);
CREATE INDEX DD_XMLD_XMLFRG_I_2 ON DD_XMLD_XMLFRGMNTS (UUID_STRING);
CREATE INDEX DD_ECOR_EATTRB_IX1 ON DD_ECOR_EATTRBT (LGCL_ID);
CREATE INDEX DD_ECOR_EATTRB_IX2 ON DD_ECOR_EATTRBT (UUID_STRING);
CREATE INDEX DD_ECOR_EANNTT_IX1 ON DD_ECOR_EANNTTN (LGCL_ID);
CREATE INDEX DD_ECOR_EANNTT_IX2 ON DD_ECOR_EANNTTN (UUID_STRING);
CREATE INDEX DD_ECOR_ECLSS_IX1 ON DD_ECOR_ECLSS (LGCL_ID);
CREATE INDEX DD_ECOR_ECLSS_IX2 ON DD_ECOR_ECLSS (UUID_STRING);
CREATE INDEX DD_ECOR_EDATTY_IX1 ON DD_ECOR_EDATTYP (LGCL_ID);
CREATE INDEX DD_ECOR_EDATTY_IX2 ON DD_ECOR_EDATTYP (UUID_STRING);
CREATE INDEX DD_ECOR_EENM_IX1 ON DD_ECOR_EENM (LGCL_ID);
CREATE INDEX DD_ECOR_EENM_IX2 ON DD_ECOR_EENM (UUID_STRING);
CREATE INDEX DD_ECOR_EENMLT_IX1 ON DD_ECOR_EENMLTRL (LGCL_ID);
CREATE INDEX DD_ECOR_EENMLT_IX2 ON DD_ECOR_EENMLTRL (UUID_STRING);
CREATE INDEX DD_ECOR_EFACTR_IX1 ON DD_ECOR_EFACTRY (LGCL_ID);
CREATE INDEX DD_ECOR_EFACTR_IX2 ON DD_ECOR_EFACTRY (UUID_STRING);
CREATE INDEX DD_ECOR_EOBJCT_IX1 ON DD_ECOR_EOBJCT (LGCL_ID);
CREATE INDEX DD_ECOR_EOBJCT_IX2 ON DD_ECOR_EOBJCT (UUID_STRING);
CREATE INDEX DD_ECOR_EOPRTN_IX1 ON DD_ECOR_EOPRTN (LGCL_ID);
CREATE INDEX DD_ECOR_EOPRTN_IX2 ON DD_ECOR_EOPRTN (UUID_STRING);
CREATE INDEX DD_ECOR_EPACKG_IX1 ON DD_ECOR_EPACKG (LGCL_ID);
CREATE INDEX DD_ECOR_EPACKG_IX2 ON DD_ECOR_EPACKG (UUID_STRING);
CREATE INDEX DD_ECOR_EPARMT_IX1 ON DD_ECOR_EPARMTR (LGCL_ID);
CREATE INDEX DD_ECOR_EPARMT_IX2 ON DD_ECOR_EPARMTR (UUID_STRING);
CREATE INDEX DD_ECOR_EREFRN_IX1 ON DD_ECOR_EREFRNC (LGCL_ID);
CREATE INDEX DD_ECOR_EREFRN_IX2 ON DD_ECOR_EREFRNC (UUID_STRING);
CREATE INDEX DD_ECOR_ESTRNG_IX1 ON DD_ECOR_ESTRNGTSTR (LGCL_ID);
CREATE INDEX DD_ECOR_ESTRNG_IX2 ON DD_ECOR_ESTRNGTSTR (UUID_STRING);
CREATE INDEX DD_UML_COMMNT_IX1 ON DD_UML_COMMNT (LGCL_ID);
CREATE INDEX DD_UML_COMMNT_IX2 ON DD_UML_COMMNT (UUID_STRING);
CREATE INDEX DD_UML_CLASS_IX1 ON DD_UML_CLASS (LGCL_ID);
CREATE INDEX DD_UML_CLASS_IX2 ON DD_UML_CLASS (UUID_STRING);
CREATE INDEX DD_UML_PROPRTY_IX1 ON DD_UML_PROPRTY (LGCL_ID);
CREATE INDEX DD_UML_PROPRTY_IX2 ON DD_UML_PROPRTY (UUID_STRING);
CREATE INDEX DD_UML_OPERTN_IX1 ON DD_UML_OPERTN (LGCL_ID);
CREATE INDEX DD_UML_OPERTN_IX2 ON DD_UML_OPERTN (UUID_STRING);
CREATE INDEX DD_UML_PARMTR_IX1 ON DD_UML_PARMTR (LGCL_ID);
CREATE INDEX DD_UML_PARMTR_IX2 ON DD_UML_PARMTR (UUID_STRING);
CREATE INDEX DD_UML_PACKG_IX1 ON DD_UML_PACKG (LGCL_ID);
CREATE INDEX DD_UML_PACKG_IX2 ON DD_UML_PACKG (UUID_STRING);
CREATE INDEX DD_UML_ENUMRTN_IX1 ON DD_UML_ENUMRTN (LGCL_ID);
CREATE INDEX DD_UML_ENUMRTN_IX2 ON DD_UML_ENUMRTN (UUID_STRING);
CREATE INDEX DD_UML_DATTYP_IX1 ON DD_UML_DATTYP (LGCL_ID);
CREATE INDEX DD_UML_DATTYP_IX2 ON DD_UML_DATTYP (UUID_STRING);
CREATE INDEX DD_UML_ENUMRTN_I_1 ON DD_UML_ENUMRTNLTRL (LGCL_ID);
CREATE INDEX DD_UML_ENUMRTN_I_2 ON DD_UML_ENUMRTNLTRL (UUID_STRING);
CREATE INDEX DD_UML_PRIMTVT_IX1 ON DD_UML_PRIMTVTYP (LGCL_ID);
CREATE INDEX DD_UML_PRIMTVT_IX2 ON DD_UML_PRIMTVTYP (UUID_STRING);
CREATE INDEX DD_UML_CONSTRN_IX1 ON DD_UML_CONSTRNT (LGCL_ID);
CREATE INDEX DD_UML_CONSTRN_IX2 ON DD_UML_CONSTRNT (UUID_STRING);
CREATE INDEX DD_UML_LITRLBL_IX1 ON DD_UML_LITRLBLN (LGCL_ID);
CREATE INDEX DD_UML_LITRLBL_IX2 ON DD_UML_LITRLBLN (UUID_STRING);
CREATE INDEX DD_UML_LITRLST_IX1 ON DD_UML_LITRLSTRNG (LGCL_ID);
CREATE INDEX DD_UML_LITRLST_IX2 ON DD_UML_LITRLSTRNG (UUID_STRING);
CREATE INDEX DD_UML_LITRLNL_IX1 ON DD_UML_LITRLNLL (LGCL_ID);
CREATE INDEX DD_UML_LITRLNL_IX2 ON DD_UML_LITRLNLL (UUID_STRING);
CREATE INDEX DD_UML_LITRLNT_IX1 ON DD_UML_LITRLNTGR (LGCL_ID);
CREATE INDEX DD_UML_LITRLNT_IX2 ON DD_UML_LITRLNTGR (UUID_STRING);
CREATE INDEX DD_UML_LITRLNL_I_1 ON DD_UML_LITRLNLMTDN (LGCL_ID);
CREATE INDEX DD_UML_LITRLNL_I_2 ON DD_UML_LITRLNLMTDN (UUID_STRING);
CREATE INDEX DD_UML_INSTNCS_IX1 ON DD_UML_INSTNCSPCFC (LGCL_ID);
CREATE INDEX DD_UML_INSTNCS_IX2 ON DD_UML_INSTNCSPCFC (UUID_STRING);
CREATE INDEX DD_UML_SLOT_IX1 ON DD_UML_SLOT (LGCL_ID);
CREATE INDEX DD_UML_SLOT_IX2 ON DD_UML_SLOT (UUID_STRING);
CREATE INDEX DD_UML_GENRLZT_IX1 ON DD_UML_GENRLZTN (LGCL_ID);
CREATE INDEX DD_UML_GENRLZT_IX2 ON DD_UML_GENRLZTN (UUID_STRING);
CREATE INDEX DD_UML_ELEMNTM_IX1 ON DD_UML_ELEMNTMPRT (LGCL_ID);
CREATE INDEX DD_UML_ELEMNTM_IX2 ON DD_UML_ELEMNTMPRT (UUID_STRING);
CREATE INDEX DD_UML_PACKGMP_IX1 ON DD_UML_PACKGMPRT (LGCL_ID);
CREATE INDEX DD_UML_PACKGMP_IX2 ON DD_UML_PACKGMPRT (UUID_STRING);
CREATE INDEX DD_UML_ASSCTN_IX1 ON DD_UML_ASSCTN (LGCL_ID);
CREATE INDEX DD_UML_ASSCTN_IX2 ON DD_UML_ASSCTN (UUID_STRING);
CREATE INDEX DD_UML_PACKGMR_IX1 ON DD_UML_PACKGMRG (LGCL_ID);
CREATE INDEX DD_UML_PACKGMR_IX2 ON DD_UML_PACKGMRG (UUID_STRING);
CREATE INDEX DD_UML_STERTYP_IX1 ON DD_UML_STERTYP (LGCL_ID);
CREATE INDEX DD_UML_STERTYP_IX2 ON DD_UML_STERTYP (UUID_STRING);
CREATE INDEX DD_UML_PROFL_IX1 ON DD_UML_PROFL (LGCL_ID);
CREATE INDEX DD_UML_PROFL_IX2 ON DD_UML_PROFL (UUID_STRING);
CREATE INDEX DD_UML_PROFLPP_IX1 ON DD_UML_PROFLPPLCTN (LGCL_ID);
CREATE INDEX DD_UML_PROFLPP_IX2 ON DD_UML_PROFLPPLCTN (UUID_STRING);
CREATE INDEX DD_UML_EXTNSN_IX1 ON DD_UML_EXTNSN (LGCL_ID);
CREATE INDEX DD_UML_EXTNSN_IX2 ON DD_UML_EXTNSN (UUID_STRING);
CREATE INDEX DD_UML_EXTNSNN_IX1 ON DD_UML_EXTNSNND (LGCL_ID);
CREATE INDEX DD_UML_EXTNSNN_IX2 ON DD_UML_EXTNSNND (UUID_STRING);
CREATE INDEX DD_UML_DEPNDNC_IX1 ON DD_UML_DEPNDNCY (LGCL_ID);
CREATE INDEX DD_UML_DEPNDNC_IX2 ON DD_UML_DEPNDNCY (UUID_STRING);
CREATE INDEX DD_UML_GENRLZT_I_1 ON DD_UML_GENRLZTNST (LGCL_ID);
CREATE INDEX DD_UML_GENRLZT_I_2 ON DD_UML_GENRLZTNST (UUID_STRING);
CREATE INDEX DD_UML_ASSCTNC_IX1 ON DD_UML_ASSCTNCLSS (LGCL_ID);
CREATE INDEX DD_UML_ASSCTNC_IX2 ON DD_UML_ASSCTNCLSS (UUID_STRING);
CREATE INDEX DD_UML_MODL_IX1 ON DD_UML_MODL (LGCL_ID);
CREATE INDEX DD_UML_MODL_IX2 ON DD_UML_MODL (UUID_STRING);
CREATE INDEX DD_UML_INTRFC_IX1 ON DD_UML_INTRFC (LGCL_ID);
CREATE INDEX DD_UML_INTRFC_IX2 ON DD_UML_INTRFC (UUID_STRING);
CREATE INDEX DD_UML_STRNGXP_IX1 ON DD_UML_STRNGXPRSSN (LGCL_ID);
CREATE INDEX DD_UML_STRNGXP_IX2 ON DD_UML_STRNGXPRSSN (UUID_STRING);
CREATE INDEX DD_UML_DURTNNT_IX1 ON DD_UML_DURTNNTRVL (LGCL_ID);
CREATE INDEX DD_UML_DURTNNT_IX2 ON DD_UML_DURTNNTRVL (UUID_STRING);
CREATE INDEX DD_UML_TIMNTRV_IX1 ON DD_UML_TIMNTRVL (LGCL_ID);
CREATE INDEX DD_UML_TIMNTRV_IX2 ON DD_UML_TIMNTRVL (UUID_STRING);
CREATE INDEX DD_UML_PARMTRS_IX1 ON DD_UML_PARMTRST (LGCL_ID);
CREATE INDEX DD_UML_PARMTRS_IX2 ON DD_UML_PARMTRST (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCSR_IX1 ON DD_JDBC_JDBCSRCPRP (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCSR_IX2 ON DD_JDBC_JDBCSRCPRP (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCDR_IX1 ON DD_JDBC_JDBCDRVR (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCDR_IX2 ON DD_JDBC_JDBCDRVR (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCSR_I_1 ON DD_JDBC_JDBCSRC (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCSR_I_2 ON DD_JDBC_JDBCSRC (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCDR_I_1 ON DD_JDBC_JDBCDRVRCN (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCDR_I_2 ON DD_JDBC_JDBCDRVRCN (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCSR_I_3 ON DD_JDBC_JDBCSRCCNT (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCSR_I_4 ON DD_JDBC_JDBCSRCCNT (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCMP_IX1 ON DD_JDBC_JDBCMPRTST (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCMP_IX2 ON DD_JDBC_JDBCMPRTST (UUID_STRING);
CREATE INDEX DD_JDBC_JDBCMP_I_1 ON DD_JDBC_JDBCMPRTPT (LGCL_ID);
CREATE INDEX DD_JDBC_JDBCMP_I_2 ON DD_JDBC_JDBCMPRTPT (UUID_STRING);
CREATE INDEX DD_COR_ANNTTN_IX1 ON DD_COR_ANNTTN (LGCL_ID);
CREATE INDEX DD_COR_ANNTTN_IX2 ON DD_COR_ANNTTN (UUID_STRING);
CREATE INDEX DD_COR_ANNTTNC_IX1 ON DD_COR_ANNTTNCNTNR (LGCL_ID);
CREATE INDEX DD_COR_ANNTTNC_IX2 ON DD_COR_ANNTTNCNTNR (UUID_STRING);
CREATE INDEX DD_COR_MODLNNT_IX1 ON DD_COR_MODLNNTTN (LGCL_ID);
CREATE INDEX DD_COR_MODLNNT_IX2 ON DD_COR_MODLNNTTN (UUID_STRING);
CREATE INDEX DD_COR_LINK_IX1 ON DD_COR_LINK (LGCL_ID);
CREATE INDEX DD_COR_LINK_IX2 ON DD_COR_LINK (UUID_STRING);
CREATE INDEX DD_COR_LINKCNT_IX1 ON DD_COR_LINKCNTNR (LGCL_ID);
CREATE INDEX DD_COR_LINKCNT_IX2 ON DD_COR_LINKCNTNR (UUID_STRING);
CREATE INDEX DD_COR_MODLMPR_IX1 ON DD_COR_MODLMPRT (LGCL_ID);
CREATE INDEX DD_COR_MODLMPR_IX2 ON DD_COR_MODLMPRT (UUID_STRING);
CREATE INDEX DD_EXTN_XCLSS_IX1 ON DD_EXTN_XCLSS (LGCL_ID);
CREATE INDEX DD_EXTN_XCLSS_IX2 ON DD_EXTN_XCLSS (UUID_STRING);
CREATE INDEX DD_EXTN_XPACKG_IX1 ON DD_EXTN_XPACKG (LGCL_ID);
CREATE INDEX DD_EXTN_XPACKG_IX2 ON DD_EXTN_XPACKG (UUID_STRING);
CREATE INDEX DD_EXTN_XATTRB_IX1 ON DD_EXTN_XATTRBT (LGCL_ID);
CREATE INDEX DD_EXTN_XATTRB_IX2 ON DD_EXTN_XATTRBT (UUID_STRING);
CREATE INDEX DD_EXTN_XENM_IX1 ON DD_EXTN_XENM (LGCL_ID);
CREATE INDEX DD_EXTN_XENM_IX2 ON DD_EXTN_XENM (UUID_STRING);
CREATE INDEX DD_EXTN_XENMLT_IX1 ON DD_EXTN_XENMLTRL (LGCL_ID);
CREATE INDEX DD_EXTN_XENMLT_IX2 ON DD_EXTN_XENMLTRL (UUID_STRING);
CREATE INDEX DD_XSD_XSDNNTT_IX1 ON DD_XSD_XSDNNTTN (LGCL_ID);
CREATE INDEX DD_XSD_XSDNNTT_IX2 ON DD_XSD_XSDNNTTN (UUID_STRING);
CREATE INDEX DD_XSD_XSDTTRB_IX1 ON DD_XSD_XSDTTRBTDCL (LGCL_ID);
CREATE INDEX DD_XSD_XSDTTRB_IX2 ON DD_XSD_XSDTTRBTDCL (UUID_STRING);
CREATE INDEX DD_XSD_XSDTTRB_I_1 ON DD_XSD_XSDTTRBTGRP (LGCL_ID);
CREATE INDEX DD_XSD_XSDTTRB_I_2 ON DD_XSD_XSDTTRBTGRP (UUID_STRING);
CREATE INDEX DD_XSD_XSDTTRB_I_3 ON DD_XSD_XSDTTRBTS (LGCL_ID);
CREATE INDEX DD_XSD_XSDTTRB_I_4 ON DD_XSD_XSDTTRBTS (UUID_STRING);
CREATE INDEX DD_XSD_XSDBNDD_IX1 ON DD_XSD_XSDBNDDFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDBNDD_IX2 ON DD_XSD_XSDBNDDFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDCRDN_IX1 ON DD_XSD_XSDCRDNLTYF (LGCL_ID);
CREATE INDEX DD_XSD_XSDCRDN_IX2 ON DD_XSD_XSDCRDNLTYF (UUID_STRING);
CREATE INDEX DD_XSD_XSDCMPL_IX1 ON DD_XSD_XSDCMPLXTYP (LGCL_ID);
CREATE INDEX DD_XSD_XSDCMPL_IX2 ON DD_XSD_XSDCMPLXTYP (UUID_STRING);
CREATE INDEX DD_XSD_XSDDGNS_IX1 ON DD_XSD_XSDDGNSTC (LGCL_ID);
CREATE INDEX DD_XSD_XSDDGNS_IX2 ON DD_XSD_XSDDGNSTC (UUID_STRING);
CREATE INDEX DD_XSD_XSDLMNT_IX1 ON DD_XSD_XSDLMNTDCLR (LGCL_ID);
CREATE INDEX DD_XSD_XSDLMNT_IX2 ON DD_XSD_XSDLMNTDCLR (UUID_STRING);
CREATE INDEX DD_XSD_XSDNMRT_IX1 ON DD_XSD_XSDNMRTNFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDNMRT_IX2 ON DD_XSD_XSDNMRTNFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDFRCT_IX1 ON DD_XSD_XSDFRCTNDGT (LGCL_ID);
CREATE INDEX DD_XSD_XSDFRCT_IX2 ON DD_XSD_XSDFRCTNDGT (UUID_STRING);
CREATE INDEX DD_XSD_XSDDNTT_IX1 ON DD_XSD_XSDDNTTYC_1 (LGCL_ID);
CREATE INDEX DD_XSD_XSDDNTT_IX2 ON DD_XSD_XSDDNTTYC_1 (UUID_STRING);
CREATE INDEX DD_XSD_XSDMPRT_IX1 ON DD_XSD_XSDMPRT (LGCL_ID);
CREATE INDEX DD_XSD_XSDMPRT_IX2 ON DD_XSD_XSDMPRT (UUID_STRING);
CREATE INDEX DD_XSD_XSDNCLD_IX1 ON DD_XSD_XSDNCLD (LGCL_ID);
CREATE INDEX DD_XSD_XSDNCLD_IX2 ON DD_XSD_XSDNCLD (UUID_STRING);
CREATE INDEX DD_XSD_XSDLNGT_IX1 ON DD_XSD_XSDLNGTHFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDLNGT_IX2 ON DD_XSD_XSDLNGTHFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDMXXC_IX1 ON DD_XSD_XSDMXXCLSVF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMXXC_IX2 ON DD_XSD_XSDMXXCLSVF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMXNC_IX1 ON DD_XSD_XSDMXNCLSVF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMXNC_IX2 ON DD_XSD_XSDMXNCLSVF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMXLN_IX1 ON DD_XSD_XSDMXLNGTHF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMXLN_IX2 ON DD_XSD_XSDMXLNGTHF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMNXC_IX1 ON DD_XSD_XSDMNXCLSVF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMNXC_IX2 ON DD_XSD_XSDMNXCLSVF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMNNC_IX1 ON DD_XSD_XSDMNNCLSVF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMNNC_IX2 ON DD_XSD_XSDMNNCLSVF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMNLN_IX1 ON DD_XSD_XSDMNLNGTHF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMNLN_IX2 ON DD_XSD_XSDMNLNGTHF (UUID_STRING);
CREATE INDEX DD_XSD_XSDMDLG_IX1 ON DD_XSD_XSDMDLGRP (LGCL_ID);
CREATE INDEX DD_XSD_XSDMDLG_IX2 ON DD_XSD_XSDMDLGRP (UUID_STRING);
CREATE INDEX DD_XSD_XSDMDLG_I_1 ON DD_XSD_XSDMDLGRPDF (LGCL_ID);
CREATE INDEX DD_XSD_XSDMDLG_I_2 ON DD_XSD_XSDMDLGRPDF (UUID_STRING);
CREATE INDEX DD_XSD_XSDNTTN_IX1 ON DD_XSD_XSDNTTNDCLR (LGCL_ID);
CREATE INDEX DD_XSD_XSDNTTN_IX2 ON DD_XSD_XSDNTTNDCLR (UUID_STRING);
CREATE INDEX DD_XSD_XSDNMRC_IX1 ON DD_XSD_XSDNMRCFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDNMRC_IX2 ON DD_XSD_XSDNMRCFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDRDRD_IX1 ON DD_XSD_XSDRDRDFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDRDRD_IX2 ON DD_XSD_XSDRDRDFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDPRTC_IX1 ON DD_XSD_XSDPRTCL (LGCL_ID);
CREATE INDEX DD_XSD_XSDPRTC_IX2 ON DD_XSD_XSDPRTCL (UUID_STRING);
CREATE INDEX DD_XSD_XSDPTTR_IX1 ON DD_XSD_XSDPTTRNFCT (LGCL_ID);
CREATE INDEX DD_XSD_XSDPTTR_IX2 ON DD_XSD_XSDPTTRNFCT (UUID_STRING);
CREATE INDEX DD_XSD_XSDRDFN_IX1 ON DD_XSD_XSDRDFN (LGCL_ID);
CREATE INDEX DD_XSD_XSDRDFN_IX2 ON DD_XSD_XSDRDFN (UUID_STRING);
CREATE INDEX DD_XSD_XSDSCHM_IX1 ON DD_XSD_XSDSCHM (LGCL_ID);
CREATE INDEX DD_XSD_XSDSCHM_IX2 ON DD_XSD_XSDSCHM (UUID_STRING);
CREATE INDEX DD_XSD_XSDSMPL_IX1 ON DD_XSD_XSDSMPLTYPD (LGCL_ID);
CREATE INDEX DD_XSD_XSDSMPL_IX2 ON DD_XSD_XSDSMPLTYPD (UUID_STRING);
CREATE INDEX DD_XSD_XSDTTLD_IX1 ON DD_XSD_XSDTTLDGTSF (LGCL_ID);
CREATE INDEX DD_XSD_XSDTTLD_IX2 ON DD_XSD_XSDTTLDGTSF (UUID_STRING);
CREATE INDEX DD_XSD_XSDWHTS_IX1 ON DD_XSD_XSDWHTSPCFC (LGCL_ID);
CREATE INDEX DD_XSD_XSDWHTS_IX2 ON DD_XSD_XSDWHTSPCFC (UUID_STRING);
CREATE INDEX DD_XSD_XSDWLDC_IX1 ON DD_XSD_XSDWLDCRD (LGCL_ID);
CREATE INDEX DD_XSD_XSDWLDC_IX2 ON DD_XSD_XSDWLDCRD (UUID_STRING);
CREATE INDEX DD_XSD_XSDXPTH_IX1 ON DD_XSD_XSDXPTHDFNT (LGCL_ID);
CREATE INDEX DD_XSD_XSDXPTH_IX2 ON DD_XSD_XSDXPTHDFNT (UUID_STRING);
CREATE INDEX DD_TRAN_TRANSF_IX1 ON DD_TRAN_TRANSFRMTN (LGCL_ID);
CREATE INDEX DD_TRAN_TRANSF_IX2 ON DD_TRAN_TRANSFRMTN (UUID_STRING);
CREATE INDEX DD_TRAN_SQLTRN_IX1 ON DD_TRAN_SQLTRNSFRM (LGCL_ID);
CREATE INDEX DD_TRAN_SQLTRN_IX2 ON DD_TRAN_SQLTRNSFRM (UUID_STRING);
CREATE INDEX DD_TRAN_TRANSF_I_1 ON DD_TRAN_TRANSFRM_1 (LGCL_ID);
CREATE INDEX DD_TRAN_TRANSF_I_2 ON DD_TRAN_TRANSFRM_1 (UUID_STRING);
CREATE INDEX DD_TRAN_SQLLS_IX1 ON DD_TRAN_SQLLS (LGCL_ID);
CREATE INDEX DD_TRAN_SQLLS_IX2 ON DD_TRAN_SQLLS (UUID_STRING);
CREATE INDEX DD_TRAN_SQLTRN_I_1 ON DD_TRAN_SQLTRNSF_1 (LGCL_ID);
CREATE INDEX DD_TRAN_SQLTRN_I_2 ON DD_TRAN_SQLTRNSF_1 (UUID_STRING);
CREATE INDEX DD_TRAN_FRAGMN_IX1 ON DD_TRAN_FRAGMNTMPP (LGCL_ID);
CREATE INDEX DD_TRAN_FRAGMN_IX2 ON DD_TRAN_FRAGMNTMPP (UUID_STRING);
CREATE INDEX DD_TRAN_TREMPP_IX1 ON DD_TRAN_TREMPPNGRT (LGCL_ID);
CREATE INDEX DD_TRAN_TREMPP_IX2 ON DD_TRAN_TREMPPNGRT (UUID_STRING);
CREATE INDEX DD_TRAN_MAPPNG_IX1 ON DD_TRAN_MAPPNGCLSS (LGCL_ID);
CREATE INDEX DD_TRAN_MAPPNG_IX2 ON DD_TRAN_MAPPNGCLSS (UUID_STRING);
CREATE INDEX DD_TRAN_MAPPNG_I_1 ON DD_TRAN_MAPPNGCL_1 (LGCL_ID);
CREATE INDEX DD_TRAN_MAPPNG_I_2 ON DD_TRAN_MAPPNGCL_1 (UUID_STRING);
CREATE INDEX DD_TRAN_STAGNG_IX1 ON DD_TRAN_STAGNGTBL (LGCL_ID);
CREATE INDEX DD_TRAN_STAGNG_IX2 ON DD_TRAN_STAGNGTBL (UUID_STRING);
CREATE INDEX DD_TRAN_MAPPNG_I_3 ON DD_TRAN_MAPPNGCL_2 (LGCL_ID);
CREATE INDEX DD_TRAN_MAPPNG_I_4 ON DD_TRAN_MAPPNGCL_2 (UUID_STRING);
CREATE INDEX DD_TRAN_MAPPNG_I_5 ON DD_TRAN_MAPPNGCL_3 (LGCL_ID);
CREATE INDEX DD_TRAN_MAPPNG_I_6 ON DD_TRAN_MAPPNGCL_3 (UUID_STRING);
CREATE INDEX DD_TRAN_INPTPR_IX1 ON DD_TRAN_INPTPRMTR (LGCL_ID);
CREATE INDEX DD_TRAN_INPTPR_IX2 ON DD_TRAN_INPTPRMTR (UUID_STRING);
CREATE INDEX DD_TRAN_INPTST_IX1 ON DD_TRAN_INPTST (LGCL_ID);
CREATE INDEX DD_TRAN_INPTST_IX2 ON DD_TRAN_INPTST (UUID_STRING);
CREATE INDEX DD_TRAN_INPTBN_IX1 ON DD_TRAN_INPTBNDNG (LGCL_ID);
CREATE INDEX DD_TRAN_INPTBN_IX2 ON DD_TRAN_INPTBNDNG (UUID_STRING);
CREATE INDEX DD_TRAN_DATFLW_IX1 ON DD_TRAN_DATFLWMPPN (LGCL_ID);
CREATE INDEX DD_TRAN_DATFLW_IX2 ON DD_TRAN_DATFLWMPPN (UUID_STRING);
CREATE INDEX DD_TRAN_DATFLW_I_1 ON DD_TRAN_DATFLWND (LGCL_ID);
CREATE INDEX DD_TRAN_DATFLW_I_2 ON DD_TRAN_DATFLWND (UUID_STRING);
CREATE INDEX DD_TRAN_DATFLW_I_3 ON DD_TRAN_DATFLWLNK (LGCL_ID);
CREATE INDEX DD_TRAN_DATFLW_I_4 ON DD_TRAN_DATFLWLNK (UUID_STRING);
CREATE INDEX DD_TRAN_EXPRSS_IX1 ON DD_TRAN_EXPRSSN (LGCL_ID);
CREATE INDEX DD_TRAN_EXPRSS_IX2 ON DD_TRAN_EXPRSSN (UUID_STRING);
CREATE INDEX DD_TRAN_TARGTN_IX1 ON DD_TRAN_TARGTND (LGCL_ID);
CREATE INDEX DD_TRAN_TARGTN_IX2 ON DD_TRAN_TARGTND (UUID_STRING);
CREATE INDEX DD_TRAN_SOURCN_IX1 ON DD_TRAN_SOURCND (LGCL_ID);
CREATE INDEX DD_TRAN_SOURCN_IX2 ON DD_TRAN_SOURCND (UUID_STRING);
CREATE INDEX DD_TRAN_OPERTN_IX1 ON DD_TRAN_OPERTNNDGR (LGCL_ID);
CREATE INDEX DD_TRAN_OPERTN_IX2 ON DD_TRAN_OPERTNNDGR (UUID_STRING);
CREATE INDEX DD_TRAN_OPERTN_I_1 ON DD_TRAN_OPERTNND (LGCL_ID);
CREATE INDEX DD_TRAN_OPERTN_I_2 ON DD_TRAN_OPERTNND (UUID_STRING);
CREATE INDEX DD_TRAN_JOINND_IX1 ON DD_TRAN_JOINND (LGCL_ID);
CREATE INDEX DD_TRAN_JOINND_IX2 ON DD_TRAN_JOINND (UUID_STRING);
CREATE INDEX DD_TRAN_UNINND_IX1 ON DD_TRAN_UNINND (LGCL_ID);
CREATE INDEX DD_TRAN_UNINND_IX2 ON DD_TRAN_UNINND (UUID_STRING);
CREATE INDEX DD_TRAN_PROJCT_IX1 ON DD_TRAN_PROJCTNND (LGCL_ID);
CREATE INDEX DD_TRAN_PROJCT_IX2 ON DD_TRAN_PROJCTNND (UUID_STRING);
CREATE INDEX DD_TRAN_FILTRN_IX1 ON DD_TRAN_FILTRND (LGCL_ID);
CREATE INDEX DD_TRAN_FILTRN_IX2 ON DD_TRAN_FILTRND (UUID_STRING);
CREATE INDEX DD_TRAN_GROPNG_IX1 ON DD_TRAN_GROPNGND (LGCL_ID);
CREATE INDEX DD_TRAN_GROPNG_IX2 ON DD_TRAN_GROPNGND (UUID_STRING);
CREATE INDEX DD_TRAN_DUPRMV_IX1 ON DD_TRAN_DUPRMVLND (LGCL_ID);
CREATE INDEX DD_TRAN_DUPRMV_IX2 ON DD_TRAN_DUPRMVLND (UUID_STRING);
CREATE INDEX DD_TRAN_SORTND_IX1 ON DD_TRAN_SORTND (LGCL_ID);
CREATE INDEX DD_TRAN_SORTND_IX2 ON DD_TRAN_SORTND (UUID_STRING);
CREATE INDEX DD_TRAN_SQLND_IX1 ON DD_TRAN_SQLND (LGCL_ID);
CREATE INDEX DD_TRAN_SQLND_IX2 ON DD_TRAN_SQLND (UUID_STRING);
CREATE INDEX DD_RELT_COLMN_IX1 ON DD_RELT_COLMN (LGCL_ID);
CREATE INDEX DD_RELT_COLMN_IX2 ON DD_RELT_COLMN (UUID_STRING);
CREATE INDEX DD_RELT_SCHM_IX1 ON DD_RELT_SCHM (LGCL_ID);
CREATE INDEX DD_RELT_SCHM_IX2 ON DD_RELT_SCHM (UUID_STRING);
CREATE INDEX DD_RELT_PRIMRY_IX1 ON DD_RELT_PRIMRYKY (LGCL_ID);
CREATE INDEX DD_RELT_PRIMRY_IX2 ON DD_RELT_PRIMRYKY (UUID_STRING);
CREATE INDEX DD_RELT_FORGNK_IX1 ON DD_RELT_FORGNKY (LGCL_ID);
CREATE INDEX DD_RELT_FORGNK_IX2 ON DD_RELT_FORGNKY (UUID_STRING);
CREATE INDEX DD_RELT_VIEW_IX1 ON DD_RELT_VIEW (LGCL_ID);
CREATE INDEX DD_RELT_VIEW_IX2 ON DD_RELT_VIEW (UUID_STRING);
CREATE INDEX DD_RELT_CATLG_IX1 ON DD_RELT_CATLG (LGCL_ID);
CREATE INDEX DD_RELT_CATLG_IX2 ON DD_RELT_CATLG (UUID_STRING);
CREATE INDEX DD_RELT_PROCDR_IX1 ON DD_RELT_PROCDR (LGCL_ID);
CREATE INDEX DD_RELT_PROCDR_IX2 ON DD_RELT_PROCDR (UUID_STRING);
CREATE INDEX DD_RELT_INDX_IX1 ON DD_RELT_INDX (LGCL_ID);
CREATE INDEX DD_RELT_INDX_IX2 ON DD_RELT_INDX (UUID_STRING);
CREATE INDEX DD_RELT_PROCDR_I_1 ON DD_RELT_PROCDRPRMT (LGCL_ID);
CREATE INDEX DD_RELT_PROCDR_I_2 ON DD_RELT_PROCDRPRMT (UUID_STRING);
CREATE INDEX DD_RELT_UNIQCN_IX1 ON DD_RELT_UNIQCNSTRN (LGCL_ID);
CREATE INDEX DD_RELT_UNIQCN_IX2 ON DD_RELT_UNIQCNSTRN (UUID_STRING);
CREATE INDEX DD_RELT_ACCSSP_IX1 ON DD_RELT_ACCSSPTTRN (LGCL_ID);
CREATE INDEX DD_RELT_ACCSSP_IX2 ON DD_RELT_ACCSSPTTRN (UUID_STRING);
CREATE INDEX DD_RELT_LOGCLR_IX1 ON DD_RELT_LOGCLRLTNS (LGCL_ID);
CREATE INDEX DD_RELT_LOGCLR_IX2 ON DD_RELT_LOGCLRLTNS (UUID_STRING);
CREATE INDEX DD_RELT_LOGCLR_I_1 ON DD_RELT_LOGCLRLT_1 (LGCL_ID);
CREATE INDEX DD_RELT_LOGCLR_I_2 ON DD_RELT_LOGCLRLT_1 (UUID_STRING);
CREATE INDEX DD_RELT_BASTBL_IX1 ON DD_RELT_BASTBL (LGCL_ID);
CREATE INDEX DD_RELT_BASTBL_IX2 ON DD_RELT_BASTBL (UUID_STRING);
CREATE INDEX DD_RELT_PROCDR_I_3 ON DD_RELT_PROCDRRSLT (LGCL_ID);
CREATE INDEX DD_RELT_PROCDR_I_4 ON DD_RELT_PROCDRRSLT (UUID_STRING);
CREATE INDEX DD_MAPP_MAPPNG_IX1 ON DD_MAPP_MAPPNGHLPR (LGCL_ID);
CREATE INDEX DD_MAPP_MAPPNG_IX2 ON DD_MAPP_MAPPNGHLPR (UUID_STRING);
CREATE INDEX DD_MAPP_MAPPNG_I_1 ON DD_MAPP_MAPPNG (LGCL_ID);
CREATE INDEX DD_MAPP_MAPPNG_I_2 ON DD_MAPP_MAPPNG (UUID_STRING);
CREATE INDEX DD_MAPP_TYPCNV_IX1 ON DD_MAPP_TYPCNVRTR (LGCL_ID);
CREATE INDEX DD_MAPP_TYPCNV_IX2 ON DD_MAPP_TYPCNVRTR (UUID_STRING);
CREATE INDEX DD_MAPP_FUNCTN_IX1 ON DD_MAPP_FUNCTNPR (LGCL_ID);
CREATE INDEX DD_MAPP_FUNCTN_IX2 ON DD_MAPP_FUNCTNPR (UUID_STRING);
CREATE INDEX DD_MAPP_FUNCTN_I_1 ON DD_MAPP_FUNCTNNMPR (LGCL_ID);
CREATE INDEX DD_MAPP_FUNCTN_I_2 ON DD_MAPP_FUNCTNNMPR (UUID_STRING);
CREATE INDEX DD_MAPP_MAPPNG_I_3 ON DD_MAPP_MAPPNGSTRT (LGCL_ID);
CREATE INDEX DD_MAPP_MAPPNG_I_4 ON DD_MAPP_MAPPNGSTRT (UUID_STRING);
CREATE INDEX DD_MAPP_MAPPNG_I_5 ON DD_MAPP_MAPPNGRT (LGCL_ID);
CREATE INDEX DD_MAPP_MAPPNG_I_6 ON DD_MAPP_MAPPNGRT (UUID_STRING);
CREATE INDEX DD_MAPP_COMPLX_IX1 ON DD_MAPP_COMPLXTYPC (LGCL_ID);
CREATE INDEX DD_MAPP_COMPLX_IX2 ON DD_MAPP_COMPLXTYPC (UUID_STRING);
CREATE INDEX DD_DATC_CATGRY_IX1 ON DD_DATC_CATGRY (LGCL_ID);
CREATE INDEX DD_DATC_CATGRY_IX2 ON DD_DATC_CATGRY (UUID_STRING);
CREATE INDEX DD_DATC_GROP_IX1 ON DD_DATC_GROP (LGCL_ID);
CREATE INDEX DD_DATC_GROP_IX2 ON DD_DATC_GROP (UUID_STRING);
CREATE INDEX DD_DATC_ELEMNT_IX1 ON DD_DATC_ELEMNT (LGCL_ID);
CREATE INDEX DD_DATC_ELEMNT_IX2 ON DD_DATC_ELEMNT (UUID_STRING);
CREATE INDEX DD_DATC_PROCDR_IX1 ON DD_DATC_PROCDR (LGCL_ID);
CREATE INDEX DD_DATC_PROCDR_IX2 ON DD_DATC_PROCDR (UUID_STRING);
CREATE INDEX DD_DATC_INDX_1_IX1 ON DD_DATC_INDX (LGCL_ID);
CREATE INDEX DD_DATC_INDX_1_IX2 ON DD_DATC_INDX (UUID_STRING);
CREATE INDEX DD_DATC_PRIMRY_IX1 ON DD_DATC_PRIMRYKY (LGCL_ID);
CREATE INDEX DD_DATC_PRIMRY_IX2 ON DD_DATC_PRIMRYKY (UUID_STRING);
CREATE INDEX DD_DATC_UNIQCN_IX1 ON DD_DATC_UNIQCNSTRN (LGCL_ID);
CREATE INDEX DD_DATC_UNIQCN_IX2 ON DD_DATC_UNIQCNSTRN (UUID_STRING);
CREATE INDEX DD_DATC_PROCDR_I_1 ON DD_DATC_PROCDRPRMT (LGCL_ID);
CREATE INDEX DD_DATC_PROCDR_I_2 ON DD_DATC_PROCDRPRMT (UUID_STRING);
CREATE INDEX DD_DATC_FORGNK_IX1 ON DD_DATC_FORGNKY (LGCL_ID);
CREATE INDEX DD_DATC_FORGNK_IX2 ON DD_DATC_FORGNKY (UUID_STRING);
CREATE INDEX DD_DATC_PROCDR_I_3 ON DD_DATC_PROCDRRSLT (LGCL_ID);
CREATE INDEX DD_DATC_PROCDR_I_4 ON DD_DATC_PROCDRRSLT (UUID_STRING);
CREATE INDEX DD_DATC_ACCSSP_IX1 ON DD_DATC_ACCSSPTTRN (LGCL_ID);
CREATE INDEX DD_DATC_ACCSSP_IX2 ON DD_DATC_ACCSSPTTRN (UUID_STRING);
CREATE INDEX DD_WEBS_OPERTN_IX1 ON DD_WEBS_OPERTN (LGCL_ID);
CREATE INDEX DD_WEBS_OPERTN_IX2 ON DD_WEBS_OPERTN (UUID_STRING);
CREATE INDEX DD_WEBS_INPT_IX1 ON DD_WEBS_INPT (LGCL_ID);
CREATE INDEX DD_WEBS_INPT_IX2 ON DD_WEBS_INPT (UUID_STRING);
CREATE INDEX DD_WEBS_OUTPT_IX1 ON DD_WEBS_OUTPT (LGCL_ID);
CREATE INDEX DD_WEBS_OUTPT_IX2 ON DD_WEBS_OUTPT (UUID_STRING);
CREATE INDEX DD_WEBS_INTRFC_IX1 ON DD_WEBS_INTRFC (LGCL_ID);
CREATE INDEX DD_WEBS_INTRFC_IX2 ON DD_WEBS_INTRFC (UUID_STRING);
CREATE INDEX DD_WEBS_SAMPLM_IX1 ON DD_WEBS_SAMPLMSSGS (LGCL_ID);
CREATE INDEX DD_WEBS_SAMPLM_IX2 ON DD_WEBS_SAMPLMSSGS (UUID_STRING);
CREATE INDEX DD_WEBS_SAMPLF_IX1 ON DD_WEBS_SAMPLFL (LGCL_ID);
CREATE INDEX DD_WEBS_SAMPLF_IX2 ON DD_WEBS_SAMPLFL (UUID_STRING);
CREATE INDEX DD_WEBS_SAMPLF_I_1 ON DD_WEBS_SAMPLFRMXS (LGCL_ID);
CREATE INDEX DD_WEBS_SAMPLF_I_2 ON DD_WEBS_SAMPLFRMXS (UUID_STRING);
CREATE INDEX DD_RELT_RELTNS_IX1 ON DD_RELT_RELTNSHP_1 (LGCL_ID);
CREATE INDEX DD_RELT_RELTNS_IX2 ON DD_RELT_RELTNSHP_1 (UUID_STRING);
CREATE INDEX DD_RELT_RELTNS_I_1 ON DD_RELT_RELTNSHP (LGCL_ID);
CREATE INDEX DD_RELT_RELTNS_I_2 ON DD_RELT_RELTNSHP (UUID_STRING);
CREATE INDEX DD_RELT_FILRFR_IX1 ON DD_RELT_FILRFRNC (LGCL_ID);
CREATE INDEX DD_RELT_FILRFR_IX2 ON DD_RELT_FILRFRNC (UUID_STRING);
CREATE INDEX DD_RELT_RELTNS_I_3 ON DD_RELT_RELTNSHPRL (LGCL_ID);
CREATE INDEX DD_RELT_RELTNS_I_4 ON DD_RELT_RELTNSHPRL (UUID_STRING);
CREATE INDEX DD_RELT_PLACHL_IX1 ON DD_RELT_PLACHLDRRF (LGCL_ID);
CREATE INDEX DD_RELT_PLACHL_IX2 ON DD_RELT_PLACHLDRRF (UUID_STRING);
CREATE INDEX DD_RELT_URIRFR_IX1 ON DD_RELT_URIRFRNC (LGCL_ID);
CREATE INDEX DD_RELT_URIRFR_IX2 ON DD_RELT_URIRFRNC (UUID_STRING);
CREATE INDEX DD_RELT_RELTNS_I_5 ON DD_RELT_RELTNSHPFL (LGCL_ID);
CREATE INDEX DD_RELT_RELTNS_I_6 ON DD_RELT_RELTNSHPFL (UUID_STRING);
ALTER TABLE DD_INDEX
  ADD CONSTRAINT PK_UUID
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MDL
  ADD CONSTRAINT PK_MDL_UUID
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MTACLS_TYPE
  ADD CONSTRAINT PK_MTACLS_ID
    PRIMARY KEY (ID)
;
ALTER TABLE DD_MTAMDL
  ADD CONSTRAINT PK_MTAMDL_ID
    PRIMARY KEY (ID)
;
ALTER TABLE DD_TXN_LOG
  ADD CONSTRAINT PK_ID
    PRIMARY KEY (ID)
;
ALTER TABLE DD_FTRE
  ADD CONSTRAINT PK_FTRE_ID
    PRIMARY KEY (ID)
;
ALTER TABLE DD_TXN_STATES
  ADD CONSTRAINT TXN_STATE_PK
    PRIMARY KEY (ID)
;
ALTER TABLE DD_METM_PUSHDWNTYP
  ADD CONSTRAINT DD_METM_PKPSHDWNTY
    PRIMARY KEY (ID)
;
ALTER TABLE DD_METM_SCALRFNCTN
  ADD CONSTRAINT DD_METM_PK_SCLRFNC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_METM_FUNCTNPRMT
  ADD CONSTRAINT DD_METM_PK_FNCTNPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_METM_RETRNPRMTR
  ADD CONSTRAINT DD_METM_PK_RTRNPRM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_SEVRTY
  ADD CONSTRAINT DD_VIRT_PKSVRTY
    PRIMARY KEY (ID)
;
ALTER TABLE DD_VIRT_MODLCCSSBL
  ADD CONSTRAINT DD_VIRT_PKMDLCCSSB
    PRIMARY KEY (ID)
;
ALTER TABLE DD_VIRT_VIRTLDTBS
  ADD CONSTRAINT DD_VIRT_PK_VRTLDTB
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_MODLRFRNC
  ADD CONSTRAINT DD_VIRT_PK_MDLRFRN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_PROBLMMRKR
  ADD CONSTRAINT DD_VIRT_PK_PRBLMMR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_MODLSRC
  ADD CONSTRAINT DD_VIRT_PK_MDLSRC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_MODLSRCPRP
  ADD CONSTRAINT DD_VIRT_PK_MDLSRCP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_WSDLPTNS
  ADD CONSTRAINT DD_VIRT_PK_WSDLPTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_VIRT_NONMDLRFRN
  ADD CONSTRAINT DD_VIRT_PK_NNMDLRF
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_SOAPNCDNG
  ADD CONSTRAINT DD_XMLD_PKSPNCDNG
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XMLD_CHOCRRRMD
  ADD CONSTRAINT DD_XMLD_PKCHCRRRMD
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XMLD_VALTYP
  ADD CONSTRAINT DD_XMLD_PKVLTYP
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XMLD_BUILDSTTS
  ADD CONSTRAINT DD_XMLD_PKBLDSTTS
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XMLD_NORMLZTNTY
  ADD CONSTRAINT DD_XMLD_PKNRMLZTNT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XMLD_XMLFRGMNT
  ADD CONSTRAINT DD_XMLD_PK_XMLFRGM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLDCMNT
  ADD CONSTRAINT DD_XMLD_PK_XMLDCMN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLLMNT
  ADD CONSTRAINT DD_XMLD_PK_XMLLMNT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLTTRBT
  ADD CONSTRAINT DD_XMLD_PK_XMLTTRB
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLRT
  ADD CONSTRAINT DD_XMLD_PK_XMLRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLCMMNT
  ADD CONSTRAINT DD_XMLD_PK_XMLCMMN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLNMSPC
  ADD CONSTRAINT DD_XMLD_PK_XMLNMSP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLSQNC
  ADD CONSTRAINT DD_XMLD_PK_XMLSQNC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLLL
  ADD CONSTRAINT DD_XMLD_PK_XMLLL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLCHC
  ADD CONSTRAINT DD_XMLD_PK_XMLCHC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_PROCSSNGNS
  ADD CONSTRAINT DD_XMLD_PK_PRCSSNG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XMLD_XMLFRGMNTS
  ADD CONSTRAINT DD_XMLD_PK_XMLFR_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EATTRBT
  ADD CONSTRAINT DD_ECOR_PK_TTRBT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EANNTTN
  ADD CONSTRAINT DD_ECOR_PK_NNTTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_ECLSS
  ADD CONSTRAINT DD_ECOR_PK_CLSS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EDATTYP
  ADD CONSTRAINT DD_ECOR_PK_DTTYP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EENM
  ADD CONSTRAINT DD_ECOR_PK_NM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EENMLTRL
  ADD CONSTRAINT DD_ECOR_PK_NMLTRL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EFACTRY
  ADD CONSTRAINT DD_ECOR_PK_FCTRY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EOBJCT
  ADD CONSTRAINT DD_ECOR_PK_BJCT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EOPRTN
  ADD CONSTRAINT DD_ECOR_PK_PRTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EPACKG
  ADD CONSTRAINT DD_ECOR_PK_PCKG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EPARMTR
  ADD CONSTRAINT DD_ECOR_PK_PRMTR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_EREFRNC
  ADD CONSTRAINT DD_ECOR_PK_RFRNC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_ECOR_ESTRNGTSTR
  ADD CONSTRAINT DD_ECOR_PK_STRNGTS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_VISBLTYKND
  ADD CONSTRAINT DD_UML_PKVSBLTYKND
    PRIMARY KEY (ID)
;
ALTER TABLE DD_UML_PARMTRDRCTN
  ADD CONSTRAINT DD_UML_PKPRMTRDRCT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_UML_AGGRGTNKND
  ADD CONSTRAINT DD_UML_PKAGGRGTNKN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_UML_CALLCNCRRNC
  ADD CONSTRAINT DD_UML_PKCLLCNCRRN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_UML_PARMTRFFCTK
  ADD CONSTRAINT DD_UML_PKPRMTRFFCT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_UML_COMMNT
  ADD CONSTRAINT DD_UML_PK_CMMNT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_CLASS
  ADD CONSTRAINT DD_UML_PK_CLSS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PROPRTY
  ADD CONSTRAINT DD_UML_PK_PRPRTY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_OPERTN
  ADD CONSTRAINT DD_UML_PK_PRTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PARMTR
  ADD CONSTRAINT DD_UML_PK_PRMTR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PACKG
  ADD CONSTRAINT DD_UML_PK_PCKG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_ENUMRTN
  ADD CONSTRAINT DD_UML_PK_NMRTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_DATTYP
  ADD CONSTRAINT DD_UML_PK_DTTYP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_ENUMRTNLTRL
  ADD CONSTRAINT DD_UML_PK_NMRTNLTR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PRIMTVTYP
  ADD CONSTRAINT DD_UML_PK_PRMTVTYP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_CONSTRNT
  ADD CONSTRAINT DD_UML_PK_CNSTRNT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_LITRLBLN
  ADD CONSTRAINT DD_UML_PK_LTRLBLN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_LITRLSTRNG
  ADD CONSTRAINT DD_UML_PK_LTRLSTRN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_LITRLNLL
  ADD CONSTRAINT DD_UML_PK_LTRLNLL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_LITRLNTGR
  ADD CONSTRAINT DD_UML_PK_LTRLNTGR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_LITRLNLMTDN
  ADD CONSTRAINT DD_UML_PK_LTRLNLMT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_INSTNCSPCFC
  ADD CONSTRAINT DD_UML_PK_NSTNCSPC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_SLOT
  ADD CONSTRAINT DD_UML_PK_SLT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_GENRLZTN
  ADD CONSTRAINT DD_UML_PK_GNRLZTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_ELEMNTMPRT
  ADD CONSTRAINT DD_UML_PK_LMNTMPRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PACKGMPRT
  ADD CONSTRAINT DD_UML_PK_PCKGMPRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_ASSCTN
  ADD CONSTRAINT DD_UML_PK_SSCTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PACKGMRG
  ADD CONSTRAINT DD_UML_PK_PCKGMRG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_STERTYP
  ADD CONSTRAINT DD_UML_PK_STRTYP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PROFL
  ADD CONSTRAINT DD_UML_PK_PRFL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PROFLPPLCTN
  ADD CONSTRAINT DD_UML_PK_PRFLPPLC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_EXTNSN
  ADD CONSTRAINT DD_UML_PK_XTNSN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_EXTNSNND
  ADD CONSTRAINT DD_UML_PK_XTNSNND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_DEPNDNCY
  ADD CONSTRAINT DD_UML_PK_DPNDNCY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_GENRLZTNST
  ADD CONSTRAINT DD_UML_PK_GNRLZTNS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_ASSCTNCLSS
  ADD CONSTRAINT DD_UML_PK_SSCTNCLS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_MODL
  ADD CONSTRAINT DD_UML_PK_MDL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_INTRFC
  ADD CONSTRAINT DD_UML_PK_NTRFC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_STRNGXPRSSN
  ADD CONSTRAINT DD_UML_PK_STRNGXPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_DURTNNTRVL
  ADD CONSTRAINT DD_UML_PK_DRTNNTRV
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_TIMNTRVL
  ADD CONSTRAINT DD_UML_PK_TMNTRVL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_UML_PARMTRST
  ADD CONSTRAINT DD_UML_PK_PRMTRST
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_CASCNVRSN
  ADD CONSTRAINT DD_JDBC_PKCSCNVRSN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_JDBC_SOURCNMS
  ADD CONSTRAINT DD_JDBC_PKSRCNMS
    PRIMARY KEY (ID)
;
ALTER TABLE DD_JDBC_JDBCSRCPRP
  ADD CONSTRAINT DD_JDBC_PK_JDBCSRC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCDRVR
  ADD CONSTRAINT DD_JDBC_PK_JDBCDRV
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCSRC
  ADD CONSTRAINT DD_JDBC_PK_JDBCS_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCDRVRCN
  ADD CONSTRAINT DD_JDBC_PK_JDBCD_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCSRCCNT
  ADD CONSTRAINT DD_JDBC_PK_JDBCS_2
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCMPRTST
  ADD CONSTRAINT DD_JDBC_PK_JDBCMPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_JDBC_JDBCMPRTPT
  ADD CONSTRAINT DD_JDBC_PK_JDBCM_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_MODLTYP
  ADD CONSTRAINT DD_COR_PKMDLTYP
    PRIMARY KEY (ID)
;
ALTER TABLE DD_COR_ANNTTN
  ADD CONSTRAINT DD_COR_PK_NNTTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_ANNTTNCNTNR
  ADD CONSTRAINT DD_COR_PK_NNTTNCNT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_MODLNNTTN
  ADD CONSTRAINT DD_COR_PK_MDLNNTTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_LINK
  ADD CONSTRAINT DD_COR_PK_LNK
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_LINKCNTNR
  ADD CONSTRAINT DD_COR_PK_LNKCNTNR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_COR_MODLMPRT
  ADD CONSTRAINT DD_COR_PK_MDLMPRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_EXTN_XCLSS
  ADD CONSTRAINT DD_EXTN_PK_XCLSS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_EXTN_XPACKG
  ADD CONSTRAINT DD_EXTN_PK_XPCKG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_EXTN_XATTRBT
  ADD CONSTRAINT DD_EXTN_PK_XTTRBT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_EXTN_XENM
  ADD CONSTRAINT DD_EXTN_PK_XNM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_EXTN_XENMLTRL
  ADD CONSTRAINT DD_EXTN_PK_XNMLTRL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDTTRBTSCT
  ADD CONSTRAINT DD_XSD_PKXSDTTRBTS
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDCRDNLTY
  ADD CONSTRAINT DD_XSD_PKXSDCRDNLT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDCMPLXFNL
  ADD CONSTRAINT DD_XSD_PKXSDCMPLXF
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDCMPSTR
  ADD CONSTRAINT DD_XSD_PKXSDCMPSTR
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDCNSTRNT
  ADD CONSTRAINT DD_XSD_PKXSDCNSTRN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDCNTNTTYP
  ADD CONSTRAINT DD_XSD_PKXSDCNTNTT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDDRVTNMTH
  ADD CONSTRAINT DD_XSD_PKXSDDRVTNM
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDDGNSTCSV
  ADD CONSTRAINT DD_XSD_PKXSDDGNSTC
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDDSLLWDSB
  ADD CONSTRAINT DD_XSD_PKXSDDSLLWD
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDFRM
  ADD CONSTRAINT DD_XSD_PKXSDFRM
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDDNTTYCNS
  ADD CONSTRAINT DD_XSD_PKXSDDNTTYC
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDNMSPCCNS
  ADD CONSTRAINT DD_XSD_PKXSDNMSPCC
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDRDRD
  ADD CONSTRAINT DD_XSD_PKXSDRDRD
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDPRCSSCNT
  ADD CONSTRAINT DD_XSD_PKXSDPRCSSC
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDPRHBTDSB
  ADD CONSTRAINT DD_XSD_PKXSDPRHBTD
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDSMPLFNL
  ADD CONSTRAINT DD_XSD_PKXSDSMPLFN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDSBSTTTNG
  ADD CONSTRAINT DD_XSD_PKXSDSBSTTT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDVRTY
  ADD CONSTRAINT DD_XSD_PKXSDVRTY
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDWHTSPC
  ADD CONSTRAINT DD_XSD_PKXSDWHTSPC
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDXPTHVRTY
  ADD CONSTRAINT DD_XSD_PKXSDXPTHVR
    PRIMARY KEY (ID)
;
ALTER TABLE DD_XSD_XSDNNTTN
  ADD CONSTRAINT DD_XSD_PK_XSDNNTTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDTTRBTDCL
  ADD CONSTRAINT DD_XSD_PK_XSDTTRBT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDTTRBTGRP
  ADD CONSTRAINT DD_XSD_PK_XSDTTR_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDTTRBTS
  ADD CONSTRAINT DD_XSD_PK_XSDTTR_2
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDBNDDFCT
  ADD CONSTRAINT DD_XSD_PK_XSDBNDDF
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDCRDNLTYF
  ADD CONSTRAINT DD_XSD_PK_XSDCRDNL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_PK_XSDCMPLX
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDDGNSTC
  ADD CONSTRAINT DD_XSD_PK_XSDDGNST
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_PK_XSDLMNTD
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDNMRTNFCT
  ADD CONSTRAINT DD_XSD_PK_XSDNMRTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDFRCTNDGT
  ADD CONSTRAINT DD_XSD_PK_XSDFRCTN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDDNTTYC_1
  ADD CONSTRAINT DD_XSD_PK_XSDDNTTY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMPRT
  ADD CONSTRAINT DD_XSD_PK_XSDMPRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDNCLD
  ADD CONSTRAINT DD_XSD_PK_XSDNCLD
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDLNGTHFCT
  ADD CONSTRAINT DD_XSD_PK_XSDLNGTH
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMXXCLSVF
  ADD CONSTRAINT DD_XSD_PK_XSDMXXCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMXNCLSVF
  ADD CONSTRAINT DD_XSD_PK_XSDMXNCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMXLNGTHF
  ADD CONSTRAINT DD_XSD_PK_XSDMXLNG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMNXCLSVF
  ADD CONSTRAINT DD_XSD_PK_XSDMNXCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMNNCLSVF
  ADD CONSTRAINT DD_XSD_PK_XSDMNNCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMNLNGTHF
  ADD CONSTRAINT DD_XSD_PK_XSDMNLNG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMDLGRP
  ADD CONSTRAINT DD_XSD_PK_XSDMDLGR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDMDLGRPDF
  ADD CONSTRAINT DD_XSD_PK_XSDMDL_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDNTTNDCLR
  ADD CONSTRAINT DD_XSD_PK_XSDNTTND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDNMRCFCT
  ADD CONSTRAINT DD_XSD_PK_XSDNMRCF
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDRDRDFCT
  ADD CONSTRAINT DD_XSD_PK_XSDRDRDF
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDPRTCL
  ADD CONSTRAINT DD_XSD_PK_XSDPRTCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDPTTRNFCT
  ADD CONSTRAINT DD_XSD_PK_XSDPTTRN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDRDFN
  ADD CONSTRAINT DD_XSD_PK_XSDRDFN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDSCHM
  ADD CONSTRAINT DD_XSD_PK_XSDSCHM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDSMPLTYPD
  ADD CONSTRAINT DD_XSD_PK_XSDSMPLT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDTTLDGTSF
  ADD CONSTRAINT DD_XSD_PK_XSDTTLDG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDWHTSPCFC
  ADD CONSTRAINT DD_XSD_PK_XSDWHTSP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDWLDCRD
  ADD CONSTRAINT DD_XSD_PK_XSDWLDCR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_XSD_XSDXPTHDFNT
  ADD CONSTRAINT DD_XSD_PK_XSDXPTHD
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_RECRSNRRRM
  ADD CONSTRAINT DD_TRAN_PKRCRSNRRR
    PRIMARY KEY (ID)
;
ALTER TABLE DD_TRAN_JOINTYP
  ADD CONSTRAINT DD_TRAN_PKJNTYP
    PRIMARY KEY (ID)
;
ALTER TABLE DD_TRAN_SORTDRCTN
  ADD CONSTRAINT DD_TRAN_PKSRTDRCTN
    PRIMARY KEY (ID)
;
ALTER TABLE DD_TRAN_TRANSFRMTN
  ADD CONSTRAINT DD_TRAN_PK_TRNSFRM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SQLTRNSFRM
  ADD CONSTRAINT DD_TRAN_PK_SQLTRNS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_TRANSFRM_1
  ADD CONSTRAINT DD_TRAN_PK_TRNSF_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SQLLS
  ADD CONSTRAINT DD_TRAN_PK_SQLLS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SQLTRNSF_1
  ADD CONSTRAINT DD_TRAN_PK_SQLTR_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_FRAGMNTMPP
  ADD CONSTRAINT DD_TRAN_PK_FRGMNTM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_TREMPPNGRT
  ADD CONSTRAINT DD_TRAN_PK_TRMPPNG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_MAPPNGCLSS
  ADD CONSTRAINT DD_TRAN_PK_MPPNGCL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_MAPPNGCL_1
  ADD CONSTRAINT DD_TRAN_PK_MPPNG_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_STAGNGTBL
  ADD CONSTRAINT DD_TRAN_PK_STGNGTB
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_MAPPNGCL_2
  ADD CONSTRAINT DD_TRAN_PK_MPPNG_2
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_MAPPNGCL_3
  ADD CONSTRAINT DD_TRAN_PK_MPPNG_3
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_INPTPRMTR
  ADD CONSTRAINT DD_TRAN_PK_NPTPRMT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_INPTST
  ADD CONSTRAINT DD_TRAN_PK_NPTST
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_INPTBNDNG
  ADD CONSTRAINT DD_TRAN_PK_NPTBNDN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_DATFLWMPPN
  ADD CONSTRAINT DD_TRAN_PK_DTFLWMP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_DATFLWND
  ADD CONSTRAINT DD_TRAN_PK_DTFLWND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_DATFLWLNK
  ADD CONSTRAINT DD_TRAN_PK_DTFLWLN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_EXPRSSN
  ADD CONSTRAINT DD_TRAN_PK_XPRSSN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_TARGTND
  ADD CONSTRAINT DD_TRAN_PK_TRGTND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SOURCND
  ADD CONSTRAINT DD_TRAN_PK_SRCND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_OPERTNNDGR
  ADD CONSTRAINT DD_TRAN_PK_PRTNNDG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_OPERTNND
  ADD CONSTRAINT DD_TRAN_PK_PRTNND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_JOINND
  ADD CONSTRAINT DD_TRAN_PK_JNND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_UNINND
  ADD CONSTRAINT DD_TRAN_PK_NNND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_PROJCTNND
  ADD CONSTRAINT DD_TRAN_PK_PRJCTNN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_FILTRND
  ADD CONSTRAINT DD_TRAN_PK_FLTRND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_GROPNGND
  ADD CONSTRAINT DD_TRAN_PK_GRPNGND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_DUPRMVLND
  ADD CONSTRAINT DD_TRAN_PK_DPRMVLN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SORTND
  ADD CONSTRAINT DD_TRAN_PK_SRTND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_TRAN_SQLND
  ADD CONSTRAINT DD_TRAN_PK_SQLND
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_NULLBLTYP
  ADD CONSTRAINT DD_RELT_PKNLLBLTYP
    PRIMARY KEY (ID)
;
ALTER TABLE DD_RELT_DIRCTNKND
  ADD CONSTRAINT DD_RELT_PKDRCTNKND
    PRIMARY KEY (ID)
;
ALTER TABLE DD_RELT_MULTPLCTYK
  ADD CONSTRAINT DD_RELT_PKMLTPLCTY
    PRIMARY KEY (ID)
;
ALTER TABLE DD_RELT_SEARCHBLTY
  ADD CONSTRAINT DD_RELT_PKSRCHBLTY
    PRIMARY KEY (ID)
;
ALTER TABLE DD_RELT_COLMN
  ADD CONSTRAINT DD_RELT_PK_CLMN
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_SCHM
  ADD CONSTRAINT DD_RELT_PK_SCHM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_PRIMRYKY
  ADD CONSTRAINT DD_RELT_PK_PRMRYKY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_FORGNKY
  ADD CONSTRAINT DD_RELT_PK_FRGNKY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_VIEW
  ADD CONSTRAINT DD_RELT_PK_VW
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_CATLG
  ADD CONSTRAINT DD_RELT_PK_CTLG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_PROCDR
  ADD CONSTRAINT DD_RELT_PK_PRCDR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_INDX
  ADD CONSTRAINT DD_RELT_PK_NDX
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_PROCDRPRMT
  ADD CONSTRAINT DD_RELT_PK_PRCDRPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_UNIQCNSTRN
  ADD CONSTRAINT DD_RELT_PK_NQCNSTR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_ACCSSPTTRN
  ADD CONSTRAINT DD_RELT_PK_CCSSPTT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_LOGCLRLTNS
  ADD CONSTRAINT DD_RELT_PK_LGCLRLT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_LOGCLRLT_1
  ADD CONSTRAINT DD_RELT_PK_LGCLR_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_BASTBL
  ADD CONSTRAINT DD_RELT_PK_BSTBL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_PROCDRRSLT
  ADD CONSTRAINT DD_RELT_PK_PRCDRRS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_MAPPNGHLPR
  ADD CONSTRAINT DD_MAPP_PK_MPPNGHL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_MAPPNG
  ADD CONSTRAINT DD_MAPP_PK_MPPNG
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_TYPCNVRTR
  ADD CONSTRAINT DD_MAPP_PK_TYPCNVR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_FUNCTNPR
  ADD CONSTRAINT DD_MAPP_PK_FNCTNPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_FUNCTNNMPR
  ADD CONSTRAINT DD_MAPP_PK_FNCTNNM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_MAPPNGSTRT
  ADD CONSTRAINT DD_MAPP_PK_MPPNGST
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_MAPPNGRT
  ADD CONSTRAINT DD_MAPP_PK_MPPNGRT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MAPP_COMPLXTYPC
  ADD CONSTRAINT DD_MAPP_PK_CMPLXTY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_CATGRY
  ADD CONSTRAINT DD_DATC_PK_CTGRY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_GROP
  ADD CONSTRAINT DD_DATC_PK_GRP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_ELEMNT
  ADD CONSTRAINT DD_DATC_PK_LMNT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_PROCDR
  ADD CONSTRAINT DD_DATC_PK_PRCDR_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_INDX
  ADD CONSTRAINT DD_DATC_PK_NDX_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_PRIMRYKY
  ADD CONSTRAINT DD_DATC_PK_PRMRYKY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_UNIQCNSTRN
  ADD CONSTRAINT DD_DATC_PK_NQCNSTR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_PROCDRPRMT
  ADD CONSTRAINT DD_DATC_PK_PRCDRPR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_FORGNKY
  ADD CONSTRAINT DD_DATC_PK_FRGNKY
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_PROCDRRSLT
  ADD CONSTRAINT DD_DATC_PK_PRCDRRS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_DATC_ACCSSPTTRN
  ADD CONSTRAINT DD_DATC_PK_CCSSPTT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_OPERTN
  ADD CONSTRAINT DD_WEBS_PK_PRTN_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_INPT
  ADD CONSTRAINT DD_WEBS_PK_NPT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_OUTPT
  ADD CONSTRAINT DD_WEBS_PK_TPT
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_INTRFC
  ADD CONSTRAINT DD_WEBS_PK_NTRFC_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_SAMPLMSSGS
  ADD CONSTRAINT DD_WEBS_PK_SMPLMSS
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_SAMPLFL
  ADD CONSTRAINT DD_WEBS_PK_SMPLFL
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_WEBS_SAMPLFRMXS
  ADD CONSTRAINT DD_WEBS_PK_SMPLFRM
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_RELTNSHPTY
  ADD CONSTRAINT DD_RELT_PKRLTNSHPT
    PRIMARY KEY (ID)
;
ALTER TABLE DD_RELT_RELTNSHP_1
  ADD CONSTRAINT DD_RELT_PK_RLTNSHP
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_RELTNSHP
  ADD CONSTRAINT DD_RELT_PK_RLTNS_1
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_FILRFRNC
  ADD CONSTRAINT DD_RELT_PK_FLRFRNC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_RELTNSHPRL
  ADD CONSTRAINT DD_RELT_PK_RLTNS_2
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_PLACHLDRRF
  ADD CONSTRAINT DD_RELT_PK_PLCHLDR
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_URIRFRNC
  ADD CONSTRAINT DD_RELT_PK_RRFRNC
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_RELT_RELTNSHPFL
  ADD CONSTRAINT DD_RELT_PK_RLTNS_3
    PRIMARY KEY (UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_INDEX
  ADD CONSTRAINT FK_MTACLS_ID
    FOREIGN KEY (MTACLS_TYPE_ID)
    REFERENCES DD_MTACLS_TYPE(ID)
;
ALTER TABLE DD_INDEX
  ADD CONSTRAINT FK_MDL_UUID
   FOREIGN KEY (MDL_UUID1,MDL_UUID2,TXN_ID)
    REFERENCES DD_MDL(UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_INDEX
  ADD CONSTRAINT FK_TXN_ID
    FOREIGN KEY (TXN_ID)
    REFERENCES DD_TXN_LOG(ID)
;
ALTER TABLE DD_MDL
  ADD CONSTRAINT FK_TXN_ID_1
   FOREIGN KEY (TXN_ID)
    REFERENCES DD_TXN_LOG(ID)
;
ALTER TABLE DD_MTACLS_TYPE
  ADD CONSTRAINT FK_MTAMDL_ID
    FOREIGN KEY (MTAMDL_ID)
    REFERENCES DD_MTAMDL(ID)
;
ALTER TABLE DD_TXN_LOG
  ADD CONSTRAINT TXN_STATE_ID
   FOREIGN KEY (TXN_STATE)
    REFERENCES DD_TXN_STATES(ID)
;
ALTER TABLE DD_RELATIONSHIPS
  ADD CONSTRAINT FK_TXN_ID_2
    FOREIGN KEY (TXN_ID)
    REFERENCES DD_TXN_LOG(ID)
;
ALTER TABLE DD_RELATIONSHIPS
  ADD CONSTRAINT FK_FTRE_ID
   FOREIGN KEY (REL_FTRE_ID)
    REFERENCES DD_FTRE(ID)
;
ALTER TABLE DD_MDL_MTAMDL
  ADD CONSTRAINT FK_MDL_UUID_1
    FOREIGN KEY (MDL_UUID1,MDL_UUID2,TXN_ID)
    REFERENCES DD_MDL(UUID1,UUID2,TXN_ID)
;
ALTER TABLE DD_MDL_MTAMDL
  ADD CONSTRAINT FK_MTAMDL_ID_1
    FOREIGN KEY (MTAMDL_ID)
    REFERENCES DD_MTAMDL(ID)
;
ALTER TABLE DD_MDL_MTAMDL
  ADD CONSTRAINT FK_TXN_ID_4
    FOREIGN KEY (TXN_ID)
    REFERENCES DD_TXN_LOG(ID)
;
ALTER TABLE DD_FTRE
  ADD CONSTRAINT FK_MTACLS_ID_1
    FOREIGN KEY (MTACLS_TYPE_ID)
    REFERENCES DD_MTACLS_TYPE(ID)
;
ALTER TABLE DD_FTRE
  ADD CONSTRAINT FK_MTAMDL_ID_2
    FOREIGN KEY (MTAMDL_ID)
    REFERENCES DD_MTAMDL(ID)
;
ALTER TABLE DD_METM_SCALRFNCTN
  ADD CONSTRAINT DD_METM_FK_PSHDWN
    FOREIGN KEY (PUSHDWN)
    REFERENCES DD_METM_PUSHDWNTYP(ID)
;
ALTER TABLE DD_VIRT_VIRTLDTBS
  ADD CONSTRAINT DD_VIRT_FK_SVRTY
    FOREIGN KEY (SEVRTY)
    REFERENCES DD_VIRT_SEVRTY(ID)
;
ALTER TABLE DD_VIRT_MODLRFRNC
  ADD CONSTRAINT DD_VIRT_FK_SVRTY_1
    FOREIGN KEY (SEVRTY)
    REFERENCES DD_VIRT_SEVRTY(ID)
;
ALTER TABLE DD_VIRT_MODLRFRNC
  ADD CONSTRAINT DD_VIRT_FK_CCSSBLT
    FOREIGN KEY (ACCSSBLTY)
    REFERENCES DD_VIRT_MODLCCSSBL(ID)
;
ALTER TABLE DD_VIRT_PROBLMMRKR
  ADD CONSTRAINT DD_VIRT_FK_SVRTY_2
    FOREIGN KEY (SEVRTY)
    REFERENCES DD_VIRT_SEVRTY(ID)
;
ALTER TABLE DD_XMLD_XMLDCMNT
  ADD CONSTRAINT DD_XMLD_FK_SPNCDNG
    FOREIGN KEY (SOAPNCDNG)
    REFERENCES DD_XMLD_SOAPNCDNG(ID)
;
ALTER TABLE DD_XMLD_XMLLMNT
  ADD CONSTRAINT DD_XMLD_FK_BLDSTT
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLLMNT
  ADD CONSTRAINT DD_XMLD_FK_VLTYP
    FOREIGN KEY (VALTYP)
    REFERENCES DD_XMLD_VALTYP(ID)
;
ALTER TABLE DD_XMLD_XMLTTRBT
  ADD CONSTRAINT DD_XMLD_FK_BLDST_1
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLTTRBT
  ADD CONSTRAINT DD_XMLD_FK_VLTYP_1
    FOREIGN KEY (VALTYP)
    REFERENCES DD_XMLD_VALTYP(ID)
;
ALTER TABLE DD_XMLD_XMLRT
  ADD CONSTRAINT DD_XMLD_FK_BLDST_2
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLRT
  ADD CONSTRAINT DD_XMLD_FK_VLTYP_2
    FOREIGN KEY (VALTYP)
    REFERENCES DD_XMLD_VALTYP(ID)
;
ALTER TABLE DD_XMLD_XMLSQNC
  ADD CONSTRAINT DD_XMLD_FK_BLDST_3
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLLL
  ADD CONSTRAINT DD_XMLD_FK_BLDST_4
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLCHC
  ADD CONSTRAINT DD_XMLD_FK_BLDST_5
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_XMLD_XMLCHC
  ADD CONSTRAINT DD_XMLD_FK_DFLTRRR
    FOREIGN KEY (DEFLTRRRMD)
    REFERENCES DD_XMLD_CHOCRRRMD(ID)
;
ALTER TABLE DD_XMLD_XMLFRGMNTS
  ADD CONSTRAINT DD_XMLD_FK_BLDST_6
    FOREIGN KEY (BUILDSTT)
    REFERENCES DD_XMLD_BUILDSTTS(ID)
;
ALTER TABLE DD_UML_CLASS
  ADD CONSTRAINT DD_UML_FK_VSBLTY
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_CLASS
  ADD CONSTRAINT DD_UML_FK_PCKGBLLM
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PROPRTY
  ADD CONSTRAINT DD_UML_FK_VSBLTY_1
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PROPRTY
  ADD CONSTRAINT DD_UML_FK_GGRGTN
    FOREIGN KEY (AGGRGTN)
    REFERENCES DD_UML_AGGRGTNKND(ID)
;
ALTER TABLE DD_UML_OPERTN
  ADD CONSTRAINT DD_UML_FK_VSBLTY_2
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_OPERTN
  ADD CONSTRAINT DD_UML_FK_CNCRRNCY
    FOREIGN KEY (CONCRRNCY)
    REFERENCES DD_UML_CALLCNCRRNC(ID)
;
ALTER TABLE DD_UML_PARMTR
  ADD CONSTRAINT DD_UML_FK_VSBLTY_3
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PARMTR
  ADD CONSTRAINT DD_UML_FK_DRCTN
    FOREIGN KEY (DIRCTN)
    REFERENCES DD_UML_PARMTRDRCTN(ID)
;
ALTER TABLE DD_UML_PARMTR
  ADD CONSTRAINT DD_UML_FK_FFCT
    FOREIGN KEY (EFFCT)
    REFERENCES DD_UML_PARMTRFFCTK(ID)
;
ALTER TABLE DD_UML_PACKG
  ADD CONSTRAINT DD_UML_FK_VSBLTY_4
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PACKG
  ADD CONSTRAINT DD_UML_FK_PCKGBL_1
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ENUMRTN
  ADD CONSTRAINT DD_UML_FK_VSBLTY_5
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ENUMRTN
  ADD CONSTRAINT DD_UML_FK_PCKGBL_2
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_DATTYP
  ADD CONSTRAINT DD_UML_FK_VSBLTY_6
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_DATTYP
  ADD CONSTRAINT DD_UML_FK_PCKGBL_3
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ENUMRTNLTRL
  ADD CONSTRAINT DD_UML_FK_VSBLTY_7
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ENUMRTNLTRL
  ADD CONSTRAINT DD_UML_FK_PCKGBL_4
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PRIMTVTYP
  ADD CONSTRAINT DD_UML_FK_VSBLTY_8
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PRIMTVTYP
  ADD CONSTRAINT DD_UML_FK_PCKGBL_5
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_CONSTRNT
  ADD CONSTRAINT DD_UML_FK_VSBLTY_9
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_CONSTRNT
  ADD CONSTRAINT DD_UML_FK_PCKGBL_6
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_LITRLBLN
  ADD CONSTRAINT DD_UML_FK_VSBLT_10
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_LITRLSTRNG
  ADD CONSTRAINT DD_UML_FK_VSBLT_11
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_LITRLNLL
  ADD CONSTRAINT DD_UML_FK_VSBLT_12
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_LITRLNTGR
  ADD CONSTRAINT DD_UML_FK_VSBLT_13
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_LITRLNLMTDN
  ADD CONSTRAINT DD_UML_FK_VSBLT_14
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_INSTNCSPCFC
  ADD CONSTRAINT DD_UML_FK_VSBLT_15
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_INSTNCSPCFC
  ADD CONSTRAINT DD_UML_FK_PCKGBL_7
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ELEMNTMPRT
  ADD CONSTRAINT DD_UML_FK_VSBLT_16
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PACKGMPRT
  ADD CONSTRAINT DD_UML_FK_VSBLT_17
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ASSCTN
  ADD CONSTRAINT DD_UML_FK_VSBLT_18
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ASSCTN
  ADD CONSTRAINT DD_UML_FK_PCKGBL_8
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_STERTYP
  ADD CONSTRAINT DD_UML_FK_VSBLT_19
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_STERTYP
  ADD CONSTRAINT DD_UML_FK_PCKGBL_9
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PROFL
  ADD CONSTRAINT DD_UML_FK_VSBLT_20
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PROFL
  ADD CONSTRAINT DD_UML_FK_PCKGB_10
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PROFLPPLCTN
  ADD CONSTRAINT DD_UML_FK_VSBLT_21
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_EXTNSN
  ADD CONSTRAINT DD_UML_FK_VSBLT_22
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_EXTNSN
  ADD CONSTRAINT DD_UML_FK_PCKGB_11
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_EXTNSNND
  ADD CONSTRAINT DD_UML_FK_VSBLT_23
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_EXTNSNND
  ADD CONSTRAINT DD_UML_FK_GGRGTN_1
    FOREIGN KEY (AGGRGTN)
    REFERENCES DD_UML_AGGRGTNKND(ID)
;
ALTER TABLE DD_UML_DEPNDNCY
  ADD CONSTRAINT DD_UML_FK_VSBLT_24
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_DEPNDNCY
  ADD CONSTRAINT DD_UML_FK_PCKGB_12
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_GENRLZTNST
  ADD CONSTRAINT DD_UML_FK_VSBLT_25
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_GENRLZTNST
  ADD CONSTRAINT DD_UML_FK_PCKGB_13
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ASSCTNCLSS
  ADD CONSTRAINT DD_UML_FK_VSBLT_26
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_ASSCTNCLSS
  ADD CONSTRAINT DD_UML_FK_PCKGB_14
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_MODL
  ADD CONSTRAINT DD_UML_FK_VSBLT_27
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_MODL
  ADD CONSTRAINT DD_UML_FK_PCKGB_15
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_INTRFC
  ADD CONSTRAINT DD_UML_FK_VSBLT_28
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_INTRFC
  ADD CONSTRAINT DD_UML_FK_PCKGB_16
    FOREIGN KEY (PACKGBLLMNT_VSBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_DURTNNTRVL
  ADD CONSTRAINT DD_UML_FK_VSBLT_29
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_TIMNTRVL
  ADD CONSTRAINT DD_UML_FK_VSBLT_30
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_UML_PARMTRST
  ADD CONSTRAINT DD_UML_FK_VSBLT_31
    FOREIGN KEY (VISBLTY)
    REFERENCES DD_UML_VISBLTYKND(ID)
;
ALTER TABLE DD_JDBC_JDBCMPRTST
  ADD CONSTRAINT DD_JDBC_FK_CNVRTCS
    FOREIGN KEY (CONVRTCSNMDL)
    REFERENCES DD_JDBC_CASCNVRSN(ID)
;
ALTER TABLE DD_JDBC_JDBCMPRTST
  ADD CONSTRAINT DD_JDBC_FK_GNRTSRC
    FOREIGN KEY (GENRTSRCNMSNMDL)
    REFERENCES DD_JDBC_SOURCNMS(ID)
;
ALTER TABLE DD_COR_MODLNNTTN
  ADD CONSTRAINT DD_COR_FK_MDLTYP
    FOREIGN KEY (MODLTYP)
    REFERENCES DD_COR_MODLTYP(ID)
;
ALTER TABLE DD_COR_MODLMPRT
  ADD CONSTRAINT DD_COR_FK_MDLTYP_1
    FOREIGN KEY (MODLTYP)
    REFERENCES DD_COR_MODLTYP(ID)
;
ALTER TABLE DD_XSD_XSDTTRBTDCL
  ADD CONSTRAINT DD_XSD_FK_CNSTRNT
    FOREIGN KEY (CONSTRNT)
    REFERENCES DD_XSD_XSDCNSTRNT(ID)
;
ALTER TABLE DD_XSD_XSDTTRBTDCL
  ADD CONSTRAINT DD_XSD_FK_FRM
    FOREIGN KEY (FORM)
    REFERENCES DD_XSD_XSDFRM(ID)
;
ALTER TABLE DD_XSD_XSDTTRBTS
  ADD CONSTRAINT DD_XSD_FK_CNSTRN_1
    FOREIGN KEY (CONSTRNT)
    REFERENCES DD_XSD_XSDCNSTRNT(ID)
;
ALTER TABLE DD_XSD_XSDTTRBTS
  ADD CONSTRAINT DD_XSD_FK_S
    FOREIGN KEY (USE1)
    REFERENCES DD_XSD_XSDTTRBTSCT(ID)
;
ALTER TABLE DD_XSD_XSDCRDNLTYF
  ADD CONSTRAINT DD_XSD_FK_VL
    FOREIGN KEY (VAL)
    REFERENCES DD_XSD_XSDCRDNLTY(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_DRVTNMTH
    FOREIGN KEY (DERVTNMTHD)
    REFERENCES DD_XSD_XSDDRVTNMTH(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_FNL
    FOREIGN KEY (FINL)
    REFERENCES DD_XSD_XSDCMPLXFNL(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_CNTNTTYP
    FOREIGN KEY (CONTNTTYPCTGRY)
    REFERENCES DD_XSD_XSDCNTNTTYP(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_PRHBTDSB
    FOREIGN KEY (PROHBTDSBSTTTNS)
    REFERENCES DD_XSD_XSDPRHBTDSB(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_LXCLFNL
    FOREIGN KEY (LEXCLFNL)
    REFERENCES DD_XSD_XSDCMPLXFNL(ID)
;
ALTER TABLE DD_XSD_XSDCMPLXTYP
  ADD CONSTRAINT DD_XSD_FK_BLCK
    FOREIGN KEY (BLOCK)
    REFERENCES DD_XSD_XSDPRHBTDSB(ID)
;
ALTER TABLE DD_XSD_XSDDGNSTC
  ADD CONSTRAINT DD_XSD_FK_SVRTY
    FOREIGN KEY (SEVRTY)
    REFERENCES DD_XSD_XSDDGNSTCSV(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_CNSTRN_2
    FOREIGN KEY (CONSTRNT)
    REFERENCES DD_XSD_XSDCNSTRNT(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_FRM_1
    FOREIGN KEY (FORM)
    REFERENCES DD_XSD_XSDFRM(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_DSLLWDSB
    FOREIGN KEY (DISLLWDSBSTTTNS)
    REFERENCES DD_XSD_XSDDSLLWDSB(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_SBSTTTNG
    FOREIGN KEY (SUBSTTTNGRPXCLSNS)
    REFERENCES DD_XSD_XSDSBSTTTNG(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_LXCLFN_1
    FOREIGN KEY (LEXCLFNL)
    REFERENCES DD_XSD_XSDPRHBTDSB(ID)
;
ALTER TABLE DD_XSD_XSDLMNTDCLR
  ADD CONSTRAINT DD_XSD_FK_BLCK_1
    FOREIGN KEY (BLOCK)
    REFERENCES DD_XSD_XSDDSLLWDSB(ID)
;
ALTER TABLE DD_XSD_XSDDNTTYC_1
  ADD CONSTRAINT DD_XSD_FK_DNTTYCNS
    FOREIGN KEY (IDENTTYCNSTRNTCTGRY)
    REFERENCES DD_XSD_XSDDNTTYCNS(ID)
;
ALTER TABLE DD_XSD_XSDMDLGRP
  ADD CONSTRAINT DD_XSD_FK_CMPSTR
    FOREIGN KEY (COMPSTR)
    REFERENCES DD_XSD_XSDCMPSTR(ID)
;
ALTER TABLE DD_XSD_XSDRDRDFCT
  ADD CONSTRAINT DD_XSD_FK_VL_1
    FOREIGN KEY (VAL)
    REFERENCES DD_XSD_XSDRDRD(ID)
;
ALTER TABLE DD_XSD_XSDSCHM
  ADD CONSTRAINT DD_XSD_FK_TTRBTFRM
    FOREIGN KEY (ATTRBTFRMDFLT)
    REFERENCES DD_XSD_XSDFRM(ID)
;
ALTER TABLE DD_XSD_XSDSCHM
  ADD CONSTRAINT DD_XSD_FK_LMNTFRMD
    FOREIGN KEY (ELEMNTFRMDFLT)
    REFERENCES DD_XSD_XSDFRM(ID)
;
ALTER TABLE DD_XSD_XSDSCHM
  ADD CONSTRAINT DD_XSD_FK_FNLDFLT
    FOREIGN KEY (FINLDFLT)
    REFERENCES DD_XSD_XSDPRHBTDSB(ID)
;
ALTER TABLE DD_XSD_XSDSCHM
  ADD CONSTRAINT DD_XSD_FK_BLCKDFLT
    FOREIGN KEY (BLOCKDFLT)
    REFERENCES DD_XSD_XSDDSLLWDSB(ID)
;
ALTER TABLE DD_XSD_XSDSMPLTYPD
  ADD CONSTRAINT DD_XSD_FK_VRTY
    FOREIGN KEY (VARTY)
    REFERENCES DD_XSD_XSDVRTY(ID)
;
ALTER TABLE DD_XSD_XSDSMPLTYPD
  ADD CONSTRAINT DD_XSD_FK_FNL_1
    FOREIGN KEY (FINL)
    REFERENCES DD_XSD_XSDSMPLFNL(ID)
;
ALTER TABLE DD_XSD_XSDSMPLTYPD
  ADD CONSTRAINT DD_XSD_FK_LXCLFN_2
    FOREIGN KEY (LEXCLFNL)
    REFERENCES DD_XSD_XSDSMPLFNL(ID)
;
ALTER TABLE DD_XSD_XSDWHTSPCFC
  ADD CONSTRAINT DD_XSD_FK_VL_2
    FOREIGN KEY (VAL)
    REFERENCES DD_XSD_XSDWHTSPC(ID)
;
ALTER TABLE DD_XSD_XSDWLDCRD
  ADD CONSTRAINT DD_XSD_FK_NMSPCCNS
    FOREIGN KEY (NAMSPCCNSTRNTCTGRY)
    REFERENCES DD_XSD_XSDNMSPCCNS(ID)
;
ALTER TABLE DD_XSD_XSDWLDCRD
  ADD CONSTRAINT DD_XSD_FK_PRCSSCNT
    FOREIGN KEY (PROCSSCNTNTS)
    REFERENCES DD_XSD_XSDPRCSSCNT(ID)
;
ALTER TABLE DD_XSD_XSDXPTHDFNT
  ADD CONSTRAINT DD_XSD_FK_VRTY_1
    FOREIGN KEY (VARTY)
    REFERENCES DD_XSD_XSDXPTHVRTY(ID)
;
ALTER TABLE DD_TRAN_MAPPNGCLSS
  ADD CONSTRAINT DD_TRAN_FK_RCRSNLM
    FOREIGN KEY (RECRSNLMTRRRMD)
    REFERENCES DD_TRAN_RECRSNRRRM(ID)
;
ALTER TABLE DD_TRAN_STAGNGTBL
  ADD CONSTRAINT DD_TRAN_FK_RCRSN_1
    FOREIGN KEY (RECRSNLMTRRRMD)
    REFERENCES DD_TRAN_RECRSNRRRM(ID)
;
ALTER TABLE DD_TRAN_JOINND
  ADD CONSTRAINT DD_TRAN_FK_TYP
    FOREIGN KEY (TYP)
    REFERENCES DD_TRAN_JOINTYP(ID)
;
ALTER TABLE DD_RELT_COLMN
  ADD CONSTRAINT DD_RELT_FK_NLLBL
    FOREIGN KEY (NULLBL)
    REFERENCES DD_RELT_NULLBLTYP(ID)
;
ALTER TABLE DD_RELT_COLMN
  ADD CONSTRAINT DD_RELT_FK_SRCHBLT
    FOREIGN KEY (SEARCHBLTY)
    REFERENCES DD_RELT_SEARCHBLTY(ID)
;
ALTER TABLE DD_RELT_FORGNKY
  ADD CONSTRAINT DD_RELT_FK_FRGNKYM
    FOREIGN KEY (FORGNKYMLTPLCTY)
    REFERENCES DD_RELT_MULTPLCTYK(ID)
;
ALTER TABLE DD_RELT_FORGNKY
  ADD CONSTRAINT DD_RELT_FK_PRMRYKY
    FOREIGN KEY (PRIMRYKYMLTPLCTY)
    REFERENCES DD_RELT_MULTPLCTYK(ID)
;
ALTER TABLE DD_RELT_PROCDRPRMT
  ADD CONSTRAINT DD_RELT_FK_DRCTN
    FOREIGN KEY (DIRCTN)
    REFERENCES DD_RELT_DIRCTNKND(ID)
;
ALTER TABLE DD_RELT_PROCDRPRMT
  ADD CONSTRAINT DD_RELT_FK_NLLBL_1
    FOREIGN KEY (NULLBL)
    REFERENCES DD_RELT_NULLBLTYP(ID)
;
ALTER TABLE DD_RELT_LOGCLRLT_1
  ADD CONSTRAINT DD_RELT_FK_MLTPLCT
    FOREIGN KEY (MULTPLCTY)
    REFERENCES DD_RELT_MULTPLCTYK(ID)
;
ALTER TABLE DD_DATC_ELEMNT
  ADD CONSTRAINT DD_DATC_FK_NLLBL
    FOREIGN KEY (NULLBL)
    REFERENCES DD_RELT_NULLBLTYP(ID)
;
ALTER TABLE DD_DATC_ELEMNT
  ADD CONSTRAINT DD_DATC_FK_SRCHBLT
    FOREIGN KEY (SEARCHBLTY)
    REFERENCES DD_RELT_SEARCHBLTY(ID)
;
ALTER TABLE DD_DATC_PROCDRPRMT
  ADD CONSTRAINT DD_DATC_FK_DRCTN
    FOREIGN KEY (DIRCTN)
    REFERENCES DD_RELT_DIRCTNKND(ID)
;
ALTER TABLE DD_DATC_PROCDRPRMT
  ADD CONSTRAINT DD_DATC_FK_NLLBL_1
    FOREIGN KEY (NULLBL)
    REFERENCES DD_RELT_NULLBLTYP(ID)
;
ALTER TABLE DD_DATC_FORGNKY
  ADD CONSTRAINT DD_DATC_FK_FRGNKYM
    FOREIGN KEY (FORGNKYMLTPLCTY)
    REFERENCES DD_RELT_MULTPLCTYK(ID)
;
ALTER TABLE DD_DATC_FORGNKY
  ADD CONSTRAINT DD_DATC_FK_PRMRYKY
    FOREIGN KEY (PRIMRYKYMLTPLCTY)
    REFERENCES DD_RELT_MULTPLCTYK(ID)
;
ALTER TABLE DD_RELT_RELTNSHP_1
  ADD CONSTRAINT DD_RELT_FK_STTS
    FOREIGN KEY (STATS)
    REFERENCES DD_RELT_RELTNSHPTY(ID)
;
CREATE VIEW MBR_READ_ENTRIES (ENTRY_ID_P1,ENTRY_ID_P2,USER_NAME) AS
SELECT MBR_POL_PERMS.ENTRY_ID_P1, MBR_POL_PERMS.ENTRY_ID_P2,
         MBR_POL_USERS.USER_NAME
FROM MBR_POL_PERMS, MBR_POL_USERS, CS_SYSTEM_PROPS
where MBR_POL_PERMS.POLICY_NAME=MBR_POL_USERS.POLICY_NAME
	AND (CS_SYSTEM_PROPS.PROPERTY_NAME='metamatrix.authorization.metabase.CheckingEnabled'
	AND CS_SYSTEM_PROPS.PROPERTY_VALUE ='true'
	AND MBR_POL_PERMS.READ_BIT='1')
UNION ALL
SELECT ENTRY_ID_P1, ENTRY_ID_P2, CHAR (NULLIF( '1' , '1' )) AS USER_NAME
FROM MBR_ENTRIES, CS_SYSTEM_PROPS
WHERE CS_SYSTEM_PROPS.PROPERTY_NAME='metamatrix.authorization.metabase.CheckingEnabled'
	AND CS_SYSTEM_PROPS.PROPERTY_VALUE ='false'
;
CREATE INDEX DD_IDX_MDLUUID_IX
    ON DD_INDEX (MDL_UUID1, MDL_UUID2)
;
CREATE INDEX MBR_POL_PERMS_IX1
    ON MBR_POL_PERMS (POLICY_NAME, READ_BIT)
;
CREATE INDEX LOGNTRIS_TMSTMP_IX 
    ON LOGENTRIES (TIMESTAMP)
;

CREATE TABLE DD_SHREDQUEUE
(
  QUEUE_ID      NUMERIC(19) NOT NULL,
  UUID1         NUMERIC(20) NOT NULL,
  UUID2         NUMERIC(20) NOT NULL,
  OBJECT_ID     VARCHAR(44) NOT NULL,
  NAME          VARCHAR(128) NOT NULL,
  VERSION       VARCHAR(20),
  MDL_PATH      VARCHAR(2000),
  CMD_ACTION    NUMERIC(1) NOT NULL,
  TXN_ID        NUMERIC(19),
  SUB_BY_NME    VARCHAR(100),
  SUB_BY_DATE   VARCHAR(50)
)
;

CREATE UNIQUE INDEX DDSQ_QUE_IX ON DD_SHREDQUEUE (QUEUE_ID)
;
CREATE UNIQUE INDEX DDSQ_TXN_IX ON DD_SHREDQUEUE (TXN_ID)
;

CREATE INDEX DDSQ_UUID_IX ON DD_SHREDQUEUE (OBJECT_ID)
;

CREATE VIEW DD_REQUEUETOSHRED_VIEW (UUID1, UUID2, MDLNAME, DDVRSION, MBRVRSION, TXN_ID, COMMAND_TYPE) AS
Select b.ENTRY_ID_P1, b.ENTRY_ID_P2, ENTRY_NAME as MDLNAME, a.VRSION as DDVRSION, b.ITEM_VERSION as MBRVRSION, a.TXN_ID, 2
 from  MBR_ENTRIES b LEFT OUTER JOIN DD_MDL a ON b.ENTRY_ID_P1=a.UUID1 and b.ENTRY_ID_P2=a.UUID2 
WHERE  b.ITEM_ID_P1 IS NOT NULL AND b.ENTRY_NAME <> '.project' and a.TXN_ID IS NULL and
                        b.ENTRY_ID_P1 NOT IN (Select c.UUID1 from  DD_SHREDQUEUE c 
                                where b.ENTRY_ID_P1=c.UUID1 and b.ENTRY_ID_P2=c.UUID2 and b.ITEM_VERSION=c.VERSION)
UNION 
Select b.ENTRY_ID_P1, b.ENTRY_ID_P2, ENTRY_NAME as MDLNAME, a.VRSION as DDVRSION, b.ITEM_VERSION as MBRVRSION, a.TXN_ID, 2
 from  MBR_ENTRIES b, DD_MDL a
 WHERE  b.ENTRY_ID_P1=a.UUID1 and b.ENTRY_ID_P2=a.UUID2 and CAST(a.VRSION as DECIMAL(8,2)) <> CAST(b.ITEM_VERSION as DECIMAL(8,2))
and  b.ENTRY_ID_P1 NOT IN (Select c.UUID1 from  DD_SHREDQUEUE c 
                                where b.ENTRY_ID_P1=c.UUID1 and b.ENTRY_ID_P2=c.UUID2 and b.ITEM_VERSION=c.VERSION)
;

-- == new DTC start ==
CREATE TABLE MMR_MODELS
(
  ID              BIGINT NOT NULL,
  NAME            VARCHAR(256),
  PATH            VARCHAR(1024),
  NAMESPACE       VARCHAR(1024),
  IS_METAMODEL    SMALLINT,
  VERSION         VARCHAR(64),
  IS_INCOMPLETE   SMALLINT,
  SHRED_TIME      TIMESTAMP
);

-- (generated from Resources)

CREATE TABLE MMR_RESOURCES
(
  MODEL_ID   BIGINT NOT NULL,
  CONTENT    CLOB NOT NULL
);

-- (generated from Objects)

CREATE TABLE MMR_OBJECTS
(
  ID              BIGINT NOT NULL,
  MODEL_ID        BIGINT NOT NULL,
  NAME            VARCHAR(256),
  PATH            VARCHAR(1024),
  CLASS_NAME      VARCHAR(256),
  UUID            VARCHAR(64),
  NDX_PATH        VARCHAR(256),
  IS_UNRESOLVED   SMALLINT
);

-- (generated from ResolvedObjects)

CREATE TABLE MMR_RESOLVED_OBJECTS
(
  OBJ_ID         BIGINT NOT NULL,
  MODEL_ID       BIGINT NOT NULL,
  CLASS_ID       BIGINT NOT NULL,
  CONTAINER_ID   BIGINT
);

-- (generated from ReferenceFeatures)

CREATE TABLE MMR_REF_FEATURES
(
  MODEL_ID         BIGINT NOT NULL,
  OBJ_ID           BIGINT NOT NULL,
  NDX              INTEGER,
  DATATYPE_ID      BIGINT,
  LOWER_BOUND      INTEGER,
  UPPER_BOUND      INTEGER,
  IS_CHANGEABLE    SMALLINT,
  IS_UNSETTABLE    SMALLINT,
  IS_CONTAINMENT   SMALLINT,
  OPPOSITE_ID      BIGINT
);

-- (generated from AttributeFeatures)

CREATE TABLE MMR_ATTR_FEATURES
(
  MODEL_ID        BIGINT NOT NULL,
  OBJ_ID          BIGINT NOT NULL,
  NDX             INTEGER,
  DATATYPE_ID     BIGINT,
  LOWER_BOUND     INTEGER,
  UPPER_BOUND     INTEGER,
  IS_CHANGEABLE   SMALLINT,
  IS_UNSETTABLE   SMALLINT
);

-- (generated from References)

CREATE TABLE MMR_REFS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  TO_ID        BIGINT NOT NULL
);

-- (generated from BooleanAttributes)

CREATE TABLE MMR_BOOLEAN_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        SMALLINT NOT NULL
);

-- (generated from ByteAttributes)

CREATE TABLE MMR_BYTE_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        SMALLINT NOT NULL
);

-- (generated from CharAttributes)

CREATE TABLE MMR_CHAR_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        CHAR(1) 
);

-- (generated from ClobAttributes)

CREATE TABLE MMR_CLOB_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        CLOB
);

-- (generated from DoubleAttributes)

CREATE TABLE MMR_DOUBLE_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        DOUBLE NOT NULL
);

-- (generated from EnumeratedAttributes)

CREATE TABLE MMR_ENUM_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        BIGINT NOT NULL
);

-- (generated from FloatAttributes)

CREATE TABLE MMR_FLOAT_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        REAL NOT NULL
);

-- (generated from IntAttributes)

CREATE TABLE MMR_INT_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        INTEGER NOT NULL
);

-- (generated from LongAttributes)

CREATE TABLE MMR_LONG_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        BIGINT NOT NULL
);

-- (generated from ShortAttributes)

CREATE TABLE MMR_SHORT_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        SMALLINT NOT NULL
);

-- (generated from StringAttributes)

CREATE TABLE MMR_STRING_ATTRS
(
  MODEL_ID     BIGINT NOT NULL,
  OBJECT_ID    BIGINT NOT NULL,
  FEATURE_ID   BIGINT NOT NULL,
  NDX          INTEGER NOT NULL,
  VALUE        VARCHAR(4000)
);

CREATE INDEX MOD_PATH_NDX ON MMR_MODELS (NAME,PATH);

CREATE INDEX MOD_NAMESPACE_NDX ON MMR_MODELS (NAMESPACE);

CREATE INDEX OBJ_FULL_QUAL_NDX ON MMR_OBJECTS (MODEL_ID,NAME,PATH,CLASS_NAME);

CREATE INDEX OBJ_UUID_NDX ON MMR_OBJECTS (UUID);

CREATE INDEX RES_OBJ_MODEL_NDX ON MMR_RESOLVED_OBJECTS (MODEL_ID);

CREATE INDEX RES_OBJ_CLASS_NDX ON MMR_RESOLVED_OBJECTS (CLASS_ID);

CREATE INDEX RF_DATATYPE_NDX ON MMR_REF_FEATURES (DATATYPE_ID);

CREATE INDEX RF_MODEL_NDX ON MMR_REF_FEATURES (MODEL_ID);

CREATE INDEX AF_DATATYPE_NDX ON MMR_ATTR_FEATURES (DATATYPE_ID);

CREATE INDEX AF_MODEL_NDX ON MMR_ATTR_FEATURES (MODEL_ID);

CREATE INDEX BOL_FEATURE_NDX ON MMR_BOOLEAN_ATTRS (FEATURE_ID);

CREATE INDEX BOL_MODEL_NDX ON MMR_BOOLEAN_ATTRS (MODEL_ID);

CREATE INDEX BYT_FEATURE_NDX ON MMR_BYTE_ATTRS (FEATURE_ID);

CREATE INDEX BYT_MODEL_NDX ON MMR_BYTE_ATTRS (MODEL_ID);

CREATE INDEX CHR_FEATURE_NDX ON MMR_CHAR_ATTRS (FEATURE_ID);

CREATE INDEX CHR_MODEL_NDX ON MMR_CHAR_ATTRS (MODEL_ID);

CREATE INDEX CLOB_FEATURE_NDX ON MMR_CLOB_ATTRS (FEATURE_ID);

CREATE INDEX CLOB_MODEL_NDX ON MMR_CLOB_ATTRS (MODEL_ID);

CREATE INDEX DBL_FEATURE_NDX ON MMR_DOUBLE_ATTRS (FEATURE_ID);

CREATE INDEX DBL_MODEL_NDX ON MMR_DOUBLE_ATTRS (MODEL_ID);

CREATE INDEX ENUM_FEATURE_NDX ON MMR_ENUM_ATTRS (FEATURE_ID);

CREATE INDEX ENUM_MODEL_NDX ON MMR_ENUM_ATTRS (MODEL_ID);

CREATE INDEX FLT_FEATURE_NDX ON MMR_FLOAT_ATTRS (FEATURE_ID);

CREATE INDEX FLT_MODEL_NDX ON MMR_FLOAT_ATTRS (MODEL_ID);

CREATE INDEX INT_FEATURE_NDX ON MMR_INT_ATTRS (FEATURE_ID);

CREATE INDEX INT_MODEL_NDX ON MMR_INT_ATTRS (MODEL_ID);

CREATE INDEX LNG_FEATURE_NDX ON MMR_LONG_ATTRS (FEATURE_ID);

CREATE INDEX LNG_MODEL_NDX ON MMR_LONG_ATTRS (MODEL_ID);

CREATE INDEX REF_FEATURE_NDX ON MMR_REFS (FEATURE_ID);

CREATE INDEX REF_TO_NDX ON MMR_REFS (TO_ID);

CREATE INDEX REF_MODEL_NDX ON MMR_REFS (MODEL_ID);

CREATE INDEX SHR_FEATURE_NDX ON MMR_SHORT_ATTRS (FEATURE_ID);

CREATE INDEX SHR_MODEL_NDX ON MMR_SHORT_ATTRS (MODEL_ID);

CREATE INDEX STR_FEATURE_NDX ON MMR_STRING_ATTRS (FEATURE_ID);

CREATE INDEX STR_MODEL_NDX ON MMR_STRING_ATTRS (MODEL_ID);

ALTER TABLE MMR_MODELS
  ADD CONSTRAINT MOD_PK
    PRIMARY KEY (ID)
;

ALTER TABLE MMR_RESOURCES
  ADD CONSTRAINT RSRC_PK
    PRIMARY KEY (MODEL_ID)
;

ALTER TABLE MMR_OBJECTS
  ADD CONSTRAINT OBJ_PK
    PRIMARY KEY (ID)
;

ALTER TABLE MMR_RESOLVED_OBJECTS
  ADD CONSTRAINT RES_OBJ_PK
    PRIMARY KEY (OBJ_ID)
;

ALTER TABLE MMR_REF_FEATURES
  ADD CONSTRAINT RF_PK
    PRIMARY KEY (OBJ_ID)
;

ALTER TABLE MMR_ATTR_FEATURES
  ADD CONSTRAINT AF_PK
    PRIMARY KEY (OBJ_ID)
;

ALTER TABLE MMR_REFS
  ADD CONSTRAINT REF_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_BOOLEAN_ATTRS
  ADD CONSTRAINT BOL_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_BYTE_ATTRS
  ADD CONSTRAINT BYT_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_CHAR_ATTRS
  ADD CONSTRAINT CHR_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_CLOB_ATTRS
  ADD CONSTRAINT CLOB_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_DOUBLE_ATTRS
  ADD CONSTRAINT DBL_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_ENUM_ATTRS
  ADD CONSTRAINT ENUM_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_FLOAT_ATTRS
  ADD CONSTRAINT FLT_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_INT_ATTRS
  ADD CONSTRAINT INT_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_LONG_ATTRS
  ADD CONSTRAINT LNG_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_SHORT_ATTRS
  ADD CONSTRAINT SHR_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

ALTER TABLE MMR_STRING_ATTRS
  ADD CONSTRAINT STR_PK
    PRIMARY KEY (OBJECT_ID,FEATURE_ID,NDX)
;

CREATE VIEW MMR_FEATURES AS 
SELECT MMR_MODELS.NAMESPACE AS NAMESPACE, 
       PARENTS.NAME AS CLASS_NAME, 
       MMR_OBJECTS.NAME AS FEATURE_NAME, 
       MMR_ATTR_FEATURES.OBJ_ID AS FEATURE_ID, 
       'Attribute' AS FEATURE_TYPE 
  FROM MMR_MODELS JOIN MMR_OBJECTS ON MMR_MODELS.ID=MMR_OBJECTS.MODEL_ID
  JOIN MMR_ATTR_FEATURES ON MMR_OBJECTS.ID = MMR_ATTR_FEATURES.OBJ_ID
  JOIN MMR_RESOLVED_OBJECTS ON MMR_OBJECTS.ID = MMR_RESOLVED_OBJECTS.OBJ_ID
  JOIN MMR_OBJECTS PARENTS ON MMR_RESOLVED_OBJECTS.CONTAINER_ID = PARENTS.ID
UNION ALL
SELECT MMR_MODELS.NAMESPACE AS NAMESPACE, 
       PARENTS.NAME AS CLASS_NAME, 
       MMR_OBJECTS.NAME AS FEATURE_NAME, 
       MMR_REF_FEATURES.OBJ_ID AS FEATURE_ID, 
       'Reference' AS FEATURE_TYPE 
  FROM MMR_MODELS JOIN MMR_OBJECTS ON MMR_MODELS.ID=MMR_OBJECTS.MODEL_ID 
  JOIN MMR_REF_FEATURES ON MMR_OBJECTS.ID = MMR_REF_FEATURES.OBJ_ID
  JOIN MMR_RESOLVED_OBJECTS ON MMR_OBJECTS.ID = MMR_RESOLVED_OBJECTS.OBJ_ID
  JOIN MMR_OBJECTS PARENTS ON MMR_RESOLVED_OBJECTS.CONTAINER_ID = PARENTS.ID
;

-- View for obtaining the feature values

CREATE VIEW MMR_FEATURE_VALUES AS
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       VALUE AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_BOOLEAN_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       VALUE AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_BYTE_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       VALUE AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_CHAR_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       VALUE AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_DOUBLE_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       VALUE AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_FLOAT_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       VALUE AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_INT_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       VALUE AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_LONG_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       VALUE AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_SHORT_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       VALUE AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_STRING_ATTRS
UNION ALL
SELECT OBJECT_ID, MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       VALUE AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_CLOB_ATTRS
UNION ALL
SELECT MMR_ENUM_ATTRS.OBJECT_ID, MMR_ENUM_ATTRS.MODEL_ID, MMR_ENUM_ATTRS.FEATURE_ID, MMR_ENUM_ATTRS.NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       MMR_OBJECTS.ID AS ENUM_ID,
       MMR_REFS.NDX AS ENUM_VALUE,
       MMR_OBJECTS.NAME AS ENUM_NAME,
       CAST(NULL AS BIGINT) AS REF_OBJ_ID,
       CAST(NULL AS VARCHAR(256)) AS REF_OBJ_NAME
  FROM MMR_ENUM_ATTRS JOIN MMR_OBJECTS ON MMR_ENUM_ATTRS.VALUE = MMR_OBJECTS.ID 
  JOIN MMR_RESOLVED_OBJECTS ON MMR_OBJECTS.ID = MMR_RESOLVED_OBJECTS.OBJ_ID
  JOIN MMR_REFS ON MMR_RESOLVED_OBJECTS.CONTAINER_ID = MMR_REFS.OBJECT_ID
               AND MMR_RESOLVED_OBJECTS.OBJ_ID = MMR_REFS.TO_ID
UNION ALL
SELECT OBJECT_ID, MMR_REFS.MODEL_ID AS MODEL_ID, FEATURE_ID, NDX, 
       CAST(NULL AS SMALLINT) AS BOOLEAN_VALUE, 
       CAST(NULL AS SMALLINT) AS BYTE_VALUE, 
       CAST(NULL AS CHAR(1)) AS CHAR_VALUE,
       CAST(NULL AS DOUBLE) AS DOUBLE_VALUE,
       CAST(NULL AS REAL) AS FLOAT_VALUE,
       CAST(NULL AS INTEGER) AS INT_VALUE,
       CAST(NULL AS BIGINT) AS LONG_VALUE,
       CAST(NULL AS SMALLINT) AS SHORT_VALUE,
       CAST(NULL AS VARCHAR(4000)) AS STRING_VALUE,
       CAST(NULL AS CLOB) AS CLOB_VALUE,
       CAST(NULL AS BIGINT) AS ENUM_ID,
       CAST(NULL AS INTEGER) AS ENUM_VALUE,
       CAST(NULL AS VARCHAR(256)) AS ENUM_NAME,
       MMR_OBJECTS.ID AS REF_OBJ_ID,
       MMR_OBJECTS.NAME AS REF_OBJ_NAME
  FROM MMR_REFS JOIN MMR_OBJECTS ON MMR_REFS.TO_ID = MMR_OBJECTS.ID
;

-- == new DTC end ==
