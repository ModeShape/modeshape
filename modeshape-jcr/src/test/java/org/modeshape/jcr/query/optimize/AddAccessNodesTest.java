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
package org.modeshape.jcr.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Collections;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryIndexes;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.AbstractQueryTest;
import org.modeshape.jcr.query.BufferManager;
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
        context = new QueryContext(new ExecutionContext(), mock(RepositoryCache.class), Collections.singleton("workspace"),
                                   mock(Schemata.class), mock(RepositoryIndexes.class), mock(NodeTypes.class),
                                   mock(BufferManager.class));
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
