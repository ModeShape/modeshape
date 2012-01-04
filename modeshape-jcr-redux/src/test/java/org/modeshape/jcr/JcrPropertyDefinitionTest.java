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
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;

public class JcrPropertyDefinitionTest extends MultiUseAbstractTest {

    protected static final String[] EXPECTED_BINARY_CONSTRAINTS = new String[] {"[,5)", "[10, 20)", "(30,40]", "[50,]"};
    protected static final String[] EXPECTED_DATE_CONSTRAINTS = new String[] {"[,+1945-08-01T01:30:00.000Z]",
        "[+1975-08-01T01:30:00.000Z,)"};
    protected static final String[] EXPECTED_DOUBLE_CONSTRAINTS = new String[] {"[,5.0)", "[10.1, 20.2)", "(30.3,40.4]",
        "[50.5,]"};
    protected static final String[] EXPECTED_LONG_CONSTRAINTS = new String[] {"[,5)", "[10, 20)", "(30,40]", "[50,]"};
    protected static final String[] EXPECTED_NAME_CONSTRAINTS = new String[] {"jcr:system", "modetest:constrainedType"};
    protected static final String[] EXPECTED_PATH_CONSTRAINTS = new String[] {"/jcr:system/*", "b", "/a/b/c"};
    protected static final String[] EXPECTED_REFERENCE_CONSTRAINTS = new String[] {"mode:root"};
    protected static final String[] EXPECTED_STRING_CONSTRAINTS = new String[] {"foo", "bar*", ".*baz"};

    protected NodeTypeManager nodeTypeManager;
    private ExecutionContext context;

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types ...
        session.getWorkspace().getNamespaceRegistry().registerNamespace(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);
        registerNodeTypes(getTestTypes());

