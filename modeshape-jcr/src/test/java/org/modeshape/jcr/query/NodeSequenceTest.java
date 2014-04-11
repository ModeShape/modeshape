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
package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.RowFilter;
import org.modeshape.jcr.value.Path;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class NodeSequenceTest extends AbstractNodeSequenceTest {

    @Test
    public void shouldCreateEmptySequenceOfVariousWidths() {
        for (int i = 0; i != 10; ++i) {
            NodeSequence seq = NodeSequence.emptySequence(i);
            assertThat("Incorrect width for empty sequence", seq.width(), is(i));
            assertThat(seq.nextBatch(), is(nullValue()));
            seq.close();
        }
    }

    @Test
    public void shouldCreateSequenceFromNodeKeysIterator() {
        // print(true);
        NodeSequence seq = allNodes();
        // The sequence should not have any duplicates, since the iterator doesn't ...
        Set<NodeKey> keys = new HashSet<NodeKey>();
        try {
            // Iterate over the batches ...
            Batch batch = null;
            while ((batch = seq.nextBatch()) != null) {
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
            seq.close();
        }
    }

    @Test
    public void shouldCreateLimitedSequenceSmallerThanDelegate() {
        assertThat(countRows(NodeSequence.limit(allNodes(), 2)), is(2L));
    }

    @Test
    public void shouldCreateLimitedSequenceSameSizeAsDelegateDelegate() {
        long countAll = countRows(allNodes());
        assertThat(countRows(NodeSequence.limit(allNodes(), countAll)), is(countAll));
    }

    @Test
    public void shouldCreateLimitedSequenceSameSizeAsDelegateDelegateWhenLimitIsLarger() {
        long countAll = countRows(allNodes());
        assertThat(countRows(NodeSequence.limit(allNodes(), countAll + 1)), is(countAll));
    }

    @Test
    public void shouldCreateAppendingSequenceFromTwoOtherSequences() {
        // print(true);
        NodeSequence seq = NodeSequence.append(allNodes(), allNodes());
        // The sequence should not have two duplicates of every node ...
        Set<NodeKey> keys = new HashSet<NodeKey>();
        try {
            // Iterate over the batches ...
            Batch batch = null;
            boolean shouldAdd = true;
            while ((batch = seq.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    CachedNode node = batch.getNode();
                    NodeKey key = node != null ? node.getKey() : null;
                    if (keys.contains(key) && shouldAdd) shouldAdd = false;
                    if (shouldAdd) {
                        boolean added = keys.add(key);
                        print("Adding " + key);
                        assertTrue("Failed to add " + key, added);
                    } else {
                        boolean removed = keys.remove(key);
                        print("Removing " + key);
                        assertTrue("Failed to remove " + key, removed);
                    }
                }
            }
        } finally {
            seq.close();
        }
    }

    @Test
    public void shouldCreateFilteringSequenceThatIncludesOnlyNonSystemNodes() {
        RowFilter nonSysFilter = rowFilterOfNodesWithKeysHavingWorkspaceKey(0, NodeKey.keyForWorkspaceName(workspaceName()));
        NodeSequence seq = NodeSequence.filter(allNodes(), nonSysFilter);
        try {
            // Iterate over the batches ...
            Batch batch = null;
            while ((batch = seq.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    CachedNode node = batch.getNode();
                    if (node != null) {
                        Path path = node.getPath(cache);
                        boolean isSystem = path.getSegment(0).getName().equals(JcrLexicon.SYSTEM);
                        assertTrue(path.isRoot() || !isSystem);
                    }
                }
            }
        } finally {
            seq.close();
        }
    }
}
