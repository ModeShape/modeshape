/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.validate;

import java.util.Map;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.AllNodes;
import org.jboss.dna.graph.query.model.ChildNode;
import org.jboss.dna.graph.query.model.ChildNodeJoinCondition;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.DescendantNode;
import org.jboss.dna.graph.query.model.DescendantNodeJoinCondition;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.FullTextSearchScore;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.LowerCase;
import org.jboss.dna.graph.query.model.NamedSelector;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.SameNode;
import org.jboss.dna.graph.query.model.SameNodeJoinCondition;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Visitor;
import org.jboss.dna.graph.query.model.Visitors.AbstractVisitor;
import org.jboss.dna.graph.query.validate.Schemata.Table;

/**
 * A {@link Visitor} implementation that validates a query's used of a {@link Schemata} and records any problems as errors.
 */
public class Validator extends AbstractVisitor {

    private final QueryContext context;
    private final Problems problems;
    private final Map<SelectorName, Table> selectorsByName;

    /**
     * @param context the query context
     * @param selectorsByName the {@link Table tables} by their name or alias, as defined by the selectors
     */
    public Validator( QueryContext context,
                      Map<SelectorName, Table> selectorsByName ) {
        this.context = context;
        this.problems = this.context.getProblems();
        this.selectorsByName = selectorsByName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.AllNodes)
     */
    @Override
    public void visit( AllNodes obj ) {
        // this table doesn't have to be in the list of selected tables
        verifyTable(obj.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.ChildNode)
     */
    @Override
    public void visit( ChildNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.ChildNodeJoinCondition)
     */
    @Override
    public void visit( ChildNodeJoinCondition obj ) {
        verify(obj.getParentSelectorName());
        verify(obj.getChildSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.Column)
     */
    @Override
    public void visit( Column obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName()); // don't care about the alias
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.DescendantNode)
     */
    @Override
    public void visit( DescendantNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.DescendantNodeJoinCondition)
     */
    @Override
    public void visit( DescendantNodeJoinCondition obj ) {
        verify(obj.getAncestorSelectorName());
        verify(obj.getDescendantSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.EquiJoinCondition)
     */
    @Override
    public void visit( EquiJoinCondition obj ) {
        verify(obj.getSelector1Name(), obj.getProperty1Name());
        verify(obj.getSelector2Name(), obj.getProperty2Name());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.FullTextSearch)
     */
    @Override
    public void visit( FullTextSearch obj ) {
        SelectorName selectorName = obj.getSelectorName();
        if (obj.getPropertyName() != null) {
            Schemata.Column column = verify(selectorName, obj.getPropertyName());
            if (column != null) {
                // Make sure the column is full-text searchable ...
                if (!column.isFullTextSearchable()) {
                    problems.addError(GraphI18n.columnIsNotFullTextSearchable, column.getName(), selectorName);
                }
            }
        } else {
            Table table = verify(selectorName);
            if (table != null) {
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
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.FullTextSearchScore)
     */
    @Override
    public void visit( FullTextSearchScore obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.Length)
     */
    @Override
    public void visit( Length obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.LowerCase)
     */
    @Override
    public void visit( LowerCase obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.NamedSelector)
     */
    @Override
    public void visit( NamedSelector obj ) {
        verify(obj.getAliasOrName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.NodeDepth)
     */
    @Override
    public void visit( NodeDepth obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.NodeLocalName)
     */
    @Override
    public void visit( NodeLocalName obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.NodeName)
     */
    @Override
    public void visit( NodeName obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.NodePath)
     */
    @Override
    public void visit( NodePath obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.PropertyExistence)
     */
    @Override
    public void visit( PropertyExistence obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.PropertyValue)
     */
    @Override
    public void visit( PropertyValue obj ) {
        verify(obj.getSelectorName(), obj.getPropertyName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.SameNode)
     */
    @Override
    public void visit( SameNode obj ) {
        verify(obj.getSelectorName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitors.AbstractVisitor#visit(org.jboss.dna.graph.query.model.SameNodeJoinCondition)
     */
    @Override
    public void visit( SameNodeJoinCondition obj ) {
        verify(obj.getSelector1Name());
        verify(obj.getSelector2Name());
    }

    protected Table verify( SelectorName selectorName ) {
        Table table = selectorsByName.get(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.getName());
        }
        return table;
    }

    protected Table verifyTable( SelectorName tableName ) {
        Table table = selectorsByName.get(tableName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, tableName.getName());
        }
        return table;
    }

    protected Schemata.Column verify( SelectorName selectorName,
                                      String propertyName ) {
        Table table = selectorsByName.get(selectorName);
        if (table == null) {
            problems.addError(GraphI18n.tableDoesNotExist, selectorName.getName());
            return null;
        }
        Schemata.Column column = table.getColumn(propertyName);
        if (column == null) {
            problems.addError(GraphI18n.columnDoesNotExistOnTable, propertyName, selectorName.getName());
        }
        return column;
    }

}
