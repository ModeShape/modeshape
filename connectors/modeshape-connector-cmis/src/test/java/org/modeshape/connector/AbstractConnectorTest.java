/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.connector;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.junit.*;
import org.modeshape.connector.cmis.test.BinaryValue;
import org.modeshape.connector.cmis.test.CmisRepository;
import org.modeshape.connector.cmis.test.Repositories;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 *
 * @author kulikov
 */
public class AbstractConnectorTest extends MultiUseAbstractTest {

    private static CmisRepository cmisRepository;

    @BeforeClass
    public static void beforeAll() throws Exception {
        setupRepository();
        RepositoryConfiguration config = RepositoryConfiguration.read("config/repository-1.json");
        startRepository(config);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    private static void setupRepository() {
        cmisRepository = new CmisRepository();
        Map<String, Object> params = new HashMap();

        params.put(PropertyIds.PATH, "/src");
        params.put(PropertyIds.NAME, "src");
        cmisRepository.createFolder(params);

        params.clear();
        params.put(PropertyIds.PATH, "/src/main");
        params.put(PropertyIds.NAME, "main");
        cmisRepository.createFolder(params);

        params.put(PropertyIds.PATH, "/src/main/java");
        params.put(PropertyIds.NAME, "java");
        cmisRepository.createFolder(params);

        params.clear();
        params.put(PropertyIds.PATH, "/src");
        params.put(PropertyIds.NAME, "pom.xml");

        byte[] content = "Hello world".getBytes();

        params.put(PropertyIds.CONTENT_STREAM_FILE_NAME, "pom.xml");
        params.put(PropertyIds.CONTENT_STREAM_LENGTH, content.length);
        params.put(PropertyIds.CONTENT_STREAM_MIME_TYPE, "text/plain");

        cmisRepository.createDocument(params, content);

        System.out.println("--- Registering session");
        Repositories.register(cmisRepository.openSession());
    }
}
