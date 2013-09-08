package org.modeshape.jcr;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * User: nick.knysh
 *
 */
public class QueryInRenamedFolderTest extends MultiUseAbstractTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("config/simple-repo-config.json");
        startRepository(config);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }


    @Test
    public void shouldFindNode() throws Exception {
        Node rootNode = session().getRootNode();
        Node folder = rootNode.addNode("folder", "nt:folder");
        Node file = folder.addNode("file", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:data", session().getValueFactory().createBinary("test".getBytes()));

        session().save();

        // Execute query
        Workspace workspace = session().getWorkspace();
        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM [nt:file] WHERE [jcr:path] LIKE '/folder/%'", Query.JCR_SQL2);


        query.setOffset(0);
        query.setLimit(Integer.MAX_VALUE);

        Assert.assertEquals(1, query.execute().getNodes().getSize());


        folder.getSession().move(folder.getPath(), "/folder_1");
        folder.getSession().save();

        workspace = session().getWorkspace();
        queryManager = workspace.getQueryManager();
        query = queryManager.createQuery("SELECT * FROM [nt:file] WHERE [jcr:path] LIKE '/folder_1/%'", Query.JCR_SQL2);


        query.setOffset(0);
        query.setLimit(Integer.MAX_VALUE);

        Assert.assertEquals(1, query.execute().getNodes().getSize());
    }


}
