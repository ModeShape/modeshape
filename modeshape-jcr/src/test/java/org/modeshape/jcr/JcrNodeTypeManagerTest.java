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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import junit.framework.TestSuite;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * The {@link JcrNodeTypeManager} test class.
 */
public final class JcrNodeTypeManagerTest extends TestSuite {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String MIXIN1 = "mix:lockable";
    private static final String MIXIN2 = "mix:referenceable";
    private static final String[] MIXINS = new String[] {MIXIN1, MIXIN2};

    private static final String HIERARCHY_NODE_TYPE = "nt:hierarchyNode";

    private static final String SUBTYPE1 = "nt:folder"; // subtype of HIERARCHY_NODE_TYPE
    private static final String SUBTYPE2 = "nt:file"; // subtype of HIERARCHY_NODE_TYPE
    private static final String[] SUBTYPES = new String[] {SUBTYPE1, SUBTYPE2};

    private static final String NO_MATCH_TYPE = "nt:query";

    private static final String[] SUBTYPES_MIXINS;

    static {
        SUBTYPES_MIXINS = new String[SUBTYPES.length + MIXINS.length];
        System.arraycopy(SUBTYPES, 0, SUBTYPES_MIXINS, 0, SUBTYPES.length);
        System.arraycopy(MIXINS, 0, SUBTYPES_MIXINS, SUBTYPES.length, MIXINS.length);
    }

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    @BeforeClass
    public static void beforeClass() {
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private JcrEngine engine;
    private Session session;
    private JcrNodeTypeManager nodeTypeMgr;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @After
    public void afterEach() {
        try {
            if (this.session != null) {
                this.session.logout();
            }
        } finally {
            this.session = null;

            try {
                this.engine.shutdown();
            } finally {
                this.engine = null;
            }
        }
    }

    @Before
    public void beforeEach() throws RepositoryException {
        final String WORKSPACE = "ws1";
        final String REPOSITORY = "r1";
        final String SOURCE = "store";

        JcrConfiguration config = new JcrConfiguration();
        config.repositorySource("store")
              .usingClass(InMemoryRepositorySource.class)
              .setRetryLimit(100)
              .setProperty("defaultWorkspaceName", WORKSPACE);
        config.repository(REPOSITORY).setSource(SOURCE).setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        config.save();

        // Create and start the engine ...
        this.engine = config.build();
        this.engine.start();

        // Create repository and session
        Repository repository = this.engine.getRepository(REPOSITORY);
        final String USER_ID = "superuser";
        Credentials credentials = new SimpleCredentials(USER_ID, USER_ID.toCharArray());
        this.session = repository.login(credentials, WORKSPACE);
        this.nodeTypeMgr = (JcrNodeTypeManager)this.session.getWorkspace().getNodeTypeManager();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSubTypeNames() throws RepositoryException {
        this.nodeTypeMgr.isDerivedFrom(null, "nt:base", MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptySubTypeNames() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(new String[0], "nt:base", MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPrimaryType() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(SUBTYPES, null, MIXINS);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyPrimaryType() throws Exception {
        this.nodeTypeMgr.isDerivedFrom(SUBTYPES, "", MIXINS);
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeMatchesPrimaryType() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, SUBTYPE2, null));
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, SUBTYPE2, MIXINS));
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeMatchesMixin() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(new String[] {MIXIN2}, SUBTYPE1, MIXINS));
    }

    @Test
    public void shouldBeDerivedFromIfSubtypeIsActualSubType() throws Exception {
        assertTrue(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, HIERARCHY_NODE_TYPE, MIXINS));
    }

    @Test
    public void shouldNotBeDerivedFromIfNoMatch() throws Exception {
        assertFalse(this.nodeTypeMgr.isDerivedFrom(SUBTYPES, NO_MATCH_TYPE, MIXINS));
    }

    @Test
    public void shouldAllowDisjunctiveResidualChildNodeDefinitions() throws Exception {
        // This is an extended test of the MODE-698 fix
        CndNodeTypeReader factory = new CndNodeTypeReader(engine.getExecutionContext());
        factory.read("/magnolia.cnd");
        nodeTypeMgr.registerNodeTypes(factory.getNodeTypeDefinitions(), false);
        session.getWorkspace().getNamespaceRegistry().registerNamespace("mgnl", "http://www.magnolia.info/jcr/mgnl");

        assertThat(nodeTypeMgr.getNodeType("mgnl:content"), is(notNullValue()));

        Node rootNode = session.getRootNode();
        Node branchNode = rootNode.addNode("disjunctiveTest", "nt:unstructured");
        Node testNode = branchNode.addNode("testNode", "mgnl:content");

        assertTrue(testNode.hasNode("MetaData"));
        session.save();

        // This residual definition comes from the ancestor - nt:hierarchyNode
        testNode.addNode("hierarchyNode", "nt:folder");

        // This residual definition comes from mgnl:content
        testNode.addNode("baseNode", "nt:unstructured");

        session.save();
    }

    @Test
    public void shouldReturnTrueForHasNodeTypeWithExistingNodeTypeName() throws Exception {
        assertTrue(nodeTypeMgr.hasNodeType("nt:base"));
        assertTrue(nodeTypeMgr.hasNodeType(HIERARCHY_NODE_TYPE));
        assertTrue(nodeTypeMgr.hasNodeType(MIXIN1));

    }

    @Test
    public void shouldReturnFalseForHasNodeTypeWithNonexistantNodeTypeName() throws Exception {
        assertFalse(nodeTypeMgr.hasNodeType("someArgleBargle"));
        assertFalse(nodeTypeMgr.hasNodeType(HIERARCHY_NODE_TYPE + "x"));
    }
}
