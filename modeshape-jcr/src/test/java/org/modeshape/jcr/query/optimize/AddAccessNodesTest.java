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
package org.modeshape.jcr.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.query.AbstractQueryTest;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * 
 */
public class AddAccessNodesTest extends AbstractQueryTest {

    private AddAccessNodes rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), mock(NodeCache.class), "workspace", mock(Schemata.class));
        rule = AddAccessNodes.INSTANCE;
    }

    /**
     * Before:
     * 
     * <pre>
     *          ...
     *           |
     *        SOURCE
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     *          ...
     *           |
     *        ACCESS
     *           |
     *        SOURCE
     * </pre>
     */
    @Test
    public void shouldAddAccessNodeAboveSourceNode() {
        PlanNode project = new PlanNode(Type.PROJECT, selector("Selector1"));
        PlanNode source = new PlanNode(Type.SOURCE, project, selector("Selector1"));

        // Execute the rule ...
        PlanNode result = rule.execute(context, project, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(project)));
        PlanNode access = project.getFirstChild();
        assertThat(access.getType(), is(Type.ACCESS));
        assertSelectors(access, "Selector1");
        assertChildren(access, source);
        assertChildren(source);
    }
}
