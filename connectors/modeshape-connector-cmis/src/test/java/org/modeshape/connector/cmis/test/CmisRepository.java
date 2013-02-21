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
package org.modeshape.connector.cmis.test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * CMIS repository simulator.
 *
 * @author kulikov
 */
public class CmisRepository {

    private FolderImpl root;
    private RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();

    protected HashMap<String, Object> map;
    protected final String username = "modeshape";

    private MessageDigest md;

    public CmisRepository() {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }

        map = new HashMap();
        String rootPath = "/";

        Map<String, Object> params = new HashMap();

        params.put(PropertyIds.OBJECT_ID, new String(md.digest(rootPath.getBytes())));
        params.put(PropertyIds.PATH, rootPath);

        root = new FolderImpl(this, null, params);
    }
    
    public Session openSession() {
        return new SessionImpl(this);
    }

    /**
     * Provides access to the root folder.
     *
     * @return root folder instance
     */
    protected FolderImpl getRootFolder() {
        return root;
    }

    /**
     * Gets information about this repository.
     *
     * @return Repository info composite object
     */
    protected RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    /**
     * Adds new folder specified by given path.
     *
     * @param path path to the folder.
     */
    public ObjectId addFolder(ObjectId id, String path) {
        FolderImpl parent = findParentFolder(path);

        String objectId = id != null ? id.getId() : new String(md.digest(path.getBytes()));

        Map<String, Object> params = new HashMap();
        params.put(PropertyIds.OBJECT_ID, objectId);
        params.put(PropertyIds.PATH, path);

        FolderImpl folder = new FolderImpl(this, parent, params);
        
        parent.add(folder);
        
        return new ObjectIdImpl(folder.getId());
    }

    public ObjectId createFolder(Map<String, Object> params) {
        String path = (String) params.get(PropertyIds.PATH);
        FolderImpl parent = findParentFolder(path);

        params.put(PropertyIds.CREATED_BY, username);
        params.put(PropertyIds.LAST_MODIFIED_BY, username);
        params.put(PropertyIds.CREATION_DATE, new GregorianCalendar());
        params.put(PropertyIds.LAST_MODIFICATION_DATE, new GregorianCalendar());
        params.put(PropertyIds.PARENT_ID, parent.getId());

        String objectId = new String(md.digest(path.getBytes()));
        params.put(PropertyIds.OBJECT_ID, objectId);

        FolderImpl folder = new FolderImpl(this, parent, params);

        parent.add(folder);

        return new ObjectIdImpl(folder.getId());
    }

    /**
     * Creates new document.
     * 
     * @param id
     * @param path
     * @param name
     * @return 
     */
    public ObjectId createDocument(Map<String, Object> params, byte[] content) {
        String path = (String) params.get(PropertyIds.PATH);
        String name = (String) params.get(PropertyIds.NAME);

        String fqn = path + "/" + name;
        String objectId = new String(md.digest(fqn.getBytes()));

        params.put(PropertyIds.OBJECT_ID, objectId);
        FolderImpl folder = (FolderImpl) find(path);

        params.put(PropertyIds.CREATED_BY, username);
        params.put(PropertyIds.LAST_MODIFIED_BY, username);
        params.put(PropertyIds.CREATION_DATE, new GregorianCalendar());
        params.put(PropertyIds.LAST_MODIFICATION_DATE, new GregorianCalendar());

        DocumentImpl doc = new DocumentImpl(this, folder, params);

        String fileName = (String)params.get(PropertyIds.CONTENT_STREAM_FILE_NAME);
        String contentId = (String)params.get(PropertyIds.CONTENT_STREAM_ID);
        Integer length = (Integer)params.get(PropertyIds.CONTENT_STREAM_LENGTH);
        String mimeType = (String)params.get(PropertyIds.CONTENT_STREAM_MIME_TYPE);

        ContentStreamImpl contentStream = new ContentStreamImpl(fileName,
                BigInteger.valueOf(length), mimeType, new ByteArrayInputStream(content));
        doc.setContentStream(contentStream, true);
        
        map.put(doc.getId(), doc);
        folder.add(doc);

        return new ObjectIdImpl(doc.getId());
    }

    public ObjectId createDocument(Map<String, Object> params, ContentStream stream) {
        String path = (String) params.get(PropertyIds.PATH);
        String name = (String) params.get(PropertyIds.NAME);

        String fqn = path + "/" + name;
        String objectId = new String(md.digest(fqn.getBytes()));

        params.put(PropertyIds.OBJECT_ID, objectId);
        FolderImpl folder = (FolderImpl) find(path);
        DocumentImpl doc = new DocumentImpl(this, folder, params);

        if (stream != null) {
            doc.setContentStream(stream, true);
        }

        folder.add(doc);

        return new ObjectIdImpl(doc.getId());
    }

    /**
     * Gets object specified by given path.
     *
     * @param path path to object
     * @return object by given path or null it it does not exist
     */
    public CmisObject find(String path) {
        if (path.equals("/")) {
            return root;
        }

        FolderImpl folder = findParentFolder(path);
        return folder == null ? null : folder.find(path);
    }

    /**
     * Gives folder specified by path.
     * 
     * @param path
     * @return
     */
    private FolderImpl findParentFolder(String path) {
        String[] nodes = path.split("/");

        FolderImpl folder = root;
        String route = "";

        for (int i = 1; i < nodes.length - 1; i++) {
            route += "/" + nodes[i];
            folder = (FolderImpl) folder.find(route);
        }

        return folder;
    }

    /**
     * Searches objects by its ID
     * 
     * @param id object id
     * @return object with given id.
     */
    public Object getObjectById(String id) {
        return map.get(id);
    }

    //-----------------------------------------------------------------------/
    // Self testing
    //----------------------------------------------------------------------/
    private CmisRepository repository;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        repository = new CmisRepository();
        repository.addFolder(null, "/src");
        repository.addFolder(null, "/src/main");
        repository.addFolder(null, "/src/main/java");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetFolderByPath() {
        Folder f = (Folder) repository.find("/src");
        assertTrue(f != null);

        f = (Folder) repository.find("/src/main");
        assertTrue(f != null);

        f = (Folder) repository.find("/src/main/java");
        assertTrue(f != null);

        f = (Folder) repository.find("/home/work");
        assertTrue(f == null);
    }

    @Test
    public void testFindById() {
        ObjectId id = repository.addFolder(null, "/src/main/java/org");
        Folder f = (Folder)repository.getObjectById(id.getId());
        assertEquals("/src/main/java/org", f.getPath());
    }
    
    @Test
    public void testChildren() {
        Folder root = repository.getRootFolder();
        Iterator<CmisObject> it = root.getChildren().iterator();

        while (it.hasNext()) {
            CmisObject obj = it.next();
            System.out.println(obj.getName());
        }
    }

    @Test
    public void testDocument() {
        Map<String, Object> params = new HashMap();
        params.put(PropertyIds.PATH, "/src/main/java");
        params.put(PropertyIds.NAME, "Foo.java");
        ObjectId id = repository.createDocument(params, "Hello worlds".getBytes());

        DocumentImpl doc = (DocumentImpl) repository.getObjectById(id.getId());
        assertTrue(doc != null);

    }

    @Test
    public void shouldFindRootById() {
       Folder f = (Folder) repository.getObjectById(repository.root.getId());
       assertEquals(f, repository.root);
    }

    @Test
    public void shouldFindRootByPath() {
       Folder f = (Folder) repository.find("/");
       assertEquals(f, repository.root);
    }

}
