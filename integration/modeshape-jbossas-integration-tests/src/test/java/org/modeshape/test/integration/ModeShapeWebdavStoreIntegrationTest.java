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
package org.modeshape.test.integration;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.modeshape.web.jcr.webdav.ModeShapeWebdavStoreClientTest;

/**
 * Integration test which runs the same tests as {@link ModeShapeWebdavStoreClientTest}, only against the AS7 distribution.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class)
@RunAsClient
public class ModeShapeWebdavStoreIntegrationTest extends ModeShapeWebdavStoreClientTest {

    @Override
    protected Sardine initializeWebDavClient() throws SardineException {
        return SardineFactory.begin("admin", "admin");
    }

    @Override
    protected String getServerContext() {
        //this should be the context of the web application deployed inside AS7
        return "http://localhost:8080/modeshape-webdav";
    }

    @Override
    protected String getRepositoryName() {
        return "sample";
    }

    @Override
    @Ignore ("Doesn't apply to the EAP kit")
    public void shouldIgnoreMultiValuedProperties() throws Exception {
    }

    @Override
    @Ignore ("Doesn't apply to the EAP kit")
    public void shouldEscapeIllegalCharsInXMLValues() throws Exception {
    }
}
