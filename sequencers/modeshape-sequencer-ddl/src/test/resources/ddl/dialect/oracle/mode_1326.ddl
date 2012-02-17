CREATE TABLE XYZ_HA
(
  DBKEY                        NUMBER(10)       NOT NULL,
  UPDTCNT                      NUMBER(10),
  BCKP                         NUMBER(10),
  CAN_INT                      NUMBER(10)       NOT NULL,
  UPDTCD                       CHAR(1 BYTE)     NOT NULL,
  CAGECDXH                     CHAR(5 BYTE),
  REFNUMHA                     CHAR(32 BYTE),
  ITNAMEHA                     CHAR(19 BYTE),
  INAMECHA                     CHAR(5 BYTE),
  REFNCCHA                     CHAR(1 BYTE),
  REFNVCHA                     CHAR(1 BYTE),
  DLSCRCHA                     CHAR(1 BYTE),
  DOCIDCHA                     CHAR(3 BYTE),
  ITMMGCHA                     CHAR(1 BYTE),
  COGNSNHA                     CHAR(2 BYTE),
  SMMNSNHA                     CHAR(2 BYTE),
  MATNSNHA                     CHAR(1 BYTE),
  FSCNSNHA                     CHAR(4 BYTE),
  NIINSNHA                     CHAR(9 BYTE),
  ACTNSNHA                     CHAR(2 BYTE),
  UICONVHA                     CHAR(5 BYTE),
  SHLIFEHA                     CHAR(1 BYTE),
  SLACTNHA                     CHAR(2 BYTE),
  PPSLSTHA                     CHAR(1 BYTE),
  DOCAVCHA                     CHAR(1 BYTE),
  PRDLDTHA                     NUMBER(5),
  SPMACCHA                     CHAR(1 BYTE),
  SMAINCHA                     CHAR(1 BYTE),
  CRITCDHA                     CHAR(1 BYTE),
  PMICODHA                     CHAR(1 BYTE),
  SAIPCDHA                     CHAR(1 BYTE),
  AAPLCCHA                     CHAR(1 BYTE),
  BBPLCCHA                     CHAR(1 BYTE),
  CCPLCCHA                     CHAR(1 BYTE),
  DDPLCCHA                     CHAR(1 BYTE),
  EEPLCCHA                     CHAR(1 BYTE),
  FFPLCCHA                     CHAR(1 BYTE),
  GGPLCCHA                     CHAR(1 BYTE),
  HHPLCCHA                     CHAR(1 BYTE),
  JJPLCCHA                     CHAR(1 BYTE),
  KKPLCCHA                     CHAR(1 BYTE),
  LLPLCCHA                     CHAR(1 BYTE),
  MMPLCCHA                     CHAR(1 BYTE),
  PHYSECHA                     CHAR(1 BYTE),
  ADPEQPHA                     CHAR(1 BYTE),
  DEMILIHA                     CHAR(1 BYTE),
  ACQMETHA                     CHAR(1 BYTE),
  AMSUFCHA                     CHAR(1 BYTE),
  HMSCOSHA                     NUMBER(10),
  HWDCOSHA                     NUMBER(10),
  HWSCOSHA                     NUMBER(10),
  CTICODHA                     CHAR(2 BYTE),
  UWEIGHHA                     CHAR(5 BYTE),
  ULENGTHA                     NUMBER,
  UWIDTHHA                     NUMBER,
  UHEIGHHA                     NUMBER,
  HAZCODHA                     CHAR(1 BYTE),
  UNITMSHA                     CHAR(2 BYTE),
  UNITISHA                     CHAR(2 BYTE),
  LINNUMHA                     CHAR(6 BYTE),
  CRITITHA                     CHAR(13 BYTE),
  INDMATHA                     CHAR(19 BYTE),
  MTLEADHA                     NUMBER(5),
  MTLWGTHA                     NUMBER,
  MATERLHA                     CHAR(240 BYTE),
  SCHEDULE_B_EXPORT_CODE_TYPE  CHAR(10 BYTE),
  EXT_PART_ID                  NUMBER(10)       DEFAULT 0
)
LOGGING ;


CREATE TABLE XYZ_HAX01
(
  DBKEY        NUMBER(10)                       NOT NULL,
  UPDTCNT      NUMBER(10),
  BCKP         NUMBER(10),
  CAN_INT      NUMBER(10)                       NOT NULL,
  UPDTCD       CHAR(1 BYTE)                     NOT NULL,
  CAGECDXH     CHAR(5 BYTE),
  REFNUMHA     CHAR(32 BYTE),
  ACTACTCD     CHAR(2 BYTE),
  JUSTNCD      CHAR(1 BYTE),
  DLSCDTE      CHAR(5 BYTE),
  SOS          CHAR(3 BYTE),
  MDPC         CHAR(1 BYTE),
  ESDCODE      CHAR(1 BYTE),
  MOERULE      CHAR(4 BYTE),
  NIINPDTE     CHAR(5 BYTE),
  IMCA         CHAR(2 BYTE),
  ANSNCOG      CHAR(2 BYTE),
  ANSNMCC      CHAR(1 BYTE),
  ANSNFSC      CHAR(4 BYTE),
  ANSNNIIN     CHAR(9 BYTE),
  ANSNSMIC     CHAR(2 BYTE),
  NCSCD        CHAR(1 BYTE),
  NICNCOG      CHAR(2 BYTE),
  NICNMCC      CHAR(1 BYTE),
  NICNFSC      CHAR(4 BYTE),
  NICNNIIN     CHAR(9 BYTE),
  NICNSMIC     CHAR(2 BYTE),
  INICNCOG     CHAR(2 BYTE),
  INICNMCC     CHAR(1 BYTE),
  INICNFSC     CHAR(4 BYTE),
  INICNNIIN    CHAR(9 BYTE),
  INICNSMIC    CHAR(2 BYTE),
  HMIC         CHAR(1 BYTE),
  ACQADVCD     CHAR(1 BYTE),
  ERRC         CHAR(1 BYTE),
  IRCODE       CHAR(1 BYTE),
  MATERCD      CHAR(8 BYTE),
  EONUMBER     CHAR(5 BYTE),
  EONUMBERREV  CHAR(2 BYTE),
  DCNNUMBER    CHAR(3 BYTE),
  XTRA1        CHAR(5 BYTE),
  XTRA2        CHAR(5 BYTE),
  XTRA3        CHAR(5 BYTE),
  XTRA4        NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HAX04
(
  DBKEY        NUMBER(10)                       NOT NULL,
  UPDTCNT      NUMBER(10),
  BCKP         NUMBER(10),
  CAN_INT      NUMBER(10)                       NOT NULL,
  UPDTCD       CHAR(1 BYTE)                     NOT NULL,
  CAGECDXH     CHAR(5 BYTE),
  REFNUMHA     CHAR(32 BYTE),
  SCM_PROGRAM  CHAR(10 BYTE),
  DEFAULT_BIN  CHAR(20 BYTE),
  XTRA1        CHAR(5 BYTE),
  XTRA2        CHAR(5 BYTE),
  XTRA3        CHAR(5 BYTE),
  XTRA4        NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HB
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10)                          DEFAULT 0,
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  CAGECDHB  CHAR(5 BYTE)                        DEFAULT ' ',
  REFNUMHB  CHAR(32 BYTE)                       DEFAULT ' ',
  ADCAGEHB  CHAR(5 BYTE)                        DEFAULT ' ',
  ADDREFHB  CHAR(32 BYTE)                       DEFAULT ' ',
  ADRNCCHB  CHAR(1 BYTE),
  ADRNVCHB  CHAR(1 BYTE)
)
LOGGING ;


CREATE TABLE XYZ_HBX01
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10),
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  CAGECDXH  CHAR(5 BYTE),
  REFNUMHA  CHAR(32 BYTE),
  ADCAGEHB  CHAR(5 BYTE),
  ADDREFHB  CHAR(32 BYTE),
  ADNIIN    CHAR(9 BYTE),
  ADSCRNDT  CHAR(5 BYTE),
  ADDAC     CHAR(1 BYTE),
  ADMOERLE  CHAR(4 BYTE),
  ADCOG     CHAR(2 BYTE),
  ADFSC     CHAR(4 BYTE),
  ADMCC     CHAR(1 BYTE),
  ADSMIC    CHAR(2 BYTE),
  ADIMC     CHAR(1 BYTE),
  ADAQTNMT  CHAR(1 BYTE),
  ADAQMTSX  CHAR(1 BYTE),
  ADDIC     CHAR(3 BYTE),
  XTRA1     CHAR(5 BYTE),
  XTRA2     CHAR(5 BYTE),
  XTRA3     CHAR(5 BYTE),
  XTRA4     NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HG
(
  DBKEY                     NUMBER(10)          NOT NULL,
  UPDTCNT                   NUMBER(10),
  BCKP                      NUMBER(10),
  CAN_INT                   NUMBER(10)          NOT NULL,
  UPDTCD                    CHAR(1 BYTE)        NOT NULL,
  CAGECDXH                  CHAR(5 BYTE),
  REFNUMHA                  CHAR(32 BYTE),
  EIACODXA                  CHAR(10 BYTE),
  LSACONXB                  CHAR(18 BYTE),
  ALTLCNXB                  CHAR(2 BYTE),
  LCNTYPXB                  CHAR(1 BYTE),
  PLISNOHG                  CHAR(5 BYTE),
  QTYASYHG                  CHAR(4 BYTE),
  SUPINDHG                  CHAR(1 BYTE),
  DATASCHG                  CHAR(1 BYTE),
  PROSICHG                  CHAR(3 BYTE),
  LLIPTDHG                  CHAR(1 BYTE),
  PPLPTDHG                  CHAR(1 BYTE),
  SFPPTDHG                  CHAR(1 BYTE),
  CBLPTDHG                  CHAR(1 BYTE),
  RILPTDHG                  CHAR(1 BYTE),
  ISLPTDHG                  CHAR(1 BYTE),
  PCLPTDHG                  CHAR(1 BYTE),
  TTLPTDHG                  CHAR(1 BYTE),
  SCPPTDHG                  CHAR(1 BYTE),
  ARAPTDHG                  CHAR(1 BYTE),
  ARBPTDHG                  CHAR(1 BYTE),
  TOCCODHG                  CHAR(1 BYTE),
  INDCODHG                  CHAR(1 BYTE),
  QTYPEIHG                  CHAR(5 BYTE),
  PIPLISHG                  CHAR(5 BYTE),
  SAPLISHG                  CHAR(5 BYTE),
  HARDCIHG                  CHAR(1 BYTE),
  REMIPIHG                  CHAR(1 BYTE),
  LRUNITHG                  CHAR(1 BYTE),
  ITMCATHG                  CHAR(2 BYTE),
  ESSCODHG                  CHAR(1 BYTE),
  SMRCODHG                  CHAR(6 BYTE),
  MRRONEHG                  NUMBER,
  MRRTWOHG                  NUMBER,
  MRRMODHG                  CHAR(7 BYTE),
  ORTDOOHG                  NUMBER(5),
  FRTDFFHG                  NUMBER(5),
  HRTDHHHG                  NUMBER(5),
  LRTDLLHG                  NUMBER(5),
  DRTDDDHG                  NUMBER(5),
  MINREUHG                  NUMBER(5),
  MAOTIMHG                  CHAR(4 BYTE),
  MAIACTHG                  CHAR(1 BYTE),
  RISSBUHG                  NUMBER(5),
  RMSSLIHG                  NUMBER(5),
  RTLLQTHG                  NUMBER(5),
  TOTQTYHG                  NUMBER(10),
  OMTDOOHG                  NUMBER(5),
  FMTDFFHG                  NUMBER(5),
  HMTDHHHG                  NUMBER(5),
  LMTDLLHG                  NUMBER(5),
  DMTDDDHG                  NUMBER(5),
  CBDMTDHG                  NUMBER(5),
  CADMTDHG                  NUMBER(5),
  ORCTOOHG                  NUMBER(5),
  FRCTFFHG                  NUMBER(5),
  HRCTHHHG                  NUMBER(5),
  LRCTLLHG                  NUMBER(5),
  DRCTDDHG                  NUMBER(5),
  CONRCTHG                  NUMBER(5),
  NORETSHG                  NUMBER(5),
  REPSURHG                  NUMBER(5),
  DRPONEHG                  CHAR(6 BYTE),
  DRPTWOHG                  CHAR(6 BYTE),
  WRKUCDHG                  CHAR(7 BYTE),
  ALLOWCHG                  CHAR(2 BYTE),
  ALIQTYHG                  NUMBER(5),
  IDENTIFICATION_NUMBER_HG  CHAR(19 BYTE),
  PCCNUMXC                  CHAR(6 BYTE)        DEFAULT ' ',
  EXT_PARTAPPL_ID           NUMBER(10)          DEFAULT 0
)
LOGGING ;