        // Create some initial content ...
        Node nodeA = session.getRootNode().addNode("a");
        Node nodeB = nodeA.addNode("b");
        Node nodeC = nodeB.addNode("c");
        assertThat(nodeC, is(notNullValue()));
        nodeA.addMixin("mix:referenceable");
        session.save();
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        context = session.context();
    }

    @SuppressWarnings( "unchecked" )
    protected static List<NodeTypeDefinition> getTestTypes() throws RepositoryException, ConstraintViolationException {
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate constrainedType = mgr.createNodeTypeTemplate();
        constrainedType.setName("modetest:constrainedType");

        PropertyDefinitionTemplate propBinary = mgr.createPropertyDefinitionTemplate();
        propBinary.setName("modetest:constrainedBinary");
        propBinary.setRequiredType(PropertyType.BINARY);
        propBinary.setValueConstraints(EXPECTED_BINARY_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propBinary);

        PropertyDefinitionTemplate propDate = mgr.createPropertyDefinitionTemplate();
        propDate.setName("modetest:constrainedDate");
        propDate.setRequiredType(PropertyType.DATE);
        propDate.setValueConstraints(EXPECTED_DATE_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propDate);

        PropertyDefinitionTemplate propDouble = mgr.createPropertyDefinitionTemplate();
        propDouble.setName("modetest:constrainedDouble");
        propDouble.setRequiredType(PropertyType.DOUBLE);
        propDouble.setValueConstraints(EXPECTED_DOUBLE_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propDouble);

        PropertyDefinitionTemplate propLong = mgr.createPropertyDefinitionTemplate();
        propLong.setName("modetest:constrainedLong");
        propLong.setRequiredType(PropertyType.LONG);
        propLong.setValueConstraints(EXPECTED_LONG_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propLong);

        PropertyDefinitionTemplate propName = mgr.createPropertyDefinitionTemplate();
        propName.setName("modetest:constrainedName");
        propName.setRequiredType(PropertyType.NAME);
        propName.setValueConstraints(EXPECTED_NAME_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propName);

        PropertyDefinitionTemplate propPath = mgr.createPropertyDefinitionTemplate();
        propPath.setName("modetest:constrainedPath");
        propPath.setRequiredType(PropertyType.PATH);
        propPath.setValueConstraints(EXPECTED_PATH_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propPath);

        PropertyDefinitionTemplate propReference = mgr.createPropertyDefinitionTemplate();
        propReference.setName("modetest:constrainedReference");
        propReference.setRequiredType(PropertyType.REFERENCE);
        propReference.setValueConstraints(EXPECTED_REFERENCE_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propReference);

        PropertyDefinitionTemplate propString = mgr.createPropertyDefinitionTemplate();
        propString.setName("modetest:constrainedString");
        propString.setRequiredType(PropertyType.STRING);
        propString.setValueConstraints(EXPECTED_STRING_CONSTRAINTS);
        constrainedType.getPropertyDefinitionTemplates().add(propString);

        return Collections.singletonList((NodeTypeDefinition)constrainedType);
    }

    private JcrPropertyDefinition propertyDefinitionFor( NodeType nodeType,
                                                         Name propertyName ) {
        PropertyDefinition propertyDefs[] = nodeType.getPropertyDefinitions();
        String property = propertyName.getString(context.getNamespaceRegistry());

        for (int i = 0; i < propertyDefs.length; i++) {
            if (propertyDefs[i].getName().equals(property)) {
                return (JcrPropertyDefinition)propertyDefs[i];
            }
        }
        throw new IllegalStateException("Could not find property definition name " + property + " for type " + nodeType.getName()
                                        + ".  Test setup is invalid.");
    }

    private void checkConstraints( NodeType nodeType,
                                   Name propertyName,
                                   String[] expectedConstraints ) {
        PropertyDefinition propertyDefs[] = nodeType.getPropertyDefinitions();
        String property = propertyName.getString(context.getNamespaceRegistry());
        String[] constraints = null;

        for (int i = 0; i < propertyDefs.length; i++) {
            if (propertyDefs[i].getName().equals(property)) {
                constraints = propertyDefs[i].getValueConstraints();
                break;
            }
        }

        if (!Arrays.equals(constraints, expectedConstraints)) {
            throw new IllegalStateException("Unexpected constraints for property: " + property);
        }
    }

    private NodeType validateTypeDefinition() throws Exception {
        NamespaceRegistry nsr = context.getNamespaceRegistry();

        NodeType constrainedType = nodeTypeManager.getNodeType(TestLexicon.CONSTRAINED_TYPE.getString(nsr));
        assertThat(constrainedType, notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DATE), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DOUBLE), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_LONG), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_PATH), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE), notNullValue());
        assertThat(propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING), notNullValue());

        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_BINARY, EXPECTED_BINARY_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_DATE, EXPECTED_DATE_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_DOUBLE, EXPECTED_DOUBLE_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_LONG, EXPECTED_LONG_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_NAME, EXPECTED_NAME_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_PATH, EXPECTED_PATH_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_REFERENCE, EXPECTED_REFERENCE_CONSTRAINTS);
        checkConstraints(constrainedType, TestLexicon.CONSTRAINED_STRING, EXPECTED_STRING_CONSTRAINTS);

        return constrainedType;
    }

    private Value valueFor( Object value,
                            int jcrType ) throws RepositoryException {
        return session.getValueFactory().createValue(value, jcrType);
    }

    private String stringOfLength( int length ) {
        StringBuffer buff = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            buff.append(i % 10);
        }

        return buff.toString();
    }

    private boolean satisfiesConstraints( JcrPropertyDefinition property,
                                          Value[] values ) {
        for (int i = 0; i < values.length; i++) {
            if (!property.satisfiesConstraints(values[i], session)) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void shouldAllowNullValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);
        assertThat(prop.satisfiesConstraints((Value)null, session), is(false));
    }

    @Test
    public void shouldAllowValidBinaryValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);

        // Making assumption that String.getBytes().length = String.length() on the platform's default encoding
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(0), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(4), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(10), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(19), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(31), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(40), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(50), PropertyType.BINARY), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(99), PropertyType.BINARY), session), is(true));
    }

    @Test
    public void shouldAllowValidBinaryValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);

        // Making assumption that String.getBytes().length = String.length() on the platform's default encoding
        Value[] values = new Value[] {valueFor(stringOfLength(4), PropertyType.BINARY),
            valueFor(stringOfLength(10), PropertyType.BINARY), valueFor(stringOfLength(19), PropertyType.BINARY),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(stringOfLength(0), PropertyType.BINARY)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidBinaryValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);

        // Making assumption that String.getBytes().length = String.length() on the platform's default encoding
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(5), PropertyType.BINARY), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(9), PropertyType.BINARY), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(20), PropertyType.BINARY), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(30), PropertyType.BINARY), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(41), PropertyType.BINARY), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(49), PropertyType.BINARY), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidBinaryValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);

        // Making assumption that String.getBytes().length = String.length() on the platform's default encoding
        Value[] values = new Value[] {valueFor(stringOfLength(4), PropertyType.BINARY),
            valueFor(stringOfLength(10), PropertyType.BINARY), valueFor(stringOfLength(20), PropertyType.BINARY),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(stringOfLength(9), PropertyType.BINARY)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidDateValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DATE);

        assertThat(prop.satisfiesConstraints(valueFor("-1945-08-01T01:30:00.000Z", PropertyType.DATE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1945-07-31T01:30:00.000Z", PropertyType.DATE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+0001-08-01T01:30:00.000Z", PropertyType.DATE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:30:00.000Z", PropertyType.DATE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:31:00.000Z", PropertyType.DATE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+2009-08-01T01:30:00.000Z", PropertyType.DATE), session), is(true));
    }

    @Test
    public void shouldAllowValidDateValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DATE);

        Value[] values = new Value[] {valueFor("-1945-08-01T01:30:00.000Z", PropertyType.DATE),
            valueFor("+2009-08-01T01:30:00.000Z", PropertyType.DATE),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("+1975-08-01T01:31:00.000Z", PropertyType.DATE)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidDateValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DATE);

        assertThat(prop.satisfiesConstraints(valueFor("+1945-08-01T01:30:00.001Z", PropertyType.DATE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:29:59.999Z", PropertyType.DATE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("+1945-08-01T01:30:00.000-05:00", PropertyType.DATE), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidDateValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DATE);

        Value[] values = new Value[] {valueFor("-1945-08-01T01:30:00.000", PropertyType.DATE),
            valueFor("+1945-08-01T01:30:00.000-05:00", PropertyType.DATE),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("+1945-08-01T01:30:00.001Z", PropertyType.DATE)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidDoubleValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DOUBLE);

        assertThat(prop.satisfiesConstraints(valueFor(Double.MIN_VALUE, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0.1, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(4.99, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10.100, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(20.19, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(30.31, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(40.4, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(50.5, PropertyType.DOUBLE), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(Double.MAX_VALUE, PropertyType.DOUBLE), session), is(true));
    }

    @Test
    public void shouldAllowValidDoubleValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DOUBLE);

        Value[] values = new Value[] {valueFor(0.1, PropertyType.DOUBLE), valueFor(20.19, PropertyType.DOUBLE),
            valueFor(50.5, PropertyType.DOUBLE)};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(4.99, PropertyType.DOUBLE)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidDoubleValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DOUBLE);

        assertThat(prop.satisfiesConstraints(valueFor(5, PropertyType.DOUBLE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(9.99999999, PropertyType.DOUBLE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(20.2, PropertyType.DOUBLE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(30.3, PropertyType.DOUBLE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(40.41, PropertyType.DOUBLE), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(49.9, PropertyType.DOUBLE), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidDoubleValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_DOUBLE);

        Value[] values = new Value[] {valueFor(0.1, PropertyType.DOUBLE), valueFor(20.19, PropertyType.DOUBLE),
            valueFor(50.49, PropertyType.DOUBLE)};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(20.2, PropertyType.DOUBLE)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidLongValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_LONG);

        assertThat(prop.satisfiesConstraints(valueFor(Long.MIN_VALUE, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0.1, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(4.99, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10.100, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(19, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(31, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(40, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(50, PropertyType.LONG), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(Long.MAX_VALUE, PropertyType.LONG), session), is(true));
    }

    @Test
    public void shouldAllowValidLongValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_LONG);

        Value[] values = new Value[] {valueFor(0.1, PropertyType.LONG), valueFor(10, PropertyType.LONG),
            valueFor(50, PropertyType.LONG)};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(4.99, PropertyType.LONG)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidLongValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_LONG);

        assertThat(prop.satisfiesConstraints(valueFor(5, PropertyType.LONG), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(9, PropertyType.LONG), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(20, PropertyType.LONG), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(30, PropertyType.LONG), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(41, PropertyType.LONG), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(49, PropertyType.LONG), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidLongValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_LONG);

        Value[] values = new Value[] {valueFor(0.1, PropertyType.LONG), valueFor(10, PropertyType.LONG),
            valueFor(49, PropertyType.LONG)};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor(30, PropertyType.LONG)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidNameValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        assertThat(prop.satisfiesConstraints(valueFor("jcr:system", PropertyType.NAME), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("modetest:constrainedType", PropertyType.NAME), session), is(true));

        // Test that names work across namespace remaps
        session.setNamespacePrefix("newprefix", TestLexicon.Namespace.URI);
        assertThat(prop.satisfiesConstraints(valueFor("newprefix:constrainedType", PropertyType.NAME), session), is(true));
    }

    @Test
    public void shouldAllowValidNameValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        Value[] values = new Value[] {valueFor("jcr:system", PropertyType.NAME),
            valueFor("modetest:constrainedType", PropertyType.NAME),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("jcr:system", PropertyType.NAME)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotAllowInvalidNameValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        assertThat(prop.satisfiesConstraints(valueFor("system", PropertyType.NAME), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("jcr:system2", PropertyType.NAME), session), is(false));

        // Test that old names fail after namespace remaps
        session.setNamespacePrefix("newprefix", TestLexicon.Namespace.URI);
        assertThat(prop.satisfiesConstraints(valueFor("modetest:constrainedType", PropertyType.NAME), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidNameValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        Value[] values = new Value[] {valueFor("jcr:system", PropertyType.NAME),
            valueFor("modetest:constrainedType2", PropertyType.NAME),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("jcr:system2", PropertyType.NAME)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidStringValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING);

        assertThat(prop.satisfiesConstraints(valueFor("foo", PropertyType.STRING), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("bar", PropertyType.STRING), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("barr", PropertyType.STRING), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("barrrrrrrrr", PropertyType.STRING), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("baz", PropertyType.STRING), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("shabaz", PropertyType.STRING), session), is(true));
    }

    @Test
    public void shouldAllowValidStringValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING);

        Value[] values = new Value[] {valueFor("foo", PropertyType.STRING), valueFor("barr", PropertyType.STRING),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("baz", PropertyType.STRING)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidStringValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING);

        assertThat(prop.satisfiesConstraints(valueFor("", PropertyType.STRING), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("foot", PropertyType.STRING), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("abar", PropertyType.STRING), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("bard", PropertyType.STRING), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("baz!", PropertyType.STRING), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("bazzat", PropertyType.STRING), session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidStringValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING);

        Value[] values = new Value[] {valueFor("foo", PropertyType.STRING), valueFor("bard", PropertyType.STRING),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("bazzat", PropertyType.STRING)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidPathValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_PATH);

        assertThat(prop.satisfiesConstraints(valueFor("b", PropertyType.PATH), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c", PropertyType.PATH), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/jcr:system/mode:namespace", PropertyType.PATH), session), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c/", PropertyType.PATH), session), is(true));

        // Test that constraints work after session rename
        session.setNamespacePrefix("jcr2", JcrLexicon.Namespace.URI);

        assertThat(prop.satisfiesConstraints(valueFor("/jcr2:system/mode:foo", PropertyType.PATH), session), is(true));
    }

    @Test
    public void shouldAllowValidPathValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_PATH);

        Value[] values = new Value[] {valueFor("b", PropertyType.PATH), valueFor("/a/b/c", PropertyType.PATH),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("/a/b/c", PropertyType.PATH)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidPathValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_PATH);

        assertThat(prop.satisfiesConstraints(valueFor("a", PropertyType.PATH), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b", PropertyType.PATH), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/jcr:system", PropertyType.PATH), session), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c/d", PropertyType.PATH), session), is(false));

    }

    @Test
    public void shouldNotAllowInvalidPathValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_PATH);

        Value[] values = new Value[] {valueFor("b", PropertyType.PATH), valueFor("/a/b/c/d", PropertyType.PATH),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("/a", PropertyType.PATH)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidReferenceValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE);

        Value value = session.getValueFactory().createValue(session.getRootNode());

        assertThat(prop.satisfiesConstraints(value, session), is(true));
    }

    @Test
    public void shouldAllowValidReferenceValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE);

        Value value = session.getValueFactory().createValue(session.getRootNode());

        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {value}), is(true));
    }

    @Test
    public void shouldNotAllowInvalidReferenceValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE);

        Value value = session.getValueFactory().createValue(session.getRootNode().getNode("a"));

        assertThat(prop.satisfiesConstraints(value, session), is(false));
    }

    @Test
    public void shouldNotAllowInvalidReferenceValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE);

        Value value = session.getValueFactory().createValue(session.getRootNode().getNode("a"));

        assertThat(satisfiesConstraints(prop, new Value[] {value}), is(false));
    }
}
