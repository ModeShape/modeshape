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
package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * 
 */
public abstract class AbstractQueryTest {

    protected SelectorName selector( String selectorName ) {
        return new SelectorName(selectorName);
    }

    protected Column column( SelectorName selector,
                             String property ) {
        return new Column(selector, property, property);
    }

    protected Column column( SelectorName selector,
                             String property,
                             String alias ) {
        return new Column(selector, property, alias);
    }

    protected List<Column> columns( QueryContext context,
                                    SelectorName selector,
                                    String... columnNames ) {
        List<Column> columns = new ArrayList<Column>();
        for (String columnName : columnNames) {
            columns.add(column(selector, columnName));
        }
        return columns;
    }

    protected List<String> columnTypes( QueryContext context,
                                        SelectorName selector,
                                        String... columnNames ) {
        List<String> types = new ArrayList<String>();
        for (@SuppressWarnings( "unused" )
        String columnName : columnNames) {
            types.add(context.getTypeSystem().getDefaultType());
        }
        return types;
    }

    protected PlanNode sourceNode( QueryContext context,
                                   PlanNode parent,
                                   String selectorName,
                                   String... columnNames ) {
        PlanNode node = new PlanNode(Type.SOURCE, parent);
        SelectorName selector = selector(selectorName);
        node.addSelector(selector);
        node.setProperty(Property.PROJECT_COLUMNS, columns(context, selector, columnNames));
        node.setProperty(Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector, columnNames));
        return node;
    }

    protected void assertChildren( PlanNode node,
                                   PlanNode... children ) {
        assertThat(node.getChildCount(), is(children.length));
        for (int i = 0; i != node.getChildCount(); ++i) {
            assertThat(node.getChild(i), is(sameInstance(children[i])));
        }
    }

    protected void assertSameChildren( PlanNode node,
                                       PlanNode... children ) {
        assertThat(node.getChildCount(), is(children.length));
        for (int i = 0; i != node.getChildCount(); ++i) {
            assertThat(node.getChild(i).isSameAs(children[i]), is(true));
        }
    }

    protected void assertSelectors( PlanNode node,
                                    String... selectors ) {
        Set<SelectorName> selectorSet = new HashSet<SelectorName>();
        for (String selectorName : selectors) {
            selectorSet.add(new SelectorName(selectorName));
        }
        assertThat("Selectors don't match", node.getSelectors(), is(selectorSet));
    }

    protected void assertProperty( PlanNode node,
                                   Property name,
                                   Object value ) {
        assertThat("Property value doesn't match", node.getProperty(name), is(value));
    }

    protected <T> void assertPropertyIsList( PlanNode node,
                                             Property name,
                                             Class<T> valueType,
                                             T... values ) {
        assertThat("Property value doesn't match", node.getPropertyAsList(name, valueType), is(Arrays.asList(values)));
    }

    protected <T> void assertPropertyIsList( PlanNode node,
                                             Property name,
                                             Class<T> valueType,
                                             List<T> values ) {
        assertThat("Property value doesn't match", node.getPropertyAsList(name, valueType), is(values));
    }

    protected void assertSortOrderBy( PlanNode sortNode,
                                      String... selectors ) {
        List<SelectorName> expected = new ArrayList<SelectorName>(selectors.length);
        for (String selectorName : selectors) {
            expected.add(new SelectorName(selectorName));
        }
        List<SelectorName> actualSortedBy = sortNode.getPropertyAsList(Property.SORT_ORDER_BY, SelectorName.class);
        assertThat("Sort node order-by doesn't match selector name list", actualSortedBy, is(expected));
    }
}
