/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.ddl.dialect.mysql;

import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class MySqlDdlLexicon extends StandardDdlLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/mysql/1.0";
        public static final String PREFIX = "mysqlddl";
    }

    // MIXINS

    public static final String TYPE_CREATE_DEFINER_STATEMENT = Namespace.PREFIX + ":createDefinerStatement";
    public static final String TYPE_CREATE_EVENT_STATEMENT = Namespace.PREFIX + ":createEventStatement";
    public static final String TYPE_CREATE_FUNCTION_STATEMENT = Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_INDEX_STATEMENT = Namespace.PREFIX + ":createIndexStatement";
    public static final String TYPE_CREATE_PROCEDURE_STATEMENT = Namespace.PREFIX + ":createProcedureStatement";
    public static final String TYPE_CREATE_SERVER_STATEMENT = Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_TABLESPACE_STATEMENT = Namespace.PREFIX + ":createTablespaceStatement";
    public static final String TYPE_CREATE_TRIGGER_STATEMENT = Namespace.PREFIX + ":createTriggerStatement";

    public static final String TYPE_DROP_DATABASE_STATEMENT = Namespace.PREFIX + ":dropDatabaseStatement";
    public static final String TYPE_DROP_EVENT_STATEMENT = Namespace.PREFIX + ":dropEventStatement";
    public static final String TYPE_DROP_FUNCTION_STATEMENT = Namespace.PREFIX + ":dropFunctionStatement";
    public static final String TYPE_DROP_INDEX_STATEMENT = Namespace.PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_LOGFILE_GROUP_STATEMENT = Namespace.PREFIX + ":dropLogfileGroupStatement";
    public static final String TYPE_DROP_PROCEDURE_STATEMENT = Namespace.PREFIX + ":dropProcedureStatement";
    public static final String TYPE_DROP_SERVER_STATEMENT = Namespace.PREFIX + ":dropServerStatement";
    public static final String TYPE_DROP_TABLESPACE_STATEMENT = Namespace.PREFIX + ":dropTablespaceStatement";
    public static final String TYPE_DROP_TRIGGER_STATEMENT = Namespace.PREFIX + ":dropTriggerStatement";

    public static final String TYPE_ALTER_ALGORITHM_STATEMENT = Namespace.PREFIX + ":alterAlgorithmStatement";
    public static final String TYPE_ALTER_DATABASE_STATEMENT = Namespace.PREFIX + ":alterDatabaseStatement";
    public static final String TYPE_ALTER_DEFINER_STATEMENT = Namespace.PREFIX + ":alterDefinerStatement";
    public static final String TYPE_ALTER_EVENT_STATEMENT = Namespace.PREFIX + ":alterEventStatement";
    public static final String TYPE_ALTER_FUNCTION_STATEMENT = Namespace.PREFIX + ":alterFunctionStatement";
    public static final String TYPE_ALTER_LOGFILE_GROUP_STATEMENT = Namespace.PREFIX + ":alterLogfileGroupStatement";
    public static final String TYPE_ALTER_PROCEDURE_STATEMENT = Namespace.PREFIX + ":alterProcedureStatement";
    public static final String TYPE_ALTER_SERVER_STATEMENT = Namespace.PREFIX + ":alterServerStatement";
    public static final String TYPE_ALTER_SCHEMA_STATEMENT = Namespace.PREFIX + ":alterSchemaStatement";
    public static final String TYPE_ALTER_TABLESPACE_STATEMENT = Namespace.PREFIX + ":alterTablespaceStatement";
    public static final String TYPE_ALTER_VIEW_STATEMENT = Namespace.PREFIX + ":alterViewStatement";

    public static final String TYPE_RENAME_DATABASE_STATEMENT = Namespace.PREFIX + ":renameDatabaseStatement";
    public static final String TYPE_RENAME_SCHEMA_STATEMENT = Namespace.PREFIX + ":renameSchemaStatement";
    public static final String TYPE_RENAME_TABLE_STATEMENT = Namespace.PREFIX + ":renameTableStatement";

}
