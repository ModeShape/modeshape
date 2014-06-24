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

package org.modeshape.jcr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.ListMultimap;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Property;
import org.modeshape.jcr.api.PropertyType;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * Unit test for the initial content import feature.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrInitialContentTest extends SingleUseAbstractTest {

    @Test
    public void shouldImportInitialContentForAllWorkspaceConfigurations() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content.json"));
        //preconfigured ws
        String ws1 = "ws1";
        assertCarsWithMixins(ws1);

        //preconfigured ws
        String ws2 = "ws2";
        assertFilesAndFolders(ws2);

        //default ws
        String defaultWs = "default";
        assertCarsWithoutNamespace(defaultWs);

        //create a new ws that has been configured with an empty import
        String ws4 = "ws4";
        session.getWorkspace().createWorkspace(ws4);
        JcrSession ws4Session = repository.login(ws4);
        NodeIterator rootIterator = ws4Session.getNode("/").getNodes();
        assertEquals("Expected an empty workspace", 1, rootIterator.getSize());

        //create a new ws that has been configured the same as ws2
        String ws5 = "ws5";
        session.getWorkspace().createWorkspace(ws5);
        assertFilesAndFolders(ws5);

        //create a new ws that doesn't have a dedicated config, but should fall back to default
        String ws6 = "ws6";
        session.getWorkspace().createWorkspace(ws6);
        assertCarsWithMixins(ws6);
    }

    @Test
    @FixFor( "MODE-1959" )
    public void shouldImportInitialContentWhenTransactionModeNone() throws  Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content-transaction-mode-none.json"));
        assertCarsWithoutNamespace("default");
    }

    @Test
    @FixFor("MODE-1995")
    public void shouldImportInitialContentWhenContainsNonHTTPNamespaceURIs() throws  Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content-with-non-HTTP-namespaces.json"));

        String defaultWs = "default";
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Hybrid", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Hybrid/{http://default.namespace.com}Toyota Prius",
                                 JcrConstants.NT_UNSTRUCTURED, null,
                                 "{http://default.namespace.com}maker=Toyota",
                                 "{http://default.namespace.com}model=Prius");
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Hybrid/{http://default.namespace.com}Toyota Highlander",
                                 JcrConstants.NT_UNSTRUCTURED, null,
                                 "{http://default.namespace.com}maker=Toyota",
                                 "{http://default.namespace.com}model=Highlander");
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Hybrid/{http://default.namespace.com}Nissan Altima",
                                 JcrConstants.NT_UNSTRUCTURED, null,
                                 "{http://default.namespace.com}maker=Nissan",
                                 "{http://default.namespace.com}model=Altima");
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Sports", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Sports/{http://default.namespace.com}Aston Martin DB9",
                                 JcrConstants.NT_UNSTRUCTURED, null,
                                 "{http://default.namespace.com}maker=Aston Martin",
                                 "{http://default.namespace.com}model=DB9");
        assertContentInWorkspace(defaultWs, "/{info:test/ns/}Cars/{info:test/ns/}Sports/{http://default.namespace.com}Infiniti G37",
                                 JcrConstants.NT_UNSTRUCTURED, null,
                                 "{http://default.namespace.com}maker=Infiniti",
                                 "{http://default.namespace.com}model=G37");
    }

    @Test
    @FixFor( "MODE-2217" )
    public void shouldKeepChildrenOrder() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content-children-order.json"));
        JcrSession session = repository.login();
        try {
            NodeIterator children = session.getNode("/testRoot").getNodes();
            assertEquals(3, children.getSize());

            Node node3 = children.nextNode();
            assertEquals("/testRoot/node3", node3.getPath());
            NodeIterator subChildren = node3.getNodes();
            assertEquals(2, subChildren.getSize());
            assertEquals("/testRoot/node3/node3_2", subChildren.nextNode().getPath());
            assertEquals("/testRoot/node3/node3_1", subChildren.nextNode().getPath());

            assertEquals("/testRoot/node2", children.nextNode().getPath());

            Node node1 = children.nextNode();
            assertEquals("/testRoot/node1", node1.getPath());
            subChildren = node1.getNodes();
            assertEquals(2, subChildren.getSize());
            assertEquals("/testRoot/node1/node1_2", subChildren.nextNode().getPath());
            assertEquals("/testRoot/node1/node1_1", subChildren.nextNode().getPath());
        } finally {
            session.logout();
        }
    }

    @Test
    @FixFor( "MODE-2241" )
    public void shouldSupportReferenceProperties() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content-references.json"));
        JcrSession session = repository.login();
        AbstractJcrNode node1 = session.getNode("/node1");
        AbstractJcrNode node2 = session.getNode("/node2");
        AbstractJcrNode node3 = session.getNode("/node3");

        Property hard_ref11 = node1.getProperty("hard_ref11");
        assertEquals(javax.jcr.PropertyType.REFERENCE, hard_ref11.getType());
        assertEquals(node2.getIdentifier(), hard_ref11.getNode().getIdentifier());
        Property hard_ref12 = node1.getProperty("hard_ref12");
        assertEquals(javax.jcr.PropertyType.REFERENCE, hard_ref12.getType());
        assertTrue(hard_ref12.isMultiple());
        List<String> nodeIdentifiers = new ArrayList<>();
        for (Value value : hard_ref12.getValues()) {
            nodeIdentifiers.add(value.getString());
        }
        assertTrue(nodeIdentifiers.remove(node2.getIdentifier()));
        assertTrue(nodeIdentifiers.remove(node3.getIdentifier()));

        Property weak_ref11 = node1.getProperty("weak_ref11");
        assertEquals(javax.jcr.PropertyType.WEAKREFERENCE, weak_ref11.getType());
        assertEquals(node2.getIdentifier(), weak_ref11.getNode().getIdentifier());
        Property weak_ref12 = node1.getProperty("weak_ref12");
        assertEquals(javax.jcr.PropertyType.WEAKREFERENCE, weak_ref12.getType());
        assertEquals(node2.getIdentifier(), weak_ref12.getNode().getIdentifier());

        Property simple_ref11 = node1.getProperty("simple_ref11");
        assertEquals(PropertyType.SIMPLE_REFERENCE, simple_ref11.getType());
        assertEquals(node2.getIdentifier(), simple_ref11.getNode().getIdentifier());
        Property simple_ref12 = node1.getProperty("simple_ref12");
        assertEquals(PropertyType.SIMPLE_REFERENCE, simple_ref12.getType());
        assertEquals(node2.getIdentifier(), simple_ref12.getNode().getIdentifier());

        Property hard_ref21 = node2.getProperty("hard_ref21");
        assertEquals(javax.jcr.PropertyType.REFERENCE, hard_ref21.getType());
        assertEquals(node1.getIdentifier(), hard_ref21.getNode().getIdentifier());
        Property hard_ref22 = node2.getProperty("hard_ref22");
        assertEquals(javax.jcr.PropertyType.REFERENCE, hard_ref22.getType());
        assertTrue(hard_ref22.isMultiple());
        nodeIdentifiers = new ArrayList<>();
        for (Value value : hard_ref22.getValues()) {
            nodeIdentifiers.add(value.getString());
        }
        assertTrue(nodeIdentifiers.remove(node1.getIdentifier()));
        assertTrue(nodeIdentifiers.remove(node3.getIdentifier()));

        Property weak_ref21 = node2.getProperty("weak_ref21");
        assertEquals(javax.jcr.PropertyType.WEAKREFERENCE, weak_ref21.getType());
        assertEquals(node1.getIdentifier(), weak_ref21.getNode().getIdentifier());
        Property weak_ref22 = node2.getProperty("weak_ref22");
        assertEquals(javax.jcr.PropertyType.WEAKREFERENCE, weak_ref22.getType());
        assertEquals(node1.getIdentifier(), weak_ref22.getNode().getIdentifier());

        Property simple_ref21 = node2.getProperty("simple_ref21");
        assertEquals(PropertyType.SIMPLE_REFERENCE, simple_ref21.getType());
        assertEquals(node1.getIdentifier(), simple_ref21.getNode().getIdentifier());
        Property simple_ref22 = node2.getProperty("simple_ref22");
        assertEquals(PropertyType.SIMPLE_REFERENCE, simple_ref22.getType());
        assertEquals(node1.getIdentifier(), simple_ref22.getNode().getIdentifier());
    }

    @Test
    @FixFor( "MODE-2241" )
    public void shouldSupportVariousPropertyTypes() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-initial-content-props.json"));
        JcrSession session = repository.login();
        AbstractJcrNode node1 = session.getNode("/node1");

        Property stringProp = node1.getProperty("string_prop");
        assertEquals("string", stringProp.getString());
        assertEquals(javax.jcr.PropertyType.STRING, stringProp.getType());

        Property booleanProp = node1.getProperty("boolean_prop");
        assertEquals(true, booleanProp.getBoolean());
        assertEquals(javax.jcr.PropertyType.BOOLEAN, booleanProp.getType());

        Property longProp = node1.getProperty("long_prop");
        assertEquals(123456l, longProp.getLong());
        assertEquals(javax.jcr.PropertyType.LONG, longProp.getType());

        Property decimalProp = node1.getProperty("decimal_prop");
        assertEquals(BigDecimal.valueOf(12.3), decimalProp.getDecimal());
        assertEquals(javax.jcr.PropertyType.DECIMAL, decimalProp.getType());

        Property dateProp = node1.getProperty("date_prop");
        assertEquals(new JodaDateTime("1994-11-05T13:15:30Z").toCalendar(), dateProp.getDate());
        assertEquals(javax.jcr.PropertyType.DATE, dateProp.getType());

        Property doubleProp = node1.getProperty("double_prop");
        assertEquals(12.3, doubleProp.getDouble(), 0);
        assertEquals(javax.jcr.PropertyType.DOUBLE, doubleProp.getType());

        Property nameProp = node1.getProperty("name_prop");
        assertEquals("nt:undefined", nameProp.getString());
        assertEquals(javax.jcr.PropertyType.NAME, nameProp.getType());

        Property pathProp = node1.getProperty("path_prop");
        assertEquals("/a/b/c", pathProp.getString());
        assertEquals(javax.jcr.PropertyType.PATH, pathProp.getType());

        Property uriProp = node1.getProperty("uri_prop");
        assertEquals("http://www.google.com", uriProp.getString());
        assertEquals(javax.jcr.PropertyType.URI, uriProp.getType());

        Property binaryProp = node1.getProperty("binary_prop");
        assertEquals(javax.jcr.PropertyType.BINARY, binaryProp.getType());
        try (InputStream is = binaryProp.getBinary().getStream()) {
            byte[] actualBytes = IoUtil.readBytes(is);
            byte[] expectedBytes = IoUtil.readBytes(getClass().getClassLoader().getResourceAsStream("io/file1.txt"));
            assertArrayEquals(expectedBytes, actualBytes);
        }

        Property referenceProp = node1.getProperty("reference_prop");
        assertEquals(javax.jcr.PropertyType.REFERENCE, referenceProp.getType());
        assertEquals(session.getNode("/node2").getIdentifier(), referenceProp.getNode().getIdentifier());

        Property weakReferenceProp = node1.getProperty("weakreference_prop");
        assertEquals(javax.jcr.PropertyType.WEAKREFERENCE, weakReferenceProp.getType());
        assertEquals(session.getNode("/node2").getIdentifier(), weakReferenceProp.getNode().getIdentifier());

        Property simpleReferenceProp = node1.getProperty("simplereference_prop");
        assertEquals(PropertyType.SIMPLE_REFERENCE, simpleReferenceProp.getType());
        assertEquals(session.getNode("/node2").getIdentifier(), simpleReferenceProp.getNode().getIdentifier());
    }

    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }

    private void assertCarsWithoutNamespace( String defaultWs ) throws Exception {
        assertContentInWorkspace(defaultWs, "/Cars", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/Cars/Hybrid", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/Cars/Hybrid/car", JcrConstants.NT_UNSTRUCTURED, null, "name=Toyota Prius",
                                 "maker=Toyota", "model=Prius");
        assertContentInWorkspace(defaultWs, "/Cars/Hybrid/car[2]", JcrConstants.NT_UNSTRUCTURED, null, "name=Toyota Highlander",
                                 "maker=Toyota", "model=Highlander");
        assertContentInWorkspace(defaultWs, "/Cars/Hybrid/car[3]", JcrConstants.NT_UNSTRUCTURED, null, "name=Nissan Altima",
                                 "maker=Nissan", "model=Altima");
        assertContentInWorkspace(defaultWs, "/Cars/Sports", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(defaultWs, "/Cars/Sports/car", JcrConstants.NT_UNSTRUCTURED, null, "name=Aston Martin DB9",
                                 "maker=Aston Martin", "model=DB9");
        assertContentInWorkspace(defaultWs, "/Cars/Sports/car[2]", JcrConstants.NT_UNSTRUCTURED, null, "name=Infiniti G37",
                                 "maker=Infiniti", "model=G37");
    }

    private void assertFilesAndFolders( String ws2 ) throws Exception {
        List<String> expectedMixinsList = Arrays.asList("mix:created", "mix:lastModified");

        assertContentInWorkspace(ws2, "/folder", JcrConstants.NT_FOLDER, expectedMixinsList);
        assertContentInWorkspace(ws2, "/folder/file1", JcrConstants.NT_FILE, null);
        assertContentInWorkspace(ws2, "/folder/file1/jcr:content", JcrConstants.NT_UNSTRUCTURED, null);
        assertContentInWorkspace(ws2, "/folder/file2", JcrConstants.NT_FILE, null);
        assertContentInWorkspace(ws2, "/folder/file2/jcr:content", JcrConstants.NT_UNSTRUCTURED, null);
    }

    private List<String> assertCarsWithMixins( String ws1 ) throws Exception {
        List<String> expectedMixinsList = Arrays.asList("mix:created", "mix:lastModified");

        assertContentInWorkspace(ws1, "/cars", JcrConstants.NT_UNSTRUCTURED, expectedMixinsList);
        return expectedMixinsList;
    }

    private void assertContentInWorkspace( String workspaceName,
                                           String nodePath,
                                           String nodeType,
                                           List<String> nodeMixins,
                                           String... properties ) throws Exception {
        JcrSession session = repository.login(workspaceName);
        try {
            AbstractJcrNode node = session.getNode(nodePath);
            assertNodeType(nodeType, node);
            assertMixins(nodeMixins, node);
            assertProperties(node, properties);
        } finally {
            session.logout();
        }
    }

    private void assertProperties( AbstractJcrNode node,
                                   String[] properties ) throws RepositoryException {
        if (properties.length > 0) {
            ListMultimap<String, String> nodeProperties = ArrayListMultimap.create();
            for (PropertyIterator propertyIterator = node.getProperties(); propertyIterator.hasNext(); ) {
                AbstractJcrProperty property = (AbstractJcrProperty)propertyIterator.nextProperty();
                String propertyName = !StringUtil.isBlank(property.name().getNamespaceUri()) ? property.name().toString() : property.getLocalName();
                if (property.isMultiple()) {
                    for (Value value : property.getValues()) {
                        nodeProperties.put(propertyName, value.getString());
                    }
                } else {
                    nodeProperties.put(propertyName, property.getValue().getString());
                }
            }

            for (String propertyValueString : properties) {
                String[] parts = propertyValueString.split("=");
                String propertyName = parts[0];
                Assert.assertTrue("Property " + propertyName + " not found", nodeProperties.containsKey(propertyName));
                String propertyValue = parts[1];

                Set<String> expectedValues = new TreeSet<String>(Arrays.asList(propertyValue.split(",")));
                Set<String> actualValues = new TreeSet<String>(nodeProperties.get(propertyName));

                assertEquals("Property values do not match for " + propertyName, expectedValues, actualValues);
            }
        }
    }

    private void assertMixins( List<String> nodeMixins,
                               AbstractJcrNode node ) throws ItemNotFoundException, InvalidItemStateException {
        if (nodeMixins != null && !nodeMixins.isEmpty()) {
            for (Name mixinName : node.getMixinTypeNames()) {
                assertTrue("Mixin not expected:" + mixinName.getString(), nodeMixins.contains(mixinName.getString()));
            }
        }
    }

    private void assertNodeType( String nodeType,
                                 AbstractJcrNode node ) throws ItemNotFoundException, InvalidItemStateException {
        if (!StringUtil.isBlank(nodeType)) {
            assertEquals("Invalid node type " + nodeType, nodeType, node.getPrimaryTypeName().getString());
        }
    }

}
