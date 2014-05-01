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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.Namespaced;

/**
 * Unit test for the node-types feature, which allows initial cnd files to be pre-configured in a repository
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrNodeTypesTest extends SingleUseAbstractTest {

    @Test
    public void shouldRegisterCustomNodeTypeAtStartup() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/repo-config-node-types.json"));

        validateNodesWithCustomTypes();
    }

    @Test
    public void shouldRegisterValidNodeTypesOnly() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-invalid-node-types.json"));

        validateNodesWithCustomTypes();
    }

    @Test
    @FixFor( "MODE-1687" )
    public void shouldRegisterBothUsedAndUnusedNamespacesFromCNDFile() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/repo-config-node-types.json"));
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        assertEquals("http://www.modeshape.org/examples/cars/1.0", namespaceRegistry.getURI("car"));
        assertEquals("http://www.modeshape.org/examples/aircraft/1.0", namespaceRegistry.getURI("air"));
        assertEquals("http://www.test1.com", namespaceRegistry.getURI("test1"));
        assertEquals("http://www.test2.com", namespaceRegistry.getURI("test2"));
    }

    @Test
    @FixFor( "MODE-1722" )
    public void shouldRegisterNodeTypeWithUriPropertyType() throws Exception {
        startRepository();
        registerNodeTypes("cnd/nodetype-with-uri-property.cnd");
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeType nt = ntmgr.getNodeType("ex:myNodeType");
        PropertyDefinition uriPropDefn = nt.getDeclaredPropertyDefinitions()[0];
        assertLocalNameAndNamespace(nt, "myNodeType", "ex");
        assertThat(uriPropDefn.getName(), is("ex:path"));
        assertThat(uriPropDefn.getRequiredType(), is(PropertyType.URI));
    }

    @Test
    @FixFor( "MODE-1878" )
    public void shouldRegisterNodeTypesWithSnsAndCreateChildren() throws Exception {
        startRepository();
        registerNodeTypes("cnd/nodetypes-with-sns.cnd");
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeType nt = ntmgr.getNodeType("inf:patient");
        PropertyDefinition uriPropDefn = nt.getDeclaredPropertyDefinitions()[0];
        assertThat(uriPropDefn.getName(), is("inf:masterId"));
        assertThat(uriPropDefn.getRequiredType(), is(PropertyType.STRING));

        Node top = session.getRootNode().addNode("top");
        Node patient = top.addNode("patient", "inf:patient");
        patient.setProperty("inf:masterId", "id1");
        patient.setProperty("inf:masterNs", "ns1");
        Node section1 = patient.addNode("section", "inf:section");
        section1.setProperty("inf:name", "sectionA");
        assertThat(section1.getIndex(), is(1));
        assertThat(section1.getPath(), is("/top/patient/section"));
        Node section2 = patient.addNode("section", "inf:section");
        section2.setProperty("inf:name", "sectionA");
        assertThat(section2.getIndex(), is(2));
        assertThat(section2.getPath(), is("/top/patient/section[2]"));
        assertThat(section2.getDefinition().allowsSameNameSiblings(), is(true));
        assertThat(section2.getDefinition().getName(), is("*"));
        session.save();

        Node section3 = patient.addNode("section", "inf:section");
        section3.setProperty("inf:name", "sectionA");
        assertThat(section3.getIndex(), is(3));
        assertThat(section3.getPath(), is("/top/patient/section[3]"));
        session.save();

        Node section4 = patient.addNode("section", "inf:section");
        section4.setProperty("inf:name", "sectionA");
        assertThat(section4.getIndex(), is(4));
        assertThat(section4.getPath(), is("/top/patient/section[4]"));
        Node section5 = patient.addNode("section", "inf:section");
        section5.setProperty("inf:name", "sectionA");
        assertThat(section5.getIndex(), is(5));
        assertThat(section5.getPath(), is("/top/patient/section[5]"));
        section2.remove();
        session.save();
    }

    @Test
    @FixFor( "MODE-1878" )
    public void shouldRegisterNodeTypesWithSnsAndCreateChildrenAlternative2() throws Exception {
        startRepository();
        registerNodeTypes("cnd/nodetypes-with-sns.cnd");
        NodeTypeManager ntmgr = session.getWorkspace().getNodeTypeManager();
        NodeType nt = ntmgr.getNodeType("inf:patient");
        PropertyDefinition uriPropDefn = nt.getDeclaredPropertyDefinitions()[0];
        assertThat(uriPropDefn.getName(), is("inf:masterId"));
        assertThat(uriPropDefn.getRequiredType(), is(PropertyType.STRING));

        Node top = session.getRootNode().addNode("top");
        Node patient = top.addNode("patient", "inf:patient");
        patient.setProperty("inf:masterId", "id1");
        patient.setProperty("inf:masterNs", "ns1");
        Node section1 = patient.addNode("section", "inf:section");
        section1.setProperty("inf:name", "sectionA");
        assertThat(section1.getIndex(), is(1));
        assertThat(section1.getPath(), is("/top/patient/section"));
        session.save();

        Node section2 = patient.addNode("section", "inf:section");
        section2.setProperty("inf:name", "sectionA");
        assertThat(section2.getIndex(), is(2));
        assertThat(section2.getPath(), is("/top/patient/section[2]"));
        assertThat(section2.getDefinition().allowsSameNameSiblings(), is(true));
        assertThat(section2.getDefinition().getName(), is("*"));
        session.save();

        Node section3 = patient.addNode("section", "inf:section");
        section3.setProperty("inf:name", "sectionA");
        assertThat(section3.getIndex(), is(3));
        assertThat(section3.getPath(), is("/top/patient/section[3]"));
        session.save();

        Node section4 = patient.addNode("section", "inf:section");
        section4.setProperty("inf:name", "sectionA");
        assertThat(section4.getIndex(), is(4));
        assertThat(section4.getPath(), is("/top/patient/section[4]"));
        Node section5 = patient.addNode("section", "inf:section");
        section5.setProperty("inf:name", "sectionA");
        assertThat(section5.getIndex(), is(5));
        assertThat(section5.getPath(), is("/top/patient/section[5]"));
        section2.remove();
        session.save();
    }

    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }

    private void validateNodesWithCustomTypes() throws RepositoryException {
        JcrRootNode rootNode = session.getRootNode();
        rootNode.addNode("car", "car:Car");
        rootNode.addNode("aircraft", "air:Aircraft");

        session.save();

        assertEquals("car:Car", session.getNode("/car").getPrimaryNodeType().getName());
        assertEquals("air:Aircraft", session.getNode("/aircraft").getPrimaryNodeType().getName());
    }

    private void assertLocalNameAndNamespace( NodeType nodeType,
                                                String expectedLocalName,
                                                String namespacePrefix ) throws RepositoryException {
        Namespaced nsed = (Namespaced)nodeType;
        assertThat(nsed.getLocalName(), is(expectedLocalName));
        assertThat(nsed.getNamespaceURI(), is(session.getNamespaceURI(namespacePrefix)));
    }

}