CREATE TABLE XYZ_HGX01
(
  DBKEY       NUMBER(10)                        NOT NULL,
  UPDTCNT     NUMBER(10),
  BCKP        NUMBER(10),
  CAN_INT     NUMBER(10)                        NOT NULL,
  UPDTCD      CHAR(1 BYTE)                      NOT NULL,
  CAGECDXH    CHAR(5 BYTE),
  REFNUMHA    CHAR(32 BYTE),
  EIACODXA    CHAR(10 BYTE),
  LSACONXB    CHAR(18 BYTE),
  ALTLCNXB    CHAR(2 BYTE),
  LCNTYPXB    CHAR(1 BYTE),
  NHACGECD    CHAR(5 BYTE),
  NHAREFNO    CHAR(32 BYTE),
  SUPLYRCD    CHAR(5 BYTE),
  PREDMRF     NUMBER,
  PREDRPF     NUMBER,
  PREDORF     NUMBER,
  ADDRESSKEY  CHAR(22 BYTE),
  XTRA1       CHAR(5 BYTE),
  XTRA2       CHAR(5 BYTE),
  XTRA3       CHAR(5 BYTE),
  XTRA4       NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HGX01A
(
  DBKEY        NUMBER(10)                       NOT NULL,
  UPDTCNT      NUMBER(10),
  BCKP         NUMBER(10),
  CAN_INT      NUMBER(10)                       NOT NULL,
  UPDTCD       CHAR(1 BYTE)                     NOT NULL,
  CAGECDXH     CHAR(5 BYTE),
  REFNUMHA     CHAR(32 BYTE),
  EIACODXA     CHAR(10 BYTE),
  LSACONXB     CHAR(18 BYTE),
  ALTLCNXB     CHAR(2 BYTE),
  LCNTYPXB     CHAR(1 BYTE),
  NHACGECD     CHAR(5 BYTE),
  NHAREFNO     CHAR(32 BYTE),
  MODEL        CHAR(1 BYTE),
  EFFFROM      NUMBER(5),
  EFFTO        NUMBER(5),
  QPA          NUMBER(5),
  NHAEONUM     CHAR(5 BYTE),
  NHAEONUMREV  CHAR(2 BYTE),
  NHADCNNUM    CHAR(3 BYTE),
  EJS          CHAR(8 BYTE),
  CLAS         CHAR(1 BYTE),
  ECPNO        CHAR(12 BYTE),
  RCPNO        CHAR(5 BYTE),
  CCPNO        CHAR(5 BYTE),
  XTRA1        CHAR(5 BYTE),
  XTRA2        CHAR(5 BYTE),
  XTRA3        CHAR(5 BYTE),
  XTRA4        NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HP
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10)                          DEFAULT 0,
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  CAGECDXH  CHAR(5 BYTE)                        DEFAULT ' ',
  REFNUMHA  CHAR(32 BYTE)                       DEFAULT ' ',
  EIACODXA  CHAR(10 BYTE)                       DEFAULT ' ',
  LSACONXB  CHAR(18 BYTE)                       DEFAULT ' ',
  ALTLCNXB  CHAR(2 BYTE)                        DEFAULT ' ',
  LCNTYPXB  CHAR(1 BYTE)                        DEFAULT ' ',
  CANUMBHP  CHAR(15 BYTE)                       DEFAULT ' ',
  RSPLISHP  CHAR(5 BYTE),
  RSPINDHP  CHAR(1 BYTE),
  INTCHCHP  CHAR(2 BYTE),
  TOTICHHP  NUMBER(5),
  QTYSHPHP  NUMBER(10),
  QTYPROHP  NUMBER(10),
  PROELIHP  CHAR(6 BYTE),
  PROQTYHP  NUMBER(10)
)
LOGGING ;


CREATE TABLE XYZ_HPX01
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10),
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  EIACODXA  CHAR(10 BYTE),
  CAGECDXH  CHAR(5 BYTE),
  REFNUMHA  CHAR(32 BYTE),
  LSACONXB  CHAR(18 BYTE),
  ALTLCNXB  CHAR(2 BYTE),
  LCNTYPXB  CHAR(1 BYTE),
  CANUMBHP  CHAR(15 BYTE),
  RSREFNO   CHAR(32 BYTE),
  RSCAGECD  CHAR(5 BYTE),
  RSIND     CHAR(1 BYTE),
  REPSUPID  CHAR(2 BYTE),
  RTROFTDC  CHAR(15 BYTE),
  XTRA1     CHAR(5 BYTE),
  XTRA2     CHAR(5 BYTE),
  XTRA3     CHAR(5 BYTE),
  XTRA4     NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_HPX01A
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10),
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  EIACODXA  CHAR(10 BYTE),
  CAGECDXH  CHAR(5 BYTE),
  REFNUMHA  CHAR(32 BYTE),
  LSACONXB  CHAR(18 BYTE),
  ALTLCNXB  CHAR(2 BYTE),
  LCNTYPXB  CHAR(1 BYTE),
  CANUMBHP  CHAR(15 BYTE),
  EFFTYPHP  CHAR(1 BYTE),
  MDLHP     CHAR(1 BYTE),
  UOEFFFRM  NUMBER(5),
  UOEFFTO   NUMBER(5),
  EJSHP     CHAR(8 BYTE),
  CLASHP    CHAR(1 BYTE),
  ECPNOHP   CHAR(12 BYTE),
  RCPNOHP   CHAR(5 BYTE),
  CCPNOHP   CHAR(5 BYTE),
  XTRA1     CHAR(5 BYTE),
  XTRA2     CHAR(5 BYTE),
  XTRA3     CHAR(5 BYTE),
  XTRA4     NUMBER(5)
)
LOGGING ;


CREATE TABLE XYZ_XA
(
  DBKEY        NUMBER(10)                       NOT NULL,
  UPDTCNT      NUMBER(10),
  BCKP         NUMBER(10),
  CAN_INT      NUMBER(10)                       NOT NULL,
  UPDTCD       CHAR(1 BYTE)                     NOT NULL,
  EIACODXA     CHAR(10 BYTE),
  LCNSTRXA     CHAR(18 BYTE),
  ADDLTMXA     NUMBER(5),
  CTDLTMXA     NUMBER(5),
  CONTNOXA     CHAR(19 BYTE),
  CSREORXA     NUMBER,
  CSPRRQXA     NUMBER,
  DEMILCXA     NUMBER(5),
  DISCNTXA     NUMBER,
  ESSALVXA     NUMBER(5),
  HLCSPCXA     NUMBER(5),
  INTBINXA     NUMBER(5),
  INCATCXA     NUMBER(5),
  INTRATXA     NUMBER,
  INVSTGXA     NUMBER,
  LODFACXA     NUMBER,
  WSOPLVXA     NUMBER(5),
  OPRLIFXA     NUMBER(5),
  PRSTOVXA     NUMBER(5),
  PRSTOMXA     NUMBER(5),
  PROFACXA     NUMBER,
  RCBINCXA     NUMBER(5),
  RCCATCXA     NUMBER(5),
  RESTCRXA     NUMBER(5),
  SAFLVLXA     NUMBER(5),
  SECSFCXA     NUMBER,
  TRNCSTXA     NUMBER,
  WSTYAQXA     CHAR(1 BYTE),
  TSSCODXA     CHAR(1 BYTE),
  EXT_EIAC_ID  NUMBER(10)
)
LOGGING ;


CREATE TABLE XYZ_XG
(
  DBKEY     NUMBER(10)                          NOT NULL,
  UPDTCNT   NUMBER(10)                          DEFAULT 0,
  BCKP      NUMBER(10),
  CAN_INT   NUMBER(10)                          NOT NULL,
  UPDTCD    CHAR(1 BYTE)                        NOT NULL,
  EIACODXA  CHAR(10 BYTE)                       DEFAULT ' ',
  PLSACNXG  CHAR(18 BYTE)                       DEFAULT ' ',
  PALCNCXG  CHAR(2 BYTE)                        DEFAULT ' ',
  PLCNTYXG  CHAR(1 BYTE)                        DEFAULT ' ',
  FLSACNXG  CHAR(18 BYTE)                       DEFAULT ' ',
  FALCNCXG  CHAR(2 BYTE)                        DEFAULT ' ',
  FLCNTYXG  CHAR(1 BYTE)                        DEFAULT ' '
)
LOGGING ;


CREATE INDEX XYZ_HAX01_IND001 ON XYZ_HAX01
(MATERCD, REFNUMHA, CAGECDXH)
LOGGING;


CREATE INDEX XYZ_HAX01_IND002 ON XYZ_HAX01
(ANSNNIIN, REFNUMHA, CAGECDXH)
LOGGING;


CREATE INDEX XYZ_HA_IND001 ON XYZ_HA
(NIINSNHA, REFNUMHA, CAGECDXH)
LOGGING;


CREATE INDEX XYZ_HBX01_IND001 ON XYZ_HBX01
(ADNIIN, REFNUMHA, CAGECDXH)
LOGGING;


CREATE INDEX XYZ_HBX01_IND002 ON XYZ_HBX01
(REFNUMHA, CAGECDXH, ADDREFHB, ADCAGEHB)
LOGGING;


CREATE INDEX XYZ_HB_INDX002 ON XYZ_HB
(ADDREFHB, REFNUMHB, CAGECDHB, ADCAGEHB)
LOGGING;


CREATE INDEX XYZ_HGX01A_IND001 ON XYZ_HGX01A
(NHAREFNO)
LOGGING;


CREATE INDEX XYZ_HGX01A_IND002 ON XYZ_HGX01A
(REFNUMHA, CAGECDXH, EIACODXA, LSACONXB, ALTLCNXB, 
NHAREFNO, NHACGECD, MODEL, EFFFROM, LCNTYPXB)
LOGGING;


CREATE INDEX XYZ_HGX01A_IND003 ON XYZ_HGX01A
(CAGECDXH, REFNUMHA, EIACODXA, LSACONXB, ALTLCNXB, 
LCNTYPXB, NHACGECD, NHAREFNO)
LOGGING;


CREATE INDEX XYZ_HGX01_IND001 ON XYZ_HGX01
(NHAREFNO)
LOGGING;


CREATE INDEX XYZ_HGX01_IND002 ON XYZ_HGX01
(ADDRESSKEY, EIACODXA)
LOGGING;


CREATE INDEX XYZ_HGX01_IND003 ON XYZ_HGX01
(REFNUMHA, SUPLYRCD)
LOGGING;


CREATE INDEX XYZ_HG_IND001 ON XYZ_HG
(WRKUCDHG)
LOGGING;


CREATE INDEX XYZ_HG_IND002 ON XYZ_HG
(LSACONXB, ALTLCNXB, EIACODXA, LCNTYPXB)
LOGGING;


CREATE INDEX XYZ_HG_IND003 ON XYZ_HG
(REFNUMHA, CAGECDXH, LSACONXB, ALTLCNXB, EIACODXA, 
LCNTYPXB)
LOGGING;


CREATE INDEX XYZ_HPX01_IND002 ON XYZ_HPX01
(REFNUMHA, CAGECDXH, RSREFNO, LSACONXB, ALTLCNXB, 
EIACODXA, LCNTYPXB)
LOGGING;


CREATE UNIQUE INDEX XYZ_I1047 ON XYZ_XA
(EIACODXA, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I1059 ON XYZ_XG
(EIACODXA, PLSACNXG, PALCNCXG, PLCNTYXG, FLSACNXG, 
FALCNCXG, FLCNTYXG, CAN_INT)
LOGGING
COMPRESS 4;


CREATE UNIQUE INDEX XYZ_I1123 ON XYZ_HA
(CAGECDXH, REFNUMHA, CAN_INT)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I1125 ON XYZ_HB
(CAGECDHB, REFNUMHB, ADCAGEHB, ADDREFHB, CAN_INT)
LOGGING
COMPRESS 2;


CREATE UNIQUE INDEX XYZ_I1135 ON XYZ_HG
(CAGECDXH, REFNUMHA, EIACODXA, LSACONXB, ALTLCNXB, 
LCNTYPXB, CAN_INT)
LOGGING
COMPRESS 2;


CREATE UNIQUE INDEX XYZ_I1153 ON XYZ_HP
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, CANUMBHP, CAN_INT)
LOGGING
COMPRESS 6;


CREATE UNIQUE INDEX XYZ_I1256 ON XYZ_XG
(EIACODXA, FLSACNXG, FALCNCXG, FLCNTYXG, CAN_INT)
LOGGING
COMPRESS 3;


CREATE UNIQUE INDEX XYZ_I1277 ON XYZ_HB
(ADCAGEHB, ADDREFHB, CAN_INT, DBKEY)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I1279 ON XYZ_HG
(EIACODXA, LSACONXB, ALTLCNXB, LCNTYPXB, CAGECDXH, 
REFNUMHA, CAN_INT)
LOGGING
COMPRESS 4;


CREATE UNIQUE INDEX XYZ_I1304 ON XYZ_HG
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, CAN_INT)
LOGGING
COMPRESS 5;


CREATE UNIQUE INDEX XYZ_I1305 ON XYZ_HG
(EIACODXA, PCCNUMXC, PLISNOHG, CAN_INT, DBKEY)
LOGGING
COMPRESS 3;


