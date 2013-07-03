CREATE FOREIGN TABLE "gp_toolkit.__gp_fullname" (
	fnoid long OPTIONS (NAMEINSOURCE '"fnoid"', NATIVE_TYPE 'oid'),
	fnnspname string(2147483647) OPTIONS (NAMEINSOURCE '"fnnspname"', NATIVE_TYPE 'name'),
	fnrelname string(2147483647) OPTIONS (NAMEINSOURCE '"fnrelname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_fullname"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_is_append_only" (
	iaooid long OPTIONS (NAMEINSOURCE '"iaooid"', NATIVE_TYPE 'oid'),
	iaotype boolean OPTIONS (NAMEINSOURCE '"iaotype"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_is_append_only"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_localid" (
	localid integer OPTIONS (NAMEINSOURCE '"localid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_localid"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_log_master_ext" (
	logtime timestamp OPTIONS (NAMEINSOURCE '"logtime"', NATIVE_TYPE 'timestamptz'),
	loguser string(2147483647) OPTIONS (NAMEINSOURCE '"loguser"', NATIVE_TYPE 'text'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	logpid string(2147483647) OPTIONS (NAMEINSOURCE '"logpid"', NATIVE_TYPE 'text'),
	logthread string(2147483647) OPTIONS (NAMEINSOURCE '"logthread"', NATIVE_TYPE 'text'),
	loghost string(2147483647) OPTIONS (NAMEINSOURCE '"loghost"', NATIVE_TYPE 'text'),
	logport string(2147483647) OPTIONS (NAMEINSOURCE '"logport"', NATIVE_TYPE 'text'),
	logsessiontime timestamp OPTIONS (NAMEINSOURCE '"logsessiontime"', NATIVE_TYPE 'timestamptz'),
	logtransaction integer OPTIONS (NAMEINSOURCE '"logtransaction"', NATIVE_TYPE 'int4'),
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logsegment string(2147483647) OPTIONS (NAMEINSOURCE '"logsegment"', NATIVE_TYPE 'text'),
	logslice string(2147483647) OPTIONS (NAMEINSOURCE '"logslice"', NATIVE_TYPE 'text'),
	logdistxact string(2147483647) OPTIONS (NAMEINSOURCE '"logdistxact"', NATIVE_TYPE 'text'),
	loglocalxact string(2147483647) OPTIONS (NAMEINSOURCE '"loglocalxact"', NATIVE_TYPE 'text'),
	logsubxact string(2147483647) OPTIONS (NAMEINSOURCE '"logsubxact"', NATIVE_TYPE 'text'),
	logseverity string(2147483647) OPTIONS (NAMEINSOURCE '"logseverity"', NATIVE_TYPE 'text'),
	logstate string(2147483647) OPTIONS (NAMEINSOURCE '"logstate"', NATIVE_TYPE 'text'),
	logmessage string(2147483647) OPTIONS (NAMEINSOURCE '"logmessage"', NATIVE_TYPE 'text'),
	logdetail string(2147483647) OPTIONS (NAMEINSOURCE '"logdetail"', NATIVE_TYPE 'text'),
	loghint string(2147483647) OPTIONS (NAMEINSOURCE '"loghint"', NATIVE_TYPE 'text'),
	logquery string(2147483647) OPTIONS (NAMEINSOURCE '"logquery"', NATIVE_TYPE 'text'),
	logquerypos integer OPTIONS (NAMEINSOURCE '"logquerypos"', NATIVE_TYPE 'int4'),
	logcontext string(2147483647) OPTIONS (NAMEINSOURCE '"logcontext"', NATIVE_TYPE 'text'),
	logdebug string(2147483647) OPTIONS (NAMEINSOURCE '"logdebug"', NATIVE_TYPE 'text'),
	logcursorpos integer OPTIONS (NAMEINSOURCE '"logcursorpos"', NATIVE_TYPE 'int4'),
	logfunction string(2147483647) OPTIONS (NAMEINSOURCE '"logfunction"', NATIVE_TYPE 'text'),
	logfile string(2147483647) OPTIONS (NAMEINSOURCE '"logfile"', NATIVE_TYPE 'text'),
	logline integer OPTIONS (NAMEINSOURCE '"logline"', NATIVE_TYPE 'int4'),
	logstack string(2147483647) OPTIONS (NAMEINSOURCE '"logstack"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_log_master_ext"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_log_segment_ext" (
	logtime timestamp OPTIONS (NAMEINSOURCE '"logtime"', NATIVE_TYPE 'timestamptz'),
	loguser string(2147483647) OPTIONS (NAMEINSOURCE '"loguser"', NATIVE_TYPE 'text'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	logpid string(2147483647) OPTIONS (NAMEINSOURCE '"logpid"', NATIVE_TYPE 'text'),
	logthread string(2147483647) OPTIONS (NAMEINSOURCE '"logthread"', NATIVE_TYPE 'text'),
	loghost string(2147483647) OPTIONS (NAMEINSOURCE '"loghost"', NATIVE_TYPE 'text'),
	logport string(2147483647) OPTIONS (NAMEINSOURCE '"logport"', NATIVE_TYPE 'text'),
	logsessiontime timestamp OPTIONS (NAMEINSOURCE '"logsessiontime"', NATIVE_TYPE 'timestamptz'),
	logtransaction integer OPTIONS (NAMEINSOURCE '"logtransaction"', NATIVE_TYPE 'int4'),
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logsegment string(2147483647) OPTIONS (NAMEINSOURCE '"logsegment"', NATIVE_TYPE 'text'),
	logslice string(2147483647) OPTIONS (NAMEINSOURCE '"logslice"', NATIVE_TYPE 'text'),
	logdistxact string(2147483647) OPTIONS (NAMEINSOURCE '"logdistxact"', NATIVE_TYPE 'text'),
	loglocalxact string(2147483647) OPTIONS (NAMEINSOURCE '"loglocalxact"', NATIVE_TYPE 'text'),
	logsubxact string(2147483647) OPTIONS (NAMEINSOURCE '"logsubxact"', NATIVE_TYPE 'text'),
	logseverity string(2147483647) OPTIONS (NAMEINSOURCE '"logseverity"', NATIVE_TYPE 'text'),
	logstate string(2147483647) OPTIONS (NAMEINSOURCE '"logstate"', NATIVE_TYPE 'text'),
	logmessage string(2147483647) OPTIONS (NAMEINSOURCE '"logmessage"', NATIVE_TYPE 'text'),
	logdetail string(2147483647) OPTIONS (NAMEINSOURCE '"logdetail"', NATIVE_TYPE 'text'),
	loghint string(2147483647) OPTIONS (NAMEINSOURCE '"loghint"', NATIVE_TYPE 'text'),
	logquery string(2147483647) OPTIONS (NAMEINSOURCE '"logquery"', NATIVE_TYPE 'text'),
	logquerypos integer OPTIONS (NAMEINSOURCE '"logquerypos"', NATIVE_TYPE 'int4'),
	logcontext string(2147483647) OPTIONS (NAMEINSOURCE '"logcontext"', NATIVE_TYPE 'text'),
	logdebug string(2147483647) OPTIONS (NAMEINSOURCE '"logdebug"', NATIVE_TYPE 'text'),
	logcursorpos integer OPTIONS (NAMEINSOURCE '"logcursorpos"', NATIVE_TYPE 'int4'),
	logfunction string(2147483647) OPTIONS (NAMEINSOURCE '"logfunction"', NATIVE_TYPE 'text'),
	logfile string(2147483647) OPTIONS (NAMEINSOURCE '"logfile"', NATIVE_TYPE 'text'),
	logline integer OPTIONS (NAMEINSOURCE '"logline"', NATIVE_TYPE 'int4'),
	logstack string(2147483647) OPTIONS (NAMEINSOURCE '"logstack"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_log_segment_ext"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_masterid" (
	masterid integer OPTIONS (NAMEINSOURCE '"masterid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_masterid"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_number_of_segments" (
	numsegments short OPTIONS (NAMEINSOURCE '"numsegments"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_number_of_segments"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_user_data_tables" (
	autnspname string(2147483647) OPTIONS (NAMEINSOURCE '"autnspname"', NATIVE_TYPE 'name'),
	autrelname string(2147483647) OPTIONS (NAMEINSOURCE '"autrelname"', NATIVE_TYPE 'name'),
	autrelkind string(1) OPTIONS (NAMEINSOURCE '"autrelkind"', NATIVE_TYPE 'char'),
	autreltuples float OPTIONS (NAMEINSOURCE '"autreltuples"', NATIVE_TYPE 'float4'),
	autrelpages integer OPTIONS (NAMEINSOURCE '"autrelpages"', NATIVE_TYPE 'int4'),
	autrelacl object OPTIONS (NAMEINSOURCE '"autrelacl"', NATIVE_TYPE '_aclitem'),
	autoid long OPTIONS (NAMEINSOURCE '"autoid"', NATIVE_TYPE 'oid'),
	auttoastoid long OPTIONS (NAMEINSOURCE '"auttoastoid"', NATIVE_TYPE 'oid'),
	autrelstorage string(1) OPTIONS (NAMEINSOURCE '"autrelstorage"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_user_data_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_user_data_tables_readable" (
	autnspname string(2147483647) OPTIONS (NAMEINSOURCE '"autnspname"', NATIVE_TYPE 'name'),
	autrelname string(2147483647) OPTIONS (NAMEINSOURCE '"autrelname"', NATIVE_TYPE 'name'),
	autrelkind string(1) OPTIONS (NAMEINSOURCE '"autrelkind"', NATIVE_TYPE 'char'),
	autreltuples float OPTIONS (NAMEINSOURCE '"autreltuples"', NATIVE_TYPE 'float4'),
	autrelpages integer OPTIONS (NAMEINSOURCE '"autrelpages"', NATIVE_TYPE 'int4'),
	autrelacl object OPTIONS (NAMEINSOURCE '"autrelacl"', NATIVE_TYPE '_aclitem'),
	autoid long OPTIONS (NAMEINSOURCE '"autoid"', NATIVE_TYPE 'oid'),
	auttoastoid long OPTIONS (NAMEINSOURCE '"auttoastoid"', NATIVE_TYPE 'oid'),
	autrelstorage string(1) OPTIONS (NAMEINSOURCE '"autrelstorage"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_user_data_tables_readable"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_user_namespaces" (
	aunoid long OPTIONS (NAMEINSOURCE '"aunoid"', NATIVE_TYPE 'oid'),
	aunnspname string(2147483647) OPTIONS (NAMEINSOURCE '"aunnspname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_user_namespaces"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.__gp_user_tables" (
	autnspname string(2147483647) OPTIONS (NAMEINSOURCE '"autnspname"', NATIVE_TYPE 'name'),
	autrelname string(2147483647) OPTIONS (NAMEINSOURCE '"autrelname"', NATIVE_TYPE 'name'),
	autrelkind string(1) OPTIONS (NAMEINSOURCE '"autrelkind"', NATIVE_TYPE 'char'),
	autreltuples float OPTIONS (NAMEINSOURCE '"autreltuples"', NATIVE_TYPE 'float4'),
	autrelpages integer OPTIONS (NAMEINSOURCE '"autrelpages"', NATIVE_TYPE 'int4'),
	autrelacl object OPTIONS (NAMEINSOURCE '"autrelacl"', NATIVE_TYPE '_aclitem'),
	autoid long OPTIONS (NAMEINSOURCE '"autoid"', NATIVE_TYPE 'oid'),
	auttoastoid long OPTIONS (NAMEINSOURCE '"auttoastoid"', NATIVE_TYPE 'oid'),
	autrelstorage string(1) OPTIONS (NAMEINSOURCE '"autrelstorage"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."__gp_user_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_bloat_diag" (
	bdirelid long OPTIONS (NAMEINSOURCE '"bdirelid"', NATIVE_TYPE 'oid'),
	bdinspname string(2147483647) OPTIONS (NAMEINSOURCE '"bdinspname"', NATIVE_TYPE 'name'),
	bdirelname string(2147483647) OPTIONS (NAMEINSOURCE '"bdirelname"', NATIVE_TYPE 'name'),
	bdirelpages integer OPTIONS (NAMEINSOURCE '"bdirelpages"', NATIVE_TYPE 'int4'),
	bdiexppages bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"bdiexppages"', NATIVE_TYPE 'numeric'),
	bdidiag string(2147483647) OPTIONS (NAMEINSOURCE '"bdidiag"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_bloat_diag"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_bloat_expected_pages" (
	btdrelid long OPTIONS (NAMEINSOURCE '"btdrelid"', NATIVE_TYPE 'oid'),
	btdrelpages integer OPTIONS (NAMEINSOURCE '"btdrelpages"', NATIVE_TYPE 'int4'),
	btdexppages bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"btdexppages"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_bloat_expected_pages"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_disk_free" (
	dfsegment integer OPTIONS (NAMEINSOURCE '"dfsegment"', NATIVE_TYPE 'int4'),
	dfhostname string(2147483647) OPTIONS (NAMEINSOURCE '"dfhostname"', NATIVE_TYPE 'text'),
	dfdevice string(2147483647) OPTIONS (NAMEINSOURCE '"dfdevice"', NATIVE_TYPE 'text'),
	dfspace long OPTIONS (NAMEINSOURCE '"dfspace"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_disk_free"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_locks_on_relation" (
	lorlocktype string(2147483647) OPTIONS (NAMEINSOURCE '"lorlocktype"', NATIVE_TYPE 'text'),
	lordatabase long OPTIONS (NAMEINSOURCE '"lordatabase"', NATIVE_TYPE 'oid'),
	lorrelname string(2147483647) OPTIONS (NAMEINSOURCE '"lorrelname"', NATIVE_TYPE 'name'),
	lorrelation long OPTIONS (NAMEINSOURCE '"lorrelation"', NATIVE_TYPE 'oid'),
	lortransaction object OPTIONS (NAMEINSOURCE '"lortransaction"', NATIVE_TYPE 'xid'),
	lorpid integer OPTIONS (NAMEINSOURCE '"lorpid"', NATIVE_TYPE 'int4'),
	lormode string(2147483647) OPTIONS (NAMEINSOURCE '"lormode"', NATIVE_TYPE 'text'),
	lorgranted boolean OPTIONS (NAMEINSOURCE '"lorgranted"', NATIVE_TYPE 'bool'),
	lorcurrentquery string(2147483647) OPTIONS (NAMEINSOURCE '"lorcurrentquery"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_locks_on_relation"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_locks_on_resqueue" (
	lorusename string(2147483647) OPTIONS (NAMEINSOURCE '"lorusename"', NATIVE_TYPE 'name'),
	lorrsqname string(2147483647) OPTIONS (NAMEINSOURCE '"lorrsqname"', NATIVE_TYPE 'name'),
	lorlocktype string(2147483647) OPTIONS (NAMEINSOURCE '"lorlocktype"', NATIVE_TYPE 'text'),
	lorobjid long OPTIONS (NAMEINSOURCE '"lorobjid"', NATIVE_TYPE 'oid'),
	lortransaction object OPTIONS (NAMEINSOURCE '"lortransaction"', NATIVE_TYPE 'xid'),
	lorpid integer OPTIONS (NAMEINSOURCE '"lorpid"', NATIVE_TYPE 'int4'),
	lormode string(2147483647) OPTIONS (NAMEINSOURCE '"lormode"', NATIVE_TYPE 'text'),
	lorgranted boolean OPTIONS (NAMEINSOURCE '"lorgranted"', NATIVE_TYPE 'bool'),
	lorwaiting boolean OPTIONS (NAMEINSOURCE '"lorwaiting"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_locks_on_resqueue"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_log_command_timings" (
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	loguser string(2147483647) OPTIONS (NAMEINSOURCE '"loguser"', NATIVE_TYPE 'text'),
	logpid string(2147483647) OPTIONS (NAMEINSOURCE '"logpid"', NATIVE_TYPE 'text'),
	logtimemin timestamp OPTIONS (NAMEINSOURCE '"logtimemin"', NATIVE_TYPE 'timestamptz'),
	logtimemax timestamp OPTIONS (NAMEINSOURCE '"logtimemax"', NATIVE_TYPE 'timestamptz'),
	logduration object(49) OPTIONS (NAMEINSOURCE '"logduration"', NATIVE_TYPE 'interval')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_log_command_timings"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_log_database" (
	logtime timestamp OPTIONS (NAMEINSOURCE '"logtime"', NATIVE_TYPE 'timestamptz'),
	loguser string(2147483647) OPTIONS (NAMEINSOURCE '"loguser"', NATIVE_TYPE 'text'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	logpid string(2147483647) OPTIONS (NAMEINSOURCE '"logpid"', NATIVE_TYPE 'text'),
	logthread string(2147483647) OPTIONS (NAMEINSOURCE '"logthread"', NATIVE_TYPE 'text'),
	loghost string(2147483647) OPTIONS (NAMEINSOURCE '"loghost"', NATIVE_TYPE 'text'),
	logport string(2147483647) OPTIONS (NAMEINSOURCE '"logport"', NATIVE_TYPE 'text'),
	logsessiontime timestamp OPTIONS (NAMEINSOURCE '"logsessiontime"', NATIVE_TYPE 'timestamptz'),
	logtransaction integer OPTIONS (NAMEINSOURCE '"logtransaction"', NATIVE_TYPE 'int4'),
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logsegment string(2147483647) OPTIONS (NAMEINSOURCE '"logsegment"', NATIVE_TYPE 'text'),
	logslice string(2147483647) OPTIONS (NAMEINSOURCE '"logslice"', NATIVE_TYPE 'text'),
	logdistxact string(2147483647) OPTIONS (NAMEINSOURCE '"logdistxact"', NATIVE_TYPE 'text'),
	loglocalxact string(2147483647) OPTIONS (NAMEINSOURCE '"loglocalxact"', NATIVE_TYPE 'text'),
	logsubxact string(2147483647) OPTIONS (NAMEINSOURCE '"logsubxact"', NATIVE_TYPE 'text'),
	logseverity string(2147483647) OPTIONS (NAMEINSOURCE '"logseverity"', NATIVE_TYPE 'text'),
	logstate string(2147483647) OPTIONS (NAMEINSOURCE '"logstate"', NATIVE_TYPE 'text'),
	logmessage string(2147483647) OPTIONS (NAMEINSOURCE '"logmessage"', NATIVE_TYPE 'text'),
	logdetail string(2147483647) OPTIONS (NAMEINSOURCE '"logdetail"', NATIVE_TYPE 'text'),
	loghint string(2147483647) OPTIONS (NAMEINSOURCE '"loghint"', NATIVE_TYPE 'text'),
	logquery string(2147483647) OPTIONS (NAMEINSOURCE '"logquery"', NATIVE_TYPE 'text'),
	logquerypos integer OPTIONS (NAMEINSOURCE '"logquerypos"', NATIVE_TYPE 'int4'),
	logcontext string(2147483647) OPTIONS (NAMEINSOURCE '"logcontext"', NATIVE_TYPE 'text'),
	logdebug string(2147483647) OPTIONS (NAMEINSOURCE '"logdebug"', NATIVE_TYPE 'text'),
	logcursorpos integer OPTIONS (NAMEINSOURCE '"logcursorpos"', NATIVE_TYPE 'int4'),
	logfunction string(2147483647) OPTIONS (NAMEINSOURCE '"logfunction"', NATIVE_TYPE 'text'),
	logfile string(2147483647) OPTIONS (NAMEINSOURCE '"logfile"', NATIVE_TYPE 'text'),
	logline integer OPTIONS (NAMEINSOURCE '"logline"', NATIVE_TYPE 'int4'),
	logstack string(2147483647) OPTIONS (NAMEINSOURCE '"logstack"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_log_database"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_log_master_concise" (
	logtime timestamp OPTIONS (NAMEINSOURCE '"logtime"', NATIVE_TYPE 'timestamptz'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logseverity string(2147483647) OPTIONS (NAMEINSOURCE '"logseverity"', NATIVE_TYPE 'text'),
	logmessage string(2147483647) OPTIONS (NAMEINSOURCE '"logmessage"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_log_master_concise"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_log_system" (
	logtime timestamp OPTIONS (NAMEINSOURCE '"logtime"', NATIVE_TYPE 'timestamptz'),
	loguser string(2147483647) OPTIONS (NAMEINSOURCE '"loguser"', NATIVE_TYPE 'text'),
	logdatabase string(2147483647) OPTIONS (NAMEINSOURCE '"logdatabase"', NATIVE_TYPE 'text'),
	logpid string(2147483647) OPTIONS (NAMEINSOURCE '"logpid"', NATIVE_TYPE 'text'),
	logthread string(2147483647) OPTIONS (NAMEINSOURCE '"logthread"', NATIVE_TYPE 'text'),
	loghost string(2147483647) OPTIONS (NAMEINSOURCE '"loghost"', NATIVE_TYPE 'text'),
	logport string(2147483647) OPTIONS (NAMEINSOURCE '"logport"', NATIVE_TYPE 'text'),
	logsessiontime timestamp OPTIONS (NAMEINSOURCE '"logsessiontime"', NATIVE_TYPE 'timestamptz'),
	logtransaction integer OPTIONS (NAMEINSOURCE '"logtransaction"', NATIVE_TYPE 'int4'),
	logsession string(2147483647) OPTIONS (NAMEINSOURCE '"logsession"', NATIVE_TYPE 'text'),
	logcmdcount string(2147483647) OPTIONS (NAMEINSOURCE '"logcmdcount"', NATIVE_TYPE 'text'),
	logsegment string(2147483647) OPTIONS (NAMEINSOURCE '"logsegment"', NATIVE_TYPE 'text'),
	logslice string(2147483647) OPTIONS (NAMEINSOURCE '"logslice"', NATIVE_TYPE 'text'),
	logdistxact string(2147483647) OPTIONS (NAMEINSOURCE '"logdistxact"', NATIVE_TYPE 'text'),
	loglocalxact string(2147483647) OPTIONS (NAMEINSOURCE '"loglocalxact"', NATIVE_TYPE 'text'),
	logsubxact string(2147483647) OPTIONS (NAMEINSOURCE '"logsubxact"', NATIVE_TYPE 'text'),
	logseverity string(2147483647) OPTIONS (NAMEINSOURCE '"logseverity"', NATIVE_TYPE 'text'),
	logstate string(2147483647) OPTIONS (NAMEINSOURCE '"logstate"', NATIVE_TYPE 'text'),
	logmessage string(2147483647) OPTIONS (NAMEINSOURCE '"logmessage"', NATIVE_TYPE 'text'),
	logdetail string(2147483647) OPTIONS (NAMEINSOURCE '"logdetail"', NATIVE_TYPE 'text'),
	loghint string(2147483647) OPTIONS (NAMEINSOURCE '"loghint"', NATIVE_TYPE 'text'),
	logquery string(2147483647) OPTIONS (NAMEINSOURCE '"logquery"', NATIVE_TYPE 'text'),
	logquerypos integer OPTIONS (NAMEINSOURCE '"logquerypos"', NATIVE_TYPE 'int4'),
	logcontext string(2147483647) OPTIONS (NAMEINSOURCE '"logcontext"', NATIVE_TYPE 'text'),
	logdebug string(2147483647) OPTIONS (NAMEINSOURCE '"logdebug"', NATIVE_TYPE 'text'),
	logcursorpos integer OPTIONS (NAMEINSOURCE '"logcursorpos"', NATIVE_TYPE 'int4'),
	logfunction string(2147483647) OPTIONS (NAMEINSOURCE '"logfunction"', NATIVE_TYPE 'text'),
	logfile string(2147483647) OPTIONS (NAMEINSOURCE '"logfile"', NATIVE_TYPE 'text'),
	logline integer OPTIONS (NAMEINSOURCE '"logline"', NATIVE_TYPE 'int4'),
	logstack string(2147483647) OPTIONS (NAMEINSOURCE '"logstack"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_log_system"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_param_setting_t" (
	paramsegment integer OPTIONS (NAMEINSOURCE '"paramsegment"', NATIVE_TYPE 'int4'),
	paramname string(2147483647) OPTIONS (NAMEINSOURCE '"paramname"', NATIVE_TYPE 'text'),
	paramvalue string(2147483647) OPTIONS (NAMEINSOURCE '"paramvalue"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_param_setting_t"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_param_settings_seg_value_diffs" (
	psdname string(2147483647) OPTIONS (NAMEINSOURCE '"psdname"', NATIVE_TYPE 'text'),
	psdvalue string(2147483647) OPTIONS (NAMEINSOURCE '"psdvalue"', NATIVE_TYPE 'text'),
	psdcount long OPTIONS (NAMEINSOURCE '"psdcount"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_param_settings_seg_value_diffs"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_pgdatabase_invalid" (
	pgdbidbid short OPTIONS (NAMEINSOURCE '"pgdbidbid"', NATIVE_TYPE 'int2'),
	pgdbiisprimary boolean OPTIONS (NAMEINSOURCE '"pgdbiisprimary"', NATIVE_TYPE 'bool'),
	pgdbicontent short OPTIONS (NAMEINSOURCE '"pgdbicontent"', NATIVE_TYPE 'int2'),
	pgdbivalid boolean OPTIONS (NAMEINSOURCE '"pgdbivalid"', NATIVE_TYPE 'bool'),
	pgdbidefinedprimary boolean OPTIONS (NAMEINSOURCE '"pgdbidefinedprimary"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_pgdatabase_invalid"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resq_activity" (
	resqprocpid integer OPTIONS (NAMEINSOURCE '"resqprocpid"', NATIVE_TYPE 'int4'),
	resqrole string(2147483647) OPTIONS (NAMEINSOURCE '"resqrole"', NATIVE_TYPE 'name'),
	resqoid long OPTIONS (NAMEINSOURCE '"resqoid"', NATIVE_TYPE 'oid'),
	resqname string(2147483647) OPTIONS (NAMEINSOURCE '"resqname"', NATIVE_TYPE 'name'),
	resqstart timestamp OPTIONS (NAMEINSOURCE '"resqstart"', NATIVE_TYPE 'timestamptz'),
	resqstatus string(2147483647) OPTIONS (NAMEINSOURCE '"resqstatus"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resq_activity"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resq_activity_by_queue" (
	resqoid long OPTIONS (NAMEINSOURCE '"resqoid"', NATIVE_TYPE 'oid'),
	resqname string(2147483647) OPTIONS (NAMEINSOURCE '"resqname"', NATIVE_TYPE 'name'),
	resqlast timestamp OPTIONS (NAMEINSOURCE '"resqlast"', NATIVE_TYPE 'timestamptz'),
	resqstatus string(2147483647) OPTIONS (NAMEINSOURCE '"resqstatus"', NATIVE_TYPE 'text'),
	resqtotal long OPTIONS (NAMEINSOURCE '"resqtotal"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resq_activity_by_queue"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resq_priority_backend" (
	rqpsession integer OPTIONS (NAMEINSOURCE '"rqpsession"', NATIVE_TYPE 'int4'),
	rqpcommand integer OPTIONS (NAMEINSOURCE '"rqpcommand"', NATIVE_TYPE 'int4'),
	rqppriority string(2147483647) OPTIONS (NAMEINSOURCE '"rqppriority"', NATIVE_TYPE 'text'),
	rqpweight integer OPTIONS (NAMEINSOURCE '"rqpweight"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resq_priority_backend"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resq_priority_statement" (
	rqpdatname string(2147483647) OPTIONS (NAMEINSOURCE '"rqpdatname"', NATIVE_TYPE 'name'),
	rqpusename string(2147483647) OPTIONS (NAMEINSOURCE '"rqpusename"', NATIVE_TYPE 'name'),
	rqpsession integer OPTIONS (NAMEINSOURCE '"rqpsession"', NATIVE_TYPE 'int4'),
	rqpcommand integer OPTIONS (NAMEINSOURCE '"rqpcommand"', NATIVE_TYPE 'int4'),
	rqppriority string(2147483647) OPTIONS (NAMEINSOURCE '"rqppriority"', NATIVE_TYPE 'text'),
	rqpweight integer OPTIONS (NAMEINSOURCE '"rqpweight"', NATIVE_TYPE 'int4'),
	rqpquery string(2147483647) OPTIONS (NAMEINSOURCE '"rqpquery"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resq_priority_statement"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resq_role" (
	rrrolname string(2147483647) OPTIONS (NAMEINSOURCE '"rrrolname"', NATIVE_TYPE 'name'),
	rrrsqname string(2147483647) OPTIONS (NAMEINSOURCE '"rrrsqname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resq_role"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_resqueue_status" (
	queueid long OPTIONS (NAMEINSOURCE '"queueid"', NATIVE_TYPE 'oid'),
	rsqname string(2147483647) OPTIONS (NAMEINSOURCE '"rsqname"', NATIVE_TYPE 'name'),
	rsqcountlimit integer OPTIONS (NAMEINSOURCE '"rsqcountlimit"', NATIVE_TYPE 'int4'),
	rsqcountvalue integer OPTIONS (NAMEINSOURCE '"rsqcountvalue"', NATIVE_TYPE 'int4'),
	rsqcostlimit float OPTIONS (NAMEINSOURCE '"rsqcostlimit"', NATIVE_TYPE 'float4'),
	rsqcostvalue float OPTIONS (NAMEINSOURCE '"rsqcostvalue"', NATIVE_TYPE 'float4'),
	rsqmemorylimit float OPTIONS (NAMEINSOURCE '"rsqmemorylimit"', NATIVE_TYPE 'float4'),
	rsqmemoryvalue float OPTIONS (NAMEINSOURCE '"rsqmemoryvalue"', NATIVE_TYPE 'float4'),
	rsqwaiters integer OPTIONS (NAMEINSOURCE '"rsqwaiters"', NATIVE_TYPE 'int4'),
	rsqholders integer OPTIONS (NAMEINSOURCE '"rsqholders"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_resqueue_status"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_roles_assigned" (
	raroleid long OPTIONS (NAMEINSOURCE '"raroleid"', NATIVE_TYPE 'oid'),
	rarolename string(2147483647) OPTIONS (NAMEINSOURCE '"rarolename"', NATIVE_TYPE 'name'),
	ramemberid long OPTIONS (NAMEINSOURCE '"ramemberid"', NATIVE_TYPE 'oid'),
	ramembername string(2147483647) OPTIONS (NAMEINSOURCE '"ramembername"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_roles_assigned"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_all_table_indexes" (
	soatioid long OPTIONS (NAMEINSOURCE '"soatioid"', NATIVE_TYPE 'oid'),
	soatisize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"soatisize"', NATIVE_TYPE 'numeric'),
	soatischemaname string(2147483647) OPTIONS (NAMEINSOURCE '"soatischemaname"', NATIVE_TYPE 'name'),
	soatitablename string(2147483647) OPTIONS (NAMEINSOURCE '"soatitablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_all_table_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_database" (
	sodddatname string(2147483647) OPTIONS (NAMEINSOURCE '"sodddatname"', NATIVE_TYPE 'name'),
	sodddatsize long OPTIONS (NAMEINSOURCE '"sodddatsize"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_database"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_index" (
	soioid long OPTIONS (NAMEINSOURCE '"soioid"', NATIVE_TYPE 'oid'),
	soitableoid long OPTIONS (NAMEINSOURCE '"soitableoid"', NATIVE_TYPE 'oid'),
	soisize long OPTIONS (NAMEINSOURCE '"soisize"', NATIVE_TYPE 'int8'),
	soiindexschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"soiindexschemaname"', NATIVE_TYPE 'name'),
	soiindexname string(2147483647) OPTIONS (NAMEINSOURCE '"soiindexname"', NATIVE_TYPE 'name'),
	soitableschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"soitableschemaname"', NATIVE_TYPE 'name'),
	soitablename string(2147483647) OPTIONS (NAMEINSOURCE '"soitablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_partition_and_indexes_disk" (
	sopaidparentoid long OPTIONS (NAMEINSOURCE '"sopaidparentoid"', NATIVE_TYPE 'oid'),
	sopaidpartitionoid long OPTIONS (NAMEINSOURCE '"sopaidpartitionoid"', NATIVE_TYPE 'oid'),
	sopaidpartitiontablesize long OPTIONS (NAMEINSOURCE '"sopaidpartitiontablesize"', NATIVE_TYPE 'int8'),
	sopaidpartitionindexessize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"sopaidpartitionindexessize"', NATIVE_TYPE 'numeric'),
	sopaidparentschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sopaidparentschemaname"', NATIVE_TYPE 'name'),
	sopaidparenttablename string(2147483647) OPTIONS (NAMEINSOURCE '"sopaidparenttablename"', NATIVE_TYPE 'name'),
	sopaidpartitionschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sopaidpartitionschemaname"', NATIVE_TYPE 'name'),
	sopaidpartitiontablename string(2147483647) OPTIONS (NAMEINSOURCE '"sopaidpartitiontablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_partition_and_indexes_disk"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_schema_disk" (
	sosdnsp string(2147483647) OPTIONS (NAMEINSOURCE '"sosdnsp"', NATIVE_TYPE 'name'),
	sosdschematablesize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"sosdschematablesize"', NATIVE_TYPE 'numeric'),
	sosdschemaidxsize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"sosdschemaidxsize"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_schema_disk"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_table_and_indexes_disk" (
	sotaidoid long OPTIONS (NAMEINSOURCE '"sotaidoid"', NATIVE_TYPE 'oid'),
	sotaidtablesize long OPTIONS (NAMEINSOURCE '"sotaidtablesize"', NATIVE_TYPE 'int8'),
	sotaididxsize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"sotaididxsize"', NATIVE_TYPE 'numeric'),
	sotaidschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sotaidschemaname"', NATIVE_TYPE 'name'),
	sotaidtablename string(2147483647) OPTIONS (NAMEINSOURCE '"sotaidtablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_table_and_indexes_disk"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_table_and_indexes_licensing" (
	sotailoid long OPTIONS (NAMEINSOURCE '"sotailoid"', NATIVE_TYPE 'oid'),
	sotailtablesizedisk long OPTIONS (NAMEINSOURCE '"sotailtablesizedisk"', NATIVE_TYPE 'int8'),
	sotailtablesizeuncompressed double OPTIONS (NAMEINSOURCE '"sotailtablesizeuncompressed"', NATIVE_TYPE 'float8'),
	sotailindexessize bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"sotailindexessize"', NATIVE_TYPE 'numeric'),
	sotailschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sotailschemaname"', NATIVE_TYPE 'name'),
	sotailtablename string(2147483647) OPTIONS (NAMEINSOURCE '"sotailtablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_table_and_indexes_licensing"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_table_disk" (
	sotdoid long OPTIONS (NAMEINSOURCE '"sotdoid"', NATIVE_TYPE 'oid'),
	sotdsize long OPTIONS (NAMEINSOURCE '"sotdsize"', NATIVE_TYPE 'int8'),
	sotdtoastsize long OPTIONS (NAMEINSOURCE '"sotdtoastsize"', NATIVE_TYPE 'int8'),
	sotdadditionalsize long OPTIONS (NAMEINSOURCE '"sotdadditionalsize"', NATIVE_TYPE 'int8'),
	sotdschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sotdschemaname"', NATIVE_TYPE 'name'),
	sotdtablename string(2147483647) OPTIONS (NAMEINSOURCE '"sotdtablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_table_disk"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_size_of_table_uncompressed" (
	sotuoid long OPTIONS (NAMEINSOURCE '"sotuoid"', NATIVE_TYPE 'oid'),
	sotusize double OPTIONS (NAMEINSOURCE '"sotusize"', NATIVE_TYPE 'float8'),
	sotuschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"sotuschemaname"', NATIVE_TYPE 'name'),
	sotutablename string(2147483647) OPTIONS (NAMEINSOURCE '"sotutablename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_size_of_table_uncompressed"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_skew_analysis_t" (
	skewoid long OPTIONS (NAMEINSOURCE '"skewoid"', NATIVE_TYPE 'oid'),
	skewval bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"skewval"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_skew_analysis_t"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_skew_coefficients" (
	skcoid long OPTIONS (NAMEINSOURCE '"skcoid"', NATIVE_TYPE 'oid'),
	skcnamespace string(2147483647) OPTIONS (NAMEINSOURCE '"skcnamespace"', NATIVE_TYPE 'name'),
	skcrelname string(2147483647) OPTIONS (NAMEINSOURCE '"skcrelname"', NATIVE_TYPE 'name'),
	skccoeff bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"skccoeff"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_skew_coefficients"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_skew_details_t" (
	segoid long OPTIONS (NAMEINSOURCE '"segoid"', NATIVE_TYPE 'oid'),
	segid integer OPTIONS (NAMEINSOURCE '"segid"', NATIVE_TYPE 'int4'),
	segtupcount long OPTIONS (NAMEINSOURCE '"segtupcount"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_skew_details_t"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_skew_idle_fractions" (
	sifoid long OPTIONS (NAMEINSOURCE '"sifoid"', NATIVE_TYPE 'oid'),
	sifnamespace string(2147483647) OPTIONS (NAMEINSOURCE '"sifnamespace"', NATIVE_TYPE 'name'),
	sifrelname string(2147483647) OPTIONS (NAMEINSOURCE '"sifrelname"', NATIVE_TYPE 'name'),
	siffraction bigdecimal(131089,2147483647) OPTIONS (NAMEINSOURCE '"siffraction"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_skew_idle_fractions"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_stats_missing" (
	smischema string(2147483647) OPTIONS (NAMEINSOURCE '"smischema"', NATIVE_TYPE 'name'),
	smitable string(2147483647) OPTIONS (NAMEINSOURCE '"smitable"', NATIVE_TYPE 'name'),
	smisize boolean OPTIONS (NAMEINSOURCE '"smisize"', NATIVE_TYPE 'bool'),
	smicols long OPTIONS (NAMEINSOURCE '"smicols"', NATIVE_TYPE 'int8'),
	smirecs long OPTIONS (NAMEINSOURCE '"smirecs"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_stats_missing"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "gp_toolkit.gp_table_indexes" (
	tireloid long OPTIONS (NAMEINSOURCE '"tireloid"', NATIVE_TYPE 'oid'),
	tiidxoid long OPTIONS (NAMEINSOURCE '"tiidxoid"', NATIVE_TYPE 'oid'),
	titableschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"titableschemaname"', NATIVE_TYPE 'name'),
	titablename string(2147483647) OPTIONS (NAMEINSOURCE '"titablename"', NATIVE_TYPE 'name'),
	tiindexschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"tiindexschemaname"', NATIVE_TYPE 'name'),
	tiindexname string(2147483647) OPTIONS (NAMEINSOURCE '"tiindexname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"gp_toolkit"."gp_table_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema._pg_foreign_data_wrappers" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid'),
	fdwowner long OPTIONS (NAMEINSOURCE '"fdwowner"', NATIVE_TYPE 'oid'),
	fdwoptions object OPTIONS (NAMEINSOURCE '"fdwoptions"', NATIVE_TYPE '_text'),
	foreign_data_wrapper_catalog object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_name object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_name"', NATIVE_TYPE 'sql_identifier'),
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_language object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_language"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."_pg_foreign_data_wrappers"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema._pg_foreign_servers" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid'),
	srvoptions object OPTIONS (NAMEINSOURCE '"srvoptions"', NATIVE_TYPE '_text'),
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_catalog object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_name object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_name"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_type object OPTIONS (NAMEINSOURCE '"foreign_server_type"', NATIVE_TYPE 'character_data'),
	foreign_server_version object OPTIONS (NAMEINSOURCE '"foreign_server_version"', NATIVE_TYPE 'character_data'),
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."_pg_foreign_servers"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema._pg_user_mappings" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid'),
	umoptions object OPTIONS (NAMEINSOURCE '"umoptions"', NATIVE_TYPE '_text'),
	umuser long OPTIONS (NAMEINSOURCE '"umuser"', NATIVE_TYPE 'oid'),
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier'),
	srvowner object OPTIONS (NAMEINSOURCE '"srvowner"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."_pg_user_mappings"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.administrable_role_authorizations" (
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	role_name object OPTIONS (NAMEINSOURCE '"role_name"', NATIVE_TYPE 'sql_identifier'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."administrable_role_authorizations"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.applicable_roles" (
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	role_name object OPTIONS (NAMEINSOURCE '"role_name"', NATIVE_TYPE 'sql_identifier'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."applicable_roles"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.attributes" (
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	attribute_name object OPTIONS (NAMEINSOURCE '"attribute_name"', NATIVE_TYPE 'sql_identifier'),
	ordinal_position object OPTIONS (NAMEINSOURCE '"ordinal_position"', NATIVE_TYPE 'cardinal_number'),
	attribute_default object OPTIONS (NAMEINSOURCE '"attribute_default"', NATIVE_TYPE 'character_data'),
	is_nullable object OPTIONS (NAMEINSOURCE '"is_nullable"', NATIVE_TYPE 'character_data'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	attribute_udt_catalog object OPTIONS (NAMEINSOURCE '"attribute_udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	attribute_udt_schema object OPTIONS (NAMEINSOURCE '"attribute_udt_schema"', NATIVE_TYPE 'sql_identifier'),
	attribute_udt_name object OPTIONS (NAMEINSOURCE '"attribute_udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier'),
	is_derived_reference_attribute object OPTIONS (NAMEINSOURCE '"is_derived_reference_attribute"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."attributes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.check_constraint_routine_usage" (
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier'),
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."check_constraint_routine_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.check_constraints" (
	constraint_catalog string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'varchar'),
	constraint_schema string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'varchar'),
	constraint_name string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'varchar'),
	check_clause string(2147483647) OPTIONS (NAMEINSOURCE '"check_clause"', NATIVE_TYPE 'varchar')
) OPTIONS (NAMEINSOURCE '"information_schema"."check_constraints"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.column_domain_usage" (
	domain_catalog object OPTIONS (NAMEINSOURCE '"domain_catalog"', NATIVE_TYPE 'sql_identifier'),
	domain_schema object OPTIONS (NAMEINSOURCE '"domain_schema"', NATIVE_TYPE 'sql_identifier'),
	domain_name object OPTIONS (NAMEINSOURCE '"domain_name"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."column_domain_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.column_privileges" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."column_privileges"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.column_udt_usage" (
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."column_udt_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.columns" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier'),
	ordinal_position object OPTIONS (NAMEINSOURCE '"ordinal_position"', NATIVE_TYPE 'cardinal_number'),
	column_default object OPTIONS (NAMEINSOURCE '"column_default"', NATIVE_TYPE 'character_data'),
	is_nullable object OPTIONS (NAMEINSOURCE '"is_nullable"', NATIVE_TYPE 'character_data'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	domain_catalog object OPTIONS (NAMEINSOURCE '"domain_catalog"', NATIVE_TYPE 'sql_identifier'),
	domain_schema object OPTIONS (NAMEINSOURCE '"domain_schema"', NATIVE_TYPE 'sql_identifier'),
	domain_name object OPTIONS (NAMEINSOURCE '"domain_name"', NATIVE_TYPE 'sql_identifier'),
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier'),
	is_self_referencing object OPTIONS (NAMEINSOURCE '"is_self_referencing"', NATIVE_TYPE 'character_data'),
	is_identity object OPTIONS (NAMEINSOURCE '"is_identity"', NATIVE_TYPE 'character_data'),
	identity_generation object OPTIONS (NAMEINSOURCE '"identity_generation"', NATIVE_TYPE 'character_data'),
	identity_start object OPTIONS (NAMEINSOURCE '"identity_start"', NATIVE_TYPE 'character_data'),
	identity_increment object OPTIONS (NAMEINSOURCE '"identity_increment"', NATIVE_TYPE 'character_data'),
	identity_maximum object OPTIONS (NAMEINSOURCE '"identity_maximum"', NATIVE_TYPE 'character_data'),
	identity_minimum object OPTIONS (NAMEINSOURCE '"identity_minimum"', NATIVE_TYPE 'character_data'),
	identity_cycle object OPTIONS (NAMEINSOURCE '"identity_cycle"', NATIVE_TYPE 'character_data'),
	is_generated object OPTIONS (NAMEINSOURCE '"is_generated"', NATIVE_TYPE 'character_data'),
	generation_expression object OPTIONS (NAMEINSOURCE '"generation_expression"', NATIVE_TYPE 'character_data'),
	is_updatable object OPTIONS (NAMEINSOURCE '"is_updatable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."columns"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.constraint_column_usage" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier'),
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."constraint_column_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.constraint_table_usage" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."constraint_table_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.data_type_privileges" (
	object_catalog object OPTIONS (NAMEINSOURCE '"object_catalog"', NATIVE_TYPE 'sql_identifier'),
	object_schema object OPTIONS (NAMEINSOURCE '"object_schema"', NATIVE_TYPE 'sql_identifier'),
	object_name object OPTIONS (NAMEINSOURCE '"object_name"', NATIVE_TYPE 'sql_identifier'),
	object_type object OPTIONS (NAMEINSOURCE '"object_type"', NATIVE_TYPE 'character_data'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."data_type_privileges"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.domain_constraints" (
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier'),
	domain_catalog object OPTIONS (NAMEINSOURCE '"domain_catalog"', NATIVE_TYPE 'sql_identifier'),
	domain_schema object OPTIONS (NAMEINSOURCE '"domain_schema"', NATIVE_TYPE 'sql_identifier'),
	domain_name object OPTIONS (NAMEINSOURCE '"domain_name"', NATIVE_TYPE 'sql_identifier'),
	is_deferrable object OPTIONS (NAMEINSOURCE '"is_deferrable"', NATIVE_TYPE 'character_data'),
	initially_deferred object OPTIONS (NAMEINSOURCE '"initially_deferred"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."domain_constraints"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.domain_udt_usage" (
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	domain_catalog object OPTIONS (NAMEINSOURCE '"domain_catalog"', NATIVE_TYPE 'sql_identifier'),
	domain_schema object OPTIONS (NAMEINSOURCE '"domain_schema"', NATIVE_TYPE 'sql_identifier'),
	domain_name object OPTIONS (NAMEINSOURCE '"domain_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."domain_udt_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.domains" (
	domain_catalog object OPTIONS (NAMEINSOURCE '"domain_catalog"', NATIVE_TYPE 'sql_identifier'),
	domain_schema object OPTIONS (NAMEINSOURCE '"domain_schema"', NATIVE_TYPE 'sql_identifier'),
	domain_name object OPTIONS (NAMEINSOURCE '"domain_name"', NATIVE_TYPE 'sql_identifier'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	domain_default object OPTIONS (NAMEINSOURCE '"domain_default"', NATIVE_TYPE 'character_data'),
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."domains"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.element_types" (
	object_catalog object OPTIONS (NAMEINSOURCE '"object_catalog"', NATIVE_TYPE 'sql_identifier'),
	object_schema object OPTIONS (NAMEINSOURCE '"object_schema"', NATIVE_TYPE 'sql_identifier'),
	object_name object OPTIONS (NAMEINSOURCE '"object_name"', NATIVE_TYPE 'sql_identifier'),
	object_type object OPTIONS (NAMEINSOURCE '"object_type"', NATIVE_TYPE 'character_data'),
	collection_type_identifier object OPTIONS (NAMEINSOURCE '"collection_type_identifier"', NATIVE_TYPE 'sql_identifier'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	domain_default object OPTIONS (NAMEINSOURCE '"domain_default"', NATIVE_TYPE 'character_data'),
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."element_types"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.enabled_roles" (
	role_name object OPTIONS (NAMEINSOURCE '"role_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."enabled_roles"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.foreign_data_wrapper_options" (
	foreign_data_wrapper_catalog object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_name object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_name"', NATIVE_TYPE 'sql_identifier'),
	option_name object OPTIONS (NAMEINSOURCE '"option_name"', NATIVE_TYPE 'sql_identifier'),
	option_value object OPTIONS (NAMEINSOURCE '"option_value"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."foreign_data_wrapper_options"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.foreign_data_wrappers" (
	foreign_data_wrapper_catalog object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_name object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_name"', NATIVE_TYPE 'sql_identifier'),
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier'),
	library_name object OPTIONS (NAMEINSOURCE '"library_name"', NATIVE_TYPE 'character_data'),
	foreign_data_wrapper_language object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_language"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."foreign_data_wrappers"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.foreign_server_options" (
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier'),
	option_name object OPTIONS (NAMEINSOURCE '"option_name"', NATIVE_TYPE 'sql_identifier'),
	option_value object OPTIONS (NAMEINSOURCE '"option_value"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."foreign_server_options"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.foreign_servers" (
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_catalog object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_data_wrapper_name object OPTIONS (NAMEINSOURCE '"foreign_data_wrapper_name"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_type object OPTIONS (NAMEINSOURCE '"foreign_server_type"', NATIVE_TYPE 'character_data'),
	foreign_server_version object OPTIONS (NAMEINSOURCE '"foreign_server_version"', NATIVE_TYPE 'character_data'),
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."foreign_servers"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.information_schema_catalog_name" (
	catalog_name object OPTIONS (NAMEINSOURCE '"catalog_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."information_schema_catalog_name"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.key_column_usage" (
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier'),
	ordinal_position object OPTIONS (NAMEINSOURCE '"ordinal_position"', NATIVE_TYPE 'cardinal_number'),
	position_in_unique_constraint object OPTIONS (NAMEINSOURCE '"position_in_unique_constraint"', NATIVE_TYPE 'cardinal_number')
) OPTIONS (NAMEINSOURCE '"information_schema"."key_column_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.parameters" (
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier'),
	ordinal_position object OPTIONS (NAMEINSOURCE '"ordinal_position"', NATIVE_TYPE 'cardinal_number'),
	parameter_mode object OPTIONS (NAMEINSOURCE '"parameter_mode"', NATIVE_TYPE 'character_data'),
	is_result object OPTIONS (NAMEINSOURCE '"is_result"', NATIVE_TYPE 'character_data'),
	as_locator object OPTIONS (NAMEINSOURCE '"as_locator"', NATIVE_TYPE 'character_data'),
	parameter_name object OPTIONS (NAMEINSOURCE '"parameter_name"', NATIVE_TYPE 'sql_identifier'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."parameters"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.referential_constraints" (
	constraint_catalog object OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	constraint_schema object OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	constraint_name object OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'sql_identifier'),
	unique_constraint_catalog object OPTIONS (NAMEINSOURCE '"unique_constraint_catalog"', NATIVE_TYPE 'sql_identifier'),
	unique_constraint_schema object OPTIONS (NAMEINSOURCE '"unique_constraint_schema"', NATIVE_TYPE 'sql_identifier'),
	unique_constraint_name object OPTIONS (NAMEINSOURCE '"unique_constraint_name"', NATIVE_TYPE 'sql_identifier'),
	match_option object OPTIONS (NAMEINSOURCE '"match_option"', NATIVE_TYPE 'character_data'),
	update_rule object OPTIONS (NAMEINSOURCE '"update_rule"', NATIVE_TYPE 'character_data'),
	delete_rule object OPTIONS (NAMEINSOURCE '"delete_rule"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."referential_constraints"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.role_column_grants" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."role_column_grants"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.role_routine_grants" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier'),
	routine_catalog object OPTIONS (NAMEINSOURCE '"routine_catalog"', NATIVE_TYPE 'sql_identifier'),
	routine_schema object OPTIONS (NAMEINSOURCE '"routine_schema"', NATIVE_TYPE 'sql_identifier'),
	routine_name object OPTIONS (NAMEINSOURCE '"routine_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."role_routine_grants"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.role_table_grants" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data'),
	with_hierarchy object OPTIONS (NAMEINSOURCE '"with_hierarchy"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."role_table_grants"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.role_usage_grants" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	object_catalog object OPTIONS (NAMEINSOURCE '"object_catalog"', NATIVE_TYPE 'sql_identifier'),
	object_schema object OPTIONS (NAMEINSOURCE '"object_schema"', NATIVE_TYPE 'sql_identifier'),
	object_name object OPTIONS (NAMEINSOURCE '"object_name"', NATIVE_TYPE 'sql_identifier'),
	object_type object OPTIONS (NAMEINSOURCE '"object_type"', NATIVE_TYPE 'character_data'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."role_usage_grants"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.routine_privileges" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier'),
	routine_catalog object OPTIONS (NAMEINSOURCE '"routine_catalog"', NATIVE_TYPE 'sql_identifier'),
	routine_schema object OPTIONS (NAMEINSOURCE '"routine_schema"', NATIVE_TYPE 'sql_identifier'),
	routine_name object OPTIONS (NAMEINSOURCE '"routine_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."routine_privileges"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.routines" (
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier'),
	routine_catalog object OPTIONS (NAMEINSOURCE '"routine_catalog"', NATIVE_TYPE 'sql_identifier'),
	routine_schema object OPTIONS (NAMEINSOURCE '"routine_schema"', NATIVE_TYPE 'sql_identifier'),
	routine_name object OPTIONS (NAMEINSOURCE '"routine_name"', NATIVE_TYPE 'sql_identifier'),
	routine_type object OPTIONS (NAMEINSOURCE '"routine_type"', NATIVE_TYPE 'character_data'),
	module_catalog object OPTIONS (NAMEINSOURCE '"module_catalog"', NATIVE_TYPE 'sql_identifier'),
	module_schema object OPTIONS (NAMEINSOURCE '"module_schema"', NATIVE_TYPE 'sql_identifier'),
	module_name object OPTIONS (NAMEINSOURCE '"module_name"', NATIVE_TYPE 'sql_identifier'),
	udt_catalog object OPTIONS (NAMEINSOURCE '"udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	udt_schema object OPTIONS (NAMEINSOURCE '"udt_schema"', NATIVE_TYPE 'sql_identifier'),
	udt_name object OPTIONS (NAMEINSOURCE '"udt_name"', NATIVE_TYPE 'sql_identifier'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	character_maximum_length object OPTIONS (NAMEINSOURCE '"character_maximum_length"', NATIVE_TYPE 'cardinal_number'),
	character_octet_length object OPTIONS (NAMEINSOURCE '"character_octet_length"', NATIVE_TYPE 'cardinal_number'),
	character_set_catalog object OPTIONS (NAMEINSOURCE '"character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	character_set_schema object OPTIONS (NAMEINSOURCE '"character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	character_set_name object OPTIONS (NAMEINSOURCE '"character_set_name"', NATIVE_TYPE 'sql_identifier'),
	collation_catalog object OPTIONS (NAMEINSOURCE '"collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	collation_schema object OPTIONS (NAMEINSOURCE '"collation_schema"', NATIVE_TYPE 'sql_identifier'),
	collation_name object OPTIONS (NAMEINSOURCE '"collation_name"', NATIVE_TYPE 'sql_identifier'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	datetime_precision object OPTIONS (NAMEINSOURCE '"datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	interval_type object OPTIONS (NAMEINSOURCE '"interval_type"', NATIVE_TYPE 'character_data'),
	interval_precision object OPTIONS (NAMEINSOURCE '"interval_precision"', NATIVE_TYPE 'character_data'),
	type_udt_catalog object OPTIONS (NAMEINSOURCE '"type_udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	type_udt_schema object OPTIONS (NAMEINSOURCE '"type_udt_schema"', NATIVE_TYPE 'sql_identifier'),
	type_udt_name object OPTIONS (NAMEINSOURCE '"type_udt_name"', NATIVE_TYPE 'sql_identifier'),
	scope_catalog object OPTIONS (NAMEINSOURCE '"scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	scope_schema object OPTIONS (NAMEINSOURCE '"scope_schema"', NATIVE_TYPE 'sql_identifier'),
	scope_name object OPTIONS (NAMEINSOURCE '"scope_name"', NATIVE_TYPE 'sql_identifier'),
	maximum_cardinality object OPTIONS (NAMEINSOURCE '"maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	dtd_identifier object OPTIONS (NAMEINSOURCE '"dtd_identifier"', NATIVE_TYPE 'sql_identifier'),
	routine_body object OPTIONS (NAMEINSOURCE '"routine_body"', NATIVE_TYPE 'character_data'),
	routine_definition object OPTIONS (NAMEINSOURCE '"routine_definition"', NATIVE_TYPE 'character_data'),
	external_name object OPTIONS (NAMEINSOURCE '"external_name"', NATIVE_TYPE 'character_data'),
	external_language object OPTIONS (NAMEINSOURCE '"external_language"', NATIVE_TYPE 'character_data'),
	parameter_style object OPTIONS (NAMEINSOURCE '"parameter_style"', NATIVE_TYPE 'character_data'),
	is_deterministic object OPTIONS (NAMEINSOURCE '"is_deterministic"', NATIVE_TYPE 'character_data'),
	sql_data_access object OPTIONS (NAMEINSOURCE '"sql_data_access"', NATIVE_TYPE 'character_data'),
	is_null_call object OPTIONS (NAMEINSOURCE '"is_null_call"', NATIVE_TYPE 'character_data'),
	sql_path object OPTIONS (NAMEINSOURCE '"sql_path"', NATIVE_TYPE 'character_data'),
	schema_level_routine object OPTIONS (NAMEINSOURCE '"schema_level_routine"', NATIVE_TYPE 'character_data'),
	max_dynamic_result_sets object OPTIONS (NAMEINSOURCE '"max_dynamic_result_sets"', NATIVE_TYPE 'cardinal_number'),
	is_user_defined_cast object OPTIONS (NAMEINSOURCE '"is_user_defined_cast"', NATIVE_TYPE 'character_data'),
	is_implicitly_invocable object OPTIONS (NAMEINSOURCE '"is_implicitly_invocable"', NATIVE_TYPE 'character_data'),
	security_type object OPTIONS (NAMEINSOURCE '"security_type"', NATIVE_TYPE 'character_data'),
	to_sql_specific_catalog object OPTIONS (NAMEINSOURCE '"to_sql_specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	to_sql_specific_schema object OPTIONS (NAMEINSOURCE '"to_sql_specific_schema"', NATIVE_TYPE 'sql_identifier'),
	to_sql_specific_name object OPTIONS (NAMEINSOURCE '"to_sql_specific_name"', NATIVE_TYPE 'sql_identifier'),
	as_locator object OPTIONS (NAMEINSOURCE '"as_locator"', NATIVE_TYPE 'character_data'),
	created object OPTIONS (NAMEINSOURCE '"created"', NATIVE_TYPE 'time_stamp'),
	last_altered object OPTIONS (NAMEINSOURCE '"last_altered"', NATIVE_TYPE 'time_stamp'),
	new_savepoint_level object OPTIONS (NAMEINSOURCE '"new_savepoint_level"', NATIVE_TYPE 'character_data'),
	is_udt_dependent object OPTIONS (NAMEINSOURCE '"is_udt_dependent"', NATIVE_TYPE 'character_data'),
	result_cast_from_data_type object OPTIONS (NAMEINSOURCE '"result_cast_from_data_type"', NATIVE_TYPE 'character_data'),
	result_cast_as_locator object OPTIONS (NAMEINSOURCE '"result_cast_as_locator"', NATIVE_TYPE 'character_data'),
	result_cast_char_max_length object OPTIONS (NAMEINSOURCE '"result_cast_char_max_length"', NATIVE_TYPE 'cardinal_number'),
	result_cast_char_octet_length object OPTIONS (NAMEINSOURCE '"result_cast_char_octet_length"', NATIVE_TYPE 'cardinal_number'),
	result_cast_char_set_catalog object OPTIONS (NAMEINSOURCE '"result_cast_char_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	result_cast_char_set_schema object OPTIONS (NAMEINSOURCE '"result_cast_char_set_schema"', NATIVE_TYPE 'sql_identifier'),
	result_cast_character_set_name object OPTIONS (NAMEINSOURCE '"result_cast_character_set_name"', NATIVE_TYPE 'sql_identifier'),
	result_cast_collation_catalog object OPTIONS (NAMEINSOURCE '"result_cast_collation_catalog"', NATIVE_TYPE 'sql_identifier'),
	result_cast_collation_schema object OPTIONS (NAMEINSOURCE '"result_cast_collation_schema"', NATIVE_TYPE 'sql_identifier'),
	result_cast_collation_name object OPTIONS (NAMEINSOURCE '"result_cast_collation_name"', NATIVE_TYPE 'sql_identifier'),
	result_cast_numeric_precision object OPTIONS (NAMEINSOURCE '"result_cast_numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	result_cast_numeric_precision_radix object OPTIONS (NAMEINSOURCE '"result_cast_numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	result_cast_numeric_scale object OPTIONS (NAMEINSOURCE '"result_cast_numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	result_cast_datetime_precision object OPTIONS (NAMEINSOURCE '"result_cast_datetime_precision"', NATIVE_TYPE 'cardinal_number'),
	result_cast_interval_type object OPTIONS (NAMEINSOURCE '"result_cast_interval_type"', NATIVE_TYPE 'character_data'),
	result_cast_interval_precision object OPTIONS (NAMEINSOURCE '"result_cast_interval_precision"', NATIVE_TYPE 'character_data'),
	result_cast_type_udt_catalog object OPTIONS (NAMEINSOURCE '"result_cast_type_udt_catalog"', NATIVE_TYPE 'sql_identifier'),
	result_cast_type_udt_schema object OPTIONS (NAMEINSOURCE '"result_cast_type_udt_schema"', NATIVE_TYPE 'sql_identifier'),
	result_cast_type_udt_name object OPTIONS (NAMEINSOURCE '"result_cast_type_udt_name"', NATIVE_TYPE 'sql_identifier'),
	result_cast_scope_catalog object OPTIONS (NAMEINSOURCE '"result_cast_scope_catalog"', NATIVE_TYPE 'sql_identifier'),
	result_cast_scope_schema object OPTIONS (NAMEINSOURCE '"result_cast_scope_schema"', NATIVE_TYPE 'sql_identifier'),
	result_cast_scope_name object OPTIONS (NAMEINSOURCE '"result_cast_scope_name"', NATIVE_TYPE 'sql_identifier'),
	result_cast_maximum_cardinality object OPTIONS (NAMEINSOURCE '"result_cast_maximum_cardinality"', NATIVE_TYPE 'cardinal_number'),
	result_cast_dtd_identifier object OPTIONS (NAMEINSOURCE '"result_cast_dtd_identifier"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."routines"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.schemata" (
	catalog_name object OPTIONS (NAMEINSOURCE '"catalog_name"', NATIVE_TYPE 'sql_identifier'),
	schema_name object OPTIONS (NAMEINSOURCE '"schema_name"', NATIVE_TYPE 'sql_identifier'),
	schema_owner object OPTIONS (NAMEINSOURCE '"schema_owner"', NATIVE_TYPE 'sql_identifier'),
	default_character_set_catalog object OPTIONS (NAMEINSOURCE '"default_character_set_catalog"', NATIVE_TYPE 'sql_identifier'),
	default_character_set_schema object OPTIONS (NAMEINSOURCE '"default_character_set_schema"', NATIVE_TYPE 'sql_identifier'),
	default_character_set_name object OPTIONS (NAMEINSOURCE '"default_character_set_name"', NATIVE_TYPE 'sql_identifier'),
	sql_path object OPTIONS (NAMEINSOURCE '"sql_path"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."schemata"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sequences" (
	sequence_catalog object OPTIONS (NAMEINSOURCE '"sequence_catalog"', NATIVE_TYPE 'sql_identifier'),
	sequence_schema object OPTIONS (NAMEINSOURCE '"sequence_schema"', NATIVE_TYPE 'sql_identifier'),
	sequence_name object OPTIONS (NAMEINSOURCE '"sequence_name"', NATIVE_TYPE 'sql_identifier'),
	data_type object OPTIONS (NAMEINSOURCE '"data_type"', NATIVE_TYPE 'character_data'),
	numeric_precision object OPTIONS (NAMEINSOURCE '"numeric_precision"', NATIVE_TYPE 'cardinal_number'),
	numeric_precision_radix object OPTIONS (NAMEINSOURCE '"numeric_precision_radix"', NATIVE_TYPE 'cardinal_number'),
	numeric_scale object OPTIONS (NAMEINSOURCE '"numeric_scale"', NATIVE_TYPE 'cardinal_number'),
	maximum_value object OPTIONS (NAMEINSOURCE '"maximum_value"', NATIVE_TYPE 'cardinal_number'),
	minimum_value object OPTIONS (NAMEINSOURCE '"minimum_value"', NATIVE_TYPE 'cardinal_number'),
	increment object OPTIONS (NAMEINSOURCE '"increment"', NATIVE_TYPE 'cardinal_number'),
	cycle_option object OPTIONS (NAMEINSOURCE '"cycle_option"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sequences"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_features" (
	feature_id object OPTIONS (NAMEINSOURCE '"feature_id"', NATIVE_TYPE 'character_data'),
	feature_name object OPTIONS (NAMEINSOURCE '"feature_name"', NATIVE_TYPE 'character_data'),
	sub_feature_id object OPTIONS (NAMEINSOURCE '"sub_feature_id"', NATIVE_TYPE 'character_data'),
	sub_feature_name object OPTIONS (NAMEINSOURCE '"sub_feature_name"', NATIVE_TYPE 'character_data'),
	is_supported object OPTIONS (NAMEINSOURCE '"is_supported"', NATIVE_TYPE 'character_data'),
	is_verified_by object OPTIONS (NAMEINSOURCE '"is_verified_by"', NATIVE_TYPE 'character_data'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_features"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_implementation_info" (
	implementation_info_id object OPTIONS (NAMEINSOURCE '"implementation_info_id"', NATIVE_TYPE 'character_data'),
	implementation_info_name object OPTIONS (NAMEINSOURCE '"implementation_info_name"', NATIVE_TYPE 'character_data'),
	integer_value object OPTIONS (NAMEINSOURCE '"integer_value"', NATIVE_TYPE 'cardinal_number'),
	character_value object OPTIONS (NAMEINSOURCE '"character_value"', NATIVE_TYPE 'character_data'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_implementation_info"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_languages" (
	sql_language_source object OPTIONS (NAMEINSOURCE '"sql_language_source"', NATIVE_TYPE 'character_data'),
	sql_language_year object OPTIONS (NAMEINSOURCE '"sql_language_year"', NATIVE_TYPE 'character_data'),
	sql_language_conformance object OPTIONS (NAMEINSOURCE '"sql_language_conformance"', NATIVE_TYPE 'character_data'),
	sql_language_integrity object OPTIONS (NAMEINSOURCE '"sql_language_integrity"', NATIVE_TYPE 'character_data'),
	sql_language_implementation object OPTIONS (NAMEINSOURCE '"sql_language_implementation"', NATIVE_TYPE 'character_data'),
	sql_language_binding_style object OPTIONS (NAMEINSOURCE '"sql_language_binding_style"', NATIVE_TYPE 'character_data'),
	sql_language_programming_language object OPTIONS (NAMEINSOURCE '"sql_language_programming_language"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_languages"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_packages" (
	feature_id object OPTIONS (NAMEINSOURCE '"feature_id"', NATIVE_TYPE 'character_data'),
	feature_name object OPTIONS (NAMEINSOURCE '"feature_name"', NATIVE_TYPE 'character_data'),
	is_supported object OPTIONS (NAMEINSOURCE '"is_supported"', NATIVE_TYPE 'character_data'),
	is_verified_by object OPTIONS (NAMEINSOURCE '"is_verified_by"', NATIVE_TYPE 'character_data'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_packages"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_parts" (
	feature_id object OPTIONS (NAMEINSOURCE '"feature_id"', NATIVE_TYPE 'character_data'),
	feature_name object OPTIONS (NAMEINSOURCE '"feature_name"', NATIVE_TYPE 'character_data'),
	is_supported object OPTIONS (NAMEINSOURCE '"is_supported"', NATIVE_TYPE 'character_data'),
	is_verified_by object OPTIONS (NAMEINSOURCE '"is_verified_by"', NATIVE_TYPE 'character_data'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_parts"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_sizing" (
	sizing_id object OPTIONS (NAMEINSOURCE '"sizing_id"', NATIVE_TYPE 'cardinal_number'),
	sizing_name object OPTIONS (NAMEINSOURCE '"sizing_name"', NATIVE_TYPE 'character_data'),
	supported_value object OPTIONS (NAMEINSOURCE '"supported_value"', NATIVE_TYPE 'cardinal_number'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_sizing"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.sql_sizing_profiles" (
	sizing_id object OPTIONS (NAMEINSOURCE '"sizing_id"', NATIVE_TYPE 'cardinal_number'),
	sizing_name object OPTIONS (NAMEINSOURCE '"sizing_name"', NATIVE_TYPE 'character_data'),
	profile_id object OPTIONS (NAMEINSOURCE '"profile_id"', NATIVE_TYPE 'character_data'),
	required_value object OPTIONS (NAMEINSOURCE '"required_value"', NATIVE_TYPE 'cardinal_number'),
	comments object OPTIONS (NAMEINSOURCE '"comments"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."sql_sizing_profiles"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.table_constraints" (
	constraint_catalog string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_catalog"', NATIVE_TYPE 'varchar'),
	constraint_schema string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_schema"', NATIVE_TYPE 'varchar'),
	constraint_name string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_name"', NATIVE_TYPE 'varchar'),
	table_catalog string(2147483647) OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'varchar'),
	table_schema string(2147483647) OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'varchar'),
	table_name string(2147483647) OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'varchar'),
	constraint_type string(2147483647) OPTIONS (NAMEINSOURCE '"constraint_type"', NATIVE_TYPE 'varchar'),
	is_deferrable string(2147483647) OPTIONS (NAMEINSOURCE '"is_deferrable"', NATIVE_TYPE 'varchar'),
	initially_deferred string(2147483647) OPTIONS (NAMEINSOURCE '"initially_deferred"', NATIVE_TYPE 'varchar')
) OPTIONS (NAMEINSOURCE '"information_schema"."table_constraints"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.table_privileges" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data'),
	with_hierarchy object OPTIONS (NAMEINSOURCE '"with_hierarchy"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."table_privileges"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.tables" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	table_type object OPTIONS (NAMEINSOURCE '"table_type"', NATIVE_TYPE 'character_data'),
	self_referencing_column_name object OPTIONS (NAMEINSOURCE '"self_referencing_column_name"', NATIVE_TYPE 'sql_identifier'),
	reference_generation object OPTIONS (NAMEINSOURCE '"reference_generation"', NATIVE_TYPE 'character_data'),
	user_defined_type_catalog object OPTIONS (NAMEINSOURCE '"user_defined_type_catalog"', NATIVE_TYPE 'sql_identifier'),
	user_defined_type_schema object OPTIONS (NAMEINSOURCE '"user_defined_type_schema"', NATIVE_TYPE 'sql_identifier'),
	user_defined_type_name object OPTIONS (NAMEINSOURCE '"user_defined_type_name"', NATIVE_TYPE 'sql_identifier'),
	is_insertable_into object OPTIONS (NAMEINSOURCE '"is_insertable_into"', NATIVE_TYPE 'character_data'),
	is_typed object OPTIONS (NAMEINSOURCE '"is_typed"', NATIVE_TYPE 'character_data'),
	commit_action object OPTIONS (NAMEINSOURCE '"commit_action"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.triggered_update_columns" (
	trigger_catalog object OPTIONS (NAMEINSOURCE '"trigger_catalog"', NATIVE_TYPE 'sql_identifier'),
	trigger_schema object OPTIONS (NAMEINSOURCE '"trigger_schema"', NATIVE_TYPE 'sql_identifier'),
	trigger_name object OPTIONS (NAMEINSOURCE '"trigger_name"', NATIVE_TYPE 'sql_identifier'),
	event_object_catalog object OPTIONS (NAMEINSOURCE '"event_object_catalog"', NATIVE_TYPE 'sql_identifier'),
	event_object_schema object OPTIONS (NAMEINSOURCE '"event_object_schema"', NATIVE_TYPE 'sql_identifier'),
	event_object_table object OPTIONS (NAMEINSOURCE '"event_object_table"', NATIVE_TYPE 'sql_identifier'),
	event_object_column object OPTIONS (NAMEINSOURCE '"event_object_column"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."triggered_update_columns"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.triggers" (
	trigger_catalog object OPTIONS (NAMEINSOURCE '"trigger_catalog"', NATIVE_TYPE 'sql_identifier'),
	trigger_schema object OPTIONS (NAMEINSOURCE '"trigger_schema"', NATIVE_TYPE 'sql_identifier'),
	trigger_name object OPTIONS (NAMEINSOURCE '"trigger_name"', NATIVE_TYPE 'sql_identifier'),
	event_manipulation object OPTIONS (NAMEINSOURCE '"event_manipulation"', NATIVE_TYPE 'character_data'),
	event_object_catalog object OPTIONS (NAMEINSOURCE '"event_object_catalog"', NATIVE_TYPE 'sql_identifier'),
	event_object_schema object OPTIONS (NAMEINSOURCE '"event_object_schema"', NATIVE_TYPE 'sql_identifier'),
	event_object_table object OPTIONS (NAMEINSOURCE '"event_object_table"', NATIVE_TYPE 'sql_identifier'),
	action_order object OPTIONS (NAMEINSOURCE '"action_order"', NATIVE_TYPE 'cardinal_number'),
	action_condition object OPTIONS (NAMEINSOURCE '"action_condition"', NATIVE_TYPE 'character_data'),
	action_statement object OPTIONS (NAMEINSOURCE '"action_statement"', NATIVE_TYPE 'character_data'),
	action_orientation object OPTIONS (NAMEINSOURCE '"action_orientation"', NATIVE_TYPE 'character_data'),
	condition_timing object OPTIONS (NAMEINSOURCE '"condition_timing"', NATIVE_TYPE 'character_data'),
	condition_reference_old_table object OPTIONS (NAMEINSOURCE '"condition_reference_old_table"', NATIVE_TYPE 'sql_identifier'),
	condition_reference_new_table object OPTIONS (NAMEINSOURCE '"condition_reference_new_table"', NATIVE_TYPE 'sql_identifier'),
	condition_reference_old_row object OPTIONS (NAMEINSOURCE '"condition_reference_old_row"', NATIVE_TYPE 'sql_identifier'),
	condition_reference_new_row object OPTIONS (NAMEINSOURCE '"condition_reference_new_row"', NATIVE_TYPE 'sql_identifier'),
	created object OPTIONS (NAMEINSOURCE '"created"', NATIVE_TYPE 'time_stamp')
) OPTIONS (NAMEINSOURCE '"information_schema"."triggers"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.usage_privileges" (
	grantor object OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'sql_identifier'),
	grantee object OPTIONS (NAMEINSOURCE '"grantee"', NATIVE_TYPE 'sql_identifier'),
	object_catalog object OPTIONS (NAMEINSOURCE '"object_catalog"', NATIVE_TYPE 'sql_identifier'),
	object_schema object OPTIONS (NAMEINSOURCE '"object_schema"', NATIVE_TYPE 'sql_identifier'),
	object_name object OPTIONS (NAMEINSOURCE '"object_name"', NATIVE_TYPE 'sql_identifier'),
	object_type object OPTIONS (NAMEINSOURCE '"object_type"', NATIVE_TYPE 'character_data'),
	privilege_type object OPTIONS (NAMEINSOURCE '"privilege_type"', NATIVE_TYPE 'character_data'),
	is_grantable object OPTIONS (NAMEINSOURCE '"is_grantable"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."usage_privileges"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.user_mapping_options" (
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier'),
	option_name object OPTIONS (NAMEINSOURCE '"option_name"', NATIVE_TYPE 'sql_identifier'),
	option_value object OPTIONS (NAMEINSOURCE '"option_value"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."user_mapping_options"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.user_mappings" (
	authorization_identifier object OPTIONS (NAMEINSOURCE '"authorization_identifier"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_catalog object OPTIONS (NAMEINSOURCE '"foreign_server_catalog"', NATIVE_TYPE 'sql_identifier'),
	foreign_server_name object OPTIONS (NAMEINSOURCE '"foreign_server_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."user_mappings"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.view_column_usage" (
	view_catalog object OPTIONS (NAMEINSOURCE '"view_catalog"', NATIVE_TYPE 'sql_identifier'),
	view_schema object OPTIONS (NAMEINSOURCE '"view_schema"', NATIVE_TYPE 'sql_identifier'),
	view_name object OPTIONS (NAMEINSOURCE '"view_name"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	column_name object OPTIONS (NAMEINSOURCE '"column_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."view_column_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.view_routine_usage" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	specific_catalog object OPTIONS (NAMEINSOURCE '"specific_catalog"', NATIVE_TYPE 'sql_identifier'),
	specific_schema object OPTIONS (NAMEINSOURCE '"specific_schema"', NATIVE_TYPE 'sql_identifier'),
	specific_name object OPTIONS (NAMEINSOURCE '"specific_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."view_routine_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.view_table_usage" (
	view_catalog object OPTIONS (NAMEINSOURCE '"view_catalog"', NATIVE_TYPE 'sql_identifier'),
	view_schema object OPTIONS (NAMEINSOURCE '"view_schema"', NATIVE_TYPE 'sql_identifier'),
	view_name object OPTIONS (NAMEINSOURCE '"view_name"', NATIVE_TYPE 'sql_identifier'),
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier')
) OPTIONS (NAMEINSOURCE '"information_schema"."view_table_usage"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "information_schema.views" (
	table_catalog object OPTIONS (NAMEINSOURCE '"table_catalog"', NATIVE_TYPE 'sql_identifier'),
	table_schema object OPTIONS (NAMEINSOURCE '"table_schema"', NATIVE_TYPE 'sql_identifier'),
	table_name object OPTIONS (NAMEINSOURCE '"table_name"', NATIVE_TYPE 'sql_identifier'),
	view_definition object OPTIONS (NAMEINSOURCE '"view_definition"', NATIVE_TYPE 'character_data'),
	check_option object OPTIONS (NAMEINSOURCE '"check_option"', NATIVE_TYPE 'character_data'),
	is_updatable object OPTIONS (NAMEINSOURCE '"is_updatable"', NATIVE_TYPE 'character_data'),
	is_insertable_into object OPTIONS (NAMEINSOURCE '"is_insertable_into"', NATIVE_TYPE 'character_data')
) OPTIONS (NAMEINSOURCE '"information_schema"."views"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_configuration" (
	content short NOT NULL OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2'),
	definedprimary boolean NOT NULL OPTIONS (NAMEINSOURCE '"definedprimary"', NATIVE_TYPE 'bool'),
	dbid short NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	isprimary boolean NOT NULL OPTIONS (NAMEINSOURCE '"isprimary"', NATIVE_TYPE 'bool'),
	valid boolean NOT NULL OPTIONS (NAMEINSOURCE '"valid"', NATIVE_TYPE 'bool'),
	hostname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"hostname"', NATIVE_TYPE 'name'),
	port integer NOT NULL OPTIONS (NAMEINSOURCE '"port"', NATIVE_TYPE 'int4'),
	datadir string(2147483647) OPTIONS (NAMEINSOURCE '"datadir"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_configuration"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_configuration_content_definedprimary_index" (
	content short OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2'),
	definedprimary boolean OPTIONS (NAMEINSOURCE '"definedprimary"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_configuration_content_definedprimary_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_configuration_dbid_index" (
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_configuration_dbid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_configuration_history" (
	"time" timestamp NOT NULL OPTIONS (NAMEINSOURCE '"time"', NATIVE_TYPE 'timestamptz'),
	dbid short NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	"desc" string(2147483647) OPTIONS (NAMEINSOURCE '"desc"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_configuration_history"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_db_interfaces" (
	dbid short NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	interfaceid short NOT NULL OPTIONS (NAMEINSOURCE '"interfaceid"', NATIVE_TYPE 'int2'),
	priority short NOT NULL OPTIONS (NAMEINSOURCE '"priority"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_db_interfaces"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_db_interfaces_dbid_index" (
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_db_interfaces_dbid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_distributed_log" (
	segment_id short OPTIONS (NAMEINSOURCE '"segment_id"', NATIVE_TYPE 'int2'),
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	distributed_xid object OPTIONS (NAMEINSOURCE '"distributed_xid"', NATIVE_TYPE 'xid'),
	distributed_id string(2147483647) OPTIONS (NAMEINSOURCE '"distributed_id"', NATIVE_TYPE 'text'),
	status string(2147483647) OPTIONS (NAMEINSOURCE '"status"', NATIVE_TYPE 'text'),
	local_transaction object OPTIONS (NAMEINSOURCE '"local_transaction"', NATIVE_TYPE 'xid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_distributed_log"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_distributed_xacts" (
	distributed_xid object OPTIONS (NAMEINSOURCE '"distributed_xid"', NATIVE_TYPE 'xid'),
	distributed_id string(2147483647) OPTIONS (NAMEINSOURCE '"distributed_id"', NATIVE_TYPE 'text'),
	state string(2147483647) OPTIONS (NAMEINSOURCE '"state"', NATIVE_TYPE 'text'),
	gp_session_id integer OPTIONS (NAMEINSOURCE '"gp_session_id"', NATIVE_TYPE 'int4'),
	xmin_distributed_snapshot object OPTIONS (NAMEINSOURCE '"xmin_distributed_snapshot"', NATIVE_TYPE 'xid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_distributed_xacts"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_distribution_policy" (
	localoid long NOT NULL OPTIONS (NAMEINSOURCE '"localoid"', NATIVE_TYPE 'oid'),
	attrnums object OPTIONS (NAMEINSOURCE '"attrnums"', NATIVE_TYPE '_int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_distribution_policy"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_fastsequence" (
	objid long NOT NULL OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	objmod long NOT NULL OPTIONS (NAMEINSOURCE '"objmod"', NATIVE_TYPE 'int8'),
	last_sequence long NOT NULL OPTIONS (NAMEINSOURCE '"last_sequence"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_fastsequence"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_fastsequence_objid_objmod_index" (
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	objmod long OPTIONS (NAMEINSOURCE '"objmod"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_fastsequence_objid_objmod_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_fault_strategy" (
	fault_strategy string(1) NOT NULL OPTIONS (NAMEINSOURCE '"fault_strategy"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_fault_strategy"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_global_sequence" (
	sequence_num long NOT NULL OPTIONS (NAMEINSOURCE '"sequence_num"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_global_sequence"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_id" (
	gpname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"gpname"', NATIVE_TYPE 'name'),
	numsegments short NOT NULL OPTIONS (NAMEINSOURCE '"numsegments"', NATIVE_TYPE 'int2'),
	dbid short NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	content short NOT NULL OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_id"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_interfaces" (
	interfaceid short NOT NULL OPTIONS (NAMEINSOURCE '"interfaceid"', NATIVE_TYPE 'int2'),
	address string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"address"', NATIVE_TYPE 'name'),
	status short NOT NULL OPTIONS (NAMEINSOURCE '"status"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_interfaces"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_interfaces_interface_index" (
	interfaceid short OPTIONS (NAMEINSOURCE '"interfaceid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_interfaces_interface_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_master_mirroring" (
	summary_state string(2147483647) OPTIONS (NAMEINSOURCE '"summary_state"', NATIVE_TYPE 'text'),
	detail_state string(2147483647) OPTIONS (NAMEINSOURCE '"detail_state"', NATIVE_TYPE 'text'),
	log_time timestamp OPTIONS (NAMEINSOURCE '"log_time"', NATIVE_TYPE 'timestamptz'),
	error_message string(2147483647) OPTIONS (NAMEINSOURCE '"error_message"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_master_mirroring"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_persistent_database_node" (
	tablespace_oid long NOT NULL OPTIONS (NAMEINSOURCE '"tablespace_oid"', NATIVE_TYPE 'oid'),
	database_oid long NOT NULL OPTIONS (NAMEINSOURCE '"database_oid"', NATIVE_TYPE 'oid'),
	persistent_state short NOT NULL OPTIONS (NAMEINSOURCE '"persistent_state"', NATIVE_TYPE 'int2'),
	create_mirror_data_loss_tracking_session_num long NOT NULL OPTIONS (NAMEINSOURCE '"create_mirror_data_loss_tracking_session_num"', NATIVE_TYPE 'int8'),
	mirror_existence_state short NOT NULL OPTIONS (NAMEINSOURCE '"mirror_existence_state"', NATIVE_TYPE 'int2'),
	reserved integer NOT NULL OPTIONS (NAMEINSOURCE '"reserved"', NATIVE_TYPE 'int4'),
	parent_xid integer NOT NULL OPTIONS (NAMEINSOURCE '"parent_xid"', NATIVE_TYPE 'int4'),
	persistent_serial_num long NOT NULL OPTIONS (NAMEINSOURCE '"persistent_serial_num"', NATIVE_TYPE 'int8'),
	previous_free_tid object NOT NULL OPTIONS (NAMEINSOURCE '"previous_free_tid"', NATIVE_TYPE 'tid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_persistent_database_node"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_persistent_filespace_node" (
	filespace_oid long NOT NULL OPTIONS (NAMEINSOURCE '"filespace_oid"', NATIVE_TYPE 'oid'),
	db_id_1 short NOT NULL OPTIONS (NAMEINSOURCE '"db_id_1"', NATIVE_TYPE 'int2'),
	location_1 string(2147483647) OPTIONS (NAMEINSOURCE '"location_1"', NATIVE_TYPE 'text'),
	db_id_2 short OPTIONS (NAMEINSOURCE '"db_id_2"', NATIVE_TYPE 'int2'),
	location_2 string(2147483647) OPTIONS (NAMEINSOURCE '"location_2"', NATIVE_TYPE 'text'),
	persistent_state short OPTIONS (NAMEINSOURCE '"persistent_state"', NATIVE_TYPE 'int2'),
	create_mirror_data_loss_tracking_session_num long OPTIONS (NAMEINSOURCE '"create_mirror_data_loss_tracking_session_num"', NATIVE_TYPE 'int8'),
	mirror_existence_state short OPTIONS (NAMEINSOURCE '"mirror_existence_state"', NATIVE_TYPE 'int2'),
	reserved integer OPTIONS (NAMEINSOURCE '"reserved"', NATIVE_TYPE 'int4'),
	parent_xid integer OPTIONS (NAMEINSOURCE '"parent_xid"', NATIVE_TYPE 'int4'),
	persistent_serial_num long OPTIONS (NAMEINSOURCE '"persistent_serial_num"', NATIVE_TYPE 'int8'),
	previous_free_tid object OPTIONS (NAMEINSOURCE '"previous_free_tid"', NATIVE_TYPE 'tid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_persistent_filespace_node"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_persistent_relation_node" (
	tablespace_oid long NOT NULL OPTIONS (NAMEINSOURCE '"tablespace_oid"', NATIVE_TYPE 'oid'),
	database_oid long NOT NULL OPTIONS (NAMEINSOURCE '"database_oid"', NATIVE_TYPE 'oid'),
	relfilenode_oid long NOT NULL OPTIONS (NAMEINSOURCE '"relfilenode_oid"', NATIVE_TYPE 'oid'),
	segment_file_num integer NOT NULL OPTIONS (NAMEINSOURCE '"segment_file_num"', NATIVE_TYPE 'int4'),
	relation_storage_manager short NOT NULL OPTIONS (NAMEINSOURCE '"relation_storage_manager"', NATIVE_TYPE 'int2'),
	persistent_state short NOT NULL OPTIONS (NAMEINSOURCE '"persistent_state"', NATIVE_TYPE 'int2'),
	create_mirror_data_loss_tracking_session_num long NOT NULL OPTIONS (NAMEINSOURCE '"create_mirror_data_loss_tracking_session_num"', NATIVE_TYPE 'int8'),
	mirror_existence_state short NOT NULL OPTIONS (NAMEINSOURCE '"mirror_existence_state"', NATIVE_TYPE 'int2'),
	mirror_data_synchronization_state short NOT NULL OPTIONS (NAMEINSOURCE '"mirror_data_synchronization_state"', NATIVE_TYPE 'int2'),
	mirror_bufpool_marked_for_scan_incremental_resync boolean NOT NULL OPTIONS (NAMEINSOURCE '"mirror_bufpool_marked_for_scan_incremental_resync"', NATIVE_TYPE 'bool'),
	mirror_bufpool_resync_changed_page_count long NOT NULL OPTIONS (NAMEINSOURCE '"mirror_bufpool_resync_changed_page_count"', NATIVE_TYPE 'int8'),
	mirror_bufpool_resync_ckpt_loc object NOT NULL OPTIONS (NAMEINSOURCE '"mirror_bufpool_resync_ckpt_loc"', NATIVE_TYPE 'gpxlogloc'),
	mirror_bufpool_resync_ckpt_block_num integer NOT NULL OPTIONS (NAMEINSOURCE '"mirror_bufpool_resync_ckpt_block_num"', NATIVE_TYPE 'int4'),
	mirror_append_only_loss_eof long NOT NULL OPTIONS (NAMEINSOURCE '"mirror_append_only_loss_eof"', NATIVE_TYPE 'int8'),
	mirror_append_only_new_eof long NOT NULL OPTIONS (NAMEINSOURCE '"mirror_append_only_new_eof"', NATIVE_TYPE 'int8'),
	relation_bufpool_kind integer NOT NULL OPTIONS (NAMEINSOURCE '"relation_bufpool_kind"', NATIVE_TYPE 'int4'),
	parent_xid integer NOT NULL OPTIONS (NAMEINSOURCE '"parent_xid"', NATIVE_TYPE 'int4'),
	persistent_serial_num long NOT NULL OPTIONS (NAMEINSOURCE '"persistent_serial_num"', NATIVE_TYPE 'int8'),
	previous_free_tid object NOT NULL OPTIONS (NAMEINSOURCE '"previous_free_tid"', NATIVE_TYPE 'tid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_persistent_relation_node"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_persistent_tablespace_node" (
	filespace_oid long NOT NULL OPTIONS (NAMEINSOURCE '"filespace_oid"', NATIVE_TYPE 'oid'),
	tablespace_oid long NOT NULL OPTIONS (NAMEINSOURCE '"tablespace_oid"', NATIVE_TYPE 'oid'),
	persistent_state short NOT NULL OPTIONS (NAMEINSOURCE '"persistent_state"', NATIVE_TYPE 'int2'),
	create_mirror_data_loss_tracking_session_num long NOT NULL OPTIONS (NAMEINSOURCE '"create_mirror_data_loss_tracking_session_num"', NATIVE_TYPE 'int8'),
	mirror_existence_state short NOT NULL OPTIONS (NAMEINSOURCE '"mirror_existence_state"', NATIVE_TYPE 'int2'),
	reserved integer NOT NULL OPTIONS (NAMEINSOURCE '"reserved"', NATIVE_TYPE 'int4'),
	parent_xid integer NOT NULL OPTIONS (NAMEINSOURCE '"parent_xid"', NATIVE_TYPE 'int4'),
	persistent_serial_num long NOT NULL OPTIONS (NAMEINSOURCE '"persistent_serial_num"', NATIVE_TYPE 'int8'),
	previous_free_tid object NOT NULL OPTIONS (NAMEINSOURCE '"previous_free_tid"', NATIVE_TYPE 'tid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_persistent_tablespace_node"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_pgdatabase" (
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	isprimary boolean OPTIONS (NAMEINSOURCE '"isprimary"', NATIVE_TYPE 'bool'),
	content short OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2'),
	valid boolean OPTIONS (NAMEINSOURCE '"valid"', NATIVE_TYPE 'bool'),
	definedprimary boolean OPTIONS (NAMEINSOURCE '"definedprimary"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_pgdatabase"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_policy_localoid_index" (
	localoid long OPTIONS (NAMEINSOURCE '"localoid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_policy_localoid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_relation_node" (
	relfilenode_oid long NOT NULL OPTIONS (NAMEINSOURCE '"relfilenode_oid"', NATIVE_TYPE 'oid'),
	segment_file_num integer NOT NULL OPTIONS (NAMEINSOURCE '"segment_file_num"', NATIVE_TYPE 'int4'),
	create_mirror_data_loss_tracking_session_num long NOT NULL OPTIONS (NAMEINSOURCE '"create_mirror_data_loss_tracking_session_num"', NATIVE_TYPE 'int8'),
	persistent_tid object NOT NULL OPTIONS (NAMEINSOURCE '"persistent_tid"', NATIVE_TYPE 'tid'),
	persistent_serial_num long NOT NULL OPTIONS (NAMEINSOURCE '"persistent_serial_num"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_relation_node"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_relation_node_index" (
	relfilenode_oid long OPTIONS (NAMEINSOURCE '"relfilenode_oid"', NATIVE_TYPE 'oid'),
	segment_file_num integer OPTIONS (NAMEINSOURCE '"segment_file_num"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_relation_node_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_san_config_mountid_index" (
	mountid short OPTIONS (NAMEINSOURCE '"mountid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_san_config_mountid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_san_configuration" (
	mountid short NOT NULL OPTIONS (NAMEINSOURCE '"mountid"', NATIVE_TYPE 'int2'),
	active_host string(1) NOT NULL OPTIONS (NAMEINSOURCE '"active_host"', NATIVE_TYPE 'char'),
	san_type string(1) NOT NULL OPTIONS (NAMEINSOURCE '"san_type"', NATIVE_TYPE 'char'),
	primary_host string(2147483647) OPTIONS (NAMEINSOURCE '"primary_host"', NATIVE_TYPE 'text'),
	primary_mountpoint string(2147483647) OPTIONS (NAMEINSOURCE '"primary_mountpoint"', NATIVE_TYPE 'text'),
	primary_device string(2147483647) OPTIONS (NAMEINSOURCE '"primary_device"', NATIVE_TYPE 'text'),
	mirror_host string(2147483647) OPTIONS (NAMEINSOURCE '"mirror_host"', NATIVE_TYPE 'text'),
	mirror_mountpoint string(2147483647) OPTIONS (NAMEINSOURCE '"mirror_mountpoint"', NATIVE_TYPE 'text'),
	mirror_device string(2147483647) OPTIONS (NAMEINSOURCE '"mirror_device"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_san_configuration"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_segment_config_content_preferred_role_index" (
	content short OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2'),
	preferred_role string(1) OPTIONS (NAMEINSOURCE '"preferred_role"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_segment_config_content_preferred_role_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_segment_config_dbid_index" (
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_segment_config_dbid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_segment_configuration" (
	dbid short NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	content short NOT NULL OPTIONS (NAMEINSOURCE '"content"', NATIVE_TYPE 'int2'),
	role string(1) NOT NULL OPTIONS (NAMEINSOURCE '"role"', NATIVE_TYPE 'char'),
	preferred_role string(1) NOT NULL OPTIONS (NAMEINSOURCE '"preferred_role"', NATIVE_TYPE 'char'),
	mode string(1) NOT NULL OPTIONS (NAMEINSOURCE '"mode"', NATIVE_TYPE 'char'),
	status string(1) NOT NULL OPTIONS (NAMEINSOURCE '"status"', NATIVE_TYPE 'char'),
	port integer NOT NULL OPTIONS (NAMEINSOURCE '"port"', NATIVE_TYPE 'int4'),
	hostname string(2147483647) OPTIONS (NAMEINSOURCE '"hostname"', NATIVE_TYPE 'text'),
	address string(2147483647) OPTIONS (NAMEINSOURCE '"address"', NATIVE_TYPE 'text'),
	replication_port integer OPTIONS (NAMEINSOURCE '"replication_port"', NATIVE_TYPE 'int4'),
	san_mounts object OPTIONS (NAMEINSOURCE '"san_mounts"', NATIVE_TYPE 'int2vector')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_segment_configuration"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_transaction_log" (
	segment_id short OPTIONS (NAMEINSOURCE '"segment_id"', NATIVE_TYPE 'int2'),
	dbid short OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'int2'),
	transaction object OPTIONS (NAMEINSOURCE '"transaction"', NATIVE_TYPE 'xid'),
	status string(2147483647) OPTIONS (NAMEINSOURCE '"status"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_transaction_log"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_verification_history" (
	vertoken string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"vertoken"', NATIVE_TYPE 'name'),
	vertype short NOT NULL OPTIONS (NAMEINSOURCE '"vertype"', NATIVE_TYPE 'int2'),
	vercontent short NOT NULL OPTIONS (NAMEINSOURCE '"vercontent"', NATIVE_TYPE 'int2'),
	verstarttime timestamp NOT NULL OPTIONS (NAMEINSOURCE '"verstarttime"', NATIVE_TYPE 'timestamptz'),
	verstate short NOT NULL OPTIONS (NAMEINSOURCE '"verstate"', NATIVE_TYPE 'int2'),
	verdone boolean NOT NULL OPTIONS (NAMEINSOURCE '"verdone"', NATIVE_TYPE 'bool'),
	verendtime timestamp NOT NULL OPTIONS (NAMEINSOURCE '"verendtime"', NATIVE_TYPE 'timestamptz'),
	vermismatch boolean NOT NULL OPTIONS (NAMEINSOURCE '"vermismatch"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_verification_history"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_verification_history_vertoken_index" (
	vertoken string(2147483647) OPTIONS (NAMEINSOURCE '"vertoken"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_verification_history_vertoken_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.gp_version_at_initdb" (
	schemaversion short NOT NULL OPTIONS (NAMEINSOURCE '"schemaversion"', NATIVE_TYPE 'int2'),
	productversion string(2147483647) OPTIONS (NAMEINSOURCE '"productversion"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."gp_version_at_initdb"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.nb_classification" (
	classes object OPTIONS (NAMEINSOURCE '"classes"', NATIVE_TYPE '_text'),
	accum object OPTIONS (NAMEINSOURCE '"accum"', NATIVE_TYPE '_float8'),
	apriori object OPTIONS (NAMEINSOURCE '"apriori"', NATIVE_TYPE '_int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."nb_classification"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_aggregate" (
	aggfnoid object NOT NULL OPTIONS (NAMEINSOURCE '"aggfnoid"', NATIVE_TYPE 'regproc'),
	aggtransfn object NOT NULL OPTIONS (NAMEINSOURCE '"aggtransfn"', NATIVE_TYPE 'regproc'),
	agginvtransfn object NOT NULL OPTIONS (NAMEINSOURCE '"agginvtransfn"', NATIVE_TYPE 'regproc'),
	aggprelimfn object NOT NULL OPTIONS (NAMEINSOURCE '"aggprelimfn"', NATIVE_TYPE 'regproc'),
	agginvprelimfn object NOT NULL OPTIONS (NAMEINSOURCE '"agginvprelimfn"', NATIVE_TYPE 'regproc'),
	aggfinalfn object NOT NULL OPTIONS (NAMEINSOURCE '"aggfinalfn"', NATIVE_TYPE 'regproc'),
	aggsortop long NOT NULL OPTIONS (NAMEINSOURCE '"aggsortop"', NATIVE_TYPE 'oid'),
	aggtranstype long NOT NULL OPTIONS (NAMEINSOURCE '"aggtranstype"', NATIVE_TYPE 'oid'),
	agginitval string(2147483647) OPTIONS (NAMEINSOURCE '"agginitval"', NATIVE_TYPE 'text'),
	aggordered boolean OPTIONS (NAMEINSOURCE '"aggordered"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_aggregate"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_aggregate_fnoid_index" (
	aggfnoid object OPTIONS (NAMEINSOURCE '"aggfnoid"', NATIVE_TYPE 'regproc')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_aggregate_fnoid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_am" (
	amname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"amname"', NATIVE_TYPE 'name'),
	amstrategies short NOT NULL OPTIONS (NAMEINSOURCE '"amstrategies"', NATIVE_TYPE 'int2'),
	amsupport short NOT NULL OPTIONS (NAMEINSOURCE '"amsupport"', NATIVE_TYPE 'int2'),
	amorderstrategy short NOT NULL OPTIONS (NAMEINSOURCE '"amorderstrategy"', NATIVE_TYPE 'int2'),
	amcanunique boolean NOT NULL OPTIONS (NAMEINSOURCE '"amcanunique"', NATIVE_TYPE 'bool'),
	amcanmulticol boolean NOT NULL OPTIONS (NAMEINSOURCE '"amcanmulticol"', NATIVE_TYPE 'bool'),
	amoptionalkey boolean NOT NULL OPTIONS (NAMEINSOURCE '"amoptionalkey"', NATIVE_TYPE 'bool'),
	amindexnulls boolean NOT NULL OPTIONS (NAMEINSOURCE '"amindexnulls"', NATIVE_TYPE 'bool'),
	amstorage boolean NOT NULL OPTIONS (NAMEINSOURCE '"amstorage"', NATIVE_TYPE 'bool'),
	amclusterable boolean NOT NULL OPTIONS (NAMEINSOURCE '"amclusterable"', NATIVE_TYPE 'bool'),
	amcanshrink boolean NOT NULL OPTIONS (NAMEINSOURCE '"amcanshrink"', NATIVE_TYPE 'bool'),
	aminsert object NOT NULL OPTIONS (NAMEINSOURCE '"aminsert"', NATIVE_TYPE 'regproc'),
	ambeginscan object NOT NULL OPTIONS (NAMEINSOURCE '"ambeginscan"', NATIVE_TYPE 'regproc'),
	amgettuple object NOT NULL OPTIONS (NAMEINSOURCE '"amgettuple"', NATIVE_TYPE 'regproc'),
	amgetmulti object NOT NULL OPTIONS (NAMEINSOURCE '"amgetmulti"', NATIVE_TYPE 'regproc'),
	amrescan object NOT NULL OPTIONS (NAMEINSOURCE '"amrescan"', NATIVE_TYPE 'regproc'),
	amendscan object NOT NULL OPTIONS (NAMEINSOURCE '"amendscan"', NATIVE_TYPE 'regproc'),
	ammarkpos object NOT NULL OPTIONS (NAMEINSOURCE '"ammarkpos"', NATIVE_TYPE 'regproc'),
	amrestrpos object NOT NULL OPTIONS (NAMEINSOURCE '"amrestrpos"', NATIVE_TYPE 'regproc'),
	ambuild object NOT NULL OPTIONS (NAMEINSOURCE '"ambuild"', NATIVE_TYPE 'regproc'),
	ambulkdelete object NOT NULL OPTIONS (NAMEINSOURCE '"ambulkdelete"', NATIVE_TYPE 'regproc'),
	amvacuumcleanup object NOT NULL OPTIONS (NAMEINSOURCE '"amvacuumcleanup"', NATIVE_TYPE 'regproc'),
	amcostestimate object NOT NULL OPTIONS (NAMEINSOURCE '"amcostestimate"', NATIVE_TYPE 'regproc'),
	amoptions object NOT NULL OPTIONS (NAMEINSOURCE '"amoptions"', NATIVE_TYPE 'regproc')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_am"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_am_name_index" (
	amname string(2147483647) OPTIONS (NAMEINSOURCE '"amname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_am_name_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_am_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_am_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_amop" (
	amopclaid long NOT NULL OPTIONS (NAMEINSOURCE '"amopclaid"', NATIVE_TYPE 'oid'),
	amopsubtype long NOT NULL OPTIONS (NAMEINSOURCE '"amopsubtype"', NATIVE_TYPE 'oid'),
	amopstrategy short NOT NULL OPTIONS (NAMEINSOURCE '"amopstrategy"', NATIVE_TYPE 'int2'),
	amopreqcheck boolean NOT NULL OPTIONS (NAMEINSOURCE '"amopreqcheck"', NATIVE_TYPE 'bool'),
	amopopr long NOT NULL OPTIONS (NAMEINSOURCE '"amopopr"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_amop"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_amop_opc_strat_index" (
	amopclaid long OPTIONS (NAMEINSOURCE '"amopclaid"', NATIVE_TYPE 'oid'),
	amopsubtype long OPTIONS (NAMEINSOURCE '"amopsubtype"', NATIVE_TYPE 'oid'),
	amopstrategy short OPTIONS (NAMEINSOURCE '"amopstrategy"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_amop_opc_strat_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_amop_opr_opc_index" (
	amopopr long OPTIONS (NAMEINSOURCE '"amopopr"', NATIVE_TYPE 'oid'),
	amopclaid long OPTIONS (NAMEINSOURCE '"amopclaid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_amop_opr_opc_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_amproc" (
	amopclaid long NOT NULL OPTIONS (NAMEINSOURCE '"amopclaid"', NATIVE_TYPE 'oid'),
	amprocsubtype long NOT NULL OPTIONS (NAMEINSOURCE '"amprocsubtype"', NATIVE_TYPE 'oid'),
	amprocnum short NOT NULL OPTIONS (NAMEINSOURCE '"amprocnum"', NATIVE_TYPE 'int2'),
	amproc object NOT NULL OPTIONS (NAMEINSOURCE '"amproc"', NATIVE_TYPE 'regproc')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_amproc"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_amproc_opc_proc_index" (
	amopclaid long OPTIONS (NAMEINSOURCE '"amopclaid"', NATIVE_TYPE 'oid'),
	amprocsubtype long OPTIONS (NAMEINSOURCE '"amprocsubtype"', NATIVE_TYPE 'oid'),
	amprocnum short OPTIONS (NAMEINSOURCE '"amprocnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_amproc_opc_proc_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_appendonly" (
	relid long NOT NULL OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	blocksize integer NOT NULL OPTIONS (NAMEINSOURCE '"blocksize"', NATIVE_TYPE 'int4'),
	safefswritesize integer NOT NULL OPTIONS (NAMEINSOURCE '"safefswritesize"', NATIVE_TYPE 'int4'),
	compresslevel short NOT NULL OPTIONS (NAMEINSOURCE '"compresslevel"', NATIVE_TYPE 'int2'),
	majorversion short NOT NULL OPTIONS (NAMEINSOURCE '"majorversion"', NATIVE_TYPE 'int2'),
	minorversion short NOT NULL OPTIONS (NAMEINSOURCE '"minorversion"', NATIVE_TYPE 'int2'),
	checksum boolean NOT NULL OPTIONS (NAMEINSOURCE '"checksum"', NATIVE_TYPE 'bool'),
	compresstype string(2147483647) OPTIONS (NAMEINSOURCE '"compresstype"', NATIVE_TYPE 'text'),
	columnstore boolean OPTIONS (NAMEINSOURCE '"columnstore"', NATIVE_TYPE 'bool'),
	segrelid long OPTIONS (NAMEINSOURCE '"segrelid"', NATIVE_TYPE 'oid'),
	segidxid long OPTIONS (NAMEINSOURCE '"segidxid"', NATIVE_TYPE 'oid'),
	blkdirrelid long OPTIONS (NAMEINSOURCE '"blkdirrelid"', NATIVE_TYPE 'oid'),
	blkdiridxid long OPTIONS (NAMEINSOURCE '"blkdiridxid"', NATIVE_TYPE 'oid'),
	version integer OPTIONS (NAMEINSOURCE '"version"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_appendonly"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_appendonly_alter_column" (
	relid long NOT NULL OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	changenum integer NOT NULL OPTIONS (NAMEINSOURCE '"changenum"', NATIVE_TYPE 'int4'),
	segfilenums object OPTIONS (NAMEINSOURCE '"segfilenums"', NATIVE_TYPE '_int4'),
	highwaterrownums varbinary(2147483647) OPTIONS (NAMEINSOURCE '"highwaterrownums"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_appendonly_alter_column"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_appendonly_alter_column_relid_index" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	changenum integer OPTIONS (NAMEINSOURCE '"changenum"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_appendonly_alter_column_relid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_appendonly_relid_index" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_appendonly_relid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attrdef" (
	adrelid long NOT NULL OPTIONS (NAMEINSOURCE '"adrelid"', NATIVE_TYPE 'oid'),
	adnum short NOT NULL OPTIONS (NAMEINSOURCE '"adnum"', NATIVE_TYPE 'int2'),
	adbin string(2147483647) OPTIONS (NAMEINSOURCE '"adbin"', NATIVE_TYPE 'text'),
	adsrc string(2147483647) OPTIONS (NAMEINSOURCE '"adsrc"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attrdef"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attrdef_adrelid_adnum_index" (
	adrelid long OPTIONS (NAMEINSOURCE '"adrelid"', NATIVE_TYPE 'oid'),
	adnum short OPTIONS (NAMEINSOURCE '"adnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attrdef_adrelid_adnum_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attrdef_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attrdef_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute" (
	attrelid long NOT NULL OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid'),
	attname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"attname"', NATIVE_TYPE 'name'),
	atttypid long NOT NULL OPTIONS (NAMEINSOURCE '"atttypid"', NATIVE_TYPE 'oid'),
	attstattarget integer NOT NULL OPTIONS (NAMEINSOURCE '"attstattarget"', NATIVE_TYPE 'int4'),
	attlen short NOT NULL OPTIONS (NAMEINSOURCE '"attlen"', NATIVE_TYPE 'int2'),
	attnum short NOT NULL OPTIONS (NAMEINSOURCE '"attnum"', NATIVE_TYPE 'int2'),
	attndims integer NOT NULL OPTIONS (NAMEINSOURCE '"attndims"', NATIVE_TYPE 'int4'),
	attcacheoff integer NOT NULL OPTIONS (NAMEINSOURCE '"attcacheoff"', NATIVE_TYPE 'int4'),
	atttypmod integer NOT NULL OPTIONS (NAMEINSOURCE '"atttypmod"', NATIVE_TYPE 'int4'),
	attbyval boolean NOT NULL OPTIONS (NAMEINSOURCE '"attbyval"', NATIVE_TYPE 'bool'),
	attstorage string(1) NOT NULL OPTIONS (NAMEINSOURCE '"attstorage"', NATIVE_TYPE 'char'),
	attalign string(1) NOT NULL OPTIONS (NAMEINSOURCE '"attalign"', NATIVE_TYPE 'char'),
	attnotnull boolean NOT NULL OPTIONS (NAMEINSOURCE '"attnotnull"', NATIVE_TYPE 'bool'),
	atthasdef boolean NOT NULL OPTIONS (NAMEINSOURCE '"atthasdef"', NATIVE_TYPE 'bool'),
	attisdropped boolean NOT NULL OPTIONS (NAMEINSOURCE '"attisdropped"', NATIVE_TYPE 'bool'),
	attislocal boolean NOT NULL OPTIONS (NAMEINSOURCE '"attislocal"', NATIVE_TYPE 'bool'),
	attinhcount integer NOT NULL OPTIONS (NAMEINSOURCE '"attinhcount"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute_encoding" (
	attrelid long NOT NULL OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid'),
	attnum short NOT NULL OPTIONS (NAMEINSOURCE '"attnum"', NATIVE_TYPE 'int2'),
	attoptions object OPTIONS (NAMEINSOURCE '"attoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute_encoding"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute_encoding_attrelid_attnum_index" (
	attrelid long OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid'),
	attnum short OPTIONS (NAMEINSOURCE '"attnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute_encoding_attrelid_attnum_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute_encoding_attrelid_index" (
	attrelid long OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute_encoding_attrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute_relid_attnam_index" (
	attrelid long OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid'),
	attname string(2147483647) OPTIONS (NAMEINSOURCE '"attname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute_relid_attnam_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_attribute_relid_attnum_index" (
	attrelid long OPTIONS (NAMEINSOURCE '"attrelid"', NATIVE_TYPE 'oid'),
	attnum short OPTIONS (NAMEINSOURCE '"attnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_attribute_relid_attnum_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_auth_members" (
	roleid long NOT NULL OPTIONS (NAMEINSOURCE '"roleid"', NATIVE_TYPE 'oid'),
	"member" long NOT NULL OPTIONS (NAMEINSOURCE '"member"', NATIVE_TYPE 'oid'),
	grantor long NOT NULL OPTIONS (NAMEINSOURCE '"grantor"', NATIVE_TYPE 'oid'),
	admin_option boolean NOT NULL OPTIONS (NAMEINSOURCE '"admin_option"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_auth_members"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_auth_members_member_role_index" (
	"member" long OPTIONS (NAMEINSOURCE '"member"', NATIVE_TYPE 'oid'),
	roleid long OPTIONS (NAMEINSOURCE '"roleid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_auth_members_member_role_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_auth_members_role_member_index" (
	roleid long OPTIONS (NAMEINSOURCE '"roleid"', NATIVE_TYPE 'oid'),
	"member" long OPTIONS (NAMEINSOURCE '"member"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_auth_members_role_member_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_auth_time_constraint" (
	authid long NOT NULL OPTIONS (NAMEINSOURCE '"authid"', NATIVE_TYPE 'oid'),
	start_day short NOT NULL OPTIONS (NAMEINSOURCE '"start_day"', NATIVE_TYPE 'int2'),
	start_time time NOT NULL OPTIONS (NAMEINSOURCE '"start_time"', NATIVE_TYPE 'time'),
	end_day short NOT NULL OPTIONS (NAMEINSOURCE '"end_day"', NATIVE_TYPE 'int2'),
	end_time time NOT NULL OPTIONS (NAMEINSOURCE '"end_time"', NATIVE_TYPE 'time')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_auth_time_constraint"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_authid" (
	rolname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"rolname"', NATIVE_TYPE 'name'),
	rolsuper boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolsuper"', NATIVE_TYPE 'bool'),
	rolinherit boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolinherit"', NATIVE_TYPE 'bool'),
	rolcreaterole boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolcreaterole"', NATIVE_TYPE 'bool'),
	rolcreatedb boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolcreatedb"', NATIVE_TYPE 'bool'),
	rolcatupdate boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolcatupdate"', NATIVE_TYPE 'bool'),
	rolcanlogin boolean NOT NULL OPTIONS (NAMEINSOURCE '"rolcanlogin"', NATIVE_TYPE 'bool'),
	rolconnlimit integer NOT NULL OPTIONS (NAMEINSOURCE '"rolconnlimit"', NATIVE_TYPE 'int4'),
	rolpassword string(2147483647) OPTIONS (NAMEINSOURCE '"rolpassword"', NATIVE_TYPE 'text'),
	rolvaliduntil timestamp OPTIONS (NAMEINSOURCE '"rolvaliduntil"', NATIVE_TYPE 'timestamptz'),
	rolconfig object OPTIONS (NAMEINSOURCE '"rolconfig"', NATIVE_TYPE '_text'),
	rolresqueue long OPTIONS (NAMEINSOURCE '"rolresqueue"', NATIVE_TYPE 'oid'),
	rolcreaterextgpfd boolean OPTIONS (NAMEINSOURCE '"rolcreaterextgpfd"', NATIVE_TYPE 'bool'),
	rolcreaterexthttp boolean OPTIONS (NAMEINSOURCE '"rolcreaterexthttp"', NATIVE_TYPE 'bool'),
	rolcreatewextgpfd boolean OPTIONS (NAMEINSOURCE '"rolcreatewextgpfd"', NATIVE_TYPE 'bool'),
	rolcreaterexthdfs boolean OPTIONS (NAMEINSOURCE '"rolcreaterexthdfs"', NATIVE_TYPE 'bool'),
	rolcreatewexthdfs boolean OPTIONS (NAMEINSOURCE '"rolcreatewexthdfs"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_authid"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_authid_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_authid_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_authid_rolname_index" (
	rolname string(2147483647) OPTIONS (NAMEINSOURCE '"rolname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_authid_rolname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_authid_rolresqueue_index" (
	rolresqueue long OPTIONS (NAMEINSOURCE '"rolresqueue"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_authid_rolresqueue_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_autovacuum" (
	vacrelid long NOT NULL OPTIONS (NAMEINSOURCE '"vacrelid"', NATIVE_TYPE 'oid'),
	enabled boolean NOT NULL OPTIONS (NAMEINSOURCE '"enabled"', NATIVE_TYPE 'bool'),
	vac_base_thresh integer NOT NULL OPTIONS (NAMEINSOURCE '"vac_base_thresh"', NATIVE_TYPE 'int4'),
	vac_scale_factor float NOT NULL OPTIONS (NAMEINSOURCE '"vac_scale_factor"', NATIVE_TYPE 'float4'),
	anl_base_thresh integer NOT NULL OPTIONS (NAMEINSOURCE '"anl_base_thresh"', NATIVE_TYPE 'int4'),
	anl_scale_factor float NOT NULL OPTIONS (NAMEINSOURCE '"anl_scale_factor"', NATIVE_TYPE 'float4'),
	vac_cost_delay integer NOT NULL OPTIONS (NAMEINSOURCE '"vac_cost_delay"', NATIVE_TYPE 'int4'),
	vac_cost_limit integer NOT NULL OPTIONS (NAMEINSOURCE '"vac_cost_limit"', NATIVE_TYPE 'int4'),
	freeze_min_age integer NOT NULL OPTIONS (NAMEINSOURCE '"freeze_min_age"', NATIVE_TYPE 'int4'),
	freeze_max_age integer NOT NULL OPTIONS (NAMEINSOURCE '"freeze_max_age"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_autovacuum"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_autovacuum_vacrelid_index" (
	vacrelid long OPTIONS (NAMEINSOURCE '"vacrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_autovacuum_vacrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_cast" (
	castsource long NOT NULL OPTIONS (NAMEINSOURCE '"castsource"', NATIVE_TYPE 'oid'),
	casttarget long NOT NULL OPTIONS (NAMEINSOURCE '"casttarget"', NATIVE_TYPE 'oid'),
	castfunc long NOT NULL OPTIONS (NAMEINSOURCE '"castfunc"', NATIVE_TYPE 'oid'),
	castcontext string(1) NOT NULL OPTIONS (NAMEINSOURCE '"castcontext"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_cast"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_cast_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_cast_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_cast_source_target_index" (
	castsource long OPTIONS (NAMEINSOURCE '"castsource"', NATIVE_TYPE 'oid'),
	casttarget long OPTIONS (NAMEINSOURCE '"casttarget"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_cast_source_target_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_class" (
	relname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	relnamespace long NOT NULL OPTIONS (NAMEINSOURCE '"relnamespace"', NATIVE_TYPE 'oid'),
	reltype long NOT NULL OPTIONS (NAMEINSOURCE '"reltype"', NATIVE_TYPE 'oid'),
	relowner long NOT NULL OPTIONS (NAMEINSOURCE '"relowner"', NATIVE_TYPE 'oid'),
	relam long NOT NULL OPTIONS (NAMEINSOURCE '"relam"', NATIVE_TYPE 'oid'),
	relfilenode long NOT NULL OPTIONS (NAMEINSOURCE '"relfilenode"', NATIVE_TYPE 'oid'),
	reltablespace long NOT NULL OPTIONS (NAMEINSOURCE '"reltablespace"', NATIVE_TYPE 'oid'),
	relpages integer NOT NULL OPTIONS (NAMEINSOURCE '"relpages"', NATIVE_TYPE 'int4'),
	reltuples float NOT NULL OPTIONS (NAMEINSOURCE '"reltuples"', NATIVE_TYPE 'float4'),
	reltoastrelid long NOT NULL OPTIONS (NAMEINSOURCE '"reltoastrelid"', NATIVE_TYPE 'oid'),
	reltoastidxid long NOT NULL OPTIONS (NAMEINSOURCE '"reltoastidxid"', NATIVE_TYPE 'oid'),
	relaosegrelid long NOT NULL OPTIONS (NAMEINSOURCE '"relaosegrelid"', NATIVE_TYPE 'oid'),
	relaosegidxid long NOT NULL OPTIONS (NAMEINSOURCE '"relaosegidxid"', NATIVE_TYPE 'oid'),
	relhasindex boolean NOT NULL OPTIONS (NAMEINSOURCE '"relhasindex"', NATIVE_TYPE 'bool'),
	relisshared boolean NOT NULL OPTIONS (NAMEINSOURCE '"relisshared"', NATIVE_TYPE 'bool'),
	relkind string(1) NOT NULL OPTIONS (NAMEINSOURCE '"relkind"', NATIVE_TYPE 'char'),
	relstorage string(1) NOT NULL OPTIONS (NAMEINSOURCE '"relstorage"', NATIVE_TYPE 'char'),
	relnatts short NOT NULL OPTIONS (NAMEINSOURCE '"relnatts"', NATIVE_TYPE 'int2'),
	relchecks short NOT NULL OPTIONS (NAMEINSOURCE '"relchecks"', NATIVE_TYPE 'int2'),
	reltriggers short NOT NULL OPTIONS (NAMEINSOURCE '"reltriggers"', NATIVE_TYPE 'int2'),
	relukeys short NOT NULL OPTIONS (NAMEINSOURCE '"relukeys"', NATIVE_TYPE 'int2'),
	relfkeys short NOT NULL OPTIONS (NAMEINSOURCE '"relfkeys"', NATIVE_TYPE 'int2'),
	relrefs short NOT NULL OPTIONS (NAMEINSOURCE '"relrefs"', NATIVE_TYPE 'int2'),
	relhasoids boolean NOT NULL OPTIONS (NAMEINSOURCE '"relhasoids"', NATIVE_TYPE 'bool'),
	relhaspkey boolean NOT NULL OPTIONS (NAMEINSOURCE '"relhaspkey"', NATIVE_TYPE 'bool'),
	relhasrules boolean NOT NULL OPTIONS (NAMEINSOURCE '"relhasrules"', NATIVE_TYPE 'bool'),
	relhassubclass boolean NOT NULL OPTIONS (NAMEINSOURCE '"relhassubclass"', NATIVE_TYPE 'bool'),
	relfrozenxid object NOT NULL OPTIONS (NAMEINSOURCE '"relfrozenxid"', NATIVE_TYPE 'xid'),
	relacl object OPTIONS (NAMEINSOURCE '"relacl"', NATIVE_TYPE '_aclitem'),
	reloptions object OPTIONS (NAMEINSOURCE '"reloptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_class"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_class_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_class_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_class_relname_nsp_index" (
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	relnamespace long OPTIONS (NAMEINSOURCE '"relnamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_class_relname_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_compression" (
	compname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"compname"', NATIVE_TYPE 'name'),
	compconstructor object NOT NULL OPTIONS (NAMEINSOURCE '"compconstructor"', NATIVE_TYPE 'regproc'),
	compdestructor object NOT NULL OPTIONS (NAMEINSOURCE '"compdestructor"', NATIVE_TYPE 'regproc'),
	compcompressor object NOT NULL OPTIONS (NAMEINSOURCE '"compcompressor"', NATIVE_TYPE 'regproc'),
	compdecompressor object NOT NULL OPTIONS (NAMEINSOURCE '"compdecompressor"', NATIVE_TYPE 'regproc'),
	compvalidator object NOT NULL OPTIONS (NAMEINSOURCE '"compvalidator"', NATIVE_TYPE 'regproc'),
	compowner long NOT NULL OPTIONS (NAMEINSOURCE '"compowner"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_compression"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_compression_compname_index" (
	compname string(2147483647) OPTIONS (NAMEINSOURCE '"compname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_compression_compname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_compression_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_compression_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_constraint" (
	conname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"conname"', NATIVE_TYPE 'name'),
	connamespace long NOT NULL OPTIONS (NAMEINSOURCE '"connamespace"', NATIVE_TYPE 'oid'),
	contype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"contype"', NATIVE_TYPE 'char'),
	condeferrable boolean NOT NULL OPTIONS (NAMEINSOURCE '"condeferrable"', NATIVE_TYPE 'bool'),
	condeferred boolean NOT NULL OPTIONS (NAMEINSOURCE '"condeferred"', NATIVE_TYPE 'bool'),
	conrelid long NOT NULL OPTIONS (NAMEINSOURCE '"conrelid"', NATIVE_TYPE 'oid'),
	contypid long NOT NULL OPTIONS (NAMEINSOURCE '"contypid"', NATIVE_TYPE 'oid'),
	confrelid long NOT NULL OPTIONS (NAMEINSOURCE '"confrelid"', NATIVE_TYPE 'oid'),
	confupdtype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"confupdtype"', NATIVE_TYPE 'char'),
	confdeltype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"confdeltype"', NATIVE_TYPE 'char'),
	confmatchtype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"confmatchtype"', NATIVE_TYPE 'char'),
	conkey object OPTIONS (NAMEINSOURCE '"conkey"', NATIVE_TYPE '_int2'),
	confkey object OPTIONS (NAMEINSOURCE '"confkey"', NATIVE_TYPE '_int2'),
	conbin string(2147483647) OPTIONS (NAMEINSOURCE '"conbin"', NATIVE_TYPE 'text'),
	consrc string(2147483647) OPTIONS (NAMEINSOURCE '"consrc"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_constraint"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_constraint_conname_nsp_index" (
	conname string(2147483647) OPTIONS (NAMEINSOURCE '"conname"', NATIVE_TYPE 'name'),
	connamespace long OPTIONS (NAMEINSOURCE '"connamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_constraint_conname_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_constraint_conrelid_index" (
	conrelid long OPTIONS (NAMEINSOURCE '"conrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_constraint_conrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_constraint_contypid_index" (
	contypid long OPTIONS (NAMEINSOURCE '"contypid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_constraint_contypid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_constraint_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_constraint_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_conversion" (
	conname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"conname"', NATIVE_TYPE 'name'),
	connamespace long NOT NULL OPTIONS (NAMEINSOURCE '"connamespace"', NATIVE_TYPE 'oid'),
	conowner long NOT NULL OPTIONS (NAMEINSOURCE '"conowner"', NATIVE_TYPE 'oid'),
	conforencoding integer NOT NULL OPTIONS (NAMEINSOURCE '"conforencoding"', NATIVE_TYPE 'int4'),
	contoencoding integer NOT NULL OPTIONS (NAMEINSOURCE '"contoencoding"', NATIVE_TYPE 'int4'),
	conproc object NOT NULL OPTIONS (NAMEINSOURCE '"conproc"', NATIVE_TYPE 'regproc'),
	condefault boolean NOT NULL OPTIONS (NAMEINSOURCE '"condefault"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_conversion"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_conversion_default_index" (
	connamespace long OPTIONS (NAMEINSOURCE '"connamespace"', NATIVE_TYPE 'oid'),
	conforencoding integer OPTIONS (NAMEINSOURCE '"conforencoding"', NATIVE_TYPE 'int4'),
	contoencoding integer OPTIONS (NAMEINSOURCE '"contoencoding"', NATIVE_TYPE 'int4'),
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_conversion_default_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_conversion_name_nsp_index" (
	conname string(2147483647) OPTIONS (NAMEINSOURCE '"conname"', NATIVE_TYPE 'name'),
	connamespace long OPTIONS (NAMEINSOURCE '"connamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_conversion_name_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_conversion_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_conversion_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_cursors" (
	name string(2147483647) OPTIONS (NAMEINSOURCE '"name"', NATIVE_TYPE 'text'),
	statement string(2147483647) OPTIONS (NAMEINSOURCE '"statement"', NATIVE_TYPE 'text'),
	is_holdable boolean OPTIONS (NAMEINSOURCE '"is_holdable"', NATIVE_TYPE 'bool'),
	is_binary boolean OPTIONS (NAMEINSOURCE '"is_binary"', NATIVE_TYPE 'bool'),
	is_scrollable boolean OPTIONS (NAMEINSOURCE '"is_scrollable"', NATIVE_TYPE 'bool'),
	creation_time timestamp OPTIONS (NAMEINSOURCE '"creation_time"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_cursors"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_database" (
	datname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"datname"', NATIVE_TYPE 'name'),
	datdba long NOT NULL OPTIONS (NAMEINSOURCE '"datdba"', NATIVE_TYPE 'oid'),
	encoding integer NOT NULL OPTIONS (NAMEINSOURCE '"encoding"', NATIVE_TYPE 'int4'),
	datistemplate boolean NOT NULL OPTIONS (NAMEINSOURCE '"datistemplate"', NATIVE_TYPE 'bool'),
	datallowconn boolean NOT NULL OPTIONS (NAMEINSOURCE '"datallowconn"', NATIVE_TYPE 'bool'),
	datconnlimit integer NOT NULL OPTIONS (NAMEINSOURCE '"datconnlimit"', NATIVE_TYPE 'int4'),
	datlastsysoid long NOT NULL OPTIONS (NAMEINSOURCE '"datlastsysoid"', NATIVE_TYPE 'oid'),
	datfrozenxid object NOT NULL OPTIONS (NAMEINSOURCE '"datfrozenxid"', NATIVE_TYPE 'xid'),
	dattablespace long NOT NULL OPTIONS (NAMEINSOURCE '"dattablespace"', NATIVE_TYPE 'oid'),
	datconfig object OPTIONS (NAMEINSOURCE '"datconfig"', NATIVE_TYPE '_text'),
	datacl object OPTIONS (NAMEINSOURCE '"datacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_database"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_database_datname_index" (
	datname string(2147483647) OPTIONS (NAMEINSOURCE '"datname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_database_datname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_database_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_database_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_depend" (
	classid long NOT NULL OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long NOT NULL OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	objsubid integer NOT NULL OPTIONS (NAMEINSOURCE '"objsubid"', NATIVE_TYPE 'int4'),
	refclassid long NOT NULL OPTIONS (NAMEINSOURCE '"refclassid"', NATIVE_TYPE 'oid'),
	refobjid long NOT NULL OPTIONS (NAMEINSOURCE '"refobjid"', NATIVE_TYPE 'oid'),
	refobjsubid integer NOT NULL OPTIONS (NAMEINSOURCE '"refobjsubid"', NATIVE_TYPE 'int4'),
	deptype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"deptype"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_depend"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_depend_depender_index" (
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	objsubid integer OPTIONS (NAMEINSOURCE '"objsubid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_depend_depender_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_depend_reference_index" (
	refclassid long OPTIONS (NAMEINSOURCE '"refclassid"', NATIVE_TYPE 'oid'),
	refobjid long OPTIONS (NAMEINSOURCE '"refobjid"', NATIVE_TYPE 'oid'),
	refobjsubid integer OPTIONS (NAMEINSOURCE '"refobjsubid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_depend_reference_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_description" (
	objoid long NOT NULL OPTIONS (NAMEINSOURCE '"objoid"', NATIVE_TYPE 'oid'),
	classoid long NOT NULL OPTIONS (NAMEINSOURCE '"classoid"', NATIVE_TYPE 'oid'),
	objsubid integer NOT NULL OPTIONS (NAMEINSOURCE '"objsubid"', NATIVE_TYPE 'int4'),
	description string(2147483647) OPTIONS (NAMEINSOURCE '"description"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_description"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_description_o_c_o_index" (
	objoid long OPTIONS (NAMEINSOURCE '"objoid"', NATIVE_TYPE 'oid'),
	classoid long OPTIONS (NAMEINSOURCE '"classoid"', NATIVE_TYPE 'oid'),
	objsubid integer OPTIONS (NAMEINSOURCE '"objsubid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_description_o_c_o_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_extprotocol" (
	ptcname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"ptcname"', NATIVE_TYPE 'name'),
	ptcreadfn long NOT NULL OPTIONS (NAMEINSOURCE '"ptcreadfn"', NATIVE_TYPE 'oid'),
	ptcwritefn long NOT NULL OPTIONS (NAMEINSOURCE '"ptcwritefn"', NATIVE_TYPE 'oid'),
	ptcvalidatorfn long NOT NULL OPTIONS (NAMEINSOURCE '"ptcvalidatorfn"', NATIVE_TYPE 'oid'),
	ptcowner long NOT NULL OPTIONS (NAMEINSOURCE '"ptcowner"', NATIVE_TYPE 'oid'),
	ptctrusted boolean NOT NULL OPTIONS (NAMEINSOURCE '"ptctrusted"', NATIVE_TYPE 'bool'),
	ptcacl object OPTIONS (NAMEINSOURCE '"ptcacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_extprotocol"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_extprotocol_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_extprotocol_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_extprotocol_ptcname_index" (
	ptcname string(2147483647) OPTIONS (NAMEINSOURCE '"ptcname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_extprotocol_ptcname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_exttable" (
	reloid long NOT NULL OPTIONS (NAMEINSOURCE '"reloid"', NATIVE_TYPE 'oid'),
	location object OPTIONS (NAMEINSOURCE '"location"', NATIVE_TYPE '_text'),
	fmttype string(1) OPTIONS (NAMEINSOURCE '"fmttype"', NATIVE_TYPE 'char'),
	fmtopts string(2147483647) OPTIONS (NAMEINSOURCE '"fmtopts"', NATIVE_TYPE 'text'),
	command string(2147483647) OPTIONS (NAMEINSOURCE '"command"', NATIVE_TYPE 'text'),
	rejectlimit integer OPTIONS (NAMEINSOURCE '"rejectlimit"', NATIVE_TYPE 'int4'),
	rejectlimittype string(1) OPTIONS (NAMEINSOURCE '"rejectlimittype"', NATIVE_TYPE 'char'),
	fmterrtbl long OPTIONS (NAMEINSOURCE '"fmterrtbl"', NATIVE_TYPE 'oid'),
	encoding integer OPTIONS (NAMEINSOURCE '"encoding"', NATIVE_TYPE 'int4'),
	writable boolean OPTIONS (NAMEINSOURCE '"writable"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_exttable"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_exttable_reloid_index" (
	reloid long OPTIONS (NAMEINSOURCE '"reloid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_exttable_reloid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace" (
	fsname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"fsname"', NATIVE_TYPE 'name'),
	fsowner long NOT NULL OPTIONS (NAMEINSOURCE '"fsowner"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace_entry" (
	fsefsoid long NOT NULL OPTIONS (NAMEINSOURCE '"fsefsoid"', NATIVE_TYPE 'oid'),
	fsedbid short NOT NULL OPTIONS (NAMEINSOURCE '"fsedbid"', NATIVE_TYPE 'int2'),
	fselocation string(2147483647) OPTIONS (NAMEINSOURCE '"fselocation"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace_entry"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace_entry_fs_index" (
	fsefsoid long OPTIONS (NAMEINSOURCE '"fsefsoid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace_entry_fs_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace_entry_fsdb_index" (
	fsefsoid long OPTIONS (NAMEINSOURCE '"fsefsoid"', NATIVE_TYPE 'oid'),
	fsedbid short OPTIONS (NAMEINSOURCE '"fsedbid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace_entry_fsdb_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace_fsname_index" (
	fsname string(2147483647) OPTIONS (NAMEINSOURCE '"fsname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace_fsname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_filespace_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_filespace_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_data_wrapper" (
	fdwname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"fdwname"', NATIVE_TYPE 'name'),
	fdwowner long NOT NULL OPTIONS (NAMEINSOURCE '"fdwowner"', NATIVE_TYPE 'oid'),
	fdwvalidator long NOT NULL OPTIONS (NAMEINSOURCE '"fdwvalidator"', NATIVE_TYPE 'oid'),
	fdwacl object OPTIONS (NAMEINSOURCE '"fdwacl"', NATIVE_TYPE '_aclitem'),
	fdwoptions object OPTIONS (NAMEINSOURCE '"fdwoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_data_wrapper"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_data_wrapper_name_index" (
	fdwname string(2147483647) OPTIONS (NAMEINSOURCE '"fdwname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_data_wrapper_name_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_data_wrapper_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_data_wrapper_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_server" (
	srvname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"srvname"', NATIVE_TYPE 'name'),
	srvowner long NOT NULL OPTIONS (NAMEINSOURCE '"srvowner"', NATIVE_TYPE 'oid'),
	srvfdw long NOT NULL OPTIONS (NAMEINSOURCE '"srvfdw"', NATIVE_TYPE 'oid'),
	srvtype string(2147483647) OPTIONS (NAMEINSOURCE '"srvtype"', NATIVE_TYPE 'text'),
	srvversion string(2147483647) OPTIONS (NAMEINSOURCE '"srvversion"', NATIVE_TYPE 'text'),
	srvacl object OPTIONS (NAMEINSOURCE '"srvacl"', NATIVE_TYPE '_aclitem'),
	srvoptions object OPTIONS (NAMEINSOURCE '"srvoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_server"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_server_name_index" (
	srvname string(2147483647) OPTIONS (NAMEINSOURCE '"srvname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_server_name_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_server_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_server_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_table" (
	reloid long NOT NULL OPTIONS (NAMEINSOURCE '"reloid"', NATIVE_TYPE 'oid'),
	server long NOT NULL OPTIONS (NAMEINSOURCE '"server"', NATIVE_TYPE 'oid'),
	tbloptions object OPTIONS (NAMEINSOURCE '"tbloptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_table"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_foreign_table_reloid_index" (
	reloid long OPTIONS (NAMEINSOURCE '"reloid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_foreign_table_reloid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_group" (
	groname string(2147483647) OPTIONS (NAMEINSOURCE '"groname"', NATIVE_TYPE 'name'),
	grosysid long OPTIONS (NAMEINSOURCE '"grosysid"', NATIVE_TYPE 'oid'),
	grolist object OPTIONS (NAMEINSOURCE '"grolist"', NATIVE_TYPE '_oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_group"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_index" (
	indexrelid long NOT NULL OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	indrelid long NOT NULL OPTIONS (NAMEINSOURCE '"indrelid"', NATIVE_TYPE 'oid'),
	indnatts short NOT NULL OPTIONS (NAMEINSOURCE '"indnatts"', NATIVE_TYPE 'int2'),
	indisunique boolean NOT NULL OPTIONS (NAMEINSOURCE '"indisunique"', NATIVE_TYPE 'bool'),
	indisprimary boolean NOT NULL OPTIONS (NAMEINSOURCE '"indisprimary"', NATIVE_TYPE 'bool'),
	indisclustered boolean NOT NULL OPTIONS (NAMEINSOURCE '"indisclustered"', NATIVE_TYPE 'bool'),
	indisvalid boolean NOT NULL OPTIONS (NAMEINSOURCE '"indisvalid"', NATIVE_TYPE 'bool'),
	indkey object NOT NULL OPTIONS (NAMEINSOURCE '"indkey"', NATIVE_TYPE 'int2vector'),
	indclass object NOT NULL OPTIONS (NAMEINSOURCE '"indclass"', NATIVE_TYPE 'oidvector'),
	indexprs string(2147483647) OPTIONS (NAMEINSOURCE '"indexprs"', NATIVE_TYPE 'text'),
	indpred string(2147483647) OPTIONS (NAMEINSOURCE '"indpred"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_index_indexrelid_index" (
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_index_indexrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_index_indrelid_index" (
	indrelid long OPTIONS (NAMEINSOURCE '"indrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_index_indrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_indexes" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	indexname string(2147483647) OPTIONS (NAMEINSOURCE '"indexname"', NATIVE_TYPE 'name'),
	tablespace string(2147483647) OPTIONS (NAMEINSOURCE '"tablespace"', NATIVE_TYPE 'name'),
	indexdef string(2147483647) OPTIONS (NAMEINSOURCE '"indexdef"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_inherits" (
	inhrelid long NOT NULL OPTIONS (NAMEINSOURCE '"inhrelid"', NATIVE_TYPE 'oid'),
	inhparent long NOT NULL OPTIONS (NAMEINSOURCE '"inhparent"', NATIVE_TYPE 'oid'),
	inhseqno integer NOT NULL OPTIONS (NAMEINSOURCE '"inhseqno"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_inherits"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_inherits_relid_seqno_index" (
	inhrelid long OPTIONS (NAMEINSOURCE '"inhrelid"', NATIVE_TYPE 'oid'),
	inhseqno integer OPTIONS (NAMEINSOURCE '"inhseqno"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_inherits_relid_seqno_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_language" (
	lanname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"lanname"', NATIVE_TYPE 'name'),
	lanispl boolean NOT NULL OPTIONS (NAMEINSOURCE '"lanispl"', NATIVE_TYPE 'bool'),
	lanpltrusted boolean NOT NULL OPTIONS (NAMEINSOURCE '"lanpltrusted"', NATIVE_TYPE 'bool'),
	lanplcallfoid long NOT NULL OPTIONS (NAMEINSOURCE '"lanplcallfoid"', NATIVE_TYPE 'oid'),
	lanvalidator long NOT NULL OPTIONS (NAMEINSOURCE '"lanvalidator"', NATIVE_TYPE 'oid'),
	lanacl object OPTIONS (NAMEINSOURCE '"lanacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_language"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_language_lanname_index" (
	lanname string(2147483647) OPTIONS (NAMEINSOURCE '"lanname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_language_lanname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_language_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_language_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_largeobject" (
	loid long NOT NULL OPTIONS (NAMEINSOURCE '"loid"', NATIVE_TYPE 'oid'),
	pageno integer NOT NULL OPTIONS (NAMEINSOURCE '"pageno"', NATIVE_TYPE 'int4'),
	data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_largeobject"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_largeobject_loid_pn_index" (
	loid long OPTIONS (NAMEINSOURCE '"loid"', NATIVE_TYPE 'oid'),
	pageno integer OPTIONS (NAMEINSOURCE '"pageno"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_largeobject_loid_pn_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_listener" (
	relname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	listenerpid integer NOT NULL OPTIONS (NAMEINSOURCE '"listenerpid"', NATIVE_TYPE 'int4'),
	notification integer NOT NULL OPTIONS (NAMEINSOURCE '"notification"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_listener"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_locks" (
	locktype string(2147483647) OPTIONS (NAMEINSOURCE '"locktype"', NATIVE_TYPE 'text'),
	database long OPTIONS (NAMEINSOURCE '"database"', NATIVE_TYPE 'oid'),
	relation long OPTIONS (NAMEINSOURCE '"relation"', NATIVE_TYPE 'oid'),
	page integer OPTIONS (NAMEINSOURCE '"page"', NATIVE_TYPE 'int4'),
	tuple short OPTIONS (NAMEINSOURCE '"tuple"', NATIVE_TYPE 'int2'),
	transactionid object OPTIONS (NAMEINSOURCE '"transactionid"', NATIVE_TYPE 'xid'),
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	objsubid short OPTIONS (NAMEINSOURCE '"objsubid"', NATIVE_TYPE 'int2'),
	transaction object OPTIONS (NAMEINSOURCE '"transaction"', NATIVE_TYPE 'xid'),
	pid integer OPTIONS (NAMEINSOURCE '"pid"', NATIVE_TYPE 'int4'),
	mode string(2147483647) OPTIONS (NAMEINSOURCE '"mode"', NATIVE_TYPE 'text'),
	granted boolean OPTIONS (NAMEINSOURCE '"granted"', NATIVE_TYPE 'bool'),
	mppsessionid integer OPTIONS (NAMEINSOURCE '"mppsessionid"', NATIVE_TYPE 'int4'),
	mppiswriter boolean OPTIONS (NAMEINSOURCE '"mppiswriter"', NATIVE_TYPE 'bool'),
	gp_segment_id integer OPTIONS (NAMEINSOURCE '"gp_segment_id"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_locks"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_max_external_files" (
	hostname string(2147483647) OPTIONS (NAMEINSOURCE '"hostname"', NATIVE_TYPE 'name'),
	maxfiles long OPTIONS (NAMEINSOURCE '"maxfiles"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_max_external_files"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_namespace" (
	nspname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"nspname"', NATIVE_TYPE 'name'),
	nspowner long NOT NULL OPTIONS (NAMEINSOURCE '"nspowner"', NATIVE_TYPE 'oid'),
	nspacl object OPTIONS (NAMEINSOURCE '"nspacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_namespace"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_namespace_nspname_index" (
	nspname string(2147483647) OPTIONS (NAMEINSOURCE '"nspname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_namespace_nspname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_namespace_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_namespace_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_opclass" (
	opcamid long NOT NULL OPTIONS (NAMEINSOURCE '"opcamid"', NATIVE_TYPE 'oid'),
	opcname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"opcname"', NATIVE_TYPE 'name'),
	opcnamespace long NOT NULL OPTIONS (NAMEINSOURCE '"opcnamespace"', NATIVE_TYPE 'oid'),
	opcowner long NOT NULL OPTIONS (NAMEINSOURCE '"opcowner"', NATIVE_TYPE 'oid'),
	opcintype long NOT NULL OPTIONS (NAMEINSOURCE '"opcintype"', NATIVE_TYPE 'oid'),
	opcdefault boolean NOT NULL OPTIONS (NAMEINSOURCE '"opcdefault"', NATIVE_TYPE 'bool'),
	opckeytype long NOT NULL OPTIONS (NAMEINSOURCE '"opckeytype"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_opclass"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_opclass_am_name_nsp_index" (
	opcamid long OPTIONS (NAMEINSOURCE '"opcamid"', NATIVE_TYPE 'oid'),
	opcname string(2147483647) OPTIONS (NAMEINSOURCE '"opcname"', NATIVE_TYPE 'name'),
	opcnamespace long OPTIONS (NAMEINSOURCE '"opcnamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_opclass_am_name_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_opclass_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_opclass_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_operator" (
	oprname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"oprname"', NATIVE_TYPE 'name'),
	oprnamespace long NOT NULL OPTIONS (NAMEINSOURCE '"oprnamespace"', NATIVE_TYPE 'oid'),
	oprowner long NOT NULL OPTIONS (NAMEINSOURCE '"oprowner"', NATIVE_TYPE 'oid'),
	oprkind string(1) NOT NULL OPTIONS (NAMEINSOURCE '"oprkind"', NATIVE_TYPE 'char'),
	oprcanhash boolean NOT NULL OPTIONS (NAMEINSOURCE '"oprcanhash"', NATIVE_TYPE 'bool'),
	oprleft long NOT NULL OPTIONS (NAMEINSOURCE '"oprleft"', NATIVE_TYPE 'oid'),
	oprright long NOT NULL OPTIONS (NAMEINSOURCE '"oprright"', NATIVE_TYPE 'oid'),
	oprresult long NOT NULL OPTIONS (NAMEINSOURCE '"oprresult"', NATIVE_TYPE 'oid'),
	oprcom long NOT NULL OPTIONS (NAMEINSOURCE '"oprcom"', NATIVE_TYPE 'oid'),
	oprnegate long NOT NULL OPTIONS (NAMEINSOURCE '"oprnegate"', NATIVE_TYPE 'oid'),
	oprlsortop long NOT NULL OPTIONS (NAMEINSOURCE '"oprlsortop"', NATIVE_TYPE 'oid'),
	oprrsortop long NOT NULL OPTIONS (NAMEINSOURCE '"oprrsortop"', NATIVE_TYPE 'oid'),
	oprltcmpop long NOT NULL OPTIONS (NAMEINSOURCE '"oprltcmpop"', NATIVE_TYPE 'oid'),
	oprgtcmpop long NOT NULL OPTIONS (NAMEINSOURCE '"oprgtcmpop"', NATIVE_TYPE 'oid'),
	oprcode object NOT NULL OPTIONS (NAMEINSOURCE '"oprcode"', NATIVE_TYPE 'regproc'),
	oprrest object NOT NULL OPTIONS (NAMEINSOURCE '"oprrest"', NATIVE_TYPE 'regproc'),
	oprjoin object NOT NULL OPTIONS (NAMEINSOURCE '"oprjoin"', NATIVE_TYPE 'regproc')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_operator"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_operator_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_operator_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_operator_oprname_l_r_n_index" (
	oprname string(2147483647) OPTIONS (NAMEINSOURCE '"oprname"', NATIVE_TYPE 'name'),
	oprleft long OPTIONS (NAMEINSOURCE '"oprleft"', NATIVE_TYPE 'oid'),
	oprright long OPTIONS (NAMEINSOURCE '"oprright"', NATIVE_TYPE 'oid'),
	oprnamespace long OPTIONS (NAMEINSOURCE '"oprnamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_operator_oprname_l_r_n_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition" (
	parrelid long NOT NULL OPTIONS (NAMEINSOURCE '"parrelid"', NATIVE_TYPE 'oid'),
	parkind string(1) NOT NULL OPTIONS (NAMEINSOURCE '"parkind"', NATIVE_TYPE 'char'),
	parlevel short NOT NULL OPTIONS (NAMEINSOURCE '"parlevel"', NATIVE_TYPE 'int2'),
	paristemplate boolean NOT NULL OPTIONS (NAMEINSOURCE '"paristemplate"', NATIVE_TYPE 'bool'),
	parnatts short NOT NULL OPTIONS (NAMEINSOURCE '"parnatts"', NATIVE_TYPE 'int2'),
	paratts object NOT NULL OPTIONS (NAMEINSOURCE '"paratts"', NATIVE_TYPE 'int2vector'),
	parclass object NOT NULL OPTIONS (NAMEINSOURCE '"parclass"', NATIVE_TYPE 'oidvector')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_columns" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	columnname string(2147483647) OPTIONS (NAMEINSOURCE '"columnname"', NATIVE_TYPE 'name'),
	partitionlevel short OPTIONS (NAMEINSOURCE '"partitionlevel"', NATIVE_TYPE 'int2'),
	position_in_partition_key integer OPTIONS (NAMEINSOURCE '"position_in_partition_key"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_columns"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_encoding" (
	parencoid long NOT NULL OPTIONS (NAMEINSOURCE '"parencoid"', NATIVE_TYPE 'oid'),
	parencattnum short NOT NULL OPTIONS (NAMEINSOURCE '"parencattnum"', NATIVE_TYPE 'int2'),
	parencattoptions object OPTIONS (NAMEINSOURCE '"parencattoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_encoding"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_encoding_parencoid_index" (
	parencoid long OPTIONS (NAMEINSOURCE '"parencoid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_encoding_parencoid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_encoding_parencoid_parencattnum_index" (
	parencoid long OPTIONS (NAMEINSOURCE '"parencoid"', NATIVE_TYPE 'oid'),
	parencattnum short OPTIONS (NAMEINSOURCE '"parencattnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_encoding_parencoid_parencattnum_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_parrelid_index" (
	parrelid long OPTIONS (NAMEINSOURCE '"parrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_parrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_parrelid_parlevel_istemplate_index" (
	parrelid long OPTIONS (NAMEINSOURCE '"parrelid"', NATIVE_TYPE 'oid'),
	parlevel short OPTIONS (NAMEINSOURCE '"parlevel"', NATIVE_TYPE 'int2'),
	paristemplate boolean OPTIONS (NAMEINSOURCE '"paristemplate"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_parrelid_parlevel_istemplate_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_rule" (
	paroid long NOT NULL OPTIONS (NAMEINSOURCE '"paroid"', NATIVE_TYPE 'oid'),
	parchildrelid long NOT NULL OPTIONS (NAMEINSOURCE '"parchildrelid"', NATIVE_TYPE 'oid'),
	parparentrule long NOT NULL OPTIONS (NAMEINSOURCE '"parparentrule"', NATIVE_TYPE 'oid'),
	parname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"parname"', NATIVE_TYPE 'name'),
	parisdefault boolean NOT NULL OPTIONS (NAMEINSOURCE '"parisdefault"', NATIVE_TYPE 'bool'),
	parruleord short NOT NULL OPTIONS (NAMEINSOURCE '"parruleord"', NATIVE_TYPE 'int2'),
	parrangestartincl boolean NOT NULL OPTIONS (NAMEINSOURCE '"parrangestartincl"', NATIVE_TYPE 'bool'),
	parrangeendincl boolean NOT NULL OPTIONS (NAMEINSOURCE '"parrangeendincl"', NATIVE_TYPE 'bool'),
	parrangestart string(2147483647) OPTIONS (NAMEINSOURCE '"parrangestart"', NATIVE_TYPE 'text'),
	parrangeend string(2147483647) OPTIONS (NAMEINSOURCE '"parrangeend"', NATIVE_TYPE 'text'),
	parrangeevery string(2147483647) OPTIONS (NAMEINSOURCE '"parrangeevery"', NATIVE_TYPE 'text'),
	parlistvalues string(2147483647) OPTIONS (NAMEINSOURCE '"parlistvalues"', NATIVE_TYPE 'text'),
	parreloptions object OPTIONS (NAMEINSOURCE '"parreloptions"', NATIVE_TYPE '_text'),
	partemplatespace long OPTIONS (NAMEINSOURCE '"partemplatespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_rule"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_rule_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_rule_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_rule_parchildrelid_index" (
	parchildrelid long OPTIONS (NAMEINSOURCE '"parchildrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_rule_parchildrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_rule_parchildrelid_parparentrule_parruleord_index" (
	parchildrelid long OPTIONS (NAMEINSOURCE '"parchildrelid"', NATIVE_TYPE 'oid'),
	parparentrule long OPTIONS (NAMEINSOURCE '"parparentrule"', NATIVE_TYPE 'oid'),
	parruleord short OPTIONS (NAMEINSOURCE '"parruleord"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_rule_parchildrelid_parparentrule_parruleord_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_rule_paroid_parentrule_ruleord_index" (
	paroid long OPTIONS (NAMEINSOURCE '"paroid"', NATIVE_TYPE 'oid'),
	parparentrule long OPTIONS (NAMEINSOURCE '"parparentrule"', NATIVE_TYPE 'oid'),
	parruleord short OPTIONS (NAMEINSOURCE '"parruleord"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_rule_paroid_parentrule_ruleord_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partition_templates" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	partitionname string(2147483647) OPTIONS (NAMEINSOURCE '"partitionname"', NATIVE_TYPE 'name'),
	partitiontype string(2147483647) OPTIONS (NAMEINSOURCE '"partitiontype"', NATIVE_TYPE 'text'),
	partitionlevel short OPTIONS (NAMEINSOURCE '"partitionlevel"', NATIVE_TYPE 'int2'),
	partitionrank long OPTIONS (NAMEINSOURCE '"partitionrank"', NATIVE_TYPE 'int8'),
	partitionposition short OPTIONS (NAMEINSOURCE '"partitionposition"', NATIVE_TYPE 'int2'),
	partitionlistvalues string(2147483647) OPTIONS (NAMEINSOURCE '"partitionlistvalues"', NATIVE_TYPE 'text'),
	partitionrangestart string(2147483647) OPTIONS (NAMEINSOURCE '"partitionrangestart"', NATIVE_TYPE 'text'),
	partitionstartinclusive boolean OPTIONS (NAMEINSOURCE '"partitionstartinclusive"', NATIVE_TYPE 'bool'),
	partitionrangeend string(2147483647) OPTIONS (NAMEINSOURCE '"partitionrangeend"', NATIVE_TYPE 'text'),
	partitionendinclusive boolean OPTIONS (NAMEINSOURCE '"partitionendinclusive"', NATIVE_TYPE 'bool'),
	partitioneveryclause string(2147483647) OPTIONS (NAMEINSOURCE '"partitioneveryclause"', NATIVE_TYPE 'text'),
	partitionisdefault boolean OPTIONS (NAMEINSOURCE '"partitionisdefault"', NATIVE_TYPE 'bool'),
	partitionboundary string(2147483647) OPTIONS (NAMEINSOURCE '"partitionboundary"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partition_templates"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_partitions" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	partitionschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"partitionschemaname"', NATIVE_TYPE 'name'),
	partitiontablename string(2147483647) OPTIONS (NAMEINSOURCE '"partitiontablename"', NATIVE_TYPE 'name'),
	partitionname string(2147483647) OPTIONS (NAMEINSOURCE '"partitionname"', NATIVE_TYPE 'name'),
	parentpartitiontablename string(2147483647) OPTIONS (NAMEINSOURCE '"parentpartitiontablename"', NATIVE_TYPE 'name'),
	parentpartitionname string(2147483647) OPTIONS (NAMEINSOURCE '"parentpartitionname"', NATIVE_TYPE 'name'),
	partitiontype string(2147483647) OPTIONS (NAMEINSOURCE '"partitiontype"', NATIVE_TYPE 'text'),
	partitionlevel short OPTIONS (NAMEINSOURCE '"partitionlevel"', NATIVE_TYPE 'int2'),
	partitionrank long OPTIONS (NAMEINSOURCE '"partitionrank"', NATIVE_TYPE 'int8'),
	partitionposition short OPTIONS (NAMEINSOURCE '"partitionposition"', NATIVE_TYPE 'int2'),
	partitionlistvalues string(2147483647) OPTIONS (NAMEINSOURCE '"partitionlistvalues"', NATIVE_TYPE 'text'),
	partitionrangestart string(2147483647) OPTIONS (NAMEINSOURCE '"partitionrangestart"', NATIVE_TYPE 'text'),
	partitionstartinclusive boolean OPTIONS (NAMEINSOURCE '"partitionstartinclusive"', NATIVE_TYPE 'bool'),
	partitionrangeend string(2147483647) OPTIONS (NAMEINSOURCE '"partitionrangeend"', NATIVE_TYPE 'text'),
	partitionendinclusive boolean OPTIONS (NAMEINSOURCE '"partitionendinclusive"', NATIVE_TYPE 'bool'),
	partitioneveryclause string(2147483647) OPTIONS (NAMEINSOURCE '"partitioneveryclause"', NATIVE_TYPE 'text'),
	partitionisdefault boolean OPTIONS (NAMEINSOURCE '"partitionisdefault"', NATIVE_TYPE 'bool'),
	partitionboundary string(2147483647) OPTIONS (NAMEINSOURCE '"partitionboundary"', NATIVE_TYPE 'text'),
	parenttablespace string(2147483647) OPTIONS (NAMEINSOURCE '"parenttablespace"', NATIVE_TYPE 'name'),
	partitiontablespace string(2147483647) OPTIONS (NAMEINSOURCE '"partitiontablespace"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_partitions"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_pltemplate" (
	tmplname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"tmplname"', NATIVE_TYPE 'name'),
	tmpltrusted boolean NOT NULL OPTIONS (NAMEINSOURCE '"tmpltrusted"', NATIVE_TYPE 'bool'),
	tmplhandler string(2147483647) OPTIONS (NAMEINSOURCE '"tmplhandler"', NATIVE_TYPE 'text'),
	tmplvalidator string(2147483647) OPTIONS (NAMEINSOURCE '"tmplvalidator"', NATIVE_TYPE 'text'),
	tmpllibrary string(2147483647) OPTIONS (NAMEINSOURCE '"tmpllibrary"', NATIVE_TYPE 'text'),
	tmplacl object OPTIONS (NAMEINSOURCE '"tmplacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_pltemplate"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_pltemplate_name_index" (
	tmplname string(2147483647) OPTIONS (NAMEINSOURCE '"tmplname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_pltemplate_name_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_prepared_statements" (
	name string(2147483647) OPTIONS (NAMEINSOURCE '"name"', NATIVE_TYPE 'text'),
	statement string(2147483647) OPTIONS (NAMEINSOURCE '"statement"', NATIVE_TYPE 'text'),
	prepare_time timestamp OPTIONS (NAMEINSOURCE '"prepare_time"', NATIVE_TYPE 'timestamptz'),
	parameter_types object OPTIONS (NAMEINSOURCE '"parameter_types"', NATIVE_TYPE '_regtype'),
	from_sql boolean OPTIONS (NAMEINSOURCE '"from_sql"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_prepared_statements"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_prepared_xacts" (
	transaction object OPTIONS (NAMEINSOURCE '"transaction"', NATIVE_TYPE 'xid'),
	gid string(2147483647) OPTIONS (NAMEINSOURCE '"gid"', NATIVE_TYPE 'text'),
	prepared timestamp OPTIONS (NAMEINSOURCE '"prepared"', NATIVE_TYPE 'timestamptz'),
	owner string(2147483647) OPTIONS (NAMEINSOURCE '"owner"', NATIVE_TYPE 'name'),
	database string(2147483647) OPTIONS (NAMEINSOURCE '"database"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_prepared_xacts"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_proc" (
	proname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"proname"', NATIVE_TYPE 'name'),
	pronamespace long NOT NULL OPTIONS (NAMEINSOURCE '"pronamespace"', NATIVE_TYPE 'oid'),
	proowner long NOT NULL OPTIONS (NAMEINSOURCE '"proowner"', NATIVE_TYPE 'oid'),
	prolang long NOT NULL OPTIONS (NAMEINSOURCE '"prolang"', NATIVE_TYPE 'oid'),
	proisagg boolean NOT NULL OPTIONS (NAMEINSOURCE '"proisagg"', NATIVE_TYPE 'bool'),
	prosecdef boolean NOT NULL OPTIONS (NAMEINSOURCE '"prosecdef"', NATIVE_TYPE 'bool'),
	proisstrict boolean NOT NULL OPTIONS (NAMEINSOURCE '"proisstrict"', NATIVE_TYPE 'bool'),
	proretset boolean NOT NULL OPTIONS (NAMEINSOURCE '"proretset"', NATIVE_TYPE 'bool'),
	provolatile string(1) NOT NULL OPTIONS (NAMEINSOURCE '"provolatile"', NATIVE_TYPE 'char'),
	pronargs short NOT NULL OPTIONS (NAMEINSOURCE '"pronargs"', NATIVE_TYPE 'int2'),
	prorettype long NOT NULL OPTIONS (NAMEINSOURCE '"prorettype"', NATIVE_TYPE 'oid'),
	proiswin boolean NOT NULL OPTIONS (NAMEINSOURCE '"proiswin"', NATIVE_TYPE 'bool'),
	proargtypes object NOT NULL OPTIONS (NAMEINSOURCE '"proargtypes"', NATIVE_TYPE 'oidvector'),
	proallargtypes object OPTIONS (NAMEINSOURCE '"proallargtypes"', NATIVE_TYPE '_oid'),
	proargmodes object OPTIONS (NAMEINSOURCE '"proargmodes"', NATIVE_TYPE '_char'),
	proargnames object OPTIONS (NAMEINSOURCE '"proargnames"', NATIVE_TYPE '_text'),
	prosrc string(2147483647) OPTIONS (NAMEINSOURCE '"prosrc"', NATIVE_TYPE 'text'),
	probin varbinary(2147483647) OPTIONS (NAMEINSOURCE '"probin"', NATIVE_TYPE 'bytea'),
	proacl object OPTIONS (NAMEINSOURCE '"proacl"', NATIVE_TYPE '_aclitem')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_proc"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_proc_callback" (
	profnoid object NOT NULL OPTIONS (NAMEINSOURCE '"profnoid"', NATIVE_TYPE 'regproc'),
	procallback object NOT NULL OPTIONS (NAMEINSOURCE '"procallback"', NATIVE_TYPE 'regproc'),
	promethod string(1) NOT NULL OPTIONS (NAMEINSOURCE '"promethod"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_proc_callback"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_proc_callback_profnoid_promethod_index" (
	profnoid object OPTIONS (NAMEINSOURCE '"profnoid"', NATIVE_TYPE 'regproc'),
	promethod string(1) OPTIONS (NAMEINSOURCE '"promethod"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_proc_callback_profnoid_promethod_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_proc_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_proc_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_proc_proname_args_nsp_index" (
	proname string(2147483647) OPTIONS (NAMEINSOURCE '"proname"', NATIVE_TYPE 'name'),
	proargtypes object OPTIONS (NAMEINSOURCE '"proargtypes"', NATIVE_TYPE 'oidvector'),
	pronamespace long OPTIONS (NAMEINSOURCE '"pronamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_proc_proname_args_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resourcetype" (
	resname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"resname"', NATIVE_TYPE 'name'),
	restypid short NOT NULL OPTIONS (NAMEINSOURCE '"restypid"', NATIVE_TYPE 'int2'),
	resrequired boolean NOT NULL OPTIONS (NAMEINSOURCE '"resrequired"', NATIVE_TYPE 'bool'),
	reshasdefault boolean NOT NULL OPTIONS (NAMEINSOURCE '"reshasdefault"', NATIVE_TYPE 'bool'),
	reshasdisable boolean NOT NULL OPTIONS (NAMEINSOURCE '"reshasdisable"', NATIVE_TYPE 'bool'),
	resdefaultsetting string(2147483647) OPTIONS (NAMEINSOURCE '"resdefaultsetting"', NATIVE_TYPE 'text'),
	resdisabledsetting string(2147483647) OPTIONS (NAMEINSOURCE '"resdisabledsetting"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resourcetype"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resourcetype_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resourcetype_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resourcetype_resname_index" (
	resname string(2147483647) OPTIONS (NAMEINSOURCE '"resname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resourcetype_resname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resourcetype_restypid_index" (
	restypid short OPTIONS (NAMEINSOURCE '"restypid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resourcetype_restypid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueue" (
	rsqname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"rsqname"', NATIVE_TYPE 'name'),
	rsqcountlimit float NOT NULL OPTIONS (NAMEINSOURCE '"rsqcountlimit"', NATIVE_TYPE 'float4'),
	rsqcostlimit float NOT NULL OPTIONS (NAMEINSOURCE '"rsqcostlimit"', NATIVE_TYPE 'float4'),
	rsqovercommit boolean NOT NULL OPTIONS (NAMEINSOURCE '"rsqovercommit"', NATIVE_TYPE 'bool'),
	rsqignorecostlimit float NOT NULL OPTIONS (NAMEINSOURCE '"rsqignorecostlimit"', NATIVE_TYPE 'float4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueue"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueue_attributes" (
	rsqname string(2147483647) OPTIONS (NAMEINSOURCE '"rsqname"', NATIVE_TYPE 'name'),
	resname string(2147483647) OPTIONS (NAMEINSOURCE '"resname"', NATIVE_TYPE 'text'),
	ressetting string(2147483647) OPTIONS (NAMEINSOURCE '"ressetting"', NATIVE_TYPE 'text'),
	restypid integer OPTIONS (NAMEINSOURCE '"restypid"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueue_attributes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueue_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueue_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueue_rsqname_index" (
	rsqname string(2147483647) OPTIONS (NAMEINSOURCE '"rsqname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueue_rsqname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueue_status" (
	rsqname string(2147483647) OPTIONS (NAMEINSOURCE '"rsqname"', NATIVE_TYPE 'name'),
	rsqcountlimit float OPTIONS (NAMEINSOURCE '"rsqcountlimit"', NATIVE_TYPE 'float4'),
	rsqcountvalue float OPTIONS (NAMEINSOURCE '"rsqcountvalue"', NATIVE_TYPE 'float4'),
	rsqcostlimit float OPTIONS (NAMEINSOURCE '"rsqcostlimit"', NATIVE_TYPE 'float4'),
	rsqcostvalue float OPTIONS (NAMEINSOURCE '"rsqcostvalue"', NATIVE_TYPE 'float4'),
	rsqwaiters integer OPTIONS (NAMEINSOURCE '"rsqwaiters"', NATIVE_TYPE 'int4'),
	rsqholders integer OPTIONS (NAMEINSOURCE '"rsqholders"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueue_status"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueuecapability" (
	resqueueid long NOT NULL OPTIONS (NAMEINSOURCE '"resqueueid"', NATIVE_TYPE 'oid'),
	restypid short NOT NULL OPTIONS (NAMEINSOURCE '"restypid"', NATIVE_TYPE 'int2'),
	ressetting string(2147483647) OPTIONS (NAMEINSOURCE '"ressetting"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueuecapability"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueuecapability_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueuecapability_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueuecapability_resqueueid_index" (
	resqueueid long OPTIONS (NAMEINSOURCE '"resqueueid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueuecapability_resqueueid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_resqueuecapability_restypid_index" (
	restypid short OPTIONS (NAMEINSOURCE '"restypid"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_resqueuecapability_restypid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_rewrite" (
	rulename string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"rulename"', NATIVE_TYPE 'name'),
	ev_class long NOT NULL OPTIONS (NAMEINSOURCE '"ev_class"', NATIVE_TYPE 'oid'),
	ev_attr short NOT NULL OPTIONS (NAMEINSOURCE '"ev_attr"', NATIVE_TYPE 'int2'),
	ev_type string(1) NOT NULL OPTIONS (NAMEINSOURCE '"ev_type"', NATIVE_TYPE 'char'),
	is_instead boolean NOT NULL OPTIONS (NAMEINSOURCE '"is_instead"', NATIVE_TYPE 'bool'),
	ev_qual string(2147483647) OPTIONS (NAMEINSOURCE '"ev_qual"', NATIVE_TYPE 'text'),
	ev_action string(2147483647) OPTIONS (NAMEINSOURCE '"ev_action"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_rewrite"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_rewrite_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_rewrite_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_rewrite_rel_rulename_index" (
	ev_class long OPTIONS (NAMEINSOURCE '"ev_class"', NATIVE_TYPE 'oid'),
	rulename string(2147483647) OPTIONS (NAMEINSOURCE '"rulename"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_rewrite_rel_rulename_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_roles" (
	rolname string(2147483647) OPTIONS (NAMEINSOURCE '"rolname"', NATIVE_TYPE 'name'),
	rolsuper boolean OPTIONS (NAMEINSOURCE '"rolsuper"', NATIVE_TYPE 'bool'),
	rolinherit boolean OPTIONS (NAMEINSOURCE '"rolinherit"', NATIVE_TYPE 'bool'),
	rolcreaterole boolean OPTIONS (NAMEINSOURCE '"rolcreaterole"', NATIVE_TYPE 'bool'),
	rolcreatedb boolean OPTIONS (NAMEINSOURCE '"rolcreatedb"', NATIVE_TYPE 'bool'),
	rolcatupdate boolean OPTIONS (NAMEINSOURCE '"rolcatupdate"', NATIVE_TYPE 'bool'),
	rolcanlogin boolean OPTIONS (NAMEINSOURCE '"rolcanlogin"', NATIVE_TYPE 'bool'),
	rolconnlimit integer OPTIONS (NAMEINSOURCE '"rolconnlimit"', NATIVE_TYPE 'int4'),
	rolpassword string(2147483647) OPTIONS (NAMEINSOURCE '"rolpassword"', NATIVE_TYPE 'text'),
	rolvaliduntil timestamp OPTIONS (NAMEINSOURCE '"rolvaliduntil"', NATIVE_TYPE 'timestamptz'),
	rolconfig object OPTIONS (NAMEINSOURCE '"rolconfig"', NATIVE_TYPE '_text'),
	rolresqueue long OPTIONS (NAMEINSOURCE '"rolresqueue"', NATIVE_TYPE 'oid'),
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid'),
	rolcreaterextgpfd boolean OPTIONS (NAMEINSOURCE '"rolcreaterextgpfd"', NATIVE_TYPE 'bool'),
	rolcreaterexthttp boolean OPTIONS (NAMEINSOURCE '"rolcreaterexthttp"', NATIVE_TYPE 'bool'),
	rolcreatewextgpfd boolean OPTIONS (NAMEINSOURCE '"rolcreatewextgpfd"', NATIVE_TYPE 'bool'),
	rolcreaterexthdfs boolean OPTIONS (NAMEINSOURCE '"rolcreaterexthdfs"', NATIVE_TYPE 'bool'),
	rolcreatewexthdfs boolean OPTIONS (NAMEINSOURCE '"rolcreatewexthdfs"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_roles"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_rules" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	rulename string(2147483647) OPTIONS (NAMEINSOURCE '"rulename"', NATIVE_TYPE 'name'),
	definition string(2147483647) OPTIONS (NAMEINSOURCE '"definition"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_rules"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_settings" (
	name string(2147483647) OPTIONS (NAMEINSOURCE '"name"', NATIVE_TYPE 'text'),
	setting string(2147483647) OPTIONS (NAMEINSOURCE '"setting"', NATIVE_TYPE 'text'),
	unit string(2147483647) OPTIONS (NAMEINSOURCE '"unit"', NATIVE_TYPE 'text'),
	category string(2147483647) OPTIONS (NAMEINSOURCE '"category"', NATIVE_TYPE 'text'),
	short_desc string(2147483647) OPTIONS (NAMEINSOURCE '"short_desc"', NATIVE_TYPE 'text'),
	extra_desc string(2147483647) OPTIONS (NAMEINSOURCE '"extra_desc"', NATIVE_TYPE 'text'),
	context string(2147483647) OPTIONS (NAMEINSOURCE '"context"', NATIVE_TYPE 'text'),
	vartype string(2147483647) OPTIONS (NAMEINSOURCE '"vartype"', NATIVE_TYPE 'text'),
	source string(2147483647) OPTIONS (NAMEINSOURCE '"source"', NATIVE_TYPE 'text'),
	min_val string(2147483647) OPTIONS (NAMEINSOURCE '"min_val"', NATIVE_TYPE 'text'),
	max_val string(2147483647) OPTIONS (NAMEINSOURCE '"max_val"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_settings"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shadow" (
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	usesysid long OPTIONS (NAMEINSOURCE '"usesysid"', NATIVE_TYPE 'oid'),
	usecreatedb boolean OPTIONS (NAMEINSOURCE '"usecreatedb"', NATIVE_TYPE 'bool'),
	usesuper boolean OPTIONS (NAMEINSOURCE '"usesuper"', NATIVE_TYPE 'bool'),
	usecatupd boolean OPTIONS (NAMEINSOURCE '"usecatupd"', NATIVE_TYPE 'bool'),
	passwd string(2147483647) OPTIONS (NAMEINSOURCE '"passwd"', NATIVE_TYPE 'text'),
	valuntil object OPTIONS (NAMEINSOURCE '"valuntil"', NATIVE_TYPE 'abstime'),
	useconfig object OPTIONS (NAMEINSOURCE '"useconfig"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shadow"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shdepend" (
	dbid long NOT NULL OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'oid'),
	classid long NOT NULL OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long NOT NULL OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	refclassid long NOT NULL OPTIONS (NAMEINSOURCE '"refclassid"', NATIVE_TYPE 'oid'),
	refobjid long NOT NULL OPTIONS (NAMEINSOURCE '"refobjid"', NATIVE_TYPE 'oid'),
	deptype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"deptype"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shdepend"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shdepend_depender_index" (
	dbid long OPTIONS (NAMEINSOURCE '"dbid"', NATIVE_TYPE 'oid'),
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shdepend_depender_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shdepend_reference_index" (
	refclassid long OPTIONS (NAMEINSOURCE '"refclassid"', NATIVE_TYPE 'oid'),
	refobjid long OPTIONS (NAMEINSOURCE '"refobjid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shdepend_reference_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shdescription" (
	objoid long NOT NULL OPTIONS (NAMEINSOURCE '"objoid"', NATIVE_TYPE 'oid'),
	classoid long NOT NULL OPTIONS (NAMEINSOURCE '"classoid"', NATIVE_TYPE 'oid'),
	description string(2147483647) OPTIONS (NAMEINSOURCE '"description"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shdescription"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_shdescription_o_c_index" (
	objoid long OPTIONS (NAMEINSOURCE '"objoid"', NATIVE_TYPE 'oid'),
	classoid long OPTIONS (NAMEINSOURCE '"classoid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_shdescription_o_c_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_activity" (
	datid long OPTIONS (NAMEINSOURCE '"datid"', NATIVE_TYPE 'oid'),
	datname string(2147483647) OPTIONS (NAMEINSOURCE '"datname"', NATIVE_TYPE 'name'),
	procpid integer OPTIONS (NAMEINSOURCE '"procpid"', NATIVE_TYPE 'int4'),
	sess_id integer OPTIONS (NAMEINSOURCE '"sess_id"', NATIVE_TYPE 'int4'),
	usesysid long OPTIONS (NAMEINSOURCE '"usesysid"', NATIVE_TYPE 'oid'),
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	current_query string(2147483647) OPTIONS (NAMEINSOURCE '"current_query"', NATIVE_TYPE 'text'),
	waiting boolean OPTIONS (NAMEINSOURCE '"waiting"', NATIVE_TYPE 'bool'),
	query_start timestamp OPTIONS (NAMEINSOURCE '"query_start"', NATIVE_TYPE 'timestamptz'),
	backend_start timestamp OPTIONS (NAMEINSOURCE '"backend_start"', NATIVE_TYPE 'timestamptz'),
	client_addr object OPTIONS (NAMEINSOURCE '"client_addr"', NATIVE_TYPE 'inet'),
	client_port integer OPTIONS (NAMEINSOURCE '"client_port"', NATIVE_TYPE 'int4'),
	application_name string(2147483647) OPTIONS (NAMEINSOURCE '"application_name"', NATIVE_TYPE 'text'),
	xact_start timestamp OPTIONS (NAMEINSOURCE '"xact_start"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_activity"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_all_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_read long OPTIONS (NAMEINSOURCE '"idx_tup_read"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_all_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_all_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	seq_scan long OPTIONS (NAMEINSOURCE '"seq_scan"', NATIVE_TYPE 'int8'),
	seq_tup_read long OPTIONS (NAMEINSOURCE '"seq_tup_read"', NATIVE_TYPE 'int8'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8'),
	n_tup_ins long OPTIONS (NAMEINSOURCE '"n_tup_ins"', NATIVE_TYPE 'int8'),
	n_tup_upd long OPTIONS (NAMEINSOURCE '"n_tup_upd"', NATIVE_TYPE 'int8'),
	n_tup_del long OPTIONS (NAMEINSOURCE '"n_tup_del"', NATIVE_TYPE 'int8'),
	last_vacuum timestamp OPTIONS (NAMEINSOURCE '"last_vacuum"', NATIVE_TYPE 'timestamptz'),
	last_autovacuum timestamp OPTIONS (NAMEINSOURCE '"last_autovacuum"', NATIVE_TYPE 'timestamptz'),
	last_analyze timestamp OPTIONS (NAMEINSOURCE '"last_analyze"', NATIVE_TYPE 'timestamptz'),
	last_autoanalyze timestamp OPTIONS (NAMEINSOURCE '"last_autoanalyze"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_all_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_database" (
	datid long OPTIONS (NAMEINSOURCE '"datid"', NATIVE_TYPE 'oid'),
	datname string(2147483647) OPTIONS (NAMEINSOURCE '"datname"', NATIVE_TYPE 'name'),
	numbackends integer OPTIONS (NAMEINSOURCE '"numbackends"', NATIVE_TYPE 'int4'),
	xact_commit long OPTIONS (NAMEINSOURCE '"xact_commit"', NATIVE_TYPE 'int8'),
	xact_rollback long OPTIONS (NAMEINSOURCE '"xact_rollback"', NATIVE_TYPE 'int8'),
	blks_read long OPTIONS (NAMEINSOURCE '"blks_read"', NATIVE_TYPE 'int8'),
	blks_hit long OPTIONS (NAMEINSOURCE '"blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_database"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_last_operation" (
	classid long NOT NULL OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long NOT NULL OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	staactionname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"staactionname"', NATIVE_TYPE 'name'),
	stasysid long NOT NULL OPTIONS (NAMEINSOURCE '"stasysid"', NATIVE_TYPE 'oid'),
	stausename string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"stausename"', NATIVE_TYPE 'name'),
	stasubtype string(2147483647) OPTIONS (NAMEINSOURCE '"stasubtype"', NATIVE_TYPE 'text'),
	statime timestamp OPTIONS (NAMEINSOURCE '"statime"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_last_operation"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_last_shoperation" (
	classid long NOT NULL OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long NOT NULL OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	staactionname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"staactionname"', NATIVE_TYPE 'name'),
	stasysid long NOT NULL OPTIONS (NAMEINSOURCE '"stasysid"', NATIVE_TYPE 'oid'),
	stausename string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"stausename"', NATIVE_TYPE 'name'),
	stasubtype string(2147483647) OPTIONS (NAMEINSOURCE '"stasubtype"', NATIVE_TYPE 'text'),
	statime timestamp OPTIONS (NAMEINSOURCE '"statime"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_last_shoperation"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_operations" (
	classname string(2147483647) OPTIONS (NAMEINSOURCE '"classname"', NATIVE_TYPE 'text'),
	objname string(2147483647) OPTIONS (NAMEINSOURCE '"objname"', NATIVE_TYPE 'name'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	usestatus string(2147483647) OPTIONS (NAMEINSOURCE '"usestatus"', NATIVE_TYPE 'text'),
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	actionname string(2147483647) OPTIONS (NAMEINSOURCE '"actionname"', NATIVE_TYPE 'name'),
	subtype string(2147483647) OPTIONS (NAMEINSOURCE '"subtype"', NATIVE_TYPE 'text'),
	statime timestamp OPTIONS (NAMEINSOURCE '"statime"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_operations"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_partition_operations" (
	classname string(2147483647) OPTIONS (NAMEINSOURCE '"classname"', NATIVE_TYPE 'text'),
	objname string(2147483647) OPTIONS (NAMEINSOURCE '"objname"', NATIVE_TYPE 'name'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	usestatus string(2147483647) OPTIONS (NAMEINSOURCE '"usestatus"', NATIVE_TYPE 'text'),
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	actionname string(2147483647) OPTIONS (NAMEINSOURCE '"actionname"', NATIVE_TYPE 'name'),
	subtype string(2147483647) OPTIONS (NAMEINSOURCE '"subtype"', NATIVE_TYPE 'text'),
	statime timestamp OPTIONS (NAMEINSOURCE '"statime"', NATIVE_TYPE 'timestamptz'),
	partitionlevel short OPTIONS (NAMEINSOURCE '"partitionlevel"', NATIVE_TYPE 'int2'),
	parenttablename string(2147483647) OPTIONS (NAMEINSOURCE '"parenttablename"', NATIVE_TYPE 'name'),
	parentschemaname string(2147483647) OPTIONS (NAMEINSOURCE '"parentschemaname"', NATIVE_TYPE 'name'),
	parent_relid long OPTIONS (NAMEINSOURCE '"parent_relid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_partition_operations"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_resqueues" (
	queueid long OPTIONS (NAMEINSOURCE '"queueid"', NATIVE_TYPE 'oid'),
	queuename string(2147483647) OPTIONS (NAMEINSOURCE '"queuename"', NATIVE_TYPE 'name'),
	n_queries_exec long OPTIONS (NAMEINSOURCE '"n_queries_exec"', NATIVE_TYPE 'int8'),
	n_queries_wait long OPTIONS (NAMEINSOURCE '"n_queries_wait"', NATIVE_TYPE 'int8'),
	elapsed_exec long OPTIONS (NAMEINSOURCE '"elapsed_exec"', NATIVE_TYPE 'int8'),
	elapsed_wait long OPTIONS (NAMEINSOURCE '"elapsed_wait"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_resqueues"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_sys_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_read long OPTIONS (NAMEINSOURCE '"idx_tup_read"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_sys_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_sys_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	seq_scan long OPTIONS (NAMEINSOURCE '"seq_scan"', NATIVE_TYPE 'int8'),
	seq_tup_read long OPTIONS (NAMEINSOURCE '"seq_tup_read"', NATIVE_TYPE 'int8'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8'),
	n_tup_ins long OPTIONS (NAMEINSOURCE '"n_tup_ins"', NATIVE_TYPE 'int8'),
	n_tup_upd long OPTIONS (NAMEINSOURCE '"n_tup_upd"', NATIVE_TYPE 'int8'),
	n_tup_del long OPTIONS (NAMEINSOURCE '"n_tup_del"', NATIVE_TYPE 'int8'),
	last_vacuum timestamp OPTIONS (NAMEINSOURCE '"last_vacuum"', NATIVE_TYPE 'timestamptz'),
	last_autovacuum timestamp OPTIONS (NAMEINSOURCE '"last_autovacuum"', NATIVE_TYPE 'timestamptz'),
	last_analyze timestamp OPTIONS (NAMEINSOURCE '"last_analyze"', NATIVE_TYPE 'timestamptz'),
	last_autoanalyze timestamp OPTIONS (NAMEINSOURCE '"last_autoanalyze"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_sys_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_user_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_read long OPTIONS (NAMEINSOURCE '"idx_tup_read"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_user_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stat_user_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	seq_scan long OPTIONS (NAMEINSOURCE '"seq_scan"', NATIVE_TYPE 'int8'),
	seq_tup_read long OPTIONS (NAMEINSOURCE '"seq_tup_read"', NATIVE_TYPE 'int8'),
	idx_scan long OPTIONS (NAMEINSOURCE '"idx_scan"', NATIVE_TYPE 'int8'),
	idx_tup_fetch long OPTIONS (NAMEINSOURCE '"idx_tup_fetch"', NATIVE_TYPE 'int8'),
	n_tup_ins long OPTIONS (NAMEINSOURCE '"n_tup_ins"', NATIVE_TYPE 'int8'),
	n_tup_upd long OPTIONS (NAMEINSOURCE '"n_tup_upd"', NATIVE_TYPE 'int8'),
	n_tup_del long OPTIONS (NAMEINSOURCE '"n_tup_del"', NATIVE_TYPE 'int8'),
	last_vacuum timestamp OPTIONS (NAMEINSOURCE '"last_vacuum"', NATIVE_TYPE 'timestamptz'),
	last_autovacuum timestamp OPTIONS (NAMEINSOURCE '"last_autovacuum"', NATIVE_TYPE 'timestamptz'),
	last_analyze timestamp OPTIONS (NAMEINSOURCE '"last_analyze"', NATIVE_TYPE 'timestamptz'),
	last_autoanalyze timestamp OPTIONS (NAMEINSOURCE '"last_autoanalyze"', NATIVE_TYPE 'timestamptz')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stat_user_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_all_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_all_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_all_sequences" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	blks_read long OPTIONS (NAMEINSOURCE '"blks_read"', NATIVE_TYPE 'int8'),
	blks_hit long OPTIONS (NAMEINSOURCE '"blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_all_sequences"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_all_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	heap_blks_read long OPTIONS (NAMEINSOURCE '"heap_blks_read"', NATIVE_TYPE 'int8'),
	heap_blks_hit long OPTIONS (NAMEINSOURCE '"heap_blks_hit"', NATIVE_TYPE 'int8'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8'),
	toast_blks_read long OPTIONS (NAMEINSOURCE '"toast_blks_read"', NATIVE_TYPE 'int8'),
	toast_blks_hit long OPTIONS (NAMEINSOURCE '"toast_blks_hit"', NATIVE_TYPE 'int8'),
	tidx_blks_read long OPTIONS (NAMEINSOURCE '"tidx_blks_read"', NATIVE_TYPE 'int8'),
	tidx_blks_hit long OPTIONS (NAMEINSOURCE '"tidx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_all_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_sys_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_sys_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_sys_sequences" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	blks_read long OPTIONS (NAMEINSOURCE '"blks_read"', NATIVE_TYPE 'int8'),
	blks_hit long OPTIONS (NAMEINSOURCE '"blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_sys_sequences"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_sys_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	heap_blks_read long OPTIONS (NAMEINSOURCE '"heap_blks_read"', NATIVE_TYPE 'int8'),
	heap_blks_hit long OPTIONS (NAMEINSOURCE '"heap_blks_hit"', NATIVE_TYPE 'int8'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8'),
	toast_blks_read long OPTIONS (NAMEINSOURCE '"toast_blks_read"', NATIVE_TYPE 'int8'),
	toast_blks_hit long OPTIONS (NAMEINSOURCE '"toast_blks_hit"', NATIVE_TYPE 'int8'),
	tidx_blks_read long OPTIONS (NAMEINSOURCE '"tidx_blks_read"', NATIVE_TYPE 'int8'),
	tidx_blks_hit long OPTIONS (NAMEINSOURCE '"tidx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_sys_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_user_indexes" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	indexrelid long OPTIONS (NAMEINSOURCE '"indexrelid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	indexrelname string(2147483647) OPTIONS (NAMEINSOURCE '"indexrelname"', NATIVE_TYPE 'name'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_user_indexes"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_user_sequences" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	blks_read long OPTIONS (NAMEINSOURCE '"blks_read"', NATIVE_TYPE 'int8'),
	blks_hit long OPTIONS (NAMEINSOURCE '"blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_user_sequences"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statio_user_tables" (
	relid long OPTIONS (NAMEINSOURCE '"relid"', NATIVE_TYPE 'oid'),
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	relname string(2147483647) OPTIONS (NAMEINSOURCE '"relname"', NATIVE_TYPE 'name'),
	heap_blks_read long OPTIONS (NAMEINSOURCE '"heap_blks_read"', NATIVE_TYPE 'int8'),
	heap_blks_hit long OPTIONS (NAMEINSOURCE '"heap_blks_hit"', NATIVE_TYPE 'int8'),
	idx_blks_read long OPTIONS (NAMEINSOURCE '"idx_blks_read"', NATIVE_TYPE 'int8'),
	idx_blks_hit long OPTIONS (NAMEINSOURCE '"idx_blks_hit"', NATIVE_TYPE 'int8'),
	toast_blks_read long OPTIONS (NAMEINSOURCE '"toast_blks_read"', NATIVE_TYPE 'int8'),
	toast_blks_hit long OPTIONS (NAMEINSOURCE '"toast_blks_hit"', NATIVE_TYPE 'int8'),
	tidx_blks_read long OPTIONS (NAMEINSOURCE '"tidx_blks_read"', NATIVE_TYPE 'int8'),
	tidx_blks_hit long OPTIONS (NAMEINSOURCE '"tidx_blks_hit"', NATIVE_TYPE 'int8')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statio_user_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statistic" (
	starelid long NOT NULL OPTIONS (NAMEINSOURCE '"starelid"', NATIVE_TYPE 'oid'),
	staattnum short NOT NULL OPTIONS (NAMEINSOURCE '"staattnum"', NATIVE_TYPE 'int2'),
	stanullfrac float NOT NULL OPTIONS (NAMEINSOURCE '"stanullfrac"', NATIVE_TYPE 'float4'),
	stawidth integer NOT NULL OPTIONS (NAMEINSOURCE '"stawidth"', NATIVE_TYPE 'int4'),
	stadistinct float NOT NULL OPTIONS (NAMEINSOURCE '"stadistinct"', NATIVE_TYPE 'float4'),
	stakind1 short NOT NULL OPTIONS (NAMEINSOURCE '"stakind1"', NATIVE_TYPE 'int2'),
	stakind2 short NOT NULL OPTIONS (NAMEINSOURCE '"stakind2"', NATIVE_TYPE 'int2'),
	stakind3 short NOT NULL OPTIONS (NAMEINSOURCE '"stakind3"', NATIVE_TYPE 'int2'),
	stakind4 short NOT NULL OPTIONS (NAMEINSOURCE '"stakind4"', NATIVE_TYPE 'int2'),
	staop1 long NOT NULL OPTIONS (NAMEINSOURCE '"staop1"', NATIVE_TYPE 'oid'),
	staop2 long NOT NULL OPTIONS (NAMEINSOURCE '"staop2"', NATIVE_TYPE 'oid'),
	staop3 long NOT NULL OPTIONS (NAMEINSOURCE '"staop3"', NATIVE_TYPE 'oid'),
	staop4 long NOT NULL OPTIONS (NAMEINSOURCE '"staop4"', NATIVE_TYPE 'oid'),
	stanumbers1 object OPTIONS (NAMEINSOURCE '"stanumbers1"', NATIVE_TYPE '_float4'),
	stanumbers2 object OPTIONS (NAMEINSOURCE '"stanumbers2"', NATIVE_TYPE '_float4'),
	stanumbers3 object OPTIONS (NAMEINSOURCE '"stanumbers3"', NATIVE_TYPE '_float4'),
	stanumbers4 object OPTIONS (NAMEINSOURCE '"stanumbers4"', NATIVE_TYPE '_float4'),
	stavalues1 object OPTIONS (NAMEINSOURCE '"stavalues1"', NATIVE_TYPE 'anyarray'),
	stavalues2 object OPTIONS (NAMEINSOURCE '"stavalues2"', NATIVE_TYPE 'anyarray'),
	stavalues3 object OPTIONS (NAMEINSOURCE '"stavalues3"', NATIVE_TYPE 'anyarray'),
	stavalues4 object OPTIONS (NAMEINSOURCE '"stavalues4"', NATIVE_TYPE 'anyarray')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statistic"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statistic_relid_att_index" (
	starelid long OPTIONS (NAMEINSOURCE '"starelid"', NATIVE_TYPE 'oid'),
	staattnum short OPTIONS (NAMEINSOURCE '"staattnum"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statistic_relid_att_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statlastop_classid_objid_index" (
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statlastop_classid_objid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statlastop_classid_objid_staactionname_index" (
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	staactionname string(2147483647) OPTIONS (NAMEINSOURCE '"staactionname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statlastop_classid_objid_staactionname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statlastshop_classid_objid_index" (
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statlastshop_classid_objid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_statlastshop_classid_objid_staactionname_index" (
	classid long OPTIONS (NAMEINSOURCE '"classid"', NATIVE_TYPE 'oid'),
	objid long OPTIONS (NAMEINSOURCE '"objid"', NATIVE_TYPE 'oid'),
	staactionname string(2147483647) OPTIONS (NAMEINSOURCE '"staactionname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_statlastshop_classid_objid_staactionname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_stats" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	attname string(2147483647) OPTIONS (NAMEINSOURCE '"attname"', NATIVE_TYPE 'name'),
	null_frac float OPTIONS (NAMEINSOURCE '"null_frac"', NATIVE_TYPE 'float4'),
	avg_width integer OPTIONS (NAMEINSOURCE '"avg_width"', NATIVE_TYPE 'int4'),
	n_distinct float OPTIONS (NAMEINSOURCE '"n_distinct"', NATIVE_TYPE 'float4'),
	most_common_vals object OPTIONS (NAMEINSOURCE '"most_common_vals"', NATIVE_TYPE 'anyarray'),
	most_common_freqs object OPTIONS (NAMEINSOURCE '"most_common_freqs"', NATIVE_TYPE '_float4'),
	histogram_bounds object OPTIONS (NAMEINSOURCE '"histogram_bounds"', NATIVE_TYPE 'anyarray'),
	correlation float OPTIONS (NAMEINSOURCE '"correlation"', NATIVE_TYPE 'float4')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_stats"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_tables" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	tablename string(2147483647) OPTIONS (NAMEINSOURCE '"tablename"', NATIVE_TYPE 'name'),
	tableowner string(2147483647) OPTIONS (NAMEINSOURCE '"tableowner"', NATIVE_TYPE 'name'),
	tablespace string(2147483647) OPTIONS (NAMEINSOURCE '"tablespace"', NATIVE_TYPE 'name'),
	hasindexes boolean OPTIONS (NAMEINSOURCE '"hasindexes"', NATIVE_TYPE 'bool'),
	hasrules boolean OPTIONS (NAMEINSOURCE '"hasrules"', NATIVE_TYPE 'bool'),
	hastriggers boolean OPTIONS (NAMEINSOURCE '"hastriggers"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_tables"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_tablespace" (
	spcname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"spcname"', NATIVE_TYPE 'name'),
	spcowner long NOT NULL OPTIONS (NAMEINSOURCE '"spcowner"', NATIVE_TYPE 'oid'),
	spclocation string(2147483647) OPTIONS (NAMEINSOURCE '"spclocation"', NATIVE_TYPE 'text'),
	spcacl object OPTIONS (NAMEINSOURCE '"spcacl"', NATIVE_TYPE '_aclitem'),
	spcprilocations object OPTIONS (NAMEINSOURCE '"spcprilocations"', NATIVE_TYPE '_text'),
	spcmirlocations object OPTIONS (NAMEINSOURCE '"spcmirlocations"', NATIVE_TYPE '_text'),
	spcfsoid long OPTIONS (NAMEINSOURCE '"spcfsoid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_tablespace"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_tablespace_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_tablespace_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_tablespace_spcname_index" (
	spcname string(2147483647) OPTIONS (NAMEINSOURCE '"spcname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_tablespace_spcname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_timezone_abbrevs" (
	abbrev string(2147483647) OPTIONS (NAMEINSOURCE '"abbrev"', NATIVE_TYPE 'text'),
	utc_offset object(49) OPTIONS (NAMEINSOURCE '"utc_offset"', NATIVE_TYPE 'interval'),
	is_dst boolean OPTIONS (NAMEINSOURCE '"is_dst"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_timezone_abbrevs"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_timezone_names" (
	name string(2147483647) OPTIONS (NAMEINSOURCE '"name"', NATIVE_TYPE 'text'),
	abbrev string(2147483647) OPTIONS (NAMEINSOURCE '"abbrev"', NATIVE_TYPE 'text'),
	utc_offset object(49) OPTIONS (NAMEINSOURCE '"utc_offset"', NATIVE_TYPE 'interval'),
	is_dst boolean OPTIONS (NAMEINSOURCE '"is_dst"', NATIVE_TYPE 'bool')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_timezone_names"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_trigger" (
	tgrelid long NOT NULL OPTIONS (NAMEINSOURCE '"tgrelid"', NATIVE_TYPE 'oid'),
	tgname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"tgname"', NATIVE_TYPE 'name'),
	tgfoid long NOT NULL OPTIONS (NAMEINSOURCE '"tgfoid"', NATIVE_TYPE 'oid'),
	tgtype short NOT NULL OPTIONS (NAMEINSOURCE '"tgtype"', NATIVE_TYPE 'int2'),
	tgenabled boolean NOT NULL OPTIONS (NAMEINSOURCE '"tgenabled"', NATIVE_TYPE 'bool'),
	tgisconstraint boolean NOT NULL OPTIONS (NAMEINSOURCE '"tgisconstraint"', NATIVE_TYPE 'bool'),
	tgconstrname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"tgconstrname"', NATIVE_TYPE 'name'),
	tgconstrrelid long NOT NULL OPTIONS (NAMEINSOURCE '"tgconstrrelid"', NATIVE_TYPE 'oid'),
	tgdeferrable boolean NOT NULL OPTIONS (NAMEINSOURCE '"tgdeferrable"', NATIVE_TYPE 'bool'),
	tginitdeferred boolean NOT NULL OPTIONS (NAMEINSOURCE '"tginitdeferred"', NATIVE_TYPE 'bool'),
	tgnargs short NOT NULL OPTIONS (NAMEINSOURCE '"tgnargs"', NATIVE_TYPE 'int2'),
	tgattr object NOT NULL OPTIONS (NAMEINSOURCE '"tgattr"', NATIVE_TYPE 'int2vector'),
	tgargs varbinary(2147483647) OPTIONS (NAMEINSOURCE '"tgargs"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_trigger"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_trigger_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_trigger_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_trigger_tgconstrname_index" (
	tgconstrname string(2147483647) OPTIONS (NAMEINSOURCE '"tgconstrname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_trigger_tgconstrname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_trigger_tgconstrrelid_index" (
	tgconstrrelid long OPTIONS (NAMEINSOURCE '"tgconstrrelid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_trigger_tgconstrrelid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_trigger_tgrelid_tgname_index" (
	tgrelid long OPTIONS (NAMEINSOURCE '"tgrelid"', NATIVE_TYPE 'oid'),
	tgname string(2147483647) OPTIONS (NAMEINSOURCE '"tgname"', NATIVE_TYPE 'name')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_trigger_tgrelid_tgname_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_type" (
	typname string(2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"typname"', NATIVE_TYPE 'name'),
	typnamespace long NOT NULL OPTIONS (NAMEINSOURCE '"typnamespace"', NATIVE_TYPE 'oid'),
	typowner long NOT NULL OPTIONS (NAMEINSOURCE '"typowner"', NATIVE_TYPE 'oid'),
	typlen short NOT NULL OPTIONS (NAMEINSOURCE '"typlen"', NATIVE_TYPE 'int2'),
	typbyval boolean NOT NULL OPTIONS (NAMEINSOURCE '"typbyval"', NATIVE_TYPE 'bool'),
	typtype string(1) NOT NULL OPTIONS (NAMEINSOURCE '"typtype"', NATIVE_TYPE 'char'),
	typisdefined boolean NOT NULL OPTIONS (NAMEINSOURCE '"typisdefined"', NATIVE_TYPE 'bool'),
	typdelim string(1) NOT NULL OPTIONS (NAMEINSOURCE '"typdelim"', NATIVE_TYPE 'char'),
	typrelid long NOT NULL OPTIONS (NAMEINSOURCE '"typrelid"', NATIVE_TYPE 'oid'),
	typelem long NOT NULL OPTIONS (NAMEINSOURCE '"typelem"', NATIVE_TYPE 'oid'),
	typinput object NOT NULL OPTIONS (NAMEINSOURCE '"typinput"', NATIVE_TYPE 'regproc'),
	typoutput object NOT NULL OPTIONS (NAMEINSOURCE '"typoutput"', NATIVE_TYPE 'regproc'),
	typreceive object NOT NULL OPTIONS (NAMEINSOURCE '"typreceive"', NATIVE_TYPE 'regproc'),
	typsend object NOT NULL OPTIONS (NAMEINSOURCE '"typsend"', NATIVE_TYPE 'regproc'),
	typanalyze object NOT NULL OPTIONS (NAMEINSOURCE '"typanalyze"', NATIVE_TYPE 'regproc'),
	typalign string(1) NOT NULL OPTIONS (NAMEINSOURCE '"typalign"', NATIVE_TYPE 'char'),
	typstorage string(1) NOT NULL OPTIONS (NAMEINSOURCE '"typstorage"', NATIVE_TYPE 'char'),
	typnotnull boolean NOT NULL OPTIONS (NAMEINSOURCE '"typnotnull"', NATIVE_TYPE 'bool'),
	typbasetype long NOT NULL OPTIONS (NAMEINSOURCE '"typbasetype"', NATIVE_TYPE 'oid'),
	typtypmod integer NOT NULL OPTIONS (NAMEINSOURCE '"typtypmod"', NATIVE_TYPE 'int4'),
	typndims integer NOT NULL OPTIONS (NAMEINSOURCE '"typndims"', NATIVE_TYPE 'int4'),
	typdefaultbin string(2147483647) OPTIONS (NAMEINSOURCE '"typdefaultbin"', NATIVE_TYPE 'text'),
	typdefault string(2147483647) OPTIONS (NAMEINSOURCE '"typdefault"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_type"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_type_encoding" (
	typid long NOT NULL OPTIONS (NAMEINSOURCE '"typid"', NATIVE_TYPE 'oid'),
	typoptions object OPTIONS (NAMEINSOURCE '"typoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_type_encoding"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_type_encoding_typid_index" (
	typid long OPTIONS (NAMEINSOURCE '"typid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_type_encoding_typid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_type_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_type_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_type_typname_nsp_index" (
	typname string(2147483647) OPTIONS (NAMEINSOURCE '"typname"', NATIVE_TYPE 'name'),
	typnamespace long OPTIONS (NAMEINSOURCE '"typnamespace"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_type_typname_nsp_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_user" (
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	usesysid long OPTIONS (NAMEINSOURCE '"usesysid"', NATIVE_TYPE 'oid'),
	usecreatedb boolean OPTIONS (NAMEINSOURCE '"usecreatedb"', NATIVE_TYPE 'bool'),
	usesuper boolean OPTIONS (NAMEINSOURCE '"usesuper"', NATIVE_TYPE 'bool'),
	usecatupd boolean OPTIONS (NAMEINSOURCE '"usecatupd"', NATIVE_TYPE 'bool'),
	passwd string(2147483647) OPTIONS (NAMEINSOURCE '"passwd"', NATIVE_TYPE 'text'),
	valuntil object OPTIONS (NAMEINSOURCE '"valuntil"', NATIVE_TYPE 'abstime'),
	useconfig object OPTIONS (NAMEINSOURCE '"useconfig"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_user"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_user_mapping" (
	umuser long NOT NULL OPTIONS (NAMEINSOURCE '"umuser"', NATIVE_TYPE 'oid'),
	umserver long NOT NULL OPTIONS (NAMEINSOURCE '"umserver"', NATIVE_TYPE 'oid'),
	umoptions object OPTIONS (NAMEINSOURCE '"umoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_user_mapping"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_user_mapping_oid_index" (
	oid long OPTIONS (NAMEINSOURCE '"oid"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_user_mapping_oid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_user_mapping_user_server_index" (
	umuser long OPTIONS (NAMEINSOURCE '"umuser"', NATIVE_TYPE 'oid'),
	umserver long OPTIONS (NAMEINSOURCE '"umserver"', NATIVE_TYPE 'oid')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_user_mapping_user_server_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_user_mappings" (
	umid long OPTIONS (NAMEINSOURCE '"umid"', NATIVE_TYPE 'oid'),
	srvid long OPTIONS (NAMEINSOURCE '"srvid"', NATIVE_TYPE 'oid'),
	srvname string(2147483647) OPTIONS (NAMEINSOURCE '"srvname"', NATIVE_TYPE 'name'),
	umuser long OPTIONS (NAMEINSOURCE '"umuser"', NATIVE_TYPE 'oid'),
	usename string(2147483647) OPTIONS (NAMEINSOURCE '"usename"', NATIVE_TYPE 'name'),
	umoptions object OPTIONS (NAMEINSOURCE '"umoptions"', NATIVE_TYPE '_text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_user_mappings"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_views" (
	schemaname string(2147483647) OPTIONS (NAMEINSOURCE '"schemaname"', NATIVE_TYPE 'name'),
	viewname string(2147483647) OPTIONS (NAMEINSOURCE '"viewname"', NATIVE_TYPE 'name'),
	viewowner string(2147483647) OPTIONS (NAMEINSOURCE '"viewowner"', NATIVE_TYPE 'name'),
	definition string(2147483647) OPTIONS (NAMEINSOURCE '"definition"', NATIVE_TYPE 'text')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_views"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_window" (
	winfnoid object NOT NULL OPTIONS (NAMEINSOURCE '"winfnoid"', NATIVE_TYPE 'regproc'),
	winrequireorder boolean NOT NULL OPTIONS (NAMEINSOURCE '"winrequireorder"', NATIVE_TYPE 'bool'),
	winallowframe boolean NOT NULL OPTIONS (NAMEINSOURCE '"winallowframe"', NATIVE_TYPE 'bool'),
	winpeercount boolean NOT NULL OPTIONS (NAMEINSOURCE '"winpeercount"', NATIVE_TYPE 'bool'),
	wincount boolean NOT NULL OPTIONS (NAMEINSOURCE '"wincount"', NATIVE_TYPE 'bool'),
	winfunc object NOT NULL OPTIONS (NAMEINSOURCE '"winfunc"', NATIVE_TYPE 'regproc'),
	winprefunc object NOT NULL OPTIONS (NAMEINSOURCE '"winprefunc"', NATIVE_TYPE 'regproc'),
	winpretype long NOT NULL OPTIONS (NAMEINSOURCE '"winpretype"', NATIVE_TYPE 'oid'),
	winfinfunc object NOT NULL OPTIONS (NAMEINSOURCE '"winfinfunc"', NATIVE_TYPE 'regproc'),
	winkind string(1) NOT NULL OPTIONS (NAMEINSOURCE '"winkind"', NATIVE_TYPE 'char')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_window"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_catalog.pg_window_fnoid_index" (
	winfnoid object OPTIONS (NAMEINSOURCE '"winfnoid"', NATIVE_TYPE 'regproc')
) OPTIONS (NAMEINSOURCE '"pg_catalog"."pg_window_fnoid_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10775" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10775"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10775_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10775_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10780" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10780"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10780_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10780_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10785" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10785"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10785_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10785_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10790" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10790"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10790_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10790_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10795" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10795"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10795_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10795_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10800" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10800"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10800_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10800_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10805" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10805"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_10805_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_10805_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1255" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1255"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1255_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1255_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1260" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1260"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1260_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1260_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1262" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1262"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_1262_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_1262_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17004" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17004"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17004_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17004_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17029" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17029"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17029_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17029_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17054" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17054"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17054_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17054_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17079" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17079"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17079_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17079_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17104" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17104"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17104_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17104_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17129" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17129"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17129_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17129_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17154" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17154"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17154_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17154_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17179" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17179"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_17179_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_17179_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2396" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2396"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2396_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2396_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2604" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2604"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2604_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2604_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2606" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2606"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2606_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2606_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2609" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2609"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2609_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2609_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2618" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2618"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2618_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2618_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2619" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2619"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_2619_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_2619_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_3220" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_3220"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_3220_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_3220_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_3231" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_3231"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_3231_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_3231_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_5033" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_5033"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_5033_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_5033_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_5036" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_5036"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_5036_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_5036_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_9903" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4'),
	chunk_data varbinary(2147483647) OPTIONS (NAMEINSOURCE '"chunk_data"', NATIVE_TYPE 'bytea')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_9903"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "pg_toast.pg_toast_9903_index" (
	chunk_id long OPTIONS (NAMEINSOURCE '"chunk_id"', NATIVE_TYPE 'oid'),
	chunk_seq integer OPTIONS (NAMEINSOURCE '"chunk_seq"', NATIVE_TYPE 'int4')
) OPTIONS (NAMEINSOURCE '"pg_toast"."pg_toast_9903_index"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.e1" (
	e1 bigdecimal(5,2147483647) OPTIONS (NAMEINSOURCE '"e1"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"public"."e1"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.g1" (
	e1 bigdecimal(5,2147483647) NOT NULL OPTIONS (NAMEINSOURCE '"e1"', NATIVE_TYPE 'numeric'),
	e2 string(50) OPTIONS (NAMEINSOURCE '"e2"', NATIVE_TYPE 'varchar')
) OPTIONS (NAMEINSOURCE '"public"."g1"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.g1_pkey" (
	e1 bigdecimal(5,2147483647) OPTIONS (NAMEINSOURCE '"e1"', NATIVE_TYPE 'numeric')
) OPTIONS (NAMEINSOURCE '"public"."g1_pkey"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.g2" (
	e1 bigdecimal(5,2147483647) OPTIONS (NAMEINSOURCE '"e1"', NATIVE_TYPE 'numeric'),
	e2 string(50) OPTIONS (NAMEINSOURCE '"e2"', NATIVE_TYPE 'varchar')
) OPTIONS (NAMEINSOURCE '"public"."g2"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.hugea" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."hugea"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.hugeb" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."hugeb"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.largea" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."largea"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.largeb" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."largeb"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.mediuma" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."mediuma"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.mediumb" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."mediumb"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.smalla" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."smalla"', UPDATABLE TRUE);

CREATE FOREIGN TABLE "public.smallb" (
	doublenum float OPTIONS (NAMEINSOURCE '"doublenum"', NATIVE_TYPE 'float4'),
	booleanvalue boolean OPTIONS (NAMEINSOURCE '"booleanvalue"', NATIVE_TYPE 'bool'),
	intnum integer OPTIONS (NAMEINSOURCE '"intnum"', NATIVE_TYPE 'int4'),
	longnum long OPTIONS (NAMEINSOURCE '"longnum"', NATIVE_TYPE 'int8'),
	floatnum float OPTIONS (NAMEINSOURCE '"floatnum"', NATIVE_TYPE 'float4'),
	bytenum bigdecimal(8,2147483647) OPTIONS (NAMEINSOURCE '"bytenum"', NATIVE_TYPE 'numeric'),
	stringkey string(20) OPTIONS (NAMEINSOURCE '"stringkey"', NATIVE_TYPE 'varchar'),
	objectvalue varbinary(2147483647) OPTIONS (NAMEINSOURCE '"objectvalue"', NATIVE_TYPE 'bytea'),
	stringnum string(20) OPTIONS (NAMEINSOURCE '"stringnum"', NATIVE_TYPE 'varchar'),
	bigintegervalue bigdecimal(38,2147483647) OPTIONS (NAMEINSOURCE '"bigintegervalue"', NATIVE_TYPE 'numeric'),
	datevalue date OPTIONS (NAMEINSOURCE '"datevalue"', NATIVE_TYPE 'date'),
	charvalue string(1) OPTIONS (NAMEINSOURCE '"charvalue"', NATIVE_TYPE 'bpchar'),
	timevalue time OPTIONS (NAMEINSOURCE '"timevalue"', NATIVE_TYPE 'time'),
	bigdecimalvalue bigdecimal(15,2147483647) OPTIONS (NAMEINSOURCE '"bigdecimalvalue"', NATIVE_TYPE 'numeric'),
	timestampvalue timestamp OPTIONS (NAMEINSOURCE '"timestampvalue"', NATIVE_TYPE 'timestamp'),
	intkey integer OPTIONS (NAMEINSOURCE '"intkey"', NATIVE_TYPE 'int4'),
	shortvalue short OPTIONS (NAMEINSOURCE '"shortvalue"', NATIVE_TYPE 'int2')
) OPTIONS (NAMEINSOURCE '"public"."smallb"', UPDATABLE TRUE);