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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import org.junit.Before;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

public abstract class AbstractJcrRepositoryTest extends AbstractTransactionalTest {

    protected boolean print;

    @Before
    public void beforeEach() throws Exception {
        print = false;
    }

    protected abstract JcrRepository repository();

    protected abstract JcrSession session();

    protected Path path( String path ) {
        return session().context().getValueFactories().getPathFactory().create(path);
    }

    protected String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected String asString( Object value ) {
        return session().context().getValueFactories().getStringFactory().create(value);
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat("Did not expect to find '" + path + "'", session().getRootNode().hasNode(relativePath(path)), is(false));
        try {
            session().getNode(path);
            fail("Did not expect to find node at \"" + path + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected Node assertNode( String path ) throws RepositoryException {
        if (print && !session().getRootNode().hasNode(path)) {
            // We won't find the node, so print out the information ...
            Node parent = session().getRootNode();
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }

        Node node = session().getNode(path);
        assertThat(node, is(notNullValue()));
        // Verify that the path can be found via navigating ...
        if (path.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(session().getRootNode(), is(notNullValue()));
        } else {

        }
        return node;
    }

    protected Node assertNode( String path,
                               String primaryType ) throws RepositoryException {
        Node node = assertNode(path);
        assertEquals(primaryType, node.getPrimaryNodeType().getName());
        return node;
    }

    protected void assertSameProperties( Node node1,
                                         Node node2,
                                         String... excludedPropertyNames ) throws RepositoryException {
        Set<String> excludedNames = new HashSet<String>(Arrays.asList(excludedPropertyNames));
        Set<String> node2Names = new HashSet<String>();

        // Find the names of all (non-excluded) proeprties in node 2 ...
        PropertyIterator iter = node2.getProperties();
        while (iter.hasNext()) {
            Property prop2 = iter.nextProperty();
            node2Names.add(prop2.getName());
        }
        node2Names.removeAll(excludedNames);

        iter = node1.getProperties();
        while (iter.hasNext()) {
            Property prop1 = iter.nextProperty();
            String name = prop1.getName();
            if (excludedNames.contains(name)) continue;
            Property prop2 = node2.getProperty(prop1.getName());
            assertThat(prop1.isMultiple(), is(prop2.isMultiple()));
            if (prop1.isMultiple()) {
                Value[] values1 = prop1.getValues();
                Value[] values2 = prop2.getValues();
                assertThat(values1, is(values2));
            } else {
                assertThat(prop1.getValue().getString(), is(prop2.getValue().getString()));
            }
            node2Names.remove(name);
        }

        // There should be no more properties left ...
        if (!node2Names.isEmpty()) {
            fail("Found extra properties in node2: " + node2Names);
        }
    }

    protected void addMixinRecursively( String path,
                                        String... nodeTypes ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        addMixin(node, true, nodeTypes);
    }

    protected Node addMixin( String path,
                             String... nodeTypes ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        return addMixin(node, false, nodeTypes);
    }

    protected Node addMixin( Node node,
                             boolean recursive,
                             String... nodeTypes ) throws RepositoryException {
        assertThat(node, is(notNullValue()));
        for (String nodeType : nodeTypes) {
            if (!hasMixin(node, nodeType)) {
                node.addMixin(nodeType);
            }
        }
        if (recursive) {
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                addMixin(children.nextNode(), true, nodeTypes);
            }
        }
        return node;
    }

    protected boolean hasMixin( Node node,
                                String mixinNodeType ) throws RepositoryException {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.getName().equals(mixinNodeType)) return true;
        }
        return false;
    }

    protected void print() throws RepositoryException {
        print(session().getRootNode(), true);
    }

    protected void print( String path ) throws RepositoryException {
        Node node = session().getRootNode().getNode(relativePath(path));
        print(node, true);
    }

    protected void print( Node node,
                          boolean includeSystem ) throws RepositoryException {
        if (print) {
            if (!includeSystem && node.getPath().equals("/jcr:system")) return;
            if (node.getDepth() != 0) {
                int snsIndex = node.getIndex();
                String segment = node.getName() + (snsIndex > 1 ? ("[" + snsIndex + "]") : "");
                System.out.println(StringUtil.createString(' ', 2 * node.getDepth()) + '/' + segment);
            }
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                print(children.nextNode(), includeSystem);
            }
        }
    }
}
