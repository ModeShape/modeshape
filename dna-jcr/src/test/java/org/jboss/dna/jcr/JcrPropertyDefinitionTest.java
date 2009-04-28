/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * Indirectly tests the JcrConstaintCheckerFactory through {@link JcrPropertyDefinition#satisfiesConstraints(Value)}, which
 * provides the wrapper around the factory that the rest of the API is expected to utilize.
 */
public class JcrPropertyDefinitionTest {

    protected final String[] EXPECTED_BINARY_CONSTRAINTS = new String[] {"[,5)", "[10, 20)", "(30,40]", "[50,]"};
    protected final String[] EXPECTED_DATE_CONSTRAINTS = new String[] {"[,+1945-08-01T01:30:00.000Z]",
        "[+1975-08-01T01:30:00.000Z,)"};
    protected final String[] EXPECTED_DOUBLE_CONSTRAINTS = new String[] {"[,5.0)", "[10.1, 20.2)", "(30.3,40.4]", "[50.5,]"};
    protected final String[] EXPECTED_LONG_CONSTRAINTS = new String[] {"[,5)", "[10, 20)", "(30,40]", "[50,]"};
    protected final String[] EXPECTED_NAME_CONSTRAINTS = new String[] {"jcr:system", "dnatest:constrainedType"};
    protected final String[] EXPECTED_PATH_CONSTRAINTS = new String[] {"/jcr:system/*", "b", "/a/b/c"};
    protected final String[] EXPECTED_REFERENCE_CONSTRAINTS = new String[] {"dna:root"};
    protected final String[] EXPECTED_STRING_CONSTRAINTS = new String[] {"foo", "bar*", ".*baz"};

    private String workspaceName;
    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private JcrSession session;
    private Graph graph;
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryNodeTypeManager repoTypeManager;
    private NodeTypeManager nodeTypeManager;
    private Map<String, Object> sessionAttributes;
    @Mock
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspaceName = "workspace1";
        final String repositorySourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();
        // Register the test namespace
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        // Set up the initial content ...
        graph = Graph.create(source, context);

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");

        graph.set("jcr:mixinTypes").on("/a").to(JcrMixLexicon.REFERENCEABLE);

        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return repositorySourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Stub out the repository, since we only need a few methods ...
        JcrNodeTypeSource source = null;
        source = new JcrBuiltinNodeTypeSource(this.context, source);
        source = new DnaBuiltinNodeTypeSource(this.context, source);
        source = new TestNodeTypeSource(this.context, source);
        repoTypeManager = new RepositoryNodeTypeManager(context, source);
        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();
        sessionAttributes.put("attribute1", "value1");

        // Now create the workspace ...
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();

        nodeTypeManager = workspace.getNodeTypeManager();
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
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
                            int jcrType ) {
        return new JcrValue(session.getExecutionContext().getValueFactories(), session.cache(), jcrType, value);
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
            if (!property.satisfiesConstraints(values[i])) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void shouldAllowNullValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);
        assertThat(prop.satisfiesConstraints((Value)null), is(false));
    }

    @Test
    public void shouldAllowValidBinaryValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_BINARY);

        // Making assumption that String.getBytes().length = String.length() on the platform's default encoding
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(0), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(4), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(10), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(19), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(31), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(40), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(50), PropertyType.BINARY)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(99), PropertyType.BINARY)), is(true));
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
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(5), PropertyType.BINARY)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(9), PropertyType.BINARY)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(20), PropertyType.BINARY)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(30), PropertyType.BINARY)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(41), PropertyType.BINARY)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(stringOfLength(49), PropertyType.BINARY)), is(false));
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

        assertThat(prop.satisfiesConstraints(valueFor("-1945-08-01T01:30:00.000Z", PropertyType.DATE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1945-07-31T01:30:00.000Z", PropertyType.DATE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+0001-08-01T01:30:00.000Z", PropertyType.DATE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:30:00.000Z", PropertyType.DATE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:31:00.000Z", PropertyType.DATE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("+2009-08-01T01:30:00.000Z", PropertyType.DATE)), is(true));
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

        assertThat(prop.satisfiesConstraints(valueFor("+1945-08-01T01:30:00.001Z", PropertyType.DATE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("+1975-08-01T01:29:59.999Z", PropertyType.DATE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("+1945-08-01T01:30:00.000-05:00", PropertyType.DATE)), is(false));
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

        assertThat(prop.satisfiesConstraints(valueFor(Double.MIN_VALUE, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0.1, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(4.99, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10.100, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(20.19, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(30.31, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(40.4, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(50.5, PropertyType.DOUBLE)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(Double.MAX_VALUE, PropertyType.DOUBLE)), is(true));
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

        assertThat(prop.satisfiesConstraints(valueFor(5, PropertyType.DOUBLE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(9.99999999, PropertyType.DOUBLE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(20.2, PropertyType.DOUBLE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(30.3, PropertyType.DOUBLE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(40.41, PropertyType.DOUBLE)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(49.9, PropertyType.DOUBLE)), is(false));
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

        assertThat(prop.satisfiesConstraints(valueFor(Long.MIN_VALUE, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(0.1, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(4.99, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(10.100, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(19, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(31, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(40, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(50, PropertyType.LONG)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor(Long.MAX_VALUE, PropertyType.LONG)), is(true));
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

        assertThat(prop.satisfiesConstraints(valueFor(5, PropertyType.LONG)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(9, PropertyType.LONG)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(20, PropertyType.LONG)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(30, PropertyType.LONG)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(41, PropertyType.LONG)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor(49, PropertyType.LONG)), is(false));
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

        assertThat(prop.satisfiesConstraints(valueFor("jcr:system", PropertyType.NAME)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("dnatest:constrainedType", PropertyType.NAME)), is(true));

        // Test that names work across namespace remaps
        session.setNamespacePrefix("newprefix", TestLexicon.Namespace.URI);
        assertThat(prop.satisfiesConstraints(valueFor("newprefix:constrainedType", PropertyType.NAME)), is(true));
    }

    @Test
    public void shouldAllowValidNameValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        Value[] values = new Value[] {valueFor("jcr:system", PropertyType.NAME),
            valueFor("dnatest:constrainedType", PropertyType.NAME),};
        assertThat(satisfiesConstraints(prop, new Value[] {}), is(true));
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("jcr:system", PropertyType.NAME)}), is(true));
        assertThat(satisfiesConstraints(prop, values), is(true));
    }

    @Test
    public void shouldNotAllowInvalidNameValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        assertThat(prop.satisfiesConstraints(valueFor("system", PropertyType.NAME)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("jcr:system2", PropertyType.NAME)), is(false));

        // Test that old names fail after namespace remaps
        session.setNamespacePrefix("newprefix", TestLexicon.Namespace.URI);
        assertThat(prop.satisfiesConstraints(valueFor("dnatest:constrainedType", PropertyType.NAME)), is(true));
    }

    @Test
    public void shouldNotAllowInvalidNameValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_NAME);

        Value[] values = new Value[] {valueFor("jcr:system", PropertyType.NAME),
            valueFor("dnatest:constrainedType2", PropertyType.NAME),};
        assertThat(satisfiesConstraints(prop, new Value[] {valueFor("jcr:system2", PropertyType.NAME)}), is(false));
        assertThat(satisfiesConstraints(prop, values), is(false));
    }

    @Test
    public void shouldAllowValidStringValue() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_STRING);

        assertThat(prop.satisfiesConstraints(valueFor("foo", PropertyType.STRING)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("bar", PropertyType.STRING)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("barr", PropertyType.STRING)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("barrrrrrrrr", PropertyType.STRING)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("baz", PropertyType.STRING)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("shabaz", PropertyType.STRING)), is(true));
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

        assertThat(prop.satisfiesConstraints(valueFor("", PropertyType.STRING)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("foot", PropertyType.STRING)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("abar", PropertyType.STRING)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("bard", PropertyType.STRING)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("baz!", PropertyType.STRING)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("bazzat", PropertyType.STRING)), is(false));
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

        assertThat(prop.satisfiesConstraints(valueFor("b", PropertyType.PATH)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c", PropertyType.PATH)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/jcr:system/dna:namespace", PropertyType.PATH)), is(true));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c/", PropertyType.PATH)), is(true));

        // Test that constraints work after session rename
        session.setNamespacePrefix("jcr2", JcrLexicon.Namespace.URI);

        assertThat(prop.satisfiesConstraints(valueFor("/jcr2:system/dna:foo", PropertyType.PATH)), is(true));
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

        assertThat(prop.satisfiesConstraints(valueFor("a", PropertyType.PATH)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b", PropertyType.PATH)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/jcr:system", PropertyType.PATH)), is(false));
        assertThat(prop.satisfiesConstraints(valueFor("/a/b/c/d", PropertyType.PATH)), is(false));

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

        assertThat(prop.satisfiesConstraints(value), is(true));
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

        assertThat(prop.satisfiesConstraints(value), is(false));
    }

    @Test
    public void shouldNotAllowInvalidReferenceValues() throws Exception {
        NodeType constrainedType = validateTypeDefinition();
        JcrPropertyDefinition prop = propertyDefinitionFor(constrainedType, TestLexicon.CONSTRAINED_REFERENCE);

        Value value = session.getValueFactory().createValue(session.getRootNode().getNode("a"));

        assertThat(satisfiesConstraints(prop, new Value[] {value}), is(false));
    }

    class TestNodeTypeSource extends AbstractJcrNodeTypeSource {

        /** The list of primary node types. */
        private final List<JcrNodeType> nodeTypes;

        TestNodeTypeSource( ExecutionContext context,
                            JcrNodeTypeSource predecessor ) {
            super(predecessor);

            nodeTypes = new ArrayList<JcrNodeType>();

            JcrNodeType base = findType(JcrNtLexicon.BASE);

            if (base == null) {
                String baseTypeName = JcrNtLexicon.BASE.getString(context.getNamespaceRegistry());
                String namespaceTypeName = DnaLexicon.NAMESPACE.getString(context.getNamespaceRegistry());
                throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
            }

            // Stubbing in child node and property definitions for now
            JcrNodeType constrainedType = new JcrNodeType(
                                                          context,
                                                          NO_NODE_TYPE_MANAGER,
                                                          TestLexicon.CONSTRAINED_TYPE,
                                                          Arrays.asList(new JcrNodeType[] {base}),
                                                          NO_PRIMARY_ITEM_NAME,
                                                          NO_CHILD_NODES,
                                                          Arrays.asList(new JcrPropertyDefinition[] {
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_BINARY,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.BINARY,
                                                                                        EXPECTED_BINARY_CONSTRAINTS, false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_DATE,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.DATE,
                                                                                        EXPECTED_DATE_CONSTRAINTS, false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_DOUBLE,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.DOUBLE,
                                                                                        EXPECTED_DOUBLE_CONSTRAINTS, false),

                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_LONG,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.LONG,
                                                                                        EXPECTED_LONG_CONSTRAINTS, false),

                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_NAME,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.NAME,
                                                                                        EXPECTED_NAME_CONSTRAINTS, false),

                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_PATH,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.PATH,
                                                                                        EXPECTED_PATH_CONSTRAINTS, false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_REFERENCE,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.REFERENCE,
                                                                                        new String[] {"dna:root",}, false),

                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        TestLexicon.CONSTRAINED_STRING,
                                                                                        OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.STRING,
                                                                                        EXPECTED_STRING_CONSTRAINTS, false),

                                                          }), NOT_MIXIN, UNORDERABLE_CHILD_NODES);

            nodeTypes.addAll(Arrays.asList(new JcrNodeType[] {constrainedType}));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.AbstractJcrNodeTypeSource#getDeclaredNodeTypes()
         */
        @Override
        public Collection<JcrNodeType> getDeclaredNodeTypes() {
            return nodeTypes;
        }

    }

}
