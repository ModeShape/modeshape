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
package org.modeshape.jcr.cache.document;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path.Segment;

public class DocumentOptimizerTest extends AbstractSessionCacheTest {

    private DocumentOptimizer optimizer;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.optimizer = new DocumentOptimizer(workspaceCache.documentStore());
    }

    @Override
    protected SessionCache createSessionCache( ExecutionContext context,
                                               WorkspaceCache cache,
                                               RepositoryEnvironment repositoryEnvironment ) {
        return new WritableSessionCache(context, workspaceCache, repositoryEnvironment);
    }

    @Test
    public void shouldNotSplitDocumentWithChildReferenceBlocksThatAreAlreadyTooSmall() throws Exception {
        NodeKey key = new NodeKey("source1works1-childB");
        txnManager().begin();
        EditableDocument doc = workspaceCache.documentStore().edit(key.toString(), true);
        EditableArray children = doc.getArray(DocumentTranslator.CHILDREN);
        String nextBlock = doc.getDocument(DocumentTranslator.CHILDREN_INFO).getString(DocumentTranslator.NEXT_BLOCK);
        boolean changed = optimizer.splitChildren(key, doc, children, 100, 50, true, nextBlock);
        txnManager().commit();
        assertThat(changed, is(false));
    }

    @Test
    public void shouldMergeDocumentWithTooSmallChildReferencesSegmentInFirstBlock() throws Exception {
        NodeKey key = new NodeKey("source1works1-childB");
        txnManager().begin();
        EditableDocument doc = workspaceCache.documentStore().edit(key.toString(), true);
        EditableArray children = doc.getArray(DocumentTranslator.CHILDREN);
        String nextBlock = doc.getDocument(DocumentTranslator.CHILDREN_INFO).getString(DocumentTranslator.NEXT_BLOCK);
        optimizer.mergeChildren(key, doc, children, true, nextBlock);
        txnManager().commit();

        // Refetch the document, which should no longer be segmented ...
        txnManager().begin();
        doc = workspaceCache.documentStore().edit(key.toString(), true);
        assertInfo(key.toString(), 2, null, null, true, 0);
        children = doc.getArray(DocumentTranslator.CHILDREN);
        txnManager().commit();
        assertThat(children.size(), is(2));
        assertChildren(doc, name("childC"), name("childD"));

        print(false);
        print(doc);
    }

    @Test
    public void shouldSplitDocumentThatContainsTooManyChildReferences() throws Exception {
        // Create a bunch of children ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        for (int i = 0; i != 10; ++i) {
            NodeKey newKey = session1.createNodeKey();
            nodeB.createChild(session(), newKey, name("newChild"), property("p1a", 344), property("p2", false));
        }
        session1.save();

        // Optimize the storage ...
        txnManager().begin();
        NodeKey key = nodeB.getKey();
        EditableDocument doc = workspaceCache.documentStore().edit(key.toString(), true);
        optimizer.optimizeChildrenBlocks(key, doc, 9, 5);
        txnManager().commit();

        print(false);
        print(doc, true);
    }

    @Test
    public void shouldSplitDocumentThatContainsTooManyChildReferencesIntoMultipleSegments() throws Exception {
        // Create a bunch of children ...
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");
        for (int i = 0; i != 10; ++i) {
            NodeKey newKey = session1.createNodeKey();
            nodeB.createChild(session(), newKey, name("newChild"), property("p1a", 344), property("p2", false));
        }
        session1.save();

        // Optimize the storage ...
        txnManager().begin();
        NodeKey key = nodeB.getKey();
        EditableDocument doc = workspaceCache.documentStore().edit(key.toString(), true);
        optimizer.optimizeChildrenBlocks(key, doc, 5, 3); // will merge into a single block ...
        optimizer.optimizeChildrenBlocks(key, doc, 5, 3); // will split into two blocks ...
        txnManager().commit();

        print(false);
        print(doc, true);
    }

    @Test
    public void shouldSplitDocumentThatRepeatedlyContainsTooManyChildReferencesIntoMultipleSegments() throws Exception {
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");

        // Make it optimum to start out ...
        txnManager().begin();
        NodeKey key = nodeB.getKey();
        optimizer.optimizeChildrenBlocks(key, null, 5, 2); // will merge into a single block ...
        txnManager().commit();
        // Save the session, otherwise the database is inconsistent after the optimize operation
        session1.save();
        nodeB = check(session1).mutableNode("/childB");
        print(false);
        print(document(key), true);
        print(false);

        for (int j = 0; j != 5; ++j) {
            // Create a bunch of children ...
            for (int i = 0; i != 5; ++i) {
                NodeKey newKey = key.withId("child" + ((j * 5) + i + 1));
                nodeB.createChild(session(), newKey, name("newChild"), property("p1a", 344), property("p2", false));
            }
            session1.save();
            // Find node B again after the save ...
            nodeB = check(session1).mutableNode("/childB");

            print(false);
            print("\nOptimizing...");
            print(document(key), true);
            txnManager().begin();
            optimizer.optimizeChildrenBlocks(key, null, 5, 2); // will split into blocks ...
            txnManager().commit();
            print("\nOptimized...");
            print(document(key), true);
            print(false);
        }

        // Optimize the storage ...
        txnManager().begin();
        optimizer.optimizeChildrenBlocks(key, null, 5, 2); // will split into blocks ...
        txnManager().commit();

        print(false);
        print(document(key), true);
    }

    protected Document document( NodeKey key ) {
        SchematicEntry entry = workspaceCache.documentStore().get(key.toString());
        return entry.getContent();
    }

    protected void assertChildren( Document doc,
                                   Segment... children ) {
        List<?> childReferences = doc.getArray(DocumentTranslator.CHILDREN);
        Iterator<?> actualIter = childReferences.iterator();
        Iterator<Segment> expectedIter = Arrays.asList(children).iterator();
        while (actualIter.hasNext()) {
            ChildReference childRef = workspaceCache.translator().childReferenceFrom(actualIter.next());
            if (!expectedIter.hasNext()) {
                fail("Found \"" + childRef + "\" but not expecting any children");
            }
            Segment expectedName = expectedIter.next();
            assertThat("Expecting child \"" + expectedName + "\" but found \"" + childRef.toString() + "\"",
                       childRef.getSegment(), is(expectedName));
        }
        if (expectedIter.hasNext()) {
            fail("Expected \"" + expectedIter.next() + "\" but found no such child");
        }
    }

    protected void assertChildren( Document doc,
                                   Name... children ) {
        List<?> childReferences = doc.getArray(DocumentTranslator.CHILDREN);
        Iterator<?> actualIter = childReferences.iterator();
        Iterator<Name> expectedIter = Arrays.asList(children).iterator();
        while (actualIter.hasNext()) {
            ChildReference childRef = workspaceCache.translator().childReferenceFrom(actualIter.next());
            if (!expectedIter.hasNext()) {
                fail("Found \"" + childRef + "\" but not expecting any children");
            }
            Name expectedName = expectedIter.next();
            assertThat("Expecting child \"" + expectedName + "[1]\" but found \"" + childRef.toString() + "\"",
                       childRef.getSegment(), is(segment(expectedName, 1)));
        }
        if (expectedIter.hasNext()) {
            fail("Expected \"" + expectedIter.next() + "\" but found no such child");
        }
    }

    protected void assertInfo( String key,
                               long expectedChildCount,
                               String expectedNextBlock,
                               String expectedLastBlock,
                               boolean firstBlock,
                               long expectedBlockSize ) {
        Document doc = workspaceCache.documentStore().get(key).getContent();
        Document info = doc.getDocument(DocumentTranslator.CHILDREN_INFO);
        assertThat(info.getLong(DocumentTranslator.COUNT), is(expectedChildCount));
        assertThat(info.getString(DocumentTranslator.NEXT_BLOCK), is(expectedNextBlock));
        assertThat(info.getString(DocumentTranslator.LAST_BLOCK), is(expectedLastBlock));
        if (firstBlock) {
            assertThat(info.containsField(DocumentTranslator.BLOCK_SIZE), is(expectedNextBlock != null));
        } else {
            assertThat(info.getLong(DocumentTranslator.BLOCK_SIZE), is(expectedBlockSize));
        }
    }
}
