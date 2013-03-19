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
            _schemaChildTypes = new String[] {AlterOptions.TABLE_NODE_TYPE, AlterOptions.VIEW_NODE_TYPE,
                AlterOptions.PROCEDURE_NODE_TYPE, CreateProcedure.FUNCTION_NODE_TYPE, CreateProcedure.PROCEDURE_NODE_TYPE,
                CreateTable.TABLE_NODE_TYPE, CreateTable.VIEW_NODE_TYPE, CreateTrigger.NODE_TYPE};
        }

        return _schemaChildTypes;
    }

    /**
     * JCR names related to the alter options DDL statement.
     */
    interface AlterOptions {

        /**
         * The node type name for an alter column clause.
         */
        String COLUMN_NODE_TYPE = Namespace.PREFIX + ":alterColumn";

        /**
         * The property name for the dropped schema elements.
         */
        String DROPPED = Namespace.PREFIX + ":dropped";

        /**
         * The node type name for a list of altered options.
         */
        String OPTIONS_LIST_NODE_TYPE = Namespace.PREFIX + ":alterOptionsList";

        /**
         * The node type name for an alter parameter clause.
         */
        String PARAMETER_NODE_TYPE = Namespace.PREFIX + ":alterParameter";

        /**
         * The node type name for an alter procedure DDL statement.
         */
        String PROCEDURE_NODE_TYPE = Namespace.PREFIX + ":alterProcedure";

        /**
         * The column, parameter, or table reference property name.
         */
        String REFERENCE = Namespace.PREFIX + ":reference";

        /**
         * The node type name for an alter table DDL statement.
         */
        String TABLE_NODE_TYPE = Namespace.PREFIX + ":alterTable";

        /**
         * The node type name for an alter view DDL statement.
         */
        String VIEW_NODE_TYPE = Namespace.PREFIX + ":alterView";

    }

    /**
     * JCR names for DDL constraint-related elements.
     */
    interface Constraint {

        /**
         * The expression property name.
         */
        String EXPRESSION = Namespace.PREFIX + ":expression";

        /**
         * The table element constraint node type name.
         */
        String EXPRESSION_NODE_TYPE = Namespace.PREFIX + ":expressionConstraint";

        /**
         * The table element references property name.
         */
        String REFERENCES = Namespace.PREFIX + ":tableElementRefs";

        /**
         * The table element constraint node type name.
         */
        String TABLE_ELEMENT_NODE_TYPE = Namespace.PREFIX + ":tableElementConstraint";

        /**
         * The table element constraint's references node type name.
         */
        String TABLE_ELEMENT_REFERENCES_NODE_TYPE = Namespace.PREFIX + ":tableElementReferencesConstraint";

        /**
         * The references table reference property name.
         */
        String TABLE_REFERENCE = Namespace.PREFIX + ":tableRef";

        /**
         * The table element references property name.
         */
        String TABLE_REFERENCE_REFERENCES = Namespace.PREFIX + ":referencesTableElementRefs";

        /**
         * The constraint type property name.
         */
        String TYPE = Namespace.PREFIX + ":constraintType";

    }

    /**
     * JCR names related to the create procedure DDL statement.
     */
    interface CreateProcedure {

        /**
         * The name of the property that indicates if a result column can be <code>null</code>.
         */
        String CAN_BE_NULL = Namespace.PREFIX + ":canBeNull";

        /**
         * The node type name for a create function statement.
         */
        String FUNCTION_NODE_TYPE = Namespace.PREFIX + ":createFunction";

        /**
         * The node type name for a create procedure parameter.
         */
        String PARAMETER_NODE_TYPE = Namespace.PREFIX + ":procedureParameter";

        /**
         * The name of the procedure parameter result flag property.
         */
        String PARAMETER_RESULT_FLAG = Namespace.PREFIX + ":result";

        /**
         * The name of the procedure parameter type property.
         */
        String PARAMETER_TYPE = Namespace.PREFIX + ":parameterType";

        /**
         * The node type name for a create procedure statement.
         */
        String PROCEDURE_NODE_TYPE = Namespace.PREFIX + ":createProcedure";

        /**
         * The node type name of a result column.
         */
        String RESULT_COLUMN_NODE_TYPE = Namespace.PREFIX + ":resultColumn";

        /**
         * The node type name of the result set that contains result columns.
         */
        String RESULT_COLUMNS_NODE_TYPE = Namespace.PREFIX + ":resultColumns";

        /**
         * The node type name of the result set that contains one unnamed data type.
         */
        String RESULT_DATA_TYPE_NODE_TYPE = Namespace.PREFIX + ":resultDataType";

        /**
         * A property name for a result set.
         */
        String RESULT_SET = Namespace.PREFIX + ":resultSet";

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
    interface CreateTable {

        /**
         * The auto-increment property name of a table element.
         */
        String AUTO_INCREMENT = Namespace.PREFIX + ":autoIncrement";

        /**
         * The name of the property that indicates if a column can be <code>null</code>.
         */
        String CAN_BE_NULL = Namespace.PREFIX + ":canBeNull";

        /**
         * The property name for create schema (table, view) statement.
         */
        String QUERY_EXPRESSION = Namespace.PREFIX + ":queryExpression";

        /**
         * The node type name for a table element.
         */
        String TABLE_ELEMENT_NODE_TYPE = Namespace.PREFIX + ":tableElement";

        /**
         * The node type name for a create table statement.
         */
        String TABLE_NODE_TYPE = Namespace.PREFIX + ":createTable";

        /**
         * The node type name for a create view statement.
         */
        String VIEW_NODE_TYPE = Namespace.PREFIX + ":createView";

    }

    /**
     * JCR names related to the create trigger DDL statement.
     */
    interface CreateTrigger {

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
         * A create trigger DDL statement node type name.
         */
        String NODE_TYPE = Namespace.PREFIX + ":createTrigger";

        /**
         * The table reference property name.
         */
        String TABLE_REFERENCE = Namespace.PREFIX + ":tableRef";

        /**
         * A trigger row action node type name.
         */
        String TRIGGER_ROW_ACTION = Namespace.PREFIX + ":triggerRowAction";

    }

    /**
     * The JCR Teiid namespace mapping.
     */
    public interface Namespace {

        String PREFIX = "teiidddl";
        String URI = "http://www.modeshape.org/ddl/teiid/1.0";

    }

    interface SchemaElement {

        String MIXIN = Namespace.PREFIX + ':' + "schemaElement";
        String TYPE = Namespace.PREFIX + ':' + "schemaElementType";

    }

}
