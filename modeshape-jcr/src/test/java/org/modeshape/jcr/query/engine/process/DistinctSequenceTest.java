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

import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.AbstractNodeSequenceTest;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.ValueTypeSystem;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class DistinctSequenceTest extends AbstractNodeSequenceTest {

    private ExecutionContext context;
    private BufferManager bufferMgr;
    private TypeSystem types;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        this.context = new ExecutionContext();
        this.bufferMgr = new BufferManager(context);
        this.types = new ValueTypeSystem(context.getValueFactories());
    }

    @Test
    public void shouldNotIncludeDuplicateNodesUsingOnHeapBuffer() {
        // print(true);
        NodeSequence nonDistinct = NodeSequence.append(allNodes(), allNodes());
        NodeSequence distinct = new DistinctSequence(nonDistinct, types, bufferMgr, true);
        assertNoDuplicates(distinct);
    }

    @Test
    public void shouldNotIncludeDuplicateNodesUsingOffHeapBuffer() {
        // print(true);
        NodeSequence nonDistinct = NodeSequence.append(allNodes(), allNodes());
        NodeSequence distinct = new DistinctSequence(nonDistinct, types, bufferMgr, false);
        assertNoDuplicates(distinct);
    }

    protected void assertNoDuplicates( NodeSequence sequence ) {
        Set<NodeKey> keys = new HashSet<NodeKey>();
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
            sequence.close();
        }
    }
}
