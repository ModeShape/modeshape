/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.connector.cmis.test;

import java.util.HashMap;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class SessionImplTest {

    private CmisRepository repo;
    private SessionImpl session;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        repo = new CmisRepository();
        session = new SessionImpl(repo);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAddthenFindByPath() {
        Map props = new HashMap();
        props.put(PropertyIds.PATH, "/src");
        session.createFolder(props, null);

        props.put(PropertyIds.PATH, "/src/main");
        session.createFolder(props, null);

        props.put(PropertyIds.PATH, "/src/main/java");
        session.createFolder(props, null);


        CmisObject object = session.getObjectByPath("/src/main/java");
        assertTrue(object != null);
    }

    @Test
    public void testCreateFolder() {
        Map props = new HashMap();
        props.put(PropertyIds.PATH, "/src");
        props.put(PropertyIds.NAME, "src");

        Folder root = session.getRootFolder();
        Folder src = root.createFolder(props);

        CmisObject cmisObject = session.getObject(src.getId());
        assertEquals(src, cmisObject);
    }
}
