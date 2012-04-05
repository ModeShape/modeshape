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
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path.Segment;

public class DocumentTranslatorTest extends AbstractSessionCacheTest {

    @Override
    protected SessionCache createSessionCache( ExecutionContext context,
                                               WorkspaceCache cache ) {
        return new WritableSessionCache(context, workspaceCache, createSessionContext());
    }

    @Test
    public void shouldNotSplitDocumentWithChildReferenceBlocksThatAreAlreadyTooSmall() throws Exception {
        NodeKey key = new NodeKey("source1works1-childB");
        SchematicEntry entry = workspaceCache.database().get(key.toString());
        EditableDocument doc = entry.editDocumentContent();
        EditableArray children = doc.getArray(DocumentTranslator.CHILDREN);
        String nextBlock = doc.getDocument(DocumentTranslator.CHILDREN_INFO).getString(DocumentTranslator.NEXT_BLOCK);
        boolean changed = workspaceCache.translator().splitChildren(key, doc, children, 100, 50, true, nextBlock);
        assertThat(changed, is(false));
    }

    @Test
    public void shouldMergeDocumentWithTooSmallChildReferencesSegmentInFirstBlock() throws Exception {
        NodeKey key = new NodeKey("source1works1-childB");
        SchematicEntry entry = workspaceCache.database().get(key.toString());
        EditableDocument doc = entry.editDocumentContent();
        EditableArray children = doc.getArray(DocumentTranslator.CHILDREN);
        String nextBlock = doc.getDocument(DocumentTranslator.CHILDREN_INFO).getString(DocumentTranslator.NEXT_BLOCK);
        workspaceCache.translator().mergeChildren(key, doc, children, true, nextBlock);

        // Refetch the document, which should no longer be segmented ...
        entry = workspaceCache.database().get(key.toString());
        doc = entry.editDocumentContent();
        assertInfo(entry, 2, null, null, true, 0);
        children = doc.getArray(DocumentTranslator.CHILDREN);
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
        NodeKey key = nodeB.getKey();
        SchematicEntry entry = workspaceCache.database().get(key.toString());
        EditableDocument doc = entry.editDocumentContent();
        workspaceCache.translator().optimizeChildrenBlocks(key, doc, 9, 5);

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
        NodeKey key = nodeB.getKey();
        SchematicEntry entry = workspaceCache.database().get(key.toString());
        EditableDocument doc = entry.editDocumentContent();
        workspaceCache.translator().optimizeChildrenBlocks(key, doc, 5, 3); // will merge into a single block ...
        workspaceCache.translator().optimizeChildrenBlocks(key, doc, 5, 3); // will split into two blocks ...

        print(false);
        print(doc, true);
    }

    @Test
    public void shouldSplitDocumentThatRepeatedlyContainsTooManyChildReferencesIntoMultipleSegments() throws Exception {
        MutableCachedNode nodeB = check(session1).mutableNode("/childB");

        // Make it optimum to start out ...
        NodeKey key = nodeB.getKey();
        workspaceCache.translator().optimizeChildrenBlocks(key, null, 5, 2); // will merge into a single block ...
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
            workspaceCache.translator().optimizeChildrenBlocks(key, null, 5, 2); // will split into blocks ...
            print("\nOptimized...");
            print(document(key), true);
            print(false);
        }

        // Optimize the storage ...
        workspaceCache.translator().optimizeChildrenBlocks(key, null, 5, 2); // will split into blocks ...

        print(false);
        print(document(key), true);
    }

    protected Document document( NodeKey key ) {
        SchematicEntry entry = workspaceCache.database().get(key.toString());
        return entry.getContentAsDocument();
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
                       childRef.getSegment(),
                       is(expectedName));
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
                       childRef.getSegment(),
                       is(segment(expectedName, 1)));
        }
        if (expectedIter.hasNext()) {
            fail("Expected \"" + expectedIter.next() + "\" but found no such child");
        }
    }

    protected void assertInfo( SchematicEntry entry,
                               long expectedChildCount,
                               String expectedNextBlock,
                               String expectedLastBlock,
                               boolean firstBlock,
                               long expectedBlockSize ) {
        Document doc = entry.getContentAsDocument();
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
