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

package org.modeshape.jcr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.JCRTestSuite;
import java.util.Enumeration;

/**
 * Helper class which provides various JUnit 3.x {@link TestSuite} instances, which should be used when running any form of Tck tests, to be able
 * to shutdown the engine at the end of the run.
 *
 * This helper provides two types of suites:
 *  - inline suites: which create an anonymous {@link TestSuite} instance and load some or all of the Tck tests in that instance
 *  - default suite: returns the suites which are part of the Tck, by default (e.g. {@link JCRTestSuite})
 *
 * @author Horia Chiorean
 */
public final class JcrTckSuites {

    public static JCRTestSuite defaultSuite() {
        return new JCRTestSuite() {
            @Override
            public void run( TestResult result ) {
                try {
                    JTATestUtil.setJBossJTADefaultStoreLocations();
                    super.run(result);
                } finally {
                    ModeShapeRepositoryStub.shutdownEngine();
                    JTATestUtil.clearJBossJTADefaultStoreLocation();
                }
            }
        };
    }

    public static TestSuite defaultSuiteInline() {
        return new TestSuite() {
            {
                for (Enumeration<Test> testsEnum = new JCRTestSuite().tests(); testsEnum.hasMoreElements(); ) {
                    addTest(testsEnum.nextElement());
                }
            }

            @Override
            public void run( TestResult result ) {
                try {
                    JTATestUtil.setJBossJTADefaultStoreLocations();
                    super.run(result);
                } finally {
                    ModeShapeRepositoryStub.shutdownEngine();
                    JTATestUtil.clearJBossJTADefaultStoreLocation();
                }
            }
        };
    }

    public static TestSuite someTestsInline( final Class<? extends TestCase> ownerClass ) {
        return new TestSuite() {
            {
                addTestSuite(ownerClass);
            }

            @Override
            public void run( TestResult result ) {
                try {
                    JTATestUtil.setJBossJTADefaultStoreLocations();
                    super.run(result);
                } finally {
                    ModeShapeRepositoryStub.shutdownEngine();
                    JTATestUtil.clearJBossJTADefaultStoreLocation();
                }
            }
        };
    }

    private JcrTckSuites() {
    }
}
