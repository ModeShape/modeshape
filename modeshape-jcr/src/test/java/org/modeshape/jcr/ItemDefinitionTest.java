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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.basic.BasicName;

/**
 * BDD test cases for property and child node definition inheritance. Could be part of RepositoryNodeTypeManagerTest, but split
 * off to isolate tests for this behavior vs. projection/inference and registration/unregistration behavior.
 */
public class ItemDefinitionTest extends AbstractSessionTest {

    private static final Name NODE_TYPE_A = new BasicName(TestLexicon.Namespace.URI, "nodeA");
    private static final Name NODE_TYPE_B = new BasicName(TestLexicon.Namespace.URI, "nodeB");
    private static final Name NODE_TYPE_C = new BasicName(TestLexicon.Namespace.URI, "nodeC");

    private static final Name SINGLE_PROP1 = new BasicName(TestLexicon.Namespace.URI, "singleProp1");
    private static final Name SINGLE_PROP2 = new BasicName(TestLexicon.Namespace.URI, "singleProp2");

    protected NameFactory nameFactory;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        nameFactory = session.getExecutionContext().getValueFactories().getNameFactory();
    }

    @Override
    protected void initializeContent() {
        graph.create("/jcr:system").and().create("/jcr:system/mode:namespaces");

    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test
    public void shouldNotFindInvalidPropertyDefinition() throws Exception {
        // This property name is not defined for any of our test types
        Name badName = nameFactory.create("undefinedName");
        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));
    }

    @Test
    public void shouldUseNearestPropertyDefinition() {
        // If a property is defined at multiple points in the type hierarchy, the property definition closest to the given type
        // should be used.

        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);
    }

    @Test
    public void shouldFindBestMatchDefinition() {
        /*
         * In cases where there is more than one valid definition for the same property,
         * the best match should be returned.
         */
        Value doubleValue = session.getValueFactory().createValue(0.7);
        Value longValue = session.getValueFactory().createValue(10);
        Value stringValue = session.getValueFactory().createValue("Should not work");

        JcrPropertyDefinition propDef;

        // Should prefer the double definition from NODE_TYPE_C since the value is of type double
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         doubleValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        // Should prefer the long definition from NODE_TYPE_C since the value is of type long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         longValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);

        // Should not allow a string though, since the NODE_TYPE_C definition narrows the acceptable types to double and long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         stringValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(nullValue()));

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
    @Override
    protected List<NodeTypeDefinition> getTestTypes() throws ConstraintViolationException {
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

        return Arrays.asList(new NodeTypeDefinition[] {nodeA, nodeB, nodeC});
    }
}
