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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Namespaced;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.basic.BasicName;

public class ItemDefinitionTest extends SingleUseAbstractTest {

    private static final Name NODE_TYPE_A = new BasicName(TestLexicon.Namespace.URI, "nodeA");
    private static final Name NODE_TYPE_B = new BasicName(TestLexicon.Namespace.URI, "nodeB");
    private static final Name NODE_TYPE_C = new BasicName(TestLexicon.Namespace.URI, "nodeC");

    private static final Name SINGLE_PROP1 = new BasicName(TestLexicon.Namespace.URI, "singleProp1");
    private static final Name SINGLE_PROP2 = new BasicName(TestLexicon.Namespace.URI, "singleProp2");

    protected NameFactory nameFactory;
    protected RepositoryNodeTypeManager repoTypeManager;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        nameFactory = session.nameFactory();
        repoTypeManager = session.repository().nodeTypeManager();
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(getTestTypes(), true);
    }

    protected NodeTypes nodeTypes() {
        return repoTypeManager.getNodeTypes();
    }

    @Test
    public void shouldNotFindInvalidPropertyDefinition() throws Exception {
        // This property name is not defined for any of our test types
        Name badName = nameFactory.create("undefinedName");
        JcrPropertyDefinition propDef;

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_A,
                                                     Collections.<Name>emptyList(),
                                                     badName,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(nullValue()));

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_B,
                                                     Collections.<Name>emptyList(),
                                                     badName,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(nullValue()));

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_C,
                                                     Collections.<Name>emptyList(),
                                                     badName,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(nullValue()));
    }

    @Test
    public void shouldUseNearestPropertyDefinition() {
        // If a property is defined at multiple points in the type hierarchy, the property definition closest to the given type
        // should be used.

        JcrPropertyDefinition propDef;

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_A,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP1,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_B,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP1,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_C,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP1,
                                                     null,
                                                     true,
                                                     true,
                                                     true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);
    }

    @Test
    public void shouldFindBestMatchDefinition() throws RepositoryException {
        /*
         * In cases where there is more than one valid definition for the same property,
         * the best match should be returned.
         */
        Value doubleValue = session.getValueFactory().createValue(0.7);
        Value longValue = session.getValueFactory().createValue(10);
        Value stringValue = session.getValueFactory().createValue("Should not work");

        JcrPropertyDefinition propDef;

        // Should prefer the double definition from NODE_TYPE_C since the value is of type double
        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_C,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP2,
                                                     doubleValue,
                                                     true,
                                                     true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        // Should prefer the long definition from NODE_TYPE_C since the value is of type long
        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_C,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP2,
                                                     longValue,
                                                     true,
                                                     true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);

        // Should not allow a string though, since the NODE_TYPE_C definition narrows the acceptable types to double and long
        propDef = nodeTypes().findPropertyDefinition(session,
                                                     NODE_TYPE_C,
                                                     Collections.<Name>emptyList(),
                                                     SINGLE_PROP2,
                                                     stringValue,
                                                     true,
                                                     true);
        assertThat(propDef, is(nullValue()));

    }

    @Test
    public void shouldBeNamespaced() throws RepositoryException {

        JcrPropertyDefinition propDef;

        propDef = nodeTypes().findPropertyDefinition(session,
                                                        NODE_TYPE_A,
                                                        Collections.<Name>emptyList(),
                                                        SINGLE_PROP1,
                                                        null,
                                                        true,
                                                        true,
                                                        true);

        assertLocalNameAndNamespace(propDef, SINGLE_PROP1.getLocalName(), TestLexicon.Namespace.PREFIX);
    }

    /*
    * Build a hierarchy of node types with the following relationships:
    *  
    *   modetest:nodeA extends nt:base
    *   modetest:nodeB extends nt:base
    *   modetest:nodeC extends modetest:nodeB
    *   
    * And the following single-valued property definitions
    * 
    *   modetest:nodeA defines properties:
    *      modetest:singleProp1 of type STRING
    *   modetest:nodeB defines properties:
    *      modetest:singleProp1 of type DOUBLE
    *      modetest:singleProp2 of type UNDEFINED
    *   modetest:nodeC defines properties:
    *      modetest:singleProp1 of type LONG
    *      modetest:singleProp2 of type DOUBLE     
    *      modetest:singleProp2 of type LONG (note the double-definition)
    */

    @SuppressWarnings( "unchecked" )
    protected NodeTypeDefinition[] getTestTypes() throws ConstraintViolationException, RepositoryException {
        ExecutionContext context = session.context();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("modetest", "http://www.modeshape.org/test/1.0");
        NodeTypeTemplate nodeA = new JcrNodeTypeTemplate(context);
        nodeA.setName("modetest:nodeA");

        JcrPropertyDefinitionTemplate nodeASingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeASingleProp1.setName("modetest:singleProp1");
        nodeASingleProp1.setRequiredType(PropertyType.STRING);
        nodeA.getPropertyDefinitionTemplates().add(nodeASingleProp1);

        NodeTypeTemplate nodeB = new JcrNodeTypeTemplate(context);
        nodeB.setName("modetest:nodeB");

        JcrPropertyDefinitionTemplate nodeBSingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeBSingleProp1.setName("modetest:singleProp1");
        nodeBSingleProp1.setRequiredType(PropertyType.DOUBLE);
        nodeB.getPropertyDefinitionTemplates().add(nodeBSingleProp1);

        JcrPropertyDefinitionTemplate nodeBSingleProp2 = new JcrPropertyDefinitionTemplate(context);
        nodeBSingleProp2.setName("modetest:singleProp2");
        nodeBSingleProp2.setRequiredType(PropertyType.UNDEFINED);
        nodeB.getPropertyDefinitionTemplates().add(nodeBSingleProp2);

        NodeTypeTemplate nodeC = new JcrNodeTypeTemplate(context);
        nodeC.setName("modetest:nodeC");
        nodeC.setDeclaredSuperTypeNames(new String[] {"modetest:nodeB"});

        JcrPropertyDefinitionTemplate nodeCSingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp1.setName("modetest:singleProp1");
        nodeCSingleProp1.setRequiredType(PropertyType.LONG);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp1);

        JcrPropertyDefinitionTemplate nodeCSingleProp2Double = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp2Double.setName("modetest:singleProp2");
        nodeCSingleProp2Double.setRequiredType(PropertyType.DOUBLE);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp2Double);

        JcrPropertyDefinitionTemplate nodeCSingleProp2Long = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp2Long.setName("modetest:singleProp2");
        nodeCSingleProp2Long.setRequiredType(PropertyType.LONG);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp2Long);

        return new NodeTypeDefinition[] {nodeA, nodeB, nodeC};
    }


    private void assertLocalNameAndNamespace( Namespaced nsed,
                                              String expectedLocalName,
                                              String namespacePrefix ) throws RepositoryException {
        assertThat(nsed.getLocalName(), is(expectedLocalName));
        assertThat(nsed.getNamespaceURI(), is(session.getNamespaceURI(namespacePrefix)));
    }

}
