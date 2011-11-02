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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.AbstractSchematicDbTest;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.PathNotFoundException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.PropertyType;

/**
 * Abstract base class for tests that operate against a NodeCache.
 */
public abstract class AbstractNodeCacheTest extends AbstractSchematicDbTest {

    protected static final String ROOT_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";
    protected static final NodeKey ROOT_KEY_WS1 = new NodeKey("source1works1-" + ROOT_UUID);
    protected static final NodeKey ROOT_KEY_WS2 = new NodeKey("source1works1-" + ROOT_UUID);

    protected NodeCache cache;
    protected ExecutionContext context;
    private boolean print;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        print = false;
        context = new ExecutionContext();
        cache = createCache();
    }

    protected boolean print() {
        return print;
    }

    protected void print( boolean onOrOff ) {
        print = onOrOff;
    }

    protected abstract NodeCache createCache();

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    protected Segment segment( String name,
                               int index ) {
        return context.getValueFactories().getPathFactory().createSegment(name, index);
    }

    protected Segment segment( Name name,
                               int index ) {
        return context.getValueFactories().getPathFactory().createSegment(name, index);
    }

    protected String string( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected float millis( long nanos ) {
        return (float)(nanos / 1000000.0D);
    }

    protected void print( String msg ) {
        if (print) System.out.println(msg);
    }

    protected void print( Document doc ) {
        if (print) {
            try {
                Json.writePretty(doc, System.out);
                System.out.flush();
            } catch (IOException err) {
                throw new AssertionError(err);
            }
        }
    }

    protected void print( Document doc,
                          boolean deep ) {
        if (print) {
            try {
                Json.writePretty(doc, System.out);
                System.out.flush();
                if (deep) {
                    Document info = doc.getDocument(DocumentTranslator.CHILDREN_INFO);
                    if (info != null) {
                        String nextBlockKey = info.getString(DocumentTranslator.NEXT_BLOCK);
                        if (nextBlockKey != null) {
                            SchematicEntry nextEntry = database().get(nextBlockKey);
                            if (nextEntry != null && nextEntry.hasDocumentContent()) {
                                Document next = nextEntry.getContentAsDocument();
                                print(next, true);
                            }
                        }
                    }
                }
            } catch (IOException err) {
                throw new AssertionError(err);
            }
        }
    }

    protected void print( Object object ) {
        if (print) System.out.println(object);
    }

    protected PropertyFactory propertyFactory() {
        return context.getPropertyFactory();
    }

    protected Property property( Name name,
                                 Object... values ) {
        return propertyFactory().create(name, values);
    }

    protected Property property( Name name,
                                 Iterable<Object> values ) {
        return propertyFactory().create(name, values);
    }

    protected Property property( Name name,
                                 Iterator<Object> values ) {
        return propertyFactory().create(name, values);
    }

    protected Property property( Name name,
                                 PropertyType type,
                                 Object... values ) {
        return propertyFactory().create(name, type, values);
    }

    protected Property property( Name name,
                                 PropertyType type,
                                 Iterable<Object> values ) {
        return propertyFactory().create(name, type, values);
    }

    protected Property property( Name name,
                                 PropertyType type,
                                 Iterator<Object> values ) {
        return propertyFactory().create(name, type, values);
    }

    protected Property property( Name name,
                                 Path pathValue ) {
        return propertyFactory().create(name, pathValue);
    }

    protected Property property( String name,
                                 Object... values ) {
        return propertyFactory().create(name(name), values);
    }

    protected Property property( String name,
                                 Iterable<Object> values ) {
        return propertyFactory().create(name(name), values);
    }

    protected Property property( String name,
                                 Iterator<Object> values ) {
        return propertyFactory().create(name(name), values);
    }

    protected Property property( String name,
                                 PropertyType type,
                                 Object... values ) {
        return propertyFactory().create(name(name), type, values);
    }

    protected Property property( String name,
                                 PropertyType type,
                                 Iterable<Object> values ) {
        return propertyFactory().create(name(name), type, values);
    }

    protected Property property( String name,
                                 PropertyType type,
                                 Iterator<Object> values ) {
        return propertyFactory().create(name(name), type, values);
    }

    protected Property property( String name,
                                 Path pathValue ) {
        return propertyFactory().create(name(name), pathValue);
    }

    protected CacheCheck check( NodeCache cache ) {
        return new CacheCheck(cache);
    }

    public class CacheCheck {
        private final NodeCache cache;

        public CacheCheck( NodeCache cache ) {
            this.cache = cache;
        }

        public NodeKey newNodeKey() {
            return ((SessionCache)cache).createNodeKey();
        }

        public CachedNode rootNode() {
            return node(cache.getRootKey());
        }

        public CachedNode node( Path path ) {
            CachedNode node = rootNode();
            for (Segment segment : path) {
                ChildReferences children = node.getChildReferences(cache);
                ChildReference child = children.getChild(segment);
                if (child == null) {
                    throw new PathNotFoundException(path, node.getKey(), node.getPath(cache));
                }
                NodeKey childKey = child.getKey();
                CachedNode childNode = cache.getNode(childKey);
                if (childNode == null) {
                    throw new PathNotFoundException(path, node.getKey(), node.getPath(cache));
                }
                node = childNode;
            }
            assertThat(node, is(notNullValue()));
            return node;
        }

        public CachedNode node( String path ) {
            return node(path(path));
        }

        public CachedNode node( NodeKey key ) {
            CachedNode node = cache.getNode(key);
            if (node == null) {
                throw new NodeNotFoundException(key);
            }
            return node;
        }

        public MutableCachedNode mutableNode( CachedNode node ) {
            if (node instanceof MutableCachedNode) return (MutableCachedNode)node;
            return ((SessionCache)cache).mutable(node.getKey());
        }

        public MutableCachedNode mutableNode( String path ) {
            return mutableNode(path(path));
        }

        public MutableCachedNode mutableNode( Path path ) {
            CachedNode node = node(path);
            return mutableNode(node);
        }

        public void noNode( String path ) {
            noNode(path(path));
        }

        public void noNode( Path path ) {
            try {
                node(path);
                fail("Found node at \"" + string(path) + "\" when it was not expected");
            } catch (PathNotFoundException e) {
                // expected ...
            }
        }

        public Property property( String path,
                                  Property expectedProperty ) {
            return property(node(path).getKey(), expectedProperty);
        }

        public Property property( Path path,
                                  Property expectedProperty ) {
            return property(node(path).getKey(), expectedProperty);
        }

        public Property property( CachedNode node,
                                  Property expectedProperty ) {
            return property(node.getKey(), expectedProperty);
        }

        public Property property( NodeKey key,
                                  Property expectedProperty ) {
            CachedNode node = node(key);
            Property actual = node.getProperty(expectedProperty.getName(), cache);
            assertThat(actual, is(notNullValue()));
            assertThat(actual, is(expectedProperty));
            return actual;
        }

        public void noProperty( String path,
                                Name propertyName ) {
            noProperty(node(path).getKey(), propertyName);
        }

        public void noProperty( Path path,
                                Name propertyName ) {
            noProperty(node(path).getKey(), propertyName);
        }

        public void noProperty( CachedNode node,
                                Name propertyName ) {
            noProperty(node.getKey(), propertyName);
        }

        public void noProperty( NodeKey key,
                                Name propertyName ) {
            CachedNode node = node(key);
            Property actual = node.getProperty(propertyName, cache);
            assertThat(actual, is(nullValue()));
        }

        public void noProperty( String path,
                                String propertyName ) {
            noProperty(node(path).getKey(), propertyName);
        }

        public void noProperty( Path path,
                                String propertyName ) {
            noProperty(node(path).getKey(), propertyName);
        }

        public void noProperty( CachedNode node,
                                String propertyName ) {
            noProperty(node.getKey(), propertyName);
        }

        public void noProperty( NodeKey key,
                                String propertyName ) {
            noProperty(key, name(propertyName));
        }

        public void children( CachedNode node,
                              String... childSegments ) {
            children(node.getKey(), childSegments);
        }

        public void children( NodeKey key,
                              String... childSegments ) {
            CachedNode node = node(key);
            List<Segment> segments = new ArrayList<Segment>();
            for (String childSegment : childSegments) {
                segments.add(segment(childSegment));
            }
            Iterator<Segment> expectedIter = segments.iterator();
            for (ChildReference childRef : node.getChildReferences(cache)) {
                Segment actual = childRef.getSegment();
                if (!expectedIter.hasNext()) {
                    fail("Found child \"" + string(actual) + "\" but expected no more children");
                }
                Segment expected = expectedIter.next();
                if (!actual.equals(expected)) {
                    Path path = node.getPath(cache);
                    String msg = "Expected \"" + string(expected) + "\" but found \"" + string(actual) + "\" in children of \""
                                 + string(path) + "\"";
                    assertThat(msg, actual, is(expected));
                }
            }
            if (expectedIter.hasNext()) {
                Path path = node.getPath(cache);
                StringBuilder str = new StringBuilder("Found extra children of \"" + string(path) + "\": ");
                str.append('"');
                str.append(string(expectedIter.next()));
                str.append('"');
                while (expectedIter.hasNext()) {
                    str.append("\", \"");
                    str.append(string(expectedIter.next()));
                    str.append('"');
                }
                fail(str.toString());
            }
        }
    }

    /**
     * The name of the JSON document which has a single "data" array field with Document instances for each node.
     * 
     * @return the string name of the resource file accessible via the class loader (e.g., "data/simple.json"); may not be null
     */
    protected String resourceNameForWorkspaceContentDocument() {
        return "data/simple.json";
    }

    @Test
    public void shouldLoadSimpleData() {
        assertThat(database().get(ROOT_KEY_WS1.toString()), is(notNullValue()));
        assertThat(database().get("source1system-jcrsystem"), is(notNullValue()));
        assertThat(database().get("source1system-jcrnamespaces"), is(notNullValue()));
        assertThat(database().get("source1works1-childA"), is(notNullValue()));
        assertThat(database().get("source1works1-childB"), is(notNullValue()));
        assertThat(database().get("source1works1-childC"), is(notNullValue()));
        assertThat(database().get(ROOT_KEY_WS2.toString()), is(notNullValue()));
        assertThat(database().get("source1works2-childX"), is(notNullValue()));
    }

    @Test
    public void shouldLoadRootNodeIntoCache() {
        CachedNode node = cache.getNode(cache.getRootKey());
        assertThat(node, is(notNullValue()));
        assertThat(node.getKey(), is(cache.getRootKey()));
        assertThat(node.getPath(cache).isRoot(), is(true));
        assertThat(node.getName(cache).getLocalName().length(), is(0));
        assertThat(node.getName(cache).getNamespaceUri().length(), is(0));

        // Check the properties ...
        assertThat(node.getProperty(JcrLexicon.UUID, cache).getFirstValue().toString(), is(node.getKey().getIdentifier()));
        assertThat(node.getProperty(JcrLexicon.PRIMARY_TYPE, cache).getFirstValue(), is((Object)ModeShapeLexicon.ROOT));

        // Check the child references ...
        ChildReferences refs = node.getChildReferences(cache);
        assertThat(refs, is(notNullValue()));
        assertThat(refs.size(), is(3L));
        Iterator<ChildReference> iter = refs.iterator();

        ChildReference system = refs.getChild(JcrLexicon.SYSTEM);
        ChildReference childA = refs.getChild(name("childA"));
        ChildReference childB = refs.getChild(name("childB"));

        assertThat(system.getKey().toString(), is("source1system-jcrsystem"));
        assertThat(childA.getKey().toString(), is("source1works1-childA"));
        assertThat(childB.getKey().toString(), is("source1works1-childB"));

        assertThat(system.getName(), is(JcrLexicon.SYSTEM));
        assertThat(childA.getName(), is(name("childA")));
        assertThat(childB.getName(), is(name("childB")));
        assertThat(childA.getSnsIndex(), is(1));
        assertThat(childB.getSnsIndex(), is(1));

        assertThat(iter.next(), is(system));
        assertThat(iter.next(), is(childA));
        assertThat(iter.next(), is(childB));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldNavigateFromRootToSystemNode() {
        CachedNode node = cache.getNode(cache.getRootKey());
        ChildReference systemRef = node.getChildReferences(cache).getChild(JcrLexicon.SYSTEM);

        // print = true;

        long nanos = System.nanoTime();
        CachedNode system = cache.getNode(systemRef);
        print("Time (load): " + millis(System.nanoTime() - nanos) + " ms");

        for (int i = 0; i != 10; ++i) {
            cache.clear();
            nanos = System.nanoTime();
            system.getKey();
            system.getPath(cache);
            system.getName(cache);
            system.getProperty(JcrLexicon.UUID, cache);
            system.getProperty(JcrLexicon.PRIMARY_TYPE, cache);
            print("Time (read): " + millis(System.nanoTime() - nanos) + " ms");
        }

        nanos = System.nanoTime();
        system.getKey();
        system.getPath(cache);
        system.getName(cache);
        system.getProperty(JcrLexicon.UUID, cache);
        system.getProperty(JcrLexicon.PRIMARY_TYPE, cache);
        print("Time (read): " + millis(System.nanoTime() - nanos) + " ms");

        assertThat(system, is(notNullValue()));
        assertThat(system.getKey(), is(systemRef.getKey()));
        assertThat(system.getPath(cache).isRoot(), is(false));
        assertThat(system.getName(cache), is(JcrLexicon.SYSTEM));
        assertThat(system.getProperty(JcrLexicon.UUID, cache).getFirstValue().toString(),
                   is("56b3feae-3def-44f7-a433-586413f312e4"));
        assertThat(system.getProperty(JcrLexicon.PRIMARY_TYPE, cache).getFirstValue(), is((Object)ModeShapeLexicon.SYSTEM));

        // Check the child references ...
        ChildReferences refs = system.getChildReferences(cache);
        assertThat(refs, is(notNullValue()));
        assertThat(refs.size(), is(1L));
        Iterator<ChildReference> iter = refs.iterator();

        ChildReference namespaces = refs.getChild(ModeShapeLexicon.NAMESPACES);
        assertThat(namespaces.getKey().toString(), is("source1system-jcrnamespaces"));
        assertThat(namespaces.getName(), is(ModeShapeLexicon.NAMESPACES));

        assertThat(iter.next(), is(namespaces));
        assertThat(iter.hasNext(), is(false));

        nanos = System.nanoTime();
        CachedNode namespacesNode = cache.getNode(namespaces);
        print("Time (load): " + millis(System.nanoTime() - nanos) + " ms");
        nanos = System.nanoTime();
        assertThat(namespacesNode.getPath(cache), is(path("/jcr:system/mode:namespaces")));
        assertThat(namespacesNode.getChildReferences(cache).isEmpty(), is(true));
        print("Time (read): " + millis(System.nanoTime() - nanos) + " ms");
    }

    @Test
    public void shouldLoadChildrenReferencesWhenBackedByMultipleBlocks() {
        CachedNode root = cache.getNode(cache.getRootKey());
        CachedNode childB = cache.getNode(root.getChildReferences(cache).getChild(name("childB")));

        ChildReferences refs = childB.getChildReferences(cache);
        assertThat(refs, is(notNullValue()));
        assertThat(refs.size(), is(2L));
        Iterator<ChildReference> iter = refs.iterator();

        ChildReference childC = refs.getChild(name("childC"));
        ChildReference childD = refs.getChild(name("childD"));

        assertThat(childC.getKey().toString(), is("source1works1-childC"));
        assertThat(childD.getKey().toString(), is("source1works1-childD"));

        assertThat(iter.next(), is(childC)); // from first block
        assertThat(iter.next(), is(childD)); // from second block
        assertThat(iter.hasNext(), is(false));
    }

    // @Test
    public void generateUuids() {
        for (int i = 0; i != 10; ++i) {
            System.out.println(UUID.randomUUID());
        }
    }

    @Test
    public void shouldGetNodesByPath() {
        CacheCheck check = check(cache);
        check.rootNode();
        check.node("/jcr:system");
        check.node("/childA");
        check.node("/childB");
        check.node("/childB/childC");
        check.node("/childB/childD");
    }

}
