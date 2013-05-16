CREATE TABLE ABN_CROSS_REFERENCE
(
  ABN_CROSS_REFERENCE_ID  NUMBER                DEFAULT 0                     NOT NULL,
  CATALOG_CD              NUMBER                DEFAULT 0                     NOT NULL,
  CPT_NOMEN_ID            NUMBER                DEFAULT 0                     NOT NULL,
  BEG_EFFECTIVE_DT_TM     DATE,
  END_EFFECTIVE_DT_TM     DATE,
  ACTIVE_IND              NUMBER                DEFAULT 0                     NOT NULL,
  ACTIVE_STATUS_CD        NUMBER                DEFAULT 0                     NOT NULL,
  ACTIVE_STATUS_DT_TM     DATE,
  ACTIVE_STATUS_PRSNL_ID  NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_CNT                NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_DT_TM              DATE                  DEFAULT SYSDATE               NOT NULL,
  UPDT_ID                 NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_APPLCTX            NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_TASK               NUMBER                DEFAULT 0                     NOT NULL
)
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING


CREATE TABLE ABSTRACT_FIELD_DEF
(
  ABSTRACT_FIELD_DEF_CD   NUMBER                DEFAULT 0                     NOT NULL,
  ABSTRACT_DESC           VARCHAR2(255 BYTE),
  ABSTRACT_FIELD_TYPE_CD  NUMBER                DEFAULT 0                     NOT NULL,
  LENGTH                  FLOAT(126),
  CODESET_NBR             NUMBER,
  CONDITIONAL_IND         NUMBER,
  REQUIRED_IND            NUMBER,
  DISPLAY_LABEL           VARCHAR2(255 BYTE),
  BEG_EFFECTIVE_DT_TM     DATE,
  END_EFFECTIVE_DT_TM     DATE,
  UPDT_CNT                NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_DT_TM              DATE                  DEFAULT SYSDATE               NOT NULL,
  UPDT_ID                 NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_TASK               NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_APPLCTX            NUMBER                DEFAULT 0                     NOT NULL,
  ACTIVE_IND              NUMBER,
  ACTIVE_STATUS_CD        NUMBER                DEFAULT 0                     NOT NULL,
  ACTIVE_STATUS_DT_TM     DATE,
  ACTIVE_STATUS_PRSNL_ID  NUMBER                DEFAULT 0                     NOT NULL,
  CALCULATE_CD            NUMBER                DEFAULT 0                     NOT NULL,
  DATE_CHK_IND            NUMBER,
  OMF_FACT_IND            NUMBER
)
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING


CREATE TABLE ABSTRACT_FIELD_FLEX
(
  ABSTRACT_FIELD_FLEX_ID  NUMBER                DEFAULT 0                     NOT NULL,
  ABSTRACT_FIELD_DEF_CD   NUMBER                DEFAULT 0,
  DEPENDENT_TABLE_NAME    VARCHAR2(255 BYTE),
  DEPENDENT_TABLE_FIELD   VARCHAR2(255 BYTE),
  DEPENDENT_TABLE_VALUE   VARCHAR2(255 BYTE),
  DEPENDENT_TABLE_DT_TM   DATE,
  RELATIONAL_OPERATOR     VARCHAR2(2 BYTE),
  REQUIRED_IND            NUMBER,
  PERSON_IND              NUMBER,
  ENCNTR_IND              NUMBER,
  START_DT_TM             DATE,
  STOP_DT_TM              DATE,
  START_VALUE             VARCHAR2(255 BYTE),
  STOP_VALUE              VARCHAR2(255 BYTE),
  BEG_EFFECTIVE_DT_TM     DATE,
  END_EFFECTIVE_DT_TM     DATE,
  UPDT_CNT                NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_DT_TM              DATE                  DEFAULT SYSDATE               NOT NULL,
  UPDT_ID                 NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_TASK               NUMBER                DEFAULT 0                     NOT NULL,
  UPDT_APPLCTX            NUMBER                DEFAULT 0                     NOT NULL,
  ACTIVE_IND              NUMBER,
  ACTIVE_STATUS_CD        NUMBER                DEFAULT 0,
  ACTIVE_STATUS_DT_TM     DATE,
  ACTIVE_STATUS_PRSNL_ID  NUMBER                DEFAULT 0,
  DEPENDENT_TABLE_CD      NUMBER                DEFAULT 0,
  SVC_CAT_IND             NUMBER                DEFAULT 0                     NOT NULL
)
LOGGING 
NOCOMPRESS 
NOCACHE
NOPARALLEL
MONITORING
