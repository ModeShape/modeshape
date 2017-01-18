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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.modeshape.jdbc.JcrHttpDriverIntegrationTest;

/**
 * Extension of the {@link JcrHttpDriverIntegrationTest} which runs the same tests, via Arquillian, in an AS7 container with the
 * ModeShape distribution kit deployed.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class )
@RunAsClient
public class JdbcDriverIntegrationTest extends JcrHttpDriverIntegrationTest {

    @Override
    protected String getWorkspaceName() {
        //must match the default config from standalone.xml
        return "default";
    }

    @Override
    protected String getUserName() {
        //must be a valid user name from the ModeShape AS security domain
        return "admin";
    }

    @Override
    protected String getRepositoryName() {
        //must match the default config from standalone.xml
        return "sample";
    }

    @Override
    protected String getPassword() {
        //must be a password for the above user, from the ModeShape AS security domain
        return "admin";
    }

    @Override
    protected String getContextPathUrl() {
        //this should be the context of the web application deployed inside AS7
        return"localhost:8080/modeshape-rest";
    }
}
