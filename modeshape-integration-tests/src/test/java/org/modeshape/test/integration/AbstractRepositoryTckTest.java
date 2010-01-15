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
package org.modeshape.test.integration;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.modeshape.jcr.ModeShapeRepositoryStub;
import org.modeshape.jcr.ModeShapeTckTest;
import org.modeshape.jcr.JcrTckTest;

/**
 * Container for the standard integration test suites that should be run against various read-only and read-write repository
 * configurations.
 * <p>
 * The tests require the repository implementation for the TCK tests to change between runs. This is accomplished by adding a
 * {@link ChangeRepositoryTestCase trivial test} to the beginning of the suite that
 * {@link ModeShapeRepositoryStub#setCurrentConfigurationName(String) changes the repository configuration} in the
 * {@link ModeShapeRepositoryStub}.
 * </p>
 */
public abstract class AbstractRepositoryTckTest {

    public static TestSuite readWriteRepositorySuite( final String name ) {
        TestSuite suite = new TestSuite("Tests for " + name + "(read-write)");
        suite.addTest(new ChangeRepositoryTestCase(name));
        suite.addTest(JcrTckTest.suite());
        suite.addTestSuite(ModeShapeTckTest.class);

        return suite;
    }

    public static TestSuite readOnlyRepositorySuite( String name ) {
        TestSuite suite = new TestSuite("Tests for " + name + "(read-only)");
        suite.addTest(new ChangeRepositoryTestCase(name));
        suite.addTest(JcrTckTest.readOnlySuite());
        suite.addTest(ModeShapeTckTest.readOnlySuite());

        return suite;
    }

    /**
     * Trivial test case that invokes {@link ModeShapeRepositoryStub#setCurrentConfigurationName(String)} to change the currently used
     * TCK configuration. If this test fails, that is an indication that the configuration is incorrect or that the {@code name}
     * parameter does not match a valid configuration name.
     */
    public static class ChangeRepositoryTestCase extends TestCase {
        private final String configurationName;

        public ChangeRepositoryTestCase( final String configurationName ) {
            super("testConfigurationNameChange");

            this.configurationName = configurationName;
        }

        public void testConfigurationNameChange() {
            ModeShapeRepositoryStub.setCurrentConfigurationName(configurationName);
        }
    }
}
