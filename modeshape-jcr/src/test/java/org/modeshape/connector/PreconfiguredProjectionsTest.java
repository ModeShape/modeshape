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

package org.modeshape.connector;

import java.io.File;
import java.io.FileOutputStream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.SingleUseAbstractTest;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the loading of preconfigured projections.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class PreconfiguredProjectionsTest extends SingleUseAbstractTest {

    @Before
    public void before() throws Exception {
        FileUtil.delete("target/files");
        File fileConnectorTestDir = new File("target/files");
        fileConnectorTestDir.mkdir();

        File testFile = new File(fileConnectorTestDir, "testFile");
        IoUtil.write(getClass().getClassLoader().getResourceAsStream("data/simple.json"), new FileOutputStream(testFile));
    }

    @Test
    public void shouldCreatePreconfiguredProjections() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/repo-config-federation-projections.json"));
        //from mock connector
        assertProjection("default", "/projection1");
        assertProjection("default", "/testRoot/projection2");
        assertProjection("ws1", "/projection1");
        assertProjection("ws2", "/testRoot/projection2");

        //from file system connector
        assertProjection("default", "/files");
        assertProjection("default", "/testFile");
    }

    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }

    private void assertProjection(String workspaceName, String projectionPath) throws RepositoryException {
        Session session = repository.login(workspaceName);
        assertNotNull(session.getNode(projectionPath));
    }
}
