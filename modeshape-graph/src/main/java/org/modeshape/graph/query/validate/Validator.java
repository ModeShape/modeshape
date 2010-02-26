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
package org.modeshape.graph.query.validate;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.AllNodes;
import org.modeshape.graph.query.model.ArithmeticOperand;
import org.modeshape.graph.query.model.ChildNode;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.DescendantNode;
import org.modeshape.graph.query.model.DescendantNodeJoinCondition;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.Visitor;
import org.modeshape.graph.query.model.Visitors.AbstractVisitor;
import org.modeshape.graph.query.validate.Schemata.Table;

/**
 * A {@link Visitor} implementation that validates a query's used of a {@link Schemata} and records any problems as errors.
 */
public class Validator extends AbstractVisitor {

    private final QueryContext context;
    private final Problems problems;
    private final Map<SelectorName, Table> selectorsByNameOrAlias;
    private final Map<SelectorName, Table> selectorsByName;
    private final Map<String, Schemata.Column> columnsByAlias;
    private final boolean validateColumnExistence;

    /**
     * @param context the query context
     * @param selectorsByName the {@link Table tables} by their name or alias, as defined by the selectors
     */
    public Validator( QueryContext context,
                      Map<SelectorName, Table> selectorsByName ) {
        this.context = context;
        this.problems = this.context.getProblems();
        this.selectorsByNameOrAlias = selectorsByName;
        this.selectorsByName = new HashMap<SelectorName, Table>();
        for (Table table : selectorsByName.values()) {
            this.selectorsByName.put(table.getName(), table);
        }
        this.columnsByAlias = new HashMap<String, Schemata.Column>();
        this.validateColumnExistence = context.getHints().validateColumnExistance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.AllNodes)
     */
    @Override
    public void visit( AllNodes obj ) {
        // this table doesn't have to be in the list of selected tables
        verifyTable(obj.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ArithmeticOperand)
     */
    @Override
    public void visit( ArithmeticOperand obj ) {
        verifyArithmeticOperand(obj.getLeft());
        verifyArithmeticOperand(obj.getRight());
    }

    protected void verifyArithmeticOperand( DynamicOperand operand ) {
        // The left and right operands must have LONG or DOUBLE types ...
        if (operand instanceof NodeDepth) {
            // good to go
        } else if (operand instanceof Length) {
            // good to go
        } else if (operand instanceof ArithmeticOperand) {
            // good to go
        } else if (operand instanceof FullTextSearchScore) {
            // good to go
        } else if (operand instanceof PropertyValue) {
            PropertyValue value = (PropertyValue)operand;
            SelectorName selector = value.getSelectorName();
            String propertyName = value.getPropertyName();
            Schemata.Column column = verify(selector, propertyName, this.validateColumnExistence);
            if (column != null) {
                // Check the type ...
                String columnType = column.getPropertyType();
                TypeSystem types = context.getTypeSystem();
                String longType = types.getLongFactory().getTypeName();
                String doubleType = types.getDoubleFactory().getTypeName();
                if (longType.equals(types.getCompatibleType(columnType, longType))) {
                    // Then the column type is long or can be converted to long ...
                } else if (doubleType.equals(types.getCompatibleType(columnType, doubleType))) {
                    // Then the column type is double or can be converted to double ...
                } else {
                    I18n msg = GraphI18n.columnTypeCannotBeUsedInArithmeticOperation;
                    problems.addError(msg, selector, propertyName, columnType);
                }
            }
        } else {
            I18n msg = GraphI18n.dynamicOperandCannotBeUsedInArithmeticOperation;
            problems.addError(msg, operand);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ChildNode)
     */
    @Override
    public void visit( ChildNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ChildNodeJoinCondition)
     */
    @Override
    public void visit( ChildNodeJoinCondition obj ) {
        verify(obj.getParentSelectorName());
        verify(obj.getChildSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Column)
     */
    @Override
    public void visit( Column obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName(), true); // don't care about the alias
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.DescendantNode)
     */
    @Override
    public void visit( DescendantNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.DescendantNodeJoinCondition)
     */
    @Override
    public void visit( DescendantNodeJoinCondition obj ) {
        verify(obj.getAncestorSelectorName());
        verify(obj.getDescendantSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
     */
    @Override
    public void visit( EquiJoinCondition obj ) {
        verify(obj.getSelector1Name(), obj.getProperty1Name(), true);
        verify(obj.getSelector2Name(), obj.getProperty2Name(), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.FullTextSearch)
     */
    @Override
    public void visit( FullTextSearch obj ) {
        SelectorName selectorName = obj.getSelectorName();
        if (obj.getPropertyName() != null) {
            Schemata.Column column = verify(selectorName, obj.getPropertyName(), this.validateColumnExistence);
            if (column != null) {
                // Make sure the column is full-text searchable ...
                if (!column.isFullTextSearchable()) {
                    problems.addError(GraphI18n.columnIsNotFullTextSearchable, column.getName(), selectorName);
                }
            }
        } else {
            Table table = verify(selectorName);
            // Don't need to check if the selector is the '__ALLNODES__' selector ...
            if (table != null && !AllNodes.ALL_NODES_NAME.equals(table.getName())) {
                // Make sure there is at least one column on the table that is full-text searchable ...
                boolean searchable = false;
                for (Schemata.Column column : table.getColumns()) {
                    if (column.isFullTextSearchable()) {
                        searchable = true;
                        break;
                    }
                }
                if (!searchable) {
                    problems.addError(GraphI18n.tableIsNotFullTextSearchable, selectorName);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.FullTextSearchScore)
     */
    @Override
    public void visit( FullTextSearchScore obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Length)
     */
    @Override
    public void visit( Length obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.LowerCase)
     */
    @Override
    public void visit( LowerCase obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NamedSelector)
     */
    @Override
    public void visit( NamedSelector obj ) {
        verify(obj.getAliasOrName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeDepth)
     */
    @Override
    public void visit( NodeDepth obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeLocalName)
     */
    @Override
    public void visit( NodeLocalName obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeName)
     */
    @Override
    public void visit( NodeName obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodePath)
     */
    @Override
    public void visit( NodePath obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyExistence)
     */
    @Override
    public void visit( PropertyExistence obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName(), this.validateColumnExistence);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyValue)
     */
    @Override
    public void visit( PropertyValue obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName(), this.validateColumnExistence);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ReferenceValue)
     */
    @Override
    public void visit( ReferenceValue obj ) {
        String propName = obj.getPropertyName();
        if (propName != null) {
            verify(obj.getSelectorName(), propName, this.validateColumnExistence);
        } else {
            verify(obj.getSelectorName());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Query)
     */
    @Override
    public void visit( Query obj ) {
        // Collect the map of columns by alias for this query ...
        this.columnsByAlias.clear();
        for (Column column : obj.getColumns()) {
            // Find the schemata column ...
            Table table = tableWithNameOrAlias(column.getSelectorName());
            if (table != null) {
                Schemata.Column tableColumn = table.getColumn(column.getPropertyName());
                if (tableColumn != null) {
                    this.columnsByAlias.put(column.getColumnName(), tableColumn);
                }
            }
        }
        super.visit(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.SameNode)
     */
    @Override
    public void visit( SameNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.SameNodeJoinCondition)
     */
    @Override
    public void visit( SameNodeJoinCondition obj ) {
        verify(obj.getSelector1Name());
        verify(obj.getSelector2Name());
    }

    protected Table tableWithNameOrAlias( SelectorName tableName ) {
        Table table = selectorsByNameOrAlias.get(tableName);
        if (table == null) {
            // Try looking up the table by it's real name (if an alias were used) ...
            table = selectorsByName.get(tableName);
        }
        return table;
    }

    protected Table verify( SelectorName selectorName ) {
        Table table = tableWithNameOrAlias(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.getName());
        }
        return table;
    }

    protected Table verifyTable( SelectorName tableName ) {
        Table table = tableWithNameOrAlias(tableName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, tableName.getName());
        }
        return table;
    }

    protected Schemata.Column verify( SelectorName selectorName,
                                      String propertyName,
                                      boolean columnIsRequired ) {
        Table table = tableWithNameOrAlias(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.getName());
            return null;
        }
        Schemata.Column column = table.getColumn(propertyName);
        if (column == null) {
            // Maybe the supplied property name is really an alias ...
            column = this.columnsByAlias.get(propertyName);
            if (column == null && columnIsRequired) {
                problems.addError(GraphI18n.columnDoesNotExistOnTable, propertyName, selectorName.getName());
            }
        }
        return column; // may be null
    }

}
