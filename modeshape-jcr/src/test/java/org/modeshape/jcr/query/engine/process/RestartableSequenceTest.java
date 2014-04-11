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
package org.modeshape.jcr.query.engine.process;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.AbstractNodeSequenceTest;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.Restartable;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RestartableSequenceTest extends AbstractNodeSequenceTest {

    private ExecutionContext context;
    private BufferManager bufferMgr;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        this.context = new ExecutionContext();
        this.bufferMgr = new BufferManager(context);
    }

    @Test
    public void shouldProduceSameOrderWithOneBatchInMemory() {
        System.out.println("Number of rows: " + countRows(allNodes()));
        int numRowsInMemory = 4;
        int numRowsInBatch = 4;
        NodeSequence batches = allNodes(1.0f, numRowsInBatch);
        RestartableSequence restartable = new RestartableSequence(workspaceName(), batches, bufferMgr, cache, numRowsInMemory);
        assertSameOrder(restartable, allNodes(), false);
        restartable.restart();
        assertSameOrder(restartable, allNodes(), true);
    }

    protected void assertSameOrder( NodeSequence sequence,
                                    NodeSequence expected,
                                    boolean closeRestartable ) {
        List<NodeKey> expectedKeys = nodesFrom(expected, closeRestartable);
        List<NodeKey> actualKeys = nodesFrom(sequence, closeRestartable);
        assertThat(actualKeys, is(expectedKeys));
    }

    protected List<NodeKey> nodesFrom( NodeSequence sequence,
                                       boolean closeRestartable ) {
        List<NodeKey> keys = new ArrayList<NodeKey>();
        // Iterate over the batches ...
        try {
            Batch batch = null;
            while ((batch = sequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    CachedNode node = batch.getNode();
                    NodeKey key = node != null ? node.getKey() : null;
                    boolean added = keys.add(key);
                    print("Adding " + key);
                    assertTrue("Failed to add " + key, added);
                }
            }
        } finally {
            if (sequence instanceof Restartable) {
                if (closeRestartable) sequence.close();
            } else {
                sequence.close();
            }
        }
        return keys;
    }
}
