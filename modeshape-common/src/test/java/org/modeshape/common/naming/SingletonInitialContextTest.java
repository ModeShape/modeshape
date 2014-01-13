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
package org.modeshape.common.naming;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SingletonInitialContextTest {

    private String validName;
    private Object registeredObject;

    @Before
    public void beforeEach() {
        this.validName = "java:jboss/unit/test/name";
        this.registeredObject = "This is the registered object";
    }

    @After
    public void afterEach() {
        SingletonInitialContextFactory.tearDown();
    }

    @Test
    public void shouldCreateInitialContextAndRegisterAnObject() throws Exception {
        SingletonInitialContext.register(this.validName, this.registeredObject);
        for (int i = 0; i != 10; ++i) {
            assertThat(new InitialContext().lookup(this.validName), is(sameInstance(this.registeredObject)));
        }
    }

    @Test
    public void shouldTearDownMockInitialContextUponRequest() throws Exception {
        // Set it up ...
        // (Don't want to use 'expected', since the NamingException could be thrown here and we wouldn't know the difference)
        SingletonInitialContext.register(this.validName, this.registeredObject);
        for (int i = 0; i != 10; ++i) {
            assertThat(new InitialContext().lookup(this.validName), is(sameInstance(this.registeredObject)));
        }
        // Tear it down ...
        SingletonInitialContextFactory.tearDown();
        try {
            new InitialContext().lookup(this.validName);
            throw new AssertionFailedError("Failed to throw exception");
        } catch (NameNotFoundException e) {
            // expected
        }
    }

}
