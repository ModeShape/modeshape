/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.naming;

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
public class MockInitialContextTest {

    private String validName;
    private Object registeredObject;

    @Before
    public void beforeEach() {
        this.validName = "java:jboss/unit/test/name";
        this.registeredObject = "This is the registered object";
    }

    @After
    public void afterEach() {
        MockInitialContext.tearDown();
    }

    @Test
    public void shouldCreateInitialContextAndRegisterAnObject() throws Exception {
        MockInitialContext.register(this.validName, this.registeredObject);
        for (int i = 0; i != 10; ++i) {
            assertThat(new InitialContext().lookup(this.validName), is(sameInstance(this.registeredObject)));
        }
    }

    @Test
    public void shouldTearDownMockInitialContextUponRequest() throws Exception {
        // Set it up ...
        // (Don't want to use 'expected', since the NamingException could be thrown here and we wouldn't know the difference)
        MockInitialContext.register(this.validName, this.registeredObject);
        for (int i = 0; i != 10; ++i) {
            assertThat(new InitialContext().lookup(this.validName), is(sameInstance(this.registeredObject)));
        }
        // Tear it down ...
        MockInitialContext.tearDown();
        try {
            new InitialContext().lookup(this.validName);
            throw new AssertionFailedError("Failed to throw exception");
        } catch (NameNotFoundException e) {
            // expected
        }
    }

}
