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
package org.modeshape.graph.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.AbstractQueryTest;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ReplaceViewsTest extends AbstractQueryTest {

    private ReplaceViews rule;
    private QueryContext context;
    private Schemata schemata;
    private ImmutableSchemata.Builder builder;

    @Before
    public void beforeEach() {
        TypeSystem typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        rule = ReplaceViews.INSTANCE;
        builder = ImmutableSchemata.createBuilder(typeSystem);
        builder.addTable("t1", "c11", "c12", "c13");
        builder.addTable("t2", "c21", "c22", "c23");
        builder.addView("v1", "SELECT c11, c12 FROM t1 WHERE c13 < CAST('3' AS LONG)");
        builder.addView("v2", "SELECT t1.c11, t1.c12, t2.c23 FROM t1 JOIN t2 ON t1.c11 = t2.c21");
        schemata = builder.build();
        context = new QueryContext(schemata, typeSystem);
    }

    /**
     * Before:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT      with the list of columns being SELECTed
     *           |
     *        SELECT1
     *           |         One or more SELECT plan nodes that each have
     *        SELECT2      a single non-join constraint that are then all AND-ed
     *           |         together
     *        SELECT3
     *           |
     *        SOURCE       A SOURCE that uses the view
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT      with the list of columns being SELECTed
     *           |
     *        SELECT1
     *           |         One or more SELECT plan nodes that each have
     *        SELECT2      a single non-join constraint that are then all AND-ed
     *           |         together
     *        SELECT3
     *           |
     *        SOURCE
     *           |
     *        PROJECT      with the list of columns in the SELECT of the view
     *           |
     *        SELECT1      One or more SELECT plan nodes that each have a single non-join constraint
     *           |         that are then all AND-ed together (as defined by the view plan
     *           |
     *        SOURCE       A SOURCE that the view uses
     * </pre>
     */
    @Test
    public void shouldReplaceViewWithPlanForViewWithSingleTable() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("v1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("v1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("v1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("v1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("v1"));
        source.setProperty(Property.SOURCE_NAME, selector("v1"));

        // Create the equivalent plan nodes for what should be created ...
        PlanNode viewProject = new PlanNode(Type.PROJECT, selector("t1"));
        PlanNode viewSelect = new PlanNode(Type.SELECT, viewProject, selector("t1"));
        PlanNode viewSource = new PlanNode(Type.SOURCE, viewSelect, selector("t1"));
        viewProject.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12")));
        viewSelect.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c13"),
                                                                        Operator.LESS_THAN, new Literal(3L)));
        viewSource.setProperty(Property.SOURCE_NAME, selector("t1"));
        viewSource.setProperty(Property.SOURCE_COLUMNS, schemata.getTable(selector("t1")).getColumns());

        // Execute the rule ...
        PlanNode result = rule.execute(context, project, new LinkedList<OptimizerRule>());
        // System.out.println(project);
        // System.out.println(result);
        assertThat(result.isSameAs(project), is(true));
        assertChildren(project, select1);
        assertChildren(select1, select2);
        assertChildren(select2, select3);
    }

    protected List<Column> columns( Column... columns ) {
        return Arrays.asList(columns);
    }

    protected Column column( String table,
                             String columnName ) {
        return new Column(new SelectorName(table), columnName, columnName);
    }

    protected Column column( String table,
                             String columnName,
                             String alias ) {
        return new Column(new SelectorName(table), columnName, alias);
    }
}
