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
package org.modeshape.sequencer.ddl.dialect.teiid;

import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 * The DDL lexicon for the Teiid DDL dialect.
 */
public class TeiidDdlLexicon extends StandardDdlLexicon implements TeiidDdlConstants {

    private static String[] _schemaChildTypes;

    static String[] getValidSchemaChildTypes() {
        if (_schemaChildTypes == null) {
            _schemaChildTypes = new String[] {AlterOptions.TABLE_STATEMENT, AlterOptions.VIEW_STATEMENT,
                AlterOptions.PROCEDURE_STATEMENT, CreateProcedure.FUNCTION_STATEMENT, CreateProcedure.PROCEDURE_STATEMENT,
                CreateTable.TABLE_STATEMENT, CreateTable.VIEW_STATEMENT, CreateTrigger.STATEMENT};
        }

        return _schemaChildTypes;
    }

    /**
     * JCR names related to the alter options DDL statement.
     */
    public interface AlterOptions {

        /**
         * The child node name for the element being altered (name is not prefixed).
         */
        String ALTERS = "alters";

        /**
         * The mixin name for an alter column clause.
         */
        String COLUMN = Namespace.PREFIX + ":alterColumn";

        /**
         * The property name for the dropped schema elements.
         */
        String DROPPED = Namespace.PREFIX + ":dropped";

        /**
         * The mixin name for a list of altered options.
         */
        String OPTIONS_LIST = Namespace.PREFIX + ":alterOptionsList";

        /**
         * The mixin name for an alter parameter clause.
         */
        String PARAMETER = Namespace.PREFIX + ":alterParameter";

        /**
         * The mixin name for an alter procedure DDL statement.
         */
        String PROCEDURE_STATEMENT = Namespace.PREFIX + ":alterProcedure";

        /**
         * The column, parameter, or table reference property name.
         */
        String REFERENCE = Namespace.PREFIX + ":reference";

        /**
         * The mixin name for an alter table DDL statement.
         */
        String TABLE_STATEMENT = Namespace.PREFIX + ":alterTable";

        /**
         * The mixin name for an alter view DDL statement.
         */
        String VIEW_STATEMENT = Namespace.PREFIX + ":alterView";

    }

    /**
     * JCR names related to the teiid materialized options.
     */
    public interface MaterializedOptions {

        /**
         * Whether table is materialized
         */
        String MATERIALIZED = "MATERIALIZED";

        /**
         * Materialized table
         */
        String MATERIALIZED_TABLE = "MATERIALIZED_TABLE";

        /**
         * Whether table is updateable
         */
        String UPDATABLE = "UPDATABLE";

        /**
         * Allow Teiid based management
         */
        String ALLOW_MATVIEW_MANAGEMENT = "teiid_rel:ALLOW_MATVIEW_MANAGEMENT";

        /**
         * Fully qualified Status Table Name defined above
         */
        String MATVIEW_STATUS_TABLE = "teiid_rel:MATVIEW_STATUS_TABLE";

        /**
         * Semi-colon(;) separated DDL/DML commands to run
         * before the actual load of the cache, typically used to
         * truncate staging table
         */
        String MATVIEW_BEFORE_LOAD_SCRIPT = "teiid_rel:MATVIEW_BEFORE_LOAD_SCRIPT";

        /**
         * semi-colon(;) separated DDL/DML commands to run for
         * loading of the cache
         */
        String MATVIEW_LOAD_SCRIPT = "teiid_rel:MATVIEW_LOAD_SCRIPT";

        /**
         * semi-colon(;) separated DDL/DML commands to run after
         * the actual load of the cache. Typically used to rename
         * staging table to actual cache table. Required when
         * MATVIEW_LOAD_SCRIPT not defined to copy data from
         * teiid_rel:MATVIEW_STAGE_TABLE to MATVIEW table
         */
        String MATVIEW_AFTER_LOAD_SCRIPT = "teiid_rel:MATVIEW_AFTER_LOAD_SCRIPT";

        /**
         * Allowed values are {NONE, VDB, SCHEMA}, which define
         * if the cached contents are shared among different VDB
         * versions and different VDBs as long as schema names match
         */
        String MATVIEW_SHARE_SCOPE = "teiid_rel:MATVIEW_SHARE_SCOPE";

        /**
         * When MATVIEW_LOAD_SCRIPT property not defined, Teiid
         * loads the cache contents into this table.
         * Required when MATVIEW_LOAD_SCRIPT not defined
         */
        String MATERIALIZED_STAGE_TABLE = "teiid_rel:MATERIALIZED_STAGE_TABLE";

        /**
         * DML commands to run start of vdb
         */
        String ON_VDB_START_SCRIPT = "teiid_rel:ON_VDB_START_SCRIPT";

        /**
         * DML commands to run at VDB un-deploy; typically
         * used for cleaning the cache/status tables
         */
        String ON_VDB_DROP_SCRIPT = "teiid_rel:ON_VDB_DROP_SCRIPT";

        /**
         * Action to be taken when mat view contents are
         * requested but cache is invalid. Allowed values
         * are
         *
         * THROW_EXCEPTION = throws an exception,
         * IGNORE = ignores the warning and supplied invalidated data,
         * WAIT = waits until the data is refreshed and valid
         *              then provides the updated data)
         */
        String MATVIEW_ONERROR_ACTION = "teiid_rel:MATVIEW_ONERROR_ACTION";

        /**
         * Time to live in milliseconds. Provide property or
         * cache hint on view transformation - property takes
         * precedence.
         */
        String MATVIEW_TTL = "teiid_rel:MATVIEW_TTL";
    }

