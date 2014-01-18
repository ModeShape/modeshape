/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
