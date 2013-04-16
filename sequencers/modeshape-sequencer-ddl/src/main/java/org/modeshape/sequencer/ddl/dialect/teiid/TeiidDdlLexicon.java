/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