    /**
     * JCR names for DDL constraint-related elements.
     */
    public interface Constraint {

        /**
         * The expression property name used in index constraints.
         */
        String EXPRESSION = Namespace.PREFIX + ":expression";

        /**
         * The table element foreign key constraint mixin name.
         */
        String FOREIGN_KEY_CONSTRAINT = Namespace.PREFIX + ":foreignKeyConstraint";

        /**
         * The table element index constraint mixin name.
         */
        String INDEX_CONSTRAINT = Namespace.PREFIX + ":indexConstraint";

        /**
         * The table element references property name.
         */
        String REFERENCES = Namespace.PREFIX + ":tableElementRefs";

        /**
         * The table element constraint mixin name.
         */
        String TABLE_ELEMENT = Namespace.PREFIX + ":tableElementConstraint";

        /**
         * The references table reference property name.
         */
        String TABLE_REFERENCE = Namespace.PREFIX + ":tableRef";

        /**
         * The table element references property name.
         */
        String TABLE_REFERENCE_REFERENCES = Namespace.PREFIX + ":tableRefElementRefs";

        /**
         * The constraint type property name.
         */
        String TYPE = Namespace.PREFIX + ":constraintType";

    }

    /**
     * JCR names related to the create procedure DDL statement.
     */
    public interface CreateProcedure {

        /**
         * The mixin name for a create function statement.
         */
        String FUNCTION_STATEMENT = Namespace.PREFIX + ":createFunction";

        /**
         * The mixin name for a create procedure parameter.
         */
        String PARAMETER = Namespace.PREFIX + ":procedureParameter";

        /**
         * The name of the procedure parameter result flag property.
         */
        String PARAMETER_RESULT_FLAG = Namespace.PREFIX + ":result";

        /**
         * The name of the procedure parameter type property.
         */
        String PARAMETER_TYPE = Namespace.PREFIX + ":parameterType";

        /**
         * The mixin name for a create procedure statement.
         */
        String PROCEDURE_STATEMENT = Namespace.PREFIX + ":createProcedure";

        /**
         * The mixin name of a result column.
         */
        String RESULT_COLUMN = Namespace.PREFIX + ":resultColumn";

        /**
         * The mixin name of the result set that contains result columns.
         */
        String RESULT_COLUMNS = Namespace.PREFIX + ":resultColumns";

        /**
         * The mixin name of the result set that contains one unnamed data type.
         */
        String RESULT_DATA_TYPE = Namespace.PREFIX + ":resultDataType";

        /**
         * The child node name for a result set (name is not prefixed).
         */
        String RESULT_SET = "resultSet";

        /**
         * The abstract mixin name for a result set.
         */
        String RESULT_SET_NODE_TYPE = Namespace.PREFIX + ":resultSet";

        /**
         * The name of the procedure statement property.
         */
        String STATEMENT = Namespace.PREFIX + ":statement";

        /**
         * The name of the procedure result columns table flag property.
         */
        String TABLE_FLAG = Namespace.PREFIX + ":table";

    }

    /**
     * JCR names related to the create table DDL statement.
     */
    public interface CreateTable {

        /**
         * The auto-increment property name of a table element.
         */
        String AUTO_INCREMENT = Namespace.PREFIX + ":autoIncrement";

        /**
         * The property name for the query expression.
         */
        String QUERY_EXPRESSION = Namespace.PREFIX + ":queryExpression";

        /**
         * The mixin name for a table element.
         */
        String TABLE_ELEMENT = Namespace.PREFIX + ":tableElement";

        /**
         * The mixin name for a create table statement.
         */
        String TABLE_STATEMENT = Namespace.PREFIX + ":createTable";

        /**
         * The mixin name for a create view statement.
         */
        String VIEW_STATEMENT = Namespace.PREFIX + ":createView";

    }

    /**
     * JCR names related to the create trigger DDL statement.
     */
    public interface CreateTrigger {

        /**
         * A property for a trigger row action that defines the action.
         */
        String ACTION = Namespace.PREFIX + ":action";

        /**
         * A property for a trigger row action.
         */
        String ATOMIC = Namespace.PREFIX + ":atomic";

        /**
         * A property for a create trigger DDL statement that indicates if an insert, delete, or update.
         */
        String INSTEAD_OF = Namespace.PREFIX + ":insteadOf";

        /**
         * A trigger row action child node name (name is not prefixed).
         */
        String ROW_ACTION = "rowAction";

        /**
         * A create trigger DDL statement mixin name.
         */
        String STATEMENT = Namespace.PREFIX + ":createTrigger";

        /**
         * The table reference property name.
         */
        String TABLE_REFERENCE = Namespace.PREFIX + ":tableRef";

        /**
         * A trigger row action mixin name.
         */
        String TRIGGER_ROW_ACTION = Namespace.PREFIX + ":triggerRowAction";

    }

    /**
     * JCR names related to the option namespace DDL statement.
     */
    public interface OptionNamespace {

        /**
         * An option namespace DDL statement mixin name.
         */
        String STATEMENT = Namespace.PREFIX + ":optionNamespace";

        /**
         * The namespace URI property name.
         */
        String URI = Namespace.PREFIX + ":uri";

    }

    /**
     * The JCR Teiid namespace mapping.
     */
    public interface Namespace {

        String PREFIX = "teiidddl";
        String URI = "http://www.modeshape.org/ddl/teiid/1.0";

    }

    public interface SchemaElement {

        String MIXIN = Namespace.PREFIX + ':' + "schemaElement";
        String TYPE = Namespace.PREFIX + ':' + "schemaElementType";

    }

}