CREATE UNIQUE INDEX XYZ_I2755 ON XYZ_HA
(REFNUMHA, CAGECDXH, CAN_INT)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I2756 ON XYZ_HA
(ITNAMEHA, CAN_INT, DBKEY)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I49154 ON XYZ_HAX01
(CAGECDXH, REFNUMHA, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49158 ON XYZ_HBX01
(CAGECDXH, REFNUMHA, ADCAGEHB, ADDREFHB, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49160 ON XYZ_HGX01
(CAGECDXH, REFNUMHA, EIACODXA, LSACONXB, ALTLCNXB, 
LCNTYPXB, NHACGECD, NHAREFNO, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49162 ON XYZ_HGX01A
(CAGECDXH, REFNUMHA, EIACODXA, LSACONXB, ALTLCNXB, 
LCNTYPXB, NHACGECD, NHAREFNO, MODEL, EFFFROM, 
CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49166 ON XYZ_HPX01
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, CANUMBHP, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49172 ON XYZ_HPX01A
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, CANUMBHP, EFFTYPHP, MDLHP, UOEFFFRM, 
CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49197 ON XYZ_HAX04
(CAGECDXH, REFNUMHA, SCM_PROGRAM, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49211 ON XYZ_HAX01
(REFNUMHA, CAGECDXH, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49215 ON XYZ_HAX04
(REFNUMHA, CAGECDXH, SCM_PROGRAM, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49216 ON XYZ_HGX01
(EIACODXA, LSACONXB, ALTLCNXB, LCNTYPXB, CAGECDXH, 
REFNUMHA, NHACGECD, NHAREFNO, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49217 ON XYZ_HGX01
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, NHACGECD, NHAREFNO, CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49218 ON XYZ_HGX01A
(EIACODXA, LSACONXB, ALTLCNXB, LCNTYPXB, CAGECDXH, 
REFNUMHA, NHACGECD, NHAREFNO, MODEL, EFFFROM, 
CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I49219 ON XYZ_HGX01A
(EIACODXA, CAGECDXH, REFNUMHA, LSACONXB, ALTLCNXB, 
LCNTYPXB, NHACGECD, NHAREFNO, MODEL, EFFFROM, 
CAN_INT)
LOGGING;


CREATE UNIQUE INDEX XYZ_I7533 ON XYZ_XA
(EXT_EIAC_ID, CAN_INT, DBKEY)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I7536 ON XYZ_HG
(EXT_PARTAPPL_ID, CAN_INT, DBKEY)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I7537 ON XYZ_HA
(EXT_PART_ID, CAN_INT, DBKEY)
LOGGING
COMPRESS 1;


CREATE UNIQUE INDEX XYZ_I7739 ON XYZ_HP
(EIACODXA, CANUMBHP, CAGECDXH, REFNUMHA, LSACONXB, 
ALTLCNXB, LCNTYPXB, CAN_INT)
LOGGING
COMPRESS 6;


CREATE UNIQUE INDEX XYZ_I7742 ON XYZ_HG
(EIACODXA, PCCNUMXC, LSACONXB, ALTLCNXB, CAGECDXH, 
REFNUMHA, CAN_INT, DBKEY)
LOGGING
COMPRESS 6;


CREATE UNIQUE INDEX XYZ_I7769 ON XYZ_HP
(EIACODXA, RSPLISHP, CAN_INT, DBKEY)
LOGGING
COMPRESS 2;


CREATE INDEX XYZ_INDX_EXT ON XYZ_HB
(REFNUMHB, CAGECDHB)
LOGGING;


CREATE INDEX XYZ_XG_IND001 ON XYZ_XG
(PLSACNXG, PALCNCXG, PLCNTYXG, FLSACNXG, FALCNCXG, 
FLCNTYXG, EIACODXA)
LOGGING;


CREATE INDEX XYZ_XG_IND002 ON XYZ_XG
(FLSACNXG, FALCNCXG, FLCNTYXG, PLSACNXG, PALCNCXG, 
PLCNTYXG, EIACODXA)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1046 ON XYZ_XA
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1058 ON XYZ_XG
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1122 ON XYZ_HA
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1124 ON XYZ_HB
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1134 ON XYZ_HG
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA1152 ON XYZ_HP
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49153 ON XYZ_HAX01
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49156 ON XYZ_HBX01
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49159 ON XYZ_HGX01
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49161 ON XYZ_HGX01A
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49165 ON XYZ_HPX01
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49171 ON XYZ_HPX01A
(DBKEY)
LOGGING;


CREATE UNIQUE INDEX XYZ_ZA49196 ON XYZ_HAX04
(DBKEY)
LOGGING;


CREATE OR REPLACE TRIGGER XYZ_intrfce_ext_ha_add_trigger
   AFTER INSERT
   ON slic2b20.XYZ_ha
   FOR EACH ROW
DECLARE
BEGIN
   BEGIN
      INSERT INTO prov.XYZ_intrfce_ext (cage_code, ref_no, ha_hb_ind, proof_transfer_flag)
           VALUES (RTRIM (:new.cagecdxh), RTRIM (:new.refnumha), 'HA', 'N');
   EXCEPTION
      WHEN DUP_VAL_ON_INDEX THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER XYZ_intrfce_ext_ha_del_trigger
   AFTER DELETE
   ON slic2b20.XYZ_ha
   FOR EACH ROW
DECLARE
BEGIN
   BEGIN
      DELETE prov.XYZ_intrfce_ext
       WHERE     cage_code = RTRIM (:old.cagecdxh)
             AND ref_no = RTRIM (:old.refnumha)
             AND ha_hb_ind = 'HA';
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_ha_del_trigger
   AFTER DELETE
   ON XYZ_ha
   FOR EACH ROW
BEGIN
   INSERT INTO slic_ha_chgs (cagecdxh,
                             refnumha,
                             chg_update_cde,
                             refnccha,
                             refnvcha,
                             prdldtha,
                             unitisha,
                             cognsnha,
                             fscnsnha,
                             niinsnha,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             datasys_flag,
                             update_date,
                             create_date,
                             prov_userid)
      SELECT RTRIM (:old.cagecdxh),
             RTRIM (:old.refnumha),
             'D',
             NULL,
             NULL,
             NULL,
             NULL,
             NULL,
             NULL,
             NULL,
             'N', -- slicgld_flag
             'Y',
             'N', -- assetmgr_notf_flag
             'D',
             SYSDATE,
             SYSDATE,
             UPPER (osuser)
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');

   UPDATE slic_ha_chgs
      SET slicgld_flag = 'N', assetmgr_delt_flag = 'Y', assetmgr_notf_flag = 'N', chg_update_cde = 'D'
    WHERE     refnumha = RTRIM (:old.refnumha)
          AND cagecdxh = RTRIM (:old.cagecdxh)
          AND slicgld_flag = 'Y';
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_ha_updt_trigger
   AFTER UPDATE
   ON XYZ_ha
   FOR EACH ROW
DECLARE
   hold_refnccha   CHAR (1);
   hold_refnvcha   CHAR (1);
   hold_prdldtha   NUMBER (5);
   hold_unitisha   VARCHAR2 (2);
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
   hold_cognsnha   VARCHAR2 (2);
   hold_fscnsnha   VARCHAR2 (4);
   hold_niinsnha   VARCHAR2 (9);
BEGIN
   IF :new.niinsnha <> :old.niinsnha THEN
      hold_niinsnha := :old.niinsnha;
   ELSE
      hold_niinsnha := NULL;
   END IF;

   IF :new.fscnsnha <> :old.fscnsnha THEN
      hold_fscnsnha := :old.fscnsnha;
   ELSE
      hold_fscnsnha := NULL;
   END IF;

   IF :new.cognsnha <> :old.cognsnha THEN
      hold_cognsnha := :old.cognsnha;
   ELSE
      hold_cognsnha := NULL;
   END IF;

   IF :new.cagecdxh <> :old.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :new.refnumha <> :old.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   IF :new.refnccha <> :old.refnccha THEN
      hold_refnccha := :old.refnccha;
   ELSE
      hold_refnccha := NULL;
   END IF;

   IF :new.refnvcha <> :old.refnvcha THEN
      hold_refnvcha := :old.refnvcha;
   ELSE
      hold_refnvcha := NULL;
   END IF;

   IF :new.prdldtha <> :old.prdldtha THEN
      hold_prdldtha := :old.prdldtha;
   ELSE
      hold_prdldtha := NULL;
   END IF;

   IF :new.unitisha <> :old.unitisha THEN
      hold_unitisha := :old.unitisha;
   ELSE
      hold_unitisha := NULL;
   END IF;

   INSERT INTO slic_ha_chgs (cagecdxh,
                             refnumha,
                             old_cagecdxh,
                             old_refnumha,
                             chg_update_cde,
                             refnccha,
                             refnvcha,
                             prdldtha,
                             unitisha,
                             cognsnha,
                             fscnsnha,
                             niinsnha,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             datasys_flag,
                             update_date,
                             create_date,
                             prov_userid)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             hold_cagecdxh,
             hold_refnumha,
             'U',
             hold_refnccha,
             hold_refnvcha,
             hold_prdldtha,
             hold_unitisha,
             hold_cognsnha,
             hold_fscnsnha,
             hold_niinsnha,
             'Y',
             'N',
             'Y',
             'U',
             SYSDATE,
             SYSDATE,
             UPPER (osuser)
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_ha_add_trigger
   AFTER INSERT
   ON XYZ_ha
   FOR EACH ROW
BEGIN
   --
   INSERT INTO scm.slic_ha_chgs (cagecdxh,
                                 refnumha,
                                 chg_update_cde,
                                 refnccha,
                                 refnvcha,
                                 prdldtha,
                                 unitisha,
                                 cognsnha,
                                 fscnsnha,
                                 niinsnha,
                                 slicgld_flag,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 datasys_flag,
                                 update_date,
                                 create_date,
                                 prov_userid)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             'A',
             RTRIM (:new.refnccha),
             RTRIM (:new.refnvcha),
             :new.prdldtha,
             RTRIM (:new.unitisha),
             RTRIM (:new.cognsnha),
             RTRIM (:new.fscnsnha),
             RTRIM (:new.niinsnha),
             'Y', -- slicgld_flag
             'Y',
             'Y', -- assetmgr_notf_flag
             'A',
             SYSDATE,
             SYSDATE,
             UPPER (osuser)
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
--
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER rbl_new_frst_prt_insrt_trgr
   AFTER INSERT
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (SUBSTR (new.scm_program, 1, 5) = 'FIRST')
BEGIN
   UPDATE rbl.rbl_partdata p
      SET p.new_first_part_indtr = 'Y', p.first_part_indtr = 'Y'
    WHERE     p.rbl_cage = RTRIM (:new.cagecdxh)
          AND p.rbl_part_nbr = RTRIM (:new.refnumha);
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER rbl_deltd_first_part_trgr
   AFTER DELETE
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (SUBSTR (old.scm_program, 1, 5) = 'FIRST')
BEGIN
   UPDATE rbl.rbl_partdata p
      SET p.new_first_part_indtr = 'N', p.first_part_indtr = 'N'
    WHERE     p.rbl_cage = RTRIM (:old.cagecdxh)
          AND p.rbl_part_nbr = RTRIM (:old.refnumha);
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER rbl_new_frst_prt_updt_trgr_1
   AFTER UPDATE
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (    SUBSTR (new.scm_program, 1, 5) = 'FIRST'
         AND SUBSTR (old.scm_program, 1, 5) != 'FIRST')
BEGIN
   UPDATE rbl.rbl_partdata p
      SET p.new_first_part_indtr = 'Y', p.first_part_indtr = 'Y'
    WHERE     p.rbl_cage = RTRIM (:new.cagecdxh)
          AND p.rbl_part_nbr = RTRIM (:new.refnumha);
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER rbl_new_frst_prt_updt_trgr_2
   AFTER UPDATE
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (    SUBSTR (new.scm_program, 1, 5) != 'FIRST'
         AND SUBSTR (old.scm_program, 1, 5) = 'FIRST')
BEGIN
   UPDATE rbl.rbl_partdata p
      SET p.new_first_part_indtr = 'N', p.first_part_indtr = 'N'
    WHERE     p.rbl_cage = RTRIM (:new.cagecdxh)
          AND p.rbl_part_nbr = RTRIM (:new.refnumha);
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hax04_add_trigger
   AFTER INSERT
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (new.scm_program > ' ')
BEGIN
   INSERT INTO slic_hax04_chgs (cagecdxh,
                                refnumha,
                                scm_program,
                                cur_scm_program,
                                chg_update_cde,
                                slicgld_flag,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                update_date,
                                create_date,
                                prov_userid,
                                datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.scm_program),
             RTRIM (:new.scm_program),
             'A',
             'Y',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (osuser),
             'A'
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE slic_hax04_chgs
            SET chg_update_cde = 'A',
                scm_program = RTRIM (:new.scm_program),
                slicgld_flag = 'Y',
                assetmgr_delt_flag = 'N',
                assetmgr_notf_flag = 'Y',
                update_date = SYSDATE,
                prov_userid =
                   (SELECT UPPER (osuser)
                      FROM v$session
                     WHERE audsid = USERENV ('SESSIONID'))
          WHERE     cagecdxh = RTRIM (:new.cagecdxh)
                AND refnumha = RTRIM (:new.refnumha)
                AND cur_scm_program = RTRIM (:new.scm_program);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hax04_del_trigger
   AFTER DELETE
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (old.scm_program > ' ')
BEGIN
   INSERT INTO slic_hax04_chgs (cagecdxh,
                                refnumha,
                                scm_program,
                                cur_scm_program,
                                chg_update_cde,
                                slicgld_flag,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                update_date,
                                create_date,
                                prov_userid,
                                datasys_flag)
      SELECT RTRIM (:old.cagecdxh),
             RTRIM (:old.refnumha),
             RTRIM (:old.scm_program),
             RTRIM (:old.scm_program),
             'D',
             'Y',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (osuser),
             'D'
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE slic_hax04_chgs
            SET chg_update_cde = 'D',
                slicgld_flag = 'Y',
                scm_program = :old.scm_program,
                assetmgr_delt_flag = 'Y',
                assetmgr_notf_flag = 'Y',
                update_date = SYSDATE,
                prov_userid =
                   (SELECT UPPER (osuser)
                      FROM v$session
                     WHERE audsid = USERENV ('SESSIONID'))
          WHERE     cagecdxh = RTRIM (:old.cagecdxh)
                AND refnumha = RTRIM (:old.refnumha)
                AND cur_scm_program = RTRIM (:old.scm_program);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hax04_updt_trigger
   AFTER UPDATE
   ON XYZ_hax04
   FOR EACH ROW
   WHEN (   old.scm_program <> new.scm_program
         OR old.cagecdxh <> new.cagecdxh
         OR old.refnumha <> new.refnumha)
DECLARE
   exist_counter   NUMBER (9);
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
BEGIN
   IF :new.cagecdxh <> :old.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :new.refnumha <> :old.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   SELECT COUNT (*)
     INTO exist_counter
     FROM slic_hax04_chgs
    WHERE     cagecdxh = RTRIM (:new.cagecdxh)
          AND refnumha = RTRIM (:new.refnumha)
          AND cur_scm_program = RTRIM (:new.scm_program);

   IF exist_counter = 0 THEN
      INSERT INTO slic_hax04_chgs (cagecdxh,
                                   refnumha,
                                   scm_program,
                                   cur_scm_program,
                                   chg_update_cde,
                                   slicgld_flag,
                                   assetmgr_delt_flag,
                                   assetmgr_notf_flag,
                                   update_date,
                                   create_date,
                                   prov_userid,
                                   datasys_flag,
                                   old_cagecdxh,
                                   old_refnumha)
         SELECT RTRIM (:new.cagecdxh),
                RTRIM (:new.refnumha),
                RTRIM (:old.scm_program),
                RTRIM (:new.scm_program),
                'A',
                'Y',
                'N',
                'Y',
                SYSDATE,
                SYSDATE,
                UPPER (osuser),
                'U',
                hold_cagecdxh,
                hold_refnumha
           FROM v$session
          WHERE audsid = USERENV ('SESSIONID');
   ELSIF exist_counter > 0 THEN
      UPDATE slic_hax04_chgs
         SET chg_update_cde = 'U', slicgld_flag = 'Y', assetmgr_delt_flag = 'N', assetmgr_notf_flag = 'Y', datasys_flag = 'U', update_date = SYSDATE, old_cagecdxh = hold_cagecdxh, old_refnumha = hold_refnumha
       WHERE     cagecdxh = RTRIM (:new.cagecdxh)
             AND refnumha = RTRIM (:new.refnumha)
             AND cur_scm_program = RTRIM (:new.scm_program);
   END IF;

   UPDATE slic_hax04_chgs
      SET cur_scm_program = RTRIM (:new.scm_program)
    WHERE     cagecdxh = RTRIM (:new.cagecdxh)
          AND refnumha = RTRIM (:new.refnumha)
          AND cur_scm_program = RTRIM (:old.scm_program);
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER XYZ_intrfce_ext_del_trigger
   AFTER DELETE
   ON slic2b20.XYZ_hb
   FOR EACH ROW
DECLARE
BEGIN
   BEGIN
      DELETE prov.XYZ_intrfce_ext
       WHERE     cage_code = RTRIM (:old.adcagehb)
             AND ref_no = RTRIM (:old.addrefhb)
             AND ha_hb_ind = 'HB';
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER XYZ_intrfce_ext_hb_add_trigger
   AFTER INSERT
   ON slic2b20.XYZ_hb
   FOR EACH ROW
DECLARE
BEGIN
   BEGIN
      INSERT INTO prov.XYZ_intrfce_ext (cage_code, ref_no, ha_hb_ind, proof_transfer_flag)
           VALUES (RTRIM (:new.adcagehb), RTRIM (:new.addrefhb), 'HB', 'N');
   EXCEPTION
      WHEN DUP_VAL_ON_INDEX THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hb_add_trigger
   AFTER INSERT
   ON XYZ_hb
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hb_chgs (cagecdhb,
                             refnumhb,
                             adcagehb,
                             addrefhb,
                             refnccha,
                             refnvcha,
                             chg_update_cde,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             datasys_flag,
                             hb_table,
                             update_date,
                             create_date,
                             prov_userid)
      SELECT RTRIM (:new.cagecdhb),
             RTRIM (:new.refnumhb),
             RTRIM (:new.adcagehb),
             RTRIM (:new.addrefhb),
             :new.adrncchb,
             :new.adrnvchb,
             'A',
             'Y',
             'N',
             'Y',
             'A',
             'HB',
             SYSDATE,
             SYSDATE,
             UPPER (osuser)
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hb_del_trigger
   AFTER DELETE
   ON XYZ_hb
   FOR EACH ROW
BEGIN
   BEGIN
      INSERT INTO slic_hb_chgs (cagecdhb,
                                refnumhb,
                                adcagehb,
                                addrefhb,
                                refnccha,
                                refnvcha,
                                chg_update_cde,
                                slicgld_flag,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                datasys_flag,
                                hb_table,
                                update_date,
                                create_date,
                                prov_userid)
         SELECT RTRIM (:old.cagecdhb),
                RTRIM (:old.refnumhb),
                RTRIM (:old.adcagehb),
                RTRIM (:old.addrefhb),
                :old.adrncchb,
                :old.adrnvchb,
                'D',
                'N',
                'Y',
                'N',
                'D',
                'HB',
                SYSDATE,
                SYSDATE,
                UPPER (osuser)
           FROM v$session
          WHERE audsid = USERENV ('SESSIONID');
   EXCEPTION
      WHEN DUP_VAL_ON_INDEX THEN
         NULL;
   END;

   BEGIN
      UPDATE slic_hb_chgs
         SET chg_update_cde = 'D', slicgld_flag = 'N', assetmgr_delt_flag = 'Y', assetmgr_notf_flag = 'N'
       WHERE     refnumhb = RTRIM (:old.refnumhb)
             AND cagecdhb = RTRIM (:old.cagecdhb)
             AND addrefhb = RTRIM (:old.addrefhb)
             AND adcagehb = RTRIM (:old.adcagehb);
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hb_updt_trigger
   AFTER UPDATE
   ON XYZ_hb
   FOR EACH ROW
DECLARE
   hold_adrncchb   CHAR (1);
   hold_adrnvchb   CHAR (1);
   hold_cagecdhb   VARCHAR (5);
   hold_refnumhb   VARCHAR (32);
   hold_adcagehb   VARCHAR (5);
   hold_addrefhb   VARCHAR (32);
BEGIN
   IF :new.adrncchb <> :old.adrncchb THEN
      hold_adrncchb := :old.adrncchb;
   ELSE
      hold_adrncchb := NULL;
   END IF;

   IF :new.adrnvchb <> :old.adrnvchb THEN
      hold_adrnvchb := :old.adrnvchb;
   ELSE
      hold_adrnvchb := NULL;
   END IF;

   IF :new.cagecdhb <> :old.cagecdhb THEN
      hold_cagecdhb := :old.cagecdhb;
   ELSE
      hold_cagecdhb := NULL;
   END IF;

   IF :new.refnumhb <> :old.refnumhb THEN
      hold_refnumhb := :old.refnumhb;
   ELSE
      hold_refnumhb := NULL;
   END IF;

   IF :new.adcagehb <> :old.adcagehb THEN
      hold_adcagehb := :old.adcagehb;
   ELSE
      hold_adcagehb := NULL;
   END IF;

   IF :new.addrefhb <> :old.addrefhb THEN
      hold_addrefhb := :old.addrefhb;
   ELSE
      hold_addrefhb := NULL;
   END IF;

   INSERT INTO slic_hb_chgs (cagecdhb,
                             refnumhb,
                             adcagehb,
                             addrefhb,
                             refnccha,
                             refnvcha,
                             chg_update_cde,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             datasys_flag,
                             update_date,
                             create_date,
                             hb_table,
                             prov_userid,
                             old_cagecdhb,
                             old_refnumhb,
                             old_adcagehb,
                             old_addrefhb)
      SELECT RTRIM (:new.cagecdhb),
             RTRIM (:new.refnumhb),
             RTRIM (:new.adcagehb),
             RTRIM (:new.addrefhb),
             hold_adrncchb,
             hold_adrnvchb,
             'U',
             'Y',
             'N',
             'Y',
             'U',
             SYSDATE,
             SYSDATE,
             'HB',
             UPPER (osuser),
             hold_cagecdhb,
             hold_refnumhb,
             hold_adcagehb,
             hold_addrefhb
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE slic_hb_chgs
            SET refnccha = :old.adrncchb,
                refnvcha = :old.adrnvchb,
                chg_update_cde = 'U',
                slicgld_flag = 'Y',
                assetmgr_delt_flag = 'N',
                assetmgr_notf_flag = 'Y',
                datasys_flag = 'U',
                update_date = SYSDATE,
                hb_table = 'HB',
                prov_userid =
                   (SELECT UPPER (osuser)
                      FROM v$session
                     WHERE audsid = USERENV ('SESSIONID'))
          WHERE     cagecdhb = RTRIM (:new.cagecdhb)
                AND refnumhb = RTRIM (:new.refnumhb)
                AND adcagehb = RTRIM (:new.adcagehb)
                AND addrefhb = RTRIM (:new.addrefhb);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hbx_del_trigger
   AFTER DELETE
   ON XYZ_hbx01
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hb_chgs (cagecdhb,
                             refnumhb,
                             adcagehb,
                             addrefhb,
                             refnccha,
                             refnvcha,
                             chg_update_cde,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             update_date,
                             create_date,
                             hb_table,
                             prov_userid,
                             datasys_flag)
      SELECT RTRIM (:old.cagecdxh),
             RTRIM (:old.refnumha),
             RTRIM (:old.adcagehb),
             RTRIM (:old.addrefhb),
             NULL,
             NULL,
             'U',
             'Y',
             'Y',
             'N',
             SYSDATE,
             SYSDATE,
             'HBX',
             UPPER (osuser),
             'U'
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE slic_hb_chgs
            SET refnccha = NULL,
                refnvcha = NULL,
                chg_update_cde = 'U',
                slicgld_flag = 'Y',
                assetmgr_delt_flag = 'Y',
                assetmgr_notf_flag = 'N',
                update_date = SYSDATE,
                hb_table = 'HBX',
                prov_userid =
                   (SELECT UPPER (osuser)
                      FROM v$session
                     WHERE audsid = USERENV ('SESSIONID')),
                datasys_flag = 'U'
          WHERE     cagecdhb = RTRIM (:old.cagecdxh)
                AND refnumhb = RTRIM (:old.refnumha)
                AND adcagehb = RTRIM (:old.adcagehb)
                AND addrefhb = RTRIM (:old.addrefhb)
                AND hb_table = 'HBX';
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hbx_updt_trigger
   AFTER UPDATE
   ON XYZ_hbx01
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hb_chgs (cagecdhb,
                             refnumhb,
                             adcagehb,
                             addrefhb,
                             refnccha,
                             refnvcha,
                             chg_update_cde,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             update_date,
                             create_date,
                             hb_table,
                             prov_userid,
                             datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.adcagehb),
             RTRIM (:new.addrefhb),
             NULL,
             NULL,
             'U',
             'Y',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             'HBX',
             UPPER (osuser),
             'U'
        FROM v$session
       WHERE audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE slic_hb_chgs
            SET refnccha = NULL,
                refnvcha = NULL,
                chg_update_cde = 'U',
                slicgld_flag = 'Y',
                assetmgr_delt_flag = 'N',
                assetmgr_notf_flag = 'Y',
                update_date = SYSDATE,
                hb_table = 'HBX',
                prov_userid =
                   (SELECT UPPER (osuser)
                      FROM v$session
                     WHERE audsid = USERENV ('SESSIONID')),
                datasys_flag = 'U'
          WHERE     cagecdhb = RTRIM (:new.cagecdxh)
                AND refnumhb = RTRIM (:new.refnumha)
                AND adcagehb = RTRIM (:new.adcagehb)
                AND addrefhb = RTRIM (:new.addrefhb);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hg_add_trigger
   AFTER INSERT
   ON XYZ_hg
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hg_chgs (cagecdxh,
                             refnumha,
                             eiacodxa,
                             lsaconxb,
                             altlcnxb,
                             lcntypxb,
                             qtyasyhg,
                             remipihg,
                             mrronehg,
                             ortdoohg,
                             frtdffhg,
                             hrtdhhhg,
                             lrtdllhg,
                             drtdddhg,
                             omtdoohg,
                             fmtdffhg,
                             hmtdhhhg,
                             lmtdllhg,
                             dmtdddhg,
                             cbdmtdhg,
                             cadmtdhg,
                             wrkucdhg,
                             smrcodhg,
                             drponehg,
                             qtypeihg,
                             itmcathg,
                             update_date,
                             create_date,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             prov_userid,
                             chg_update_cde,
                             datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.qtyasyhg),
             :new.remipihg,
             :new.mrronehg,
             :new.ortdoohg,
             :new.frtdffhg,
             :new.hrtdhhhg,
             :new.lrtdllhg,
             :new.drtdddhg,
             :new.omtdoohg,
             :new.fmtdffhg,
             :new.hmtdhhhg,
             :new.lmtdllhg,
             :new.dmtdddhg,
             :new.cbdmtdhg,
             :new.cadmtdhg,
             :new.wrkucdhg,
             :new.smrcodhg,
             :new.drponehg,
             :new.qtypeihg,
             RTRIM (:new.itmcathg),
             SYSDATE,
             SYSDATE,
             'Y',
             'Y',
             'Y',
             UPPER (vs.osuser),
             'A',
             'A'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hg_del_trigger
   AFTER DELETE
   ON XYZ_hg
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hg_chgs (cagecdxh,
                             refnumha,
                             eiacodxa,
                             lsaconxb,
                             altlcnxb,
                             lcntypxb,
                             qtyasyhg,
                             remipihg,
                             mrronehg,
                             ortdoohg,
                             frtdffhg,
                             hrtdhhhg,
                             lrtdllhg,
                             drtdddhg,
                             omtdoohg,
                             fmtdffhg,
                             hmtdhhhg,
                             lmtdllhg,
                             dmtdddhg,
                             cbdmtdhg,
                             cadmtdhg,
                             wrkucdhg,
                             smrcodhg,
                             drponehg,
                             qtypeihg,
                             itmcathg,
                             update_date,
                             create_date,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             prov_userid,
                             chg_update_cde,
                             datasys_flag)
      SELECT RTRIM (:old.cagecdxh),
             RTRIM (:old.refnumha),
             RTRIM (:old.eiacodxa),
             RTRIM (:old.lsaconxb),
             RTRIM (:old.altlcnxb),
             RTRIM (:old.lcntypxb),
             RTRIM (:old.qtyasyhg),
             :old.remipihg,
             :old.mrronehg,
             :old.ortdoohg,
             :old.frtdffhg,
             :old.hrtdhhhg,
             :old.lrtdllhg,
             :old.drtdddhg,
             :old.omtdoohg,
             :old.fmtdffhg,
             :old.hmtdhhhg,
             :old.lmtdllhg,
             :old.dmtdddhg,
             :old.cbdmtdhg,
             :old.cadmtdhg,
             :old.wrkucdhg,
             :old.smrcodhg,
             :old.drponehg,
             :old.qtypeihg,
             RTRIM (:old.itmcathg),
             SYSDATE,
             SYSDATE,
             'Y',
             'Y',
             'Y',
             UPPER (vs.osuser),
             'D',
             'D'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hg_updt_trigger
   BEFORE UPDATE
   ON XYZ_hg
   FOR EACH ROW
   WHEN (   new.smrcodhg <> old.smrcodhg
         OR new.drponehg <> old.drponehg
         OR new.remipihg <> old.remipihg
         OR new.mrronehg <> old.mrronehg
         OR new.ortdoohg <> old.ortdoohg
         OR new.frtdffhg <> old.frtdffhg
         OR new.hrtdhhhg <> old.hrtdhhhg
         OR new.lrtdllhg <> old.lrtdllhg
         OR new.drtdddhg <> old.drtdddhg
         OR new.omtdoohg <> old.omtdoohg
         OR new.fmtdffhg <> old.fmtdffhg
         OR new.hmtdhhhg <> old.hmtdhhhg
         OR new.lmtdllhg <> old.lmtdllhg
         OR new.dmtdddhg <> old.dmtdddhg
         OR new.cbdmtdhg <> old.cbdmtdhg
         OR new.cadmtdhg <> old.cadmtdhg
         OR new.qtyasyhg <> old.qtyasyhg
         OR new.qtypeihg <> old.qtypeihg
         OR new.wrkucdhg <> old.wrkucdhg
         OR new.itmcathg <> old.itmcathg
         OR new.esscodhg <> old.esscodhg
         OR new.cagecdxh <> old.cagecdxh
         OR new.refnumha <> old.refnumha
         OR new.eiacodxa <> old.eiacodxa
         OR new.lsaconxb <> old.lsaconxb
         OR new.altlcnxb <> old.altlcnxb
         OR new.lcntypxb <> old.lcntypxb)
DECLARE
   hold_qtyasyhg   CHAR (4);
   hold_qtypeihg   CHAR (5);
   hold_remipihg   CHAR (1);
   hold_mrronehg   NUMBER;
   hold_ortdoohg   NUMBER (5);
   hold_frtdffhg   NUMBER (5);
   hold_hrtdhhhg   NUMBER (5);
   hold_lrtdllhg   NUMBER (5);
   hold_drtdddhg   NUMBER (5);
   hold_omtdoohg   NUMBER (5);
   hold_fmtdffhg   NUMBER (5);
   hold_hmtdhhhg   NUMBER (5);
   hold_lmtdllhg   NUMBER (5);
   hold_dmtdddhg   NUMBER (5);
   hold_cbdmtdhg   NUMBER (5);
   hold_cadmtdhg   NUMBER (5);
   hold_wrkucdhg   VARCHAR (7);
   hold_smrcodhg   VARCHAR (6);
   hold_drponehg   VARCHAR (6);
   hold_itmcathg   VARCHAR (2);
   hold_cagecdxh   VARCHAR (5);
   hold_refnumha   VARCHAR (32);
   hold_eiacodxa   VARCHAR (10);
   hold_lsaconxb   VARCHAR (18);
   hold_altlcnxb   VARCHAR (2);
   hold_lcntypxb   CHAR (1);
   hold_esscodhg   CHAR (1);
BEGIN
   IF :new.itmcathg <> :old.itmcathg THEN
      hold_itmcathg := :old.itmcathg;
   ELSE
      hold_itmcathg := NULL;
   END IF;

   IF :new.qtypeihg <> :old.qtypeihg THEN
      hold_qtypeihg := :old.qtypeihg;
   ELSE
      hold_qtypeihg := NULL;
   END IF;

   IF :new.qtyasyhg <> :old.qtyasyhg THEN
      hold_qtyasyhg := :old.qtyasyhg;
   ELSE
      hold_qtyasyhg := NULL;
   END IF;

   IF :new.smrcodhg <> :old.smrcodhg THEN
      hold_smrcodhg := :old.smrcodhg;
   ELSE
      hold_smrcodhg := NULL;
   END IF;

   IF :new.drponehg <> :old.drponehg THEN
      hold_drponehg := :old.drponehg;
   ELSE
      hold_drponehg := NULL;
   END IF;

   IF :new.remipihg <> :old.remipihg THEN
      hold_remipihg := :old.remipihg;
   ELSE
      hold_remipihg := NULL;
   END IF;

   IF :new.mrronehg <> :old.mrronehg THEN
      hold_mrronehg := :old.mrronehg;
   ELSE
      hold_mrronehg := NULL;
   END IF;

   IF :new.ortdoohg <> :old.ortdoohg THEN
      hold_ortdoohg := :old.ortdoohg;
   ELSE
      hold_ortdoohg := NULL;
   END IF;

   IF :new.frtdffhg <> :old.frtdffhg THEN
      hold_frtdffhg := :old.frtdffhg;
   ELSE
      hold_frtdffhg := NULL;
   END IF;

   IF :new.hrtdhhhg <> :old.hrtdhhhg THEN
      hold_hrtdhhhg := :old.hrtdhhhg;
   ELSE
      hold_hrtdhhhg := NULL;
   END IF;

   IF :new.lrtdllhg <> :old.lrtdllhg THEN
      hold_lrtdllhg := :old.lrtdllhg;
   ELSE
      hold_lrtdllhg := NULL;
   END IF;

   IF :new.drtdddhg <> :old.drtdddhg THEN
      hold_drtdddhg := :old.drtdddhg;
   ELSE
      hold_drtdddhg := NULL;
   END IF;

   IF :new.omtdoohg <> :old.omtdoohg THEN
      hold_omtdoohg := :old.omtdoohg;
   ELSE
      hold_omtdoohg := NULL;
   END IF;

   IF :new.fmtdffhg <> :old.fmtdffhg THEN
      hold_fmtdffhg := :old.fmtdffhg;
   ELSE
      hold_fmtdffhg := NULL;
   END IF;

   IF :new.hmtdhhhg <> :old.hmtdhhhg THEN
      hold_hmtdhhhg := :old.hmtdhhhg;
   ELSE
      hold_hmtdhhhg := NULL;
   END IF;

   IF :new.lmtdllhg <> :old.lmtdllhg THEN
      hold_lmtdllhg := :old.lmtdllhg;
   ELSE
      hold_lmtdllhg := NULL;
   END IF;

   IF :new.dmtdddhg <> :old.dmtdddhg THEN
      hold_dmtdddhg := :old.dmtdddhg;
   ELSE
      hold_dmtdddhg := NULL;
   END IF;

   IF :new.cbdmtdhg <> :old.cbdmtdhg THEN
      hold_cbdmtdhg := :old.cbdmtdhg;
   ELSE
      hold_cbdmtdhg := NULL;
   END IF;

   IF :new.cadmtdhg <> :old.cadmtdhg THEN
      hold_cadmtdhg := :old.cadmtdhg;
   ELSE
      hold_cadmtdhg := NULL;
   END IF;

   IF :new.wrkucdhg <> :old.wrkucdhg THEN
      hold_wrkucdhg := :old.wrkucdhg;
   ELSE
      hold_wrkucdhg := NULL;
   END IF;

   IF :new.esscodhg <> :old.esscodhg THEN
      hold_esscodhg := :old.esscodhg;
   ELSE
      hold_esscodhg := NULL;
   END IF;

   IF :new.cagecdxh <> :old.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :new.refnumha <> :old.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   IF :new.eiacodxa <> :old.eiacodxa THEN
      hold_eiacodxa := :old.eiacodxa;
   ELSE
      hold_eiacodxa := NULL;
   END IF;

   IF :new.lsaconxb <> :old.lsaconxb THEN
      hold_lsaconxb := :old.lsaconxb;
   ELSE
      hold_lsaconxb := NULL;
   END IF;

   IF :new.altlcnxb <> :old.altlcnxb THEN
      hold_altlcnxb := :old.altlcnxb;
   ELSE
      hold_altlcnxb := NULL;
   END IF;

   IF :new.lcntypxb <> :old.lcntypxb THEN
      hold_lcntypxb := :old.lcntypxb;
   ELSE
      hold_lcntypxb := NULL;
   END IF;

   INSERT INTO slic_hg_chgs (cagecdxh,
                             refnumha,
                             eiacodxa,
                             lsaconxb,
                             altlcnxb,
                             lcntypxb,
                             qtyasyhg,
                             remipihg,
                             mrronehg,
                             ortdoohg,
                             frtdffhg,
                             hrtdhhhg,
                             lrtdllhg,
                             drtdddhg,
                             omtdoohg,
                             fmtdffhg,
                             hmtdhhhg,
                             lmtdllhg,
                             dmtdddhg,
                             cbdmtdhg,
                             cadmtdhg,
                             wrkucdhg,
                             smrcodhg,
                             drponehg,
                             qtypeihg,
                             itmcathg,
                             update_date,
                             create_date,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             prov_userid,
                             chg_update_cde,
                             datasys_flag,
                             esscodhg,
                             old_cagecdxh,
                             old_refnumha,
                             old_eiacodxa,
                             old_lsaconxb,
                             old_altlcnxb,
                             old_lcntypxb)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             hold_qtyasyhg,
             hold_remipihg,
             hold_mrronehg,
             hold_ortdoohg,
             hold_frtdffhg,
             hold_hrtdhhhg,
             hold_lrtdllhg,
             hold_drtdddhg,
             hold_omtdoohg,
             hold_fmtdffhg,
             hold_hmtdhhhg,
             hold_lmtdllhg,
             hold_dmtdddhg,
             hold_cbdmtdhg,
             hold_cadmtdhg,
             hold_wrkucdhg,
             hold_smrcodhg,
             hold_drponehg,
             hold_qtypeihg,
             hold_itmcathg,
             SYSDATE,
             SYSDATE,
             'Y',
             'N',
             'Y',
             UPPER (vs.osuser),
             'U',
             'U',
             hold_esscodhg,
             hold_cagecdxh,
             hold_refnumha,
             hold_eiacodxa,
             hold_lsaconxb,
             hold_altlcnxb,
             hold_lcntypxb
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01_delt_trigger
   AFTER DELETE
   ON XYZ_hgx01
   FOR EACH ROW
BEGIN
   DELETE FROM scm.slic_hgx01_chgs
         WHERE     cagecdxh = RTRIM (:old.cagecdxh)
               AND refnumha = RTRIM (:old.refnumha)
               AND eiacodxa = RTRIM (:old.eiacodxa)
               AND lsaconxb = RTRIM (:old.lsaconxb)
               AND altlcnxb = RTRIM (:old.altlcnxb)
               AND lcntypxb = RTRIM (:old.lcntypxb)
               AND nhacgecd = RTRIM (:old.nhacgecd)
               AND nharefno = RTRIM (:old.nharefno);
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01_insrt_trigger
   AFTER INSERT
   ON XYZ_hgx01
   FOR EACH ROW
   WHEN (   new.suplyrcd <> old.suplyrcd
         OR new.suplyrcd > ' '
         OR new.nharefno <> old.nharefno
         OR new.nhacgecd <> old.nhacgecd)
DECLARE
BEGIN
   INSERT INTO slic_hgx01_chgs (cagecdxh,
                                refnumha,
                                eiacodxa,
                                lsaconxb,
                                altlcnxb,
                                lcntypxb,
                                nhacgecd,
                                nharefno,
                                cur_nharefno,
                                cur_nhacgecd,
                                suplyrcd,
                                chg_update_cde,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                prov_userid,
                                update_date,
                                create_date)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.nhacgecd),
             RTRIM (:new.nharefno),
             RTRIM (:new.nharefno),
             RTRIM (:new.nhacgecd),
             RTRIM (:old.suplyrcd),
             'A',
             'N',
             'Y',
             UPPER (vs.osuser),
             SYSDATE,
             SYSDATE
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hgx01_chgs
      SET cur_nharefno = RTRIM (:new.nharefno), cur_nhacgecd = RTRIM (:new.nhacgecd)
    WHERE     cagecdxh = RTRIM (:new.cagecdxh)
          AND refnumha = RTRIM (:new.refnumha)
          AND eiacodxa = RTRIM (:new.eiacodxa)
          AND lsaconxb = RTRIM (:new.lsaconxb)
          AND altlcnxb = RTRIM (:new.altlcnxb)
          AND lcntypxb = RTRIM (:new.lcntypxb)
          AND cur_nhacgecd = RTRIM (:old.nhacgecd)
          AND cur_nharefno = RTRIM (:old.nharefno);
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01_updt_trigger
   BEFORE UPDATE
   ON XYZ_hgx01
   FOR EACH ROW
   WHEN (   new.suplyrcd <> old.suplyrcd
         OR new.suplyrcd > ' '
         OR new.nharefno <> old.nharefno
         OR new.nhacgecd <> old.nhacgecd)
DECLARE
   hold_suplyrcd   CHAR (5);
   hold_nharefno   CHAR (32);
   hold_nhacgecd   CHAR (5);
BEGIN
   IF :new.suplyrcd <> :old.suplyrcd THEN
      hold_suplyrcd := :old.suplyrcd;
   ELSE
      hold_suplyrcd := NULL;
   END IF;

   IF :new.nharefno <> :old.nharefno THEN
      hold_nharefno := :old.nharefno;
   ELSE
      hold_nharefno := NULL;
   END IF;

   IF :new.nhacgecd <> :old.nhacgecd THEN
      hold_nhacgecd := :old.nhacgecd;
   ELSE
      hold_nhacgecd := NULL;
   END IF;

   INSERT INTO slic_hgx01_chgs (cagecdxh,
                                refnumha,
                                eiacodxa,
                                lsaconxb,
                                altlcnxb,
                                lcntypxb,
                                nhacgecd,
                                nharefno,
                                cur_nharefno,
                                cur_nhacgecd,
                                suplyrcd,
                                chg_update_cde,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                prov_userid,
                                update_date,
                                create_date)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             hold_nhacgecd,
             hold_nharefno,
             RTRIM (:new.nharefno),
             RTRIM (:new.nhacgecd),
             hold_suplyrcd,
             'U',
             'N',
             'Y',
             UPPER (vs.osuser),
             SYSDATE,
             SYSDATE
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hgx01_chgs
      SET cur_nharefno = RTRIM (:new.nharefno), cur_nhacgecd = RTRIM (:new.nhacgecd)
    WHERE     cagecdxh = RTRIM (:new.cagecdxh)
          AND refnumha = RTRIM (:new.refnumha)
          AND eiacodxa = RTRIM (:new.eiacodxa)
          AND lsaconxb = RTRIM (:new.lsaconxb)
          AND altlcnxb = RTRIM (:new.altlcnxb)
          AND lcntypxb = RTRIM (:new.lcntypxb)
          AND cur_nhacgecd = RTRIM (:old.nhacgecd)
          AND cur_nharefno = RTRIM (:old.nharefno);
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01a_delt_trigger
   AFTER DELETE
   ON slic2b20.XYZ_hgx01a
   REFERENCING NEW AS new OLD AS old
   FOR EACH ROW
BEGIN
   BEGIN
      INSERT INTO slic_hgx01a_chgs (cagecdxh,
                                    refnumha,
                                    eiacodxa,
                                    lsaconxb,
                                    altlcnxb,
                                    lcntypxb,
                                    nhacgecd,
                                    nharefno,
                                    cur_nhacgecd,
                                    cur_nharefno,
                                    old_model,
                                    old_efffrom,
                                    old_effto,
                                    curr_model,
                                    curr_efffrom,
                                    curr_effto,
                                    chg_update_cde,
                                    assetmgr_delt_flag,
                                    assetmgr_notf_flag,
                                    prov_userid,
                                    update_date,
                                    create_date,
                                    datasys_flag)
         SELECT RTRIM (:old.cagecdxh),
                RTRIM (:old.refnumha),
                RTRIM (:old.eiacodxa),
                RTRIM (:old.lsaconxb),
                RTRIM (:old.altlcnxb),
                RTRIM (:old.lcntypxb),
                RTRIM (:old.nhacgecd),
                RTRIM (:old.nharefno),
                RTRIM (:old.nhacgecd),
                RTRIM (:old.nharefno),
                RTRIM (:old.model),
                RTRIM (:old.efffrom),
                RTRIM (:old.effto),
                RTRIM (:old.model),
                RTRIM (:old.efffrom),
                RTRIM (:old.effto),
                'D',
                'N',
                'N',
                UPPER (vs.osuser),
                SYSDATE,
                SYSDATE,
                'D'
           FROM v$session vs
          WHERE vs.audsid = USERENV ('SESSIONID');
   EXCEPTION
      WHEN OTHERS THEN
         NULL;
   END;

   BEGIN
      UPDATE scm.slic_hgx01a_chgs
         SET chg_update_cde = 'D', assetmgr_notf_flag = 'N', assetmgr_delt_flag = 'Y'
       WHERE     cagecdxh = RTRIM (:old.cagecdxh)
             AND refnumha = RTRIM (:old.refnumha)
             AND eiacodxa = RTRIM (:old.eiacodxa)
             AND lsaconxb = RTRIM (:old.lsaconxb)
             AND altlcnxb = RTRIM (:old.altlcnxb)
             AND lcntypxb = RTRIM (:old.lcntypxb)
             AND cur_nhacgecd = RTRIM (:old.nhacgecd)
             AND cur_nharefno = RTRIM (:old.nharefno)
             AND curr_model = RTRIM (:old.model)
             AND curr_efffrom = RTRIM (:old.efffrom)
             AND curr_effto = RTRIM (:old.effto);
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01a_insrt_trigger
   AFTER INSERT
   ON XYZ_hgx01a
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hgx01a_chgs (cagecdxh,
                                 refnumha,
                                 eiacodxa,
                                 lsaconxb,
                                 altlcnxb,
                                 lcntypxb,
                                 nhacgecd,
                                 nharefno,
                                 cur_nhacgecd,
                                 cur_nharefno,
                                 old_model,
                                 old_efffrom,
                                 old_effto,
                                 curr_model,
                                 curr_efffrom,
                                 curr_effto,
                                 chg_update_cde,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 prov_userid,
                                 update_date,
                                 create_date,
                                 datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.nhacgecd),
             RTRIM (:new.nharefno),
             RTRIM (:new.nhacgecd),
             RTRIM (:new.nharefno),
             RTRIM (:new.model),
             RTRIM (:new.efffrom),
             RTRIM (:new.effto),
             RTRIM (:new.model),
             RTRIM (:new.efffrom),
             RTRIM (:new.effto),
             'A',
             'N',
             'Y',
             UPPER (vs.osuser),
             SYSDATE,
             SYSDATE,
             'A'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hgx01a_updt_trigger
   BEFORE UPDATE
   ON XYZ_hgx01a
   FOR EACH ROW
   WHEN (   new.model <> old.model
         OR new.efffrom <> old.efffrom
         OR new.effto <> old.effto
         OR new.nharefno <> old.nharefno
         OR new.nhacgecd <> old.nhacgecd
         OR new.cagecdxh <> old.cagecdxh
         OR new.refnumha <> old.refnumha)
DECLARE
   hold_nharefno   CHAR (32);
   hold_nhacgecd   CHAR (5);
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
BEGIN
   IF :new.nharefno <> :old.nharefno THEN
      hold_nharefno := :old.nharefno;
   ELSE
      hold_nharefno := NULL;
   END IF;

   IF :new.nhacgecd <> :old.nhacgecd THEN
      hold_nhacgecd := :old.nhacgecd;
   ELSE
      hold_nhacgecd := NULL;
   END IF;

   IF :new.cagecdxh <> :old.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :new.refnumha <> :old.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   INSERT INTO slic_hgx01a_chgs (cagecdxh,
                                 refnumha,
                                 eiacodxa,
                                 lsaconxb,
                                 altlcnxb,
                                 lcntypxb,
                                 nhacgecd,
                                 nharefno,
                                 cur_nhacgecd,
                                 cur_nharefno,
                                 old_model,
                                 old_efffrom,
                                 old_effto,
                                 curr_model,
                                 curr_efffrom,
                                 curr_effto,
                                 chg_update_cde,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 prov_userid,
                                 update_date,
                                 create_date,
                                 datasys_flag,
                                 old_cagecdxh,
                                 old_refnumha)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             hold_nhacgecd,
             hold_nharefno,
             RTRIM (:new.nhacgecd),
             RTRIM (:new.nharefno),
             RTRIM (:old.model),
             RTRIM (:old.efffrom),
             RTRIM (:old.effto),
             RTRIM (:new.model),
             RTRIM (:new.efffrom),
             RTRIM (:new.effto),
             'U',
             'N',
             'Y',
             UPPER (vs.osuser),
             SYSDATE,
             SYSDATE,
             'U',
             hold_cagecdxh,
             hold_refnumha
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hgx01a_chgs
      SET cur_nharefno = RTRIM (:new.nharefno), cur_nhacgecd = RTRIM (:new.nhacgecd)
    WHERE     cagecdxh = RTRIM (:new.cagecdxh)
          AND refnumha = RTRIM (:new.refnumha)
          AND eiacodxa = RTRIM (:new.eiacodxa)
          AND lsaconxb = RTRIM (:new.lsaconxb)
          AND altlcnxb = RTRIM (:new.altlcnxb)
          AND lcntypxb = RTRIM (:new.lcntypxb)
          AND cur_nhacgecd = RTRIM (:old.nhacgecd)
          AND cur_nharefno = RTRIM (:old.nharefno)
          AND curr_model = :old.model
          AND curr_efffrom = :old.efffrom
          AND curr_effto = :old.effto;
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hp_add_trigger
   AFTER INSERT
   ON XYZ_hp
   FOR EACH ROW
   WHEN (new.canumbhp > ' ')
BEGIN
   INSERT INTO slic_hp_chgs (cagecdxh,
                             refnumha,
                             eiacodxa,
                             lsaconxb,
                             altlcnxb,
                             lcntypxb,
                             canumbhp,
                             create_date,
                             update_date,
                             chg_update_cde,
                             rspindhp,
                             intchchp,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             prov_userid,
                             datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             SYSDATE,
             SYSDATE,
             'A',
             RTRIM (:new.rspindhp),
             RTRIM (:new.intchchp),
             'Y',
             'N',
             'Y',
             UPPER (vs.osuser),
             'A'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hp_del_trigger
   AFTER DELETE
   ON XYZ_hp
   FOR EACH ROW
DECLARE
   hold_canumbhp   VARCHAR2 (15);
BEGIN
   BEGIN
      IF RTRIM (:old.canumbhp) > ' ' THEN
         hold_canumbhp := :old.canumbhp;
      ELSE
         hold_canumbhp := 'UNKNOWN';
      END IF;

      INSERT INTO slic_hp_chgs (cagecdxh,
                                refnumha,
                                eiacodxa,
                                lsaconxb,
                                altlcnxb,
                                lcntypxb,
                                canumbhp,
                                create_date,
                                update_date,
                                chg_update_cde,
                                rspindhp,
                                intchchp,
                                slicgld_flag,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                prov_userid,
                                datasys_flag)
         SELECT RTRIM (:old.cagecdxh),
                RTRIM (:old.refnumha),
                RTRIM (:old.eiacodxa),
                RTRIM (:old.lsaconxb),
                RTRIM (:old.altlcnxb),
                RTRIM (:old.lcntypxb),
                hold_canumbhp,
                SYSDATE,
                SYSDATE,
                'D',
                RTRIM (:old.rspindhp),
                RTRIM (:old.intchchp),
                'N',
                'Y',
                'N',
                UPPER (vs.osuser),
                'D'
           FROM v$session vs
          WHERE vs.audsid = USERENV ('SESSIONID');
   EXCEPTION
      WHEN DUP_VAL_ON_INDEX THEN
         NULL;
      WHEN OTHERS THEN
         NULL;
   END;

   BEGIN
      UPDATE slic_hp_chgs
         SET chg_update_cde = 'D', slicgld_flag = 'N', assetmgr_delt_flag = 'Y', assetmgr_notf_flag = 'N'
       WHERE     cagecdxh = RTRIM (:old.cagecdxh)
             AND refnumha = RTRIM (:old.refnumha)
             AND eiacodxa = RTRIM (:old.eiacodxa)
             AND lsaconxb = RTRIM (:old.lsaconxb)
             AND altlcnxb = RTRIM (:old.altlcnxb)
             AND lcntypxb = RTRIM (:old.lcntypxb)
             AND canumbhp = RTRIM (:old.canumbhp);
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hp_updte_trigger
   BEFORE UPDATE
   ON XYZ_hp
   FOR EACH ROW
   WHEN (   old.rspindhp <> new.rspindhp
         OR old.intchchp <> new.intchchp
         OR old.cagecdxh <> new.cagecdxh
         OR old.refnumha <> new.refnumha)
DECLARE
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
BEGIN
   IF :new.cagecdxh <> :old.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :new.refnumha <> :old.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   INSERT INTO slic_hp_chgs (cagecdxh,
                             refnumha,
                             eiacodxa,
                             lsaconxb,
                             altlcnxb,
                             lcntypxb,
                             canumbhp,
                             create_date,
                             update_date,
                             chg_update_cde,
                             rspindhp,
                             intchchp,
                             slicgld_flag,
                             assetmgr_delt_flag,
                             assetmgr_notf_flag,
                             prov_userid,
                             datasys_flag,
                             old_cagecdxh,
                             old_refnumha)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             SYSDATE,
             SYSDATE,
             'A',
             RTRIM (:new.rspindhp),
             RTRIM (:new.intchchp),
             'Y',
             'N',
             'Y',
             UPPER (vs.osuser),
             'U',
             hold_cagecdxh,
             hold_refnumha
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01_updte_trigger
   BEFORE UPDATE
   ON XYZ_hpx01
   FOR EACH ROW
   WHEN (   old.rsind <> new.rsind
         OR old.rsrefno <> new.rsrefno
         OR old.rscagecd <> new.rscagecd
         OR old.repsupid <> new.repsupid
         OR old.cagecdxh <> new.cagecdxh
         OR old.refnumha <> new.refnumha)
DECLARE
   hold_repsupid   VARCHAR (2);
   hold_rsrefno    VARCHAR (32);
   hold_rscagecd   VARCHAR (5);
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
BEGIN
   IF :old.repsupid <> :new.repsupid THEN
      hold_repsupid := :old.repsupid;
   ELSE
      hold_repsupid := NULL;
   END IF;

   IF :old.rsrefno <> :new.rsrefno THEN
      hold_rsrefno := :old.rsrefno;
   ELSE
      hold_rsrefno := NULL;
   END IF;

   IF :old.rscagecd <> :new.rscagecd THEN
      hold_rscagecd := :old.rscagecd;
   ELSE
      hold_rscagecd := NULL;
   END IF;

   IF :old.cagecdxh <> :new.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :old.refnumha <> :new.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   INSERT INTO slic_hpx01_chgs (cagecdxh,
                                refnumha,
                                eiacodxa,
                                lsaconxb,
                                altlcnxb,
                                lcntypxb,
                                canumbhp,
                                old_rsind,
                                repsupid,
                                rsrefno,
                                rscagecd,
                                curr_rsind,
                                chg_update_cde,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                update_date,
                                create_date,
                                prov_userid,
                                datasys_flag,
                                old_cagecdxh,
                                old_refnumha)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             RTRIM (:old.rsind),
             RTRIM (hold_repsupid),
             RTRIM (hold_rsrefno),
             RTRIM (hold_rscagecd),
             RTRIM (:new.rsind),
             'U',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (vs.osuser),
             'U',
             hold_cagecdxh,
             hold_refnumha
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hpx01_chgs
      SET curr_rsind = RTRIM (:new.rsind)
    WHERE     cagecdxh = :old.cagecdxh
          AND refnumha = RTRIM (:old.refnumha)
          AND eiacodxa = RTRIM (:old.eiacodxa)
          AND lsaconxb = RTRIM (:old.lsaconxb)
          AND altlcnxb = RTRIM (:old.altlcnxb)
          AND lcntypxb = :old.lcntypxb
          AND canumbhp = RTRIM (:old.canumbhp)
          AND curr_rsind = :old.rsind;
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01_del_trigger
   BEFORE DELETE
   ON XYZ_hpx01
   FOR EACH ROW
BEGIN
   BEGIN
      INSERT INTO slic_hpx01_chgs (cagecdxh,
                                   refnumha,
                                   eiacodxa,
                                   lsaconxb,
                                   altlcnxb,
                                   lcntypxb,
                                   canumbhp,
                                   old_rsind,
                                   repsupid,
                                   rsrefno,
                                   rscagecd,
                                   curr_rsind,
                                   chg_update_cde,
                                   assetmgr_delt_flag,
                                   assetmgr_notf_flag,
                                   update_date,
                                   create_date,
                                   prov_userid,
                                   datasys_flag)
         SELECT RTRIM (:old.cagecdxh),
                RTRIM (:old.refnumha),
                RTRIM (:old.eiacodxa),
                RTRIM (:old.lsaconxb),
                RTRIM (:old.altlcnxb),
                RTRIM (:old.lcntypxb),
                RTRIM (:old.canumbhp),
                RTRIM (:old.rsind),
                RTRIM (:old.repsupid),
                RTRIM (:old.rsrefno),
                RTRIM (:old.rscagecd),
                RTRIM (:old.rsind),
                'D',
                'Y',
                'Y',
                SYSDATE,
                SYSDATE,
                UPPER (vs.osuser),
                'D'
           FROM v$session vs
          WHERE vs.audsid = USERENV ('SESSIONID');
   EXCEPTION
      WHEN OTHERS THEN
         NULL;
   END;

   BEGIN
      UPDATE slic_hpx01_chgs
         SET chg_update_cde = 'D', assetmgr_delt_flag = 'Y', assetmgr_notf_flag = 'Y', update_date = SYSDATE
       WHERE     cagecdxh = RTRIM (:old.cagecdxh)
             AND refnumha = RTRIM (:old.refnumha)
             AND eiacodxa = RTRIM (:old.eiacodxa)
             AND lsaconxb = RTRIM (:old.lsaconxb)
             AND altlcnxb = RTRIM (:old.altlcnxb)
             AND lcntypxb = :old.lcntypxb
             AND canumbhp = RTRIM (:old.canumbhp)
             AND curr_rsind = :old.rsind;
   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         NULL;
      WHEN OTHERS THEN
         NULL;
   END;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01_insrt_trigger
   AFTER INSERT
   ON XYZ_hpx01
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hpx01_chgs (cagecdxh,
                                refnumha,
                                eiacodxa,
                                lsaconxb,
                                altlcnxb,
                                lcntypxb,
                                canumbhp,
                                old_rsind,
                                repsupid,
                                rsrefno,
                                rscagecd,
                                curr_rsind,
                                chg_update_cde,
                                assetmgr_delt_flag,
                                assetmgr_notf_flag,
                                update_date,
                                create_date,
                                prov_userid,
                                datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             RTRIM (:new.rsind),
             RTRIM (:new.repsupid),
             RTRIM (:new.rsrefno),
             RTRIM (:new.rscagecd),
             RTRIM (:new.rsind),
             'U',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (vs.osuser),
             'A'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01a_del_trigger
   BEFORE DELETE
   ON XYZ_hpx01a
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hpx01a_chgs (cagecdxh,
                                 refnumha,
                                 eiacodxa,
                                 lsaconxb,
                                 altlcnxb,
                                 lcntypxb,
                                 canumbhp,
                                 old_efftyphp,
                                 old_mdlhp,
                                 old_uoefffrm,
                                 old_uoeffto,
                                 curr_efftyphp,
                                 curr_mdlhp,
                                 curr_uoefffrm,
                                 curr_uoeffto,
                                 chg_update_cde,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 update_date,
                                 create_date,
                                 prov_userid,
                                 datasys_flag)
      SELECT RTRIM (:old.cagecdxh),
             RTRIM (:old.refnumha),
             RTRIM (:old.eiacodxa),
             RTRIM (:old.lsaconxb),
             RTRIM (:old.altlcnxb),
             RTRIM (:old.lcntypxb),
             RTRIM (:old.canumbhp),
             RTRIM (:old.efftyphp),
             RTRIM (:old.mdlhp),
             RTRIM (:old.uoefffrm),
             RTRIM (:old.uoeffto),
             RTRIM (:old.efftyphp),
             RTRIM (:old.mdlhp),
             RTRIM (:old.uoefffrm),
             RTRIM (:old.uoeffto),
             'D',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (vs.osuser),
             'D'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hpx01a_chgs
      SET chg_update_cde = 'D', assetmgr_delt_flag = 'N', assetmgr_notf_flag = 'Y'
    WHERE     cagecdxh = RTRIM (:old.cagecdxh)
          AND refnumha = RTRIM (:old.refnumha)
          AND eiacodxa = RTRIM (:old.eiacodxa)
          AND lsaconxb = RTRIM (:old.lsaconxb)
          AND altlcnxb = RTRIM (:old.altlcnxb)
          AND lcntypxb = :old.lcntypxb
          AND canumbhp = RTRIM (:old.canumbhp)
          AND curr_efftyphp = :old.efftyphp
          AND curr_mdlhp = :old.mdlhp
          AND curr_uoefffrm = :old.uoefffrm
          AND curr_uoeffto = :old.uoeffto;
EXCEPTION
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01a_insrt_trigger
   AFTER INSERT
   ON XYZ_hpx01a
   FOR EACH ROW
BEGIN
   INSERT INTO slic_hpx01a_chgs (cagecdxh,
                                 refnumha,
                                 eiacodxa,
                                 lsaconxb,
                                 altlcnxb,
                                 lcntypxb,
                                 canumbhp,
                                 old_efftyphp,
                                 old_mdlhp,
                                 old_uoefffrm,
                                 old_uoeffto,
                                 curr_efftyphp,
                                 curr_mdlhp,
                                 curr_uoefffrm,
                                 curr_uoeffto,
                                 chg_update_cde,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 update_date,
                                 create_date,
                                 prov_userid,
                                 datasys_flag)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             RTRIM (:new.efftyphp),
             RTRIM (:new.mdlhp),
             RTRIM (:new.uoefffrm),
             RTRIM (:new.uoeffto),
             RTRIM (:new.efftyphp),
             RTRIM (:new.mdlhp),
             RTRIM (:new.uoefffrm),
             RTRIM (:new.uoeffto),
             'A',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (vs.osuser),
             'A'
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER scm_slic_hpx01a_updte_trigger
   BEFORE UPDATE
   ON XYZ_hpx01a
   FOR EACH ROW
   WHEN (   old.efftyphp <> new.efftyphp
         OR old.mdlhp <> new.mdlhp
         OR old.uoefffrm <> new.uoefffrm
         OR old.uoeffto <> new.uoeffto
         OR old.cagecdxh <> new.cagecdxh
         OR old.refnumha <> new.refnumha)
DECLARE
   process_flag    CHAR (1) := 'Y';
   hold_cagecdxh   VARCHAR2 (5);
   hold_refnumha   VARCHAR2 (32);
BEGIN
   IF :old.cagecdxh <> :new.cagecdxh THEN
      hold_cagecdxh := :old.cagecdxh;
   ELSE
      hold_cagecdxh := NULL;
   END IF;

   IF :old.refnumha <> :new.refnumha THEN
      hold_refnumha := :old.refnumha;
   ELSE
      hold_refnumha := NULL;
   END IF;

   INSERT INTO slic_hpx01a_chgs (cagecdxh,
                                 refnumha,
                                 eiacodxa,
                                 lsaconxb,
                                 altlcnxb,
                                 lcntypxb,
                                 canumbhp,
                                 old_efftyphp,
                                 old_mdlhp,
                                 old_uoefffrm,
                                 old_uoeffto,
                                 curr_efftyphp,
                                 curr_mdlhp,
                                 curr_uoefffrm,
                                 curr_uoeffto,
                                 chg_update_cde,
                                 assetmgr_delt_flag,
                                 assetmgr_notf_flag,
                                 update_date,
                                 create_date,
                                 prov_userid,
                                 datasys_flag,
                                 old_cagecdxh,
                                 old_refnumha)
      SELECT RTRIM (:new.cagecdxh),
             RTRIM (:new.refnumha),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.lsaconxb),
             RTRIM (:new.altlcnxb),
             RTRIM (:new.lcntypxb),
             RTRIM (:new.canumbhp),
             RTRIM (:old.efftyphp),
             RTRIM (:old.mdlhp),
             RTRIM (:old.uoefffrm),
             RTRIM (:old.uoeffto),
             RTRIM (:new.efftyphp),
             RTRIM (:new.mdlhp),
             RTRIM (:new.uoefffrm),
             RTRIM (:new.uoeffto),
             'U',
             'N',
             'Y',
             SYSDATE,
             SYSDATE,
             UPPER (vs.osuser),
             'U',
             hold_cagecdxh,
             hold_refnumha
        FROM v$session vs
       WHERE vs.audsid = USERENV ('SESSIONID');

   UPDATE slic_hpx01a_chgs
      SET curr_efftyphp = RTRIM (:new.efftyphp), curr_mdlhp = RTRIM (:new.mdlhp), curr_uoefffrm = RTRIM (:new.uoefffrm), curr_uoeffto = RTRIM (:new.uoeffto)
    WHERE     cagecdxh = :old.cagecdxh
          AND refnumha = RTRIM (:old.refnumha)
          AND eiacodxa = RTRIM (:old.eiacodxa)
          AND lsaconxb = RTRIM (:old.lsaconxb)
          AND altlcnxb = RTRIM (:old.altlcnxb)
          AND lcntypxb = :old.lcntypxb
          AND canumbhp = RTRIM (:old.canumbhp)
          AND curr_efftyphp = :old.efftyphp
          AND curr_mdlhp = :old.mdlhp
          AND curr_uoefffrm = :old.uoefffrm
          AND curr_uoeffto = :old.uoeffto;
EXCEPTION
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE TRIGGER isets_xg_on_insert_trigger
   AFTER INSERT
   ON XYZ_xg
   FOR EACH ROW
DECLARE
   eiac_cnt   NUMBER;
   err_num    NUMBER;
   err_msg    CHAR (100);
   err_date   DATE;
BEGIN
   SELECT COUNT (*)
     INTO eiac_cnt
     FROM isets.eiac_codes
    WHERE RTRIM (:new.eiacodxa) = eiac;

   IF     eiac_cnt <> 0
      AND SUBSTR (:new.plsacnxg, 1, 1) IN ('A', 'F', 'B', 'P', 'D', 'S') THEN
      isets.insert_lcn_xg (:new.eiacodxa, :new.flsacnxg, :new.falcncxg, :new.plsacnxg, :new.palcncxg, 'XG_INSERT');
   END IF;
EXCEPTION
   WHEN OTHERS THEN
      BEGIN
         err_num := SQLCODE;
         err_msg := SUBSTR (SQLERRM, 1, 100);
         err_date := SYSDATE;

         INSERT INTO isets.isets_cg_trigger_errors (trigger_type, err_num, err_msg, err_date, lcn, alc, eiac)
              VALUES ('XG_ON_INSERT_CALL', err_num, err_msg, err_date, :new.flsacnxg, :new.falcncxg, :new.eiacodxa);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER isets_xg_on_delete_trigger
   AFTER DELETE
   ON XYZ_xg
   FOR EACH ROW
DECLARE
   err_num    NUMBER;
   err_msg    CHAR (100);
   err_date   DATE;
BEGIN
   UPDATE isets.XYZ_se_part_system
      SET status_date = SYSDATE, task_status_flag = 'D'
    WHERE     lcn_source = 'S'
          AND eiac = RTRIM (:old.eiacodxa)
          AND lcn = RTRIM (:old.flsacnxg)
          AND alc = :old.falcncxg
          AND p_lcn = RTRIM (:old.plsacnxg)
          AND p_alc = :old.palcncxg
          AND task_status_flag IS NULL;

   DELETE FROM isets.XYZ_se_part_system
         WHERE     lcn_source = 'S'
               AND eiac = RTRIM (:old.eiacodxa)
               AND lcn = RTRIM (:old.flsacnxg)
               AND alc = :old.falcncxg
               AND p_lcn = RTRIM (:old.plsacnxg)
               AND p_alc = :old.palcncxg
               AND NVL (task_status_flag, ' ') IN ('I', 'N');
EXCEPTION
   WHEN OTHERS THEN
      BEGIN
         err_num := SQLCODE;
         err_msg := SUBSTR (SQLERRM, 1, 100);
         err_date := SYSDATE;

         INSERT INTO isets.isets_cg_trigger_errors (trigger_type, err_num, err_msg, err_date, lcn, alc, eiac)
              VALUES ('XG_ON_DELETE', err_num, err_msg, err_date, :old.flsacnxg, :old.falcncxg, :old.eiacodxa);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
END;
/


CREATE OR REPLACE TRIGGER rbl_new_part_trgr_xg_insrt
   AFTER INSERT
   ON XYZ_xg
   FOR EACH ROW
   WHEN (    new.flsacnxg LIKE 'P%'
         AND new.flcntyxg = 'F'
         AND new.eiacodxa = 'F/A-18E/F ')
BEGIN
   INSERT INTO rbl.rbl_partdata p (p.rbl_cage,
                                   p.rbl_part_nbr,
                                   p.bl_mtbf,
                                   p.bl_mtbr,
                                   p.bl_mtbm_nd,
                                   p.new_part_indtr,
                                   p.first_part_indtr,
                                   p.new_first_part_indtr,
                                   p.eiacodxa,
                                   p.flsacnxg,
                                   p.falcncxg,
                                   p.plsacnxg,
                                   p.palcncxg)
      SELECT RTRIM (hg.cagecdxh),
             RTRIM (hg.refnumha),
             bd.temtbfbd,
             bd.mtbrxxbd,
             bd.nomtbmbd,
             'Y',
             DECODE (SUBSTR (hax4.scm_program, 1, 5), 'FIRST', 'Y', 'N'),
             DECODE (SUBSTR (hax4.scm_program, 1, 5), 'FIRST', 'Y', 'N'),
             RTRIM (:new.eiacodxa),
             RTRIM (:new.flsacnxg),
             RTRIM (:new.falcncxg),
             RTRIM (:new.plsacnxg),
             RTRIM (:new.palcncxg)
        FROM slic2b20.XYZ_hax04 hax4, slic2b20.XYZ_hg hg, slic2b20.XYZ_bd bd
       WHERE     hg.eiacodxa = :new.eiacodxa
             AND hg.lsaconxb = :new.plsacnxg
             AND hg.altlcnxb = :new.palcncxg
             AND bd.eiacodxa(+) = :new.eiacodxa
             AND bd.lsaconxb(+) = :new.plsacnxg
             AND bd.altlcnxb(+) = :new.palcncxg
             AND bd.ramindbd = 'P'
             AND hax4.cagecdxh(+) = hg.cagecdxh
             AND hax4.refnumha(+) = hg.refnumha;
EXCEPTION
   WHEN DUP_VAL_ON_INDEX THEN
      BEGIN
         UPDATE rbl.rbl_partdata p
            SET p.flsacnxg = RTRIM (:new.flsacnxg),
                p.falcncxg = RTRIM (:new.falcncxg),
                p.plsacnxg = RTRIM (:new.plsacnxg),
                p.palcncxg = RTRIM (:new.palcncxg),
                p.first_part_indtr =
                   (SELECT DECODE (SUBSTR (hax4.scm_program, 1, 5), 'FIRST', 'Y', 'N')
                      FROM slic2b20.XYZ_hax04 hax4, slic2b20.XYZ_hg hg
                     WHERE     hg.eiacodxa = :new.eiacodxa
                           AND hg.lsaconxb = :new.plsacnxg
                           AND hg.altlcnxb = :new.palcncxg
                           AND hax4.cagecdxh(+) = hg.cagecdxh
                           AND hax4.refnumha(+) = hg.refnumha);
      EXCEPTION
         WHEN OTHERS THEN
            NULL;
      END;
   WHEN NO_DATA_FOUND THEN
      NULL;
   WHEN OTHERS THEN
      NULL;
END;
/


CREATE OR REPLACE SYNONYM IETMS.HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM IETMS.HAX01 FOR XYZ_HAX01;


CREATE OR REPLACE SYNONYM IETMS.HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM IETMS.HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM IETMS.HGX01 FOR XYZ_HGX01;


CREATE OR REPLACE SYNONYM IETMS.HGX01A FOR XYZ_HGX01A;


CREATE OR REPLACE SYNONYM IETMS.HPX01 FOR XYZ_HPX01;


CREATE OR REPLACE SYNONYM OPS$B1552048.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$B1552048.XYZ_HAX04 FOR XYZ_HAX04;


CREATE OR REPLACE SYNONYM OPS$B1552048.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$B1552048.XYZ_HBX01 FOR XYZ_HBX01;


CREATE OR REPLACE SYNONYM OPS$B1552048.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M066719.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M066719.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M066719.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M066719.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M072971.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M072971.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M072971.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M072971.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M101340.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M101340.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M101340.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M101340.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M106554.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M106554.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M106554.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M106554.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M108694.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M108694.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M108694.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M108694.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M112610.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M112610.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M112610.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M112610.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M116444.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M116444.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M116444.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M116444.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M116710.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M116710.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M116710.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M116710.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M118470.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M118470.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M118470.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M118470.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M119639.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M119639.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M119639.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M119639.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M119987.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M119987.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M119987.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M119987.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M123489.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M123489.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M123489.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M123489.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M124366.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M124366.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M124366.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M124366.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M124639.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M124639.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M124639.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M124639.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M124709.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M124709.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M124709.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M124709.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M127767.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M127767.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M127767.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M127767.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M129835.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M129835.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M129835.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M129835.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M130515.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M130515.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M130515.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M130515.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M134727.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M134727.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M134727.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M134727.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M148910.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M148910.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M148910.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M148910.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M160237.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M160237.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M160237.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M160237.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M161030.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M161030.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M161030.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M161030.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M166083.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M166083.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M166083.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M166083.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M169494.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M169494.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M169494.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M169494.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M169592.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M169592.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M169592.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M169592.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M169675.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M169675.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M169675.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M169675.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M170007.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M170007.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M170007.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M170007.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M170396.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M170396.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M170396.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M170396.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M172738.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M172738.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M172738.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M172738.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M175277.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M175277.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M175277.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M175277.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M178201.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M178201.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M178201.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M178201.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M179406.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M179406.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M179406.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M179406.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M183800.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M183800.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M183800.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M183800.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M184513.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M184513.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M184513.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M184513.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M184686.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M184686.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M184686.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M184686.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M186335.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M186335.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M186335.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M186335.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M186893.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M186893.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M186893.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M186893.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M187290.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M187290.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M187290.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M187290.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M187460.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M187460.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M187460.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M187460.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M187830.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M187830.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M187830.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M187830.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M188200.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M188200.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M188200.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M188200.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M188834.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M188834.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M188834.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M188834.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M189541.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M189541.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M189541.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M189541.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M189851.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M189851.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M189851.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M189851.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M192249.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M192249.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M192249.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M192249.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M192251.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M192251.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M192251.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M192251.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M194423.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M194423.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M194423.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M194423.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M195731.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M195731.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M195731.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M195731.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M196280.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M196280.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M196280.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M196280.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M197282.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M197282.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M197282.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M197282.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M197563.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M197563.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M197563.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M197563.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M200468.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M200468.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M200468.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M200468.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M201082.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M201082.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M201082.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M201082.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M201499.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M201499.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M201499.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M201499.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M202536.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M202536.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M202536.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M202536.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M204708.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M204708.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M204708.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M204708.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M204862.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M204862.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M204862.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M204862.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M207186.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M207186.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M207186.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M207186.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M207861.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M207861.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M207861.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M207861.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M210800.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M210800.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M210800.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M210800.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M218777.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M218777.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M218777.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M218777.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M222315.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M222315.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M222315.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M222315.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M223470.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M223470.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M223470.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M223470.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M224623.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M224623.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M224623.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M224623.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M230702.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M230702.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M230702.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M230702.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M231399.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M231399.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M231399.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M231399.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M233747.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M233747.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M233747.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M233747.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M234188.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M234188.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M234188.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M234188.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M236298.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M236298.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M236298.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M236298.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M242681.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M242681.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M242681.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M242681.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M243178.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M243178.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M243178.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M243178.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M248566.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M248566.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M248566.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M248566.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M254377.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M254377.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M254377.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M254377.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M257794.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M257794.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M257794.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M257794.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M258023.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M258023.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M258023.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M258023.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M258032.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M258032.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M258032.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M258032.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M261750.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M261750.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M261750.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M261750.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M730841.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M730841.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M730841.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M730841.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM OPS$M736221.XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM OPS$M736221.XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE SYNONYM OPS$M736221.XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM OPS$M736221.XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_HA FOR XYZ_HA;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_HG FOR XYZ_HG;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_HGX FOR XYZ_HGX01A;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_HPX FOR XYZ_HPX01A;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_XA FOR XYZ_XA;


CREATE OR REPLACE SYNONYM PSPMT.SLIC_XG FOR XYZ_XG;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HA FOR XYZ_HA;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HAX01 FOR XYZ_HAX01;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HAX04 FOR XYZ_HAX04;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HB FOR XYZ_HB;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HBX01 FOR XYZ_HBX01;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HG FOR XYZ_HG;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HGX01 FOR XYZ_HGX01;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HGX01A FOR XYZ_HGX01A;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HP FOR XYZ_HP;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HPX01 FOR XYZ_HPX01;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_HPX01A FOR XYZ_HPX01A;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_XA FOR XYZ_XA;


CREATE OR REPLACE PUBLIC SYNONYM XYZ_XG FOR XYZ_XG;


GRANT SELECT ON XYZ_HA TO PUBLIC;

GRANT SELECT ON XYZ_HAX01 TO PUBLIC;

GRANT SELECT ON XYZ_HAX04 TO PUBLIC;

GRANT SELECT ON XYZ_HB TO PUBLIC;

GRANT SELECT ON XYZ_HBX01 TO PUBLIC;

GRANT SELECT ON XYZ_HG TO PUBLIC;

GRANT SELECT ON XYZ_HGX01 TO PUBLIC;

GRANT SELECT ON XYZ_HGX01A TO PUBLIC;

GRANT SELECT ON XYZ_HP TO PUBLIC;

GRANT SELECT ON XYZ_HPX01 TO PUBLIC;

GRANT SELECT ON XYZ_HPX01A TO PUBLIC;

GRANT SELECT ON XYZ_XA TO PUBLIC;

GRANT SELECT ON XYZ_XG TO PUBLIC;
