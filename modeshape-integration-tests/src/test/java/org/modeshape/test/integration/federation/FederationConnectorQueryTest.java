package org.modeshape.test.integration.federation;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import static junit.framework.Assert.assertEquals;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JaasTestUtil;
import org.modeshape.test.ModeShapeSingleUseTest;

/**
 * Uni test for searching a federated repository
 *
 * @author Horia Chiorean
 */
public class FederationConnectorQueryTest extends ModeShapeSingleUseTest {
    /**
     * defined in security/jaas.conf.xml
     */
    private static final Credentials SUPERUSER_CREDENTIALS = new SimpleCredentials("superuser", "superuser".toCharArray());

    private Session session;

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session = sessionTo("My repository", "default", SUPERUSER_CREDENTIALS);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @BeforeClass
    public static void beforeAll() {
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForFederatedSearch.xml";
    }

    @FixFor("MODE-1238")
    @Test
    public void shouldUpdateQueryIndex() throws Exception {
        // Get the root node ...
        Node root = session.getRootNode();
        assertNotNull(root);

        Node testNode;
        if (session.itemExists("/inmemory1")) {
            testNode = session.getNode("/inmemory1").addNode("testNode", NodeType.NT_UNSTRUCTURED);
        }
        else {
            testNode = root.addNode("/inmemory1", NodeType.NT_UNSTRUCTURED).addNode("testNode", NodeType.NT_UNSTRUCTURED);
        }

        testNode.setProperty("prop", "Hello World");
        session.save();

        QueryManager qm = session.getWorkspace().getQueryManager();
        Query query = qm.createQuery("select * from [nt:unstructured] where prop = 'Hello World'", Query.JCR_SQL2);
        QueryResult queryResult = query.execute();

        NodeIterator iter = queryResult.getNodes();
        assertEquals(1, iter.getSize());
        Node foundNode = iter.nextNode();
        assertEquals(testNode.getPath(), foundNode.getPath());        
    }        
}
