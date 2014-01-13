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
package org.modeshape.jdbc;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.sql.Driver;
import java.util.Properties;
import javax.naming.Context;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the
 * {@link ModeShapeEngine}. Essentially this is an integration test, but it does test lower-level functionality of the
 * implementation of the JCR interfaces related to querying. (It is simply more difficult to unit test these implementations
 * because of the difficulty in mocking the many other components to replicate the same functionality.)
 * <p>
 * Also, because queries are read-only, the engine is set up once and used for the entire set of test methods.
 * </p>
 * <p>
 * The following are the SQL semantics that the tests will be covering:
 * <li>variations of simple SELECT * FROM</li>
 * <li>JOIN
 * </p>
 * <p>
 * To create the expected results to be used to run a test, use the test and print method: example:
 * DriverTestUtil.executeTestAndPrint(this.connection, "SELECT * FROM [nt:base]"); This will print the expected results like this:
 * String[] expected = { "jcr:primaryType[STRING]", "mode:root", "car:Car", "car:Car", "nt:unstructured" } Now copy the expected
 * results to the test method. Then change the test to run the executeTest method passing in the <code>expected</code> results:
 * example: DriverTestUtil.executeTest(this.connection, "SELECT * FROM [nt:base]", expected);
 * </p>
 */
public class JcrDriverIntegrationTest extends AbstractJdbcDriverIntegrationTest {

    @Override
    protected Driver createDriver( JcrRepository repository ) throws Exception {
        // Create a JcrDriver instance that uses JNDI ...
        final Context jndi = mock(Context.class);
        when(jndi.lookup(anyString())).thenReturn(repository);
        JcrDriver.JcrContextFactory contextFactory = new JcrDriver.JcrContextFactory() {
            @Override
            public Context createContext( Properties properties ) {
                return jndi;
            }
        };

        return new JcrDriver(contextFactory);
    }

    @Override
    protected String createConnectionUrl( JcrRepository repository ) throws Exception {
        return LocalJcrDriver.JNDI_URL_PREFIX + "jcr/local?repositoryName=" + repository.getName();
    }
}
