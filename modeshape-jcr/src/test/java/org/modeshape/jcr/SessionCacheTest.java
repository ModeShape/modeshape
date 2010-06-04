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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.graph.session.GraphSession.PropertyInfo;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;

/**
 * 
 */
public class SessionCacheTest extends AbstractJcrTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @Test
    public void shouldRepeatedlyFindRootNode() throws Exception {
        AbstractJcrNode root = cache.findJcrRootNode();
        for (int i = 0; i != 10; ++i) {
            AbstractJcrNode node = cache.findJcrRootNode();
            assertThat(node, is(sameInstance(root)));
            // Look up the graph node ...
            assertMatchesStore(node);
        }
    }

    @Test
    public void shouldRepeatedlyFindRootNodeByPath() throws Exception {
        AbstractJcrNode root = cache.findJcrNode(null, path("/"));
        for (int i = 0; i != 10; ++i) {
            AbstractJcrNode node = cache.findJcrRootNode();
            assertThat(node, is(sameInstance(root)));
            // Look up the graph node ...
            assertMatchesStore(node);
        }
    }

    @Test
    public void shouldRepeatedlyFindRootNodeByLocationWithoutPath() throws Exception {
        AbstractJcrNode root = cache.findJcrRootNode();
        AbstractJcrNode root2 = cache.findJcrNode(Location.create(root.location.getIdProperties()));
        assertThat(root2, is(sameInstance(root)));
    }

    @Test
    public void shouldRepeatedlyFindNodeByPath() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        for (int i = 0; i != 10; ++i) {
            AbstractJcrNode hybrid2 = cache.findJcrNode(null, hybrid.path());
            assertThat(hybrid, is(sameInstance(hybrid2)));
            // Look up the graph node ...
            assertMatchesStore(hybrid2);
        }
    }

    @Test
    public void shouldRepeatedlyFindNodeByNodeId() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        for (int i = 0; i != 10; ++i) {
            AbstractJcrNode hybrid2 = cache.findJcrNode(hybrid.nodeId, null);
            assertThat(hybrid, is(sameInstance(hybrid2)));
            // Look up the graph node ...
            assertMatchesStore(hybrid2);
        }
    }

    @Test
    public void shouldRepeatedlyFindNodeByLocationWithoutPath() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        AbstractJcrNode hybrid2 = cache.findJcrNode(Location.create(hybrid.location.getIdProperties()));
        assertThat(hybrid2, is(sameInstance(hybrid)));
    }

    @Test
    public void shouldFindNodeUsingStartingNodeAndRelativePath() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        AbstractJcrNode highlander = cache.findJcrNode(hybrid.nodeId, hybrid.path(), path("../Hybrid/Toyota Highlander"));
        // Make sure this is the same as if we find it directly ...
        AbstractJcrNode highlander2 = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Highlander"));
        assertThat(highlander, is(sameInstance(highlander2)));
        assertMatchesStore(highlander);
    }

    @Test
    public void shouldFindNodeItemUsingStartingNodeAndRelativePath() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        AbstractJcrItem highlander = cache.findJcrItem(hybrid.nodeId, hybrid.path(), path("../Hybrid/Toyota Highlander"));
        assertThat(highlander.isNode(), is(true));
        // Make sure this is the same as if we find it directly ...
        AbstractJcrNode highlander2 = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Highlander"));
        assertThat((AbstractJcrNode)highlander, is(sameInstance(highlander2)));
    }

    @Test
    public void shouldFindPropertyItemUsingStartingNodeAndRelativePath() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        AbstractJcrItem altimaModel = cache.findJcrItem(hybrid.nodeId, hybrid.path(), path("../Hybrid/Nissan Altima/vehix:model"));
        assertThat(altimaModel.isNode(), is(false));
        javax.jcr.Node altimaModelParent = altimaModel.getParent();
        javax.jcr.Node altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertThat(altima, is(sameInstance(altimaModelParent)));
    }

    @Test
    public void shouldFindPropertyForNodeUsingPropertyName() throws Exception {
        AbstractJcrNode altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        AbstractJcrItem altimaModel = cache.findJcrProperty(altima.nodeId, altima.path(), name("vehix:model"));
        assertThat(altimaModel.isNode(), is(false));
        javax.jcr.Node altimaModelParent = altimaModel.getParent();
        assertThat(altima, is(sameInstance(altimaModelParent)));
    }

    @Test
    public void shouldFindPropertiesForNode() throws Exception {
        AbstractJcrNode altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(altima.nodeId, altima.path());
        assertThat(properties.size(), is(7));
        List<AbstractJcrProperty> properties2 = new ArrayList<AbstractJcrProperty>(properties);
        Collections.sort(properties2);
        Iterator<AbstractJcrProperty> iter = properties2.iterator();
        assertProperty(iter.next(), altima, "jcr:primaryType", PropertyType.NAME, "vehix:car");
        assertProperty(iter.next(), altima, "vehix:maker", PropertyType.STRING, "Nissan");
        assertProperty(iter.next(), altima, "vehix:model", PropertyType.STRING, "Altima");
        assertProperty(iter.next(), altima, "vehix:mpgCity", PropertyType.LONG, "23");
        assertProperty(iter.next(), altima, "vehix:mpgHighway", PropertyType.LONG, "32");
        assertProperty(iter.next(), altima, "vehix:msrp", PropertyType.STRING, "$18,260");
        assertProperty(iter.next(), altima, "vehix:year", PropertyType.LONG, "2008");
    }

    @Test
    public void shouldRefreshWithoutKeepingChanges() throws Exception {
        AbstractJcrNode altima1 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima1);
        assertThat(altima1.isNew(), is(false));

        // Add the child ...
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        javax.jcr.Node child = hybrid.addNode("child");
        assertThat(hybrid.isModified(), is(true));
        assertThat(child.isNew(), is(true));
        assertThat(hybrid.hasNode("child"), is(true));

        cache.refresh(false);
        AbstractJcrNode altima2 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima2);

        // The objects should no longer be the same ...
        assertThat(altima1, is(altima2));
        assertThat(altima1, is(not(sameInstance(altima2))));
        assertThat(altima2.isNew(), is(false));

        // The new child should no longer exist ...
        assertThat(hybrid.hasNode("child"), is(false));
    }

    @Test
    public void shouldRefreshAndKeepChanges() throws Exception {
        AbstractJcrNode altima1 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima1);
        assertThat(altima1.isNew(), is(false));

        // Add the child ...
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        javax.jcr.Node child = hybrid.addNode("child");
        assertThat(hybrid.isModified(), is(true));
        assertThat(child.isNew(), is(true));
        assertThat(hybrid.hasNode("child"), is(true));

        cache.refresh(true);
        AbstractJcrNode altima2 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima2);

        // The objects should still be the same ...
        assertThat(altima1, is(altima2));
        assertThat(altima1, is(sameInstance(altima2)));
        assertThat(altima2.isNew(), is(false));

        // The new child should still exist ...
        assertThat(hybrid.hasNode("child"), is(true));
    }

    @Test
    public void shouldSaveChanges() throws Exception {
        AbstractJcrNode altima1 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima1);
        assertThat(altima1.isNew(), is(false));

        // Add the child ...
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        javax.jcr.Node child = hybrid.addNode("child");
        assertThat(hybrid.isModified(), is(true));
        assertThat(child.isNew(), is(true));
        assertThat(hybrid.hasNode("child"), is(true));

        cache.save();
        AbstractJcrNode altima2 = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        assertMatchesStore(altima2);

        // The objects should no longer be the same ...
        assertThat(altima1, is(altima2));
        assertThat(altima1, is(not(sameInstance(altima2))));
        assertThat(altima2.isNew(), is(false));

        // The new child should still exist ...
        assertThat(hybrid.hasNode("child"), is(true));
    }

    @Test
    public void shouldNotExposeUuidPropertyOnNonReferenceableNodes() throws Exception {
        AbstractJcrNode highlander = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Highlander"));
        assertNotReferenceable(highlander);
    }

    @Test
    public void shouldExposeUuidPropertyOnReferenceableNodes() throws Exception {
        AbstractJcrNode hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        hybrid.addMixin("mix:referenceable"); // we don't have any referenceable nodes in our tests
        assertReferenceable(hybrid);
    }

    @Test
    public void shouldExposeUuidPropertyOnlyWhenNonReferenceableNodeTransitionsToReferenceable() throws Exception {
        AbstractJcrNode highlander = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Highlander"));
        assertNotReferenceable(highlander);
        highlander.addMixin("mix:referenceable");
        assertReferenceable(highlander);
    }

    @SuppressWarnings( "deprecation" )
    protected void assertNotReferenceable( AbstractJcrNode node ) throws RepositoryException {
        assertThat(node.isReferenceable(), is(false));
        assertThat(node.getIdentifier(), is(notNullValue()));
        try {
            node.getUUID();
            fail("should not return UUID if the node is not referenceable");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
        // Should not have a "jcr:uuid" property ...
        assertThat(node.hasProperty(JcrLexicon.UUID), is(false));
        assertThat(node.getProperty(JcrLexicon.UUID), is(nullValue()));
    }

    @SuppressWarnings( "deprecation" )
    protected void assertReferenceable( AbstractJcrNode node ) throws RepositoryException {
        assertThat(node.isReferenceable(), is(true));
        assertThat(node.getIdentifier(), is(notNullValue()));
        String uuidValue = null;
        try {
            uuidValue = node.getUUID();
            assertThat(uuidValue, is(notNullValue()));
        } catch (UnsupportedOperationException e) {
            fail("should return UUID if the node is referenceable");
        }
        // Should have a "jcr:uuid" property ...
        assertThat(node.hasProperty(JcrLexicon.UUID), is(true));
        javax.jcr.Property uuidProp = node.getProperty(JcrLexicon.UUID);
        assertThat(uuidProp, is(notNullValue()));
        assertThat(uuidProp.getString(), is(uuidValue));
        assertThat(node.getIdentifier(), is(uuidValue));
    }

    protected void assertProperty( AbstractJcrProperty property,
                                   javax.jcr.Node node,
                                   String name,
                                   int propertyType,
                                   Object... values ) throws Exception {
        assertThat(property.getName(), is(name));
        assertThat(property.getType(), is(propertyType));
        assertThat(property.getParent(), is(node));
        if (values.length > 1) {
            int i = 0;
            for (Value actual : property.getValues()) {
                String actualString = actual.getString();
                String expectedString = context.getValueFactories().getStringFactory().create(values[i]);
                assertThat(actualString, is(expectedString));
                assertCanObtainValue(actual, propertyType);
                ++i;
            }
            // Getting the single value should result in an error ...
            try {
                property.getValue();
                fail("Should not be able to call Property.getValue() on multi-valued properties");
            } catch (ValueFormatException e) {
                // expected ...
            }
        } else {
            String actualString = property.getValue().getString();
            String expectedString = context.getValueFactories().getStringFactory().create(values[0]);
            assertThat(actualString, is(expectedString));
            assertThat(actualString, is(property.getString()));
            assertCanObtainValue(property.getValue(), propertyType);
            // Getting the multiple values should result in an error ...
            try {
                property.getValues();
                fail("Should not be able to call Property.getValues() on single-valued properties");
            } catch (ValueFormatException e) {
                // expected ...
            }
            // Check resolving the reference ...
            if (propertyType == PropertyType.REFERENCE) {
                javax.jcr.Node referenced = property.getNode();
                assertThat(referenced, is(notNullValue()));
            }
        }
    }

    protected void assertCanObtainValue( Value value,
                                         int expectedType ) throws Exception {
        switch (expectedType) {
            case PropertyType.BINARY:
                Binary binary = value.getBinary();
                try {
                    InputStream stream = binary.getStream();
                    assertThat(stream, is(notNullValue()));
                    try {
                        stream.read();
                    } finally {
                        stream.close();
                    }
                } finally {
                    binary.dispose();
                }
                break;
            case PropertyType.BOOLEAN:
                assertThat(value.getBoolean() || !value.getBoolean(), is(true));
                break;
            case PropertyType.DATE:
                Calendar cal = value.getDate();
                assertThat(cal, is(notNullValue()));
                break;
            case PropertyType.DOUBLE:
                double doubleValue = value.getDouble();
                assertThat(doubleValue < 0.0d || doubleValue >= -1.0d, is(true));
                break;
            case PropertyType.LONG:
                long longValue = value.getLong();
                assertThat(longValue < 0L || longValue >= 0L, is(true));
                break;
            case PropertyType.NAME:
                context.getValueFactories().getNameFactory().create(value.getString());
                break;
            case PropertyType.PATH:
                context.getValueFactories().getPathFactory().create(value.getString());
                break;
            case PropertyType.REFERENCE:
                UUID uuid = context.getValueFactories().getUuidFactory().create(value.getString());
                assertThat(uuid, is(notNullValue()));
                break;
            case PropertyType.STRING:
                value.getString();
                break;
        }
    }

    protected void assertMatchesStore( AbstractJcrNode jcrNode ) throws RepositoryException {
        // Find the corresponding session node ...
        Node<JcrNodePayload, JcrPropertyPayload> nodeInfo = cache.findNode(jcrNode.nodeId, jcrNode.path());
        // And the graph node ...
        org.modeshape.graph.Node dnaNode = store.getNodeAt(jcrNode.location);

        assertThat(nodeInfo.getLocation(), is(dnaNode.getLocation()));
        Set<Name> propertyNames = nodeInfo.getPropertyNames();
        for (Name propertyName : propertyNames) {
            PropertyInfo<JcrPropertyPayload> info = nodeInfo.getProperty(propertyName);
            assertThat(info.getName(), is(propertyName));
            assertThat(info.getProperty().getName(), is(propertyName));
            Property actual = dnaNode.getProperty(propertyName);
            if (actual != null) {
                assertThat(info.getProperty().size(), is(actual.size()));
                assertThat(info.getProperty().getValuesAsArray(), is(actual.getValuesAsArray()));
            } else {
                if (propertyName.equals(JcrLexicon.UUID)) {
                    // check for a ModeShape UUID property ...
                    actual = dnaNode.getProperty(ModeShapeLexicon.UUID);
                    if (actual != null) {
                        assertThat(info.getProperty().size(), is(actual.size()));
                        assertThat(info.getProperty().getValuesAsArray(), is(actual.getValuesAsArray()));
                    } else {
                        fail("missing property \"" + propertyName + "\" on " + dnaNode);
                    }
                } else if (propertyName.equals(JcrLexicon.PRIMARY_TYPE)) {
                    // This is okay
                } else if (propertyName.equals(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES)) {
                    // This is okay
                } else {
                    fail("missing property \"" + propertyName + "\" on " + dnaNode);
                }
            }
        }
    }
}
