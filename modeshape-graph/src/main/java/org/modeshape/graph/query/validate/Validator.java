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
import org.modeshape.graph.query.model.Comparison;
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
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Subquery;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.UpperCase;
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
        verifyTable(obj.name());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ArithmeticOperand)
     */
    @Override
    public void visit( ArithmeticOperand obj ) {
        verifyArithmeticOperand(obj.left());
        verifyArithmeticOperand(obj.right());
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
            SelectorName selector = value.selectorName();
            String propertyName = value.propertyName();
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
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ChildNodeJoinCondition)
     */
    @Override
    public void visit( ChildNodeJoinCondition obj ) {
        verify(obj.parentSelectorName());
        verify(obj.childSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Column)
     */
    @Override
    public void visit( Column obj ) {
        verify(obj.selectorName(), obj.propertyName(), this.validateColumnExistence); // don't care about the alias
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Comparison)
     */
    @Override
    public void visit( Comparison obj ) {
        // The dynamic operand itself will be visited by the validator as it walks the comparison object.
        // All we need to do here is check the operator ...
        verifyOperator(obj.operand1(), obj.operator());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.DescendantNode)
     */
    @Override
    public void visit( DescendantNode obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.DescendantNodeJoinCondition)
     */
    @Override
    public void visit( DescendantNodeJoinCondition obj ) {
        verify(obj.ancestorSelectorName());
        verify(obj.descendantSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
     */
    @Override
    public void visit( EquiJoinCondition obj ) {
        verify(obj.selector1Name(), obj.property1Name(), this.validateColumnExistence);
        verify(obj.selector2Name(), obj.property2Name(), this.validateColumnExistence);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.FullTextSearch)
     */
    @Override
    public void visit( FullTextSearch obj ) {
        SelectorName selectorName = obj.selectorName();
        if (obj.propertyName() != null) {
            Schemata.Column column = verify(selectorName, obj.propertyName(), this.validateColumnExistence);
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
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Length)
     */
    @Override
    public void visit( Length obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.LowerCase)
     */
    @Override
    public void visit( LowerCase obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NamedSelector)
     */
    @Override
    public void visit( NamedSelector obj ) {
        verify(obj.aliasOrName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeDepth)
     */
    @Override
    public void visit( NodeDepth obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeLocalName)
     */
    @Override
    public void visit( NodeLocalName obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodeName)
     */
    @Override
    public void visit( NodeName obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.NodePath)
     */
    @Override
    public void visit( NodePath obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Ordering)
     */
    @Override
    public void visit( Ordering obj ) {
        verifyOrdering(obj.operand());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyExistence)
     */
    @Override
    public void visit( PropertyExistence obj ) {
        verify(obj.selectorName(), obj.propertyName(), this.validateColumnExistence);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyValue)
     */
    @Override
    public void visit( PropertyValue obj ) {
        verify(obj.selectorName(), obj.propertyName(), this.validateColumnExistence);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.ReferenceValue)
     */
    @Override
    public void visit( ReferenceValue obj ) {
        String propName = obj.propertyName();
        if (propName != null) {
            verify(obj.selectorName(), propName, this.validateColumnExistence);
        } else {
            verify(obj.selectorName());
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
        for (Column column : obj.columns()) {
            // Find the schemata column ...
            Table table = tableWithNameOrAlias(column.selectorName());
            if (table != null) {
                Schemata.Column tableColumn = table.getColumn(column.propertyName());
                if (tableColumn != null) {
                    this.columnsByAlias.put(column.columnName(), tableColumn);
                }
            }
        }
        super.visit(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Subquery)
     */
    @Override
    public void visit( Subquery subquery ) {
        // Don't validate subqueries; this is done as a separate step ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.SameNode)
     */
    @Override
    public void visit( SameNode obj ) {
        verify(obj.selectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.SameNodeJoinCondition)
     */
    @Override
    public void visit( SameNodeJoinCondition obj ) {
        verify(obj.selector1Name());
        verify(obj.selector2Name());
    }

    protected void verifyOrdering( DynamicOperand operand ) {
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            verifyOrdering(propValue.selectorName(), propValue.propertyName());
        } else if (operand instanceof ReferenceValue) {
            ReferenceValue value = (ReferenceValue)operand;
            verifyOrdering(value.selectorName(), value.propertyName());
        } else if (operand instanceof Length) {
            Length length = (Length)operand;
            verifyOrdering(length.propertyValue());
        } else if (operand instanceof LowerCase) {
            verifyOrdering(((LowerCase)operand).operand());
        } else if (operand instanceof UpperCase) {
            verifyOrdering(((UpperCase)operand).operand());
            // } else if (operand instanceof NodeDepth) {
            // NodeDepth depth = (NodeDepth)operand;
            // verifyOrdering(depth.selectorName(), "mode:depth");
            // } else if (operand instanceof NodePath) {
            // NodePath depth = (NodePath)operand;
            // verifyOrdering(depth.selectorName(), "jcr:path");
            // } else if (operand instanceof NodeLocalName) {
            // NodeLocalName depth = (NodeLocalName)operand;
            // verifyOrdering(depth.selectorName(), "mode:localName");
            // } else if (operand instanceof NodeName) {
            // NodeName depth = (NodeName)operand;
            // verifyOrdering(depth.selectorName(), "jcr:name");
        } else if (operand instanceof ArithmeticOperand) {
            // The LEFT and RIGHT dynamic operands must both work with this operator ...
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            verifyOrdering(arith.left());
            verifyOrdering(arith.right());
        }
    }

    protected void verifyOrdering( SelectorName selectorName,
                                   String propertyName ) {
        Schemata.Column column = verify(selectorName, propertyName, false);
        if (column != null && !column.isOrderable()) {
            problems.addError(GraphI18n.columnInTableIsNotOrderable, propertyName, selectorName.getString());
        }
    }

    protected void verifyOperator( DynamicOperand operand,
                                   Operator op ) {
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            verifyOperator(propValue.selectorName(), propValue.propertyName(), op);
        } else if (operand instanceof ReferenceValue) {
            ReferenceValue value = (ReferenceValue)operand;
            verifyOperator(value.selectorName(), value.propertyName(), op);
        } else if (operand instanceof Length) {
            Length length = (Length)operand;
            verifyOperator(length.propertyValue(), op);
        } else if (operand instanceof LowerCase) {
            verifyOperator(((LowerCase)operand).operand(), op);
        } else if (operand instanceof UpperCase) {
            verifyOperator(((UpperCase)operand).operand(), op);
            // } else if (operand instanceof NodeDepth) {
            // NodeDepth depth = (NodeDepth)operand;
            // verifyOperator(depth.selectorName(), "mode:depth", op);
            // } else if (operand instanceof NodePath) {
            // NodePath depth = (NodePath)operand;
            // verifyOperator(depth.selectorName(), "jcr:path", op);
            // } else if (operand instanceof NodeLocalName) {
            // NodeLocalName depth = (NodeLocalName)operand;
            // verifyOperator(depth.selectorName(), "mode:localName", op);
            // } else if (operand instanceof NodeName) {
            // NodeName depth = (NodeName)operand;
            // verifyOperator(depth.selectorName(), "jcr:name", op);
        } else if (operand instanceof ArithmeticOperand) {
            // The LEFT and RIGHT dynamic operands must both work with this operator ...
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            verifyOperator(arith.left(), op);
            verifyOperator(arith.right(), op);
        }
    }

    protected void verifyOperator( SelectorName selectorName,
                                   String propertyName,
                                   Operator op ) {
        Schemata.Column column = verify(selectorName, propertyName, false);
        if (column != null) {
            if (!column.getOperators().contains(op)) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Operator allowed : column.getOperators()) {
                    if (first) first = false;
                    else sb.append(", ");
                    sb.append(allowed.symbol());
                }
                problems.addError(GraphI18n.operatorIsNotValidAgainstColumnInTable,
                                  op.symbol(),
                                  propertyName,
                                  selectorName.getString(),
                                  sb);
            }
        }
    }

    protected Table tableWithNameOrAlias( SelectorName tableName ) {
        Table table = selectorsByNameOrAlias.get(tableName);
        if (table == null) {
            // Try looking up the table by it's real name (if an alias were used) ...
            table = selectorsByName.get(tableName);
        }
        return table; // may be null
    }

    protected Table verify( SelectorName selectorName ) {
        Table table = tableWithNameOrAlias(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.name());
        }
        return table; // may be null
    }

    protected Table verifyTable( SelectorName tableName ) {
        Table table = tableWithNameOrAlias(tableName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, tableName.name());
        }
        return table; // may be null
    }

    protected Schemata.Column verify( SelectorName selectorName,
                                      String propertyName,
                                      boolean columnIsRequired ) {
        Table table = tableWithNameOrAlias(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.name());
            return null;
        }
        Schemata.Column column = table.getColumn(propertyName);
        if (column == null) {
            // Maybe the supplied property name is really an alias ...
            column = this.columnsByAlias.get(propertyName);
            if (column == null && !"*".equals(propertyName) && columnIsRequired && !table.hasExtraColumns()) {
                problems.addError(GraphI18n.columnDoesNotExistOnTable, propertyName, selectorName.name());
            }
        }
        return column; // may be null
    }

}
