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

package org.modeshape.webdav;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Integration test for the {@link org.modeshape.webdav.IWebdavStore} default implementation, tested using a real web-dav compliant client.
 * This should be updated whenever the default webdav support is updated (e.g. when fixing compliance bugs).
 * <p>
 * Ideally this would contain at least the test cases that <a href="http://www.webdav.org/neon/litmus">Litmus</a> has.
 * </p>
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class WebdavStoreClientTest {

    /**
     * The servlet context name comes from the pom.xml
     */
    private static final String SERVER_CONTEXT = "http://localhost:8090/webdav";

    protected Sardine sardine;

    @Before
    public void beforeEach() throws Exception {
        sardine = initializeWebDavClient();
    }

    protected Sardine initializeWebDavClient() throws SardineException {
        return SardineFactory.begin();
    }

    @Test
    public void shouldConnectWithValidCredentials() throws Exception {
        String uri = resourceUri(null);
        assertTrue(sardine.exists(uri));
        try {
            assertNotNull(sardine.getResources(uri));
        } catch (SardineException e) {
            //there seems to be a bug in Sardine146 (which is the only one available via Maven atm)
            assertEquals(302, e.getStatusCode());
        }
    }

    @Test
    public void shouldCreateFolder() throws Exception {
        String folderName = "testFolder" + UUID.randomUUID().toString();
        String uri = resourceUri(folderName);
        sardine.createDirectory(uri);
        assertTrue(sardine.exists(uri));
        DavResource folder = getResourceAtURI(uri);
        assertEquals(0l, folder.getContentLength().longValue());
    }

    protected DavResource getResourceAtURI( String uri ) throws SardineException {
        List<DavResource> resourcesList = sardine.getResources(uri);
        assertEquals(1, resourcesList.size());
        return resourcesList.get(0);
    }

    @Test
    public void shouldCreateFile() throws Exception {
        String folderUri = resourceUri("testDirectory" + UUID.randomUUID().toString());
        sardine.createDirectory(folderUri);
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("textfile.txt");
        assertNotNull(fileStream);

        String fileUri = folderUri + "/testFile" + UUID.randomUUID().toString();
        sardine.put(fileUri, fileStream);

        assertTrue(sardine.exists(fileUri));
        DavResource file = getResourceAtURI(fileUri);
        byte[] fileBytes = IoUtil.readBytes(getClass().getClassLoader().getResourceAsStream("textfile.txt"));
        assertEquals(fileBytes.length, file.getContentLength().longValue());
    }

    protected String resourceUri( String resourceName ) {
        return !StringUtil.isBlank(resourceName) ? getServerContext() + "/" + resourceName : getServerContext();
    }

    protected String getServerContext() {
        return SERVER_CONTEXT;
    }
}
