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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.ListMultimap;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.Name;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
