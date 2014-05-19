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

package org.modeshape.jcr;

import java.util.Enumeration;
import org.apache.jackrabbit.test.JCRTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

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

    public static TestSuite someTestsInline( final Class<? extends TestCase>... classes ) {
        return new TestSuite() {
            {
                for (Class<? extends TestCase> testClass : classes) {
                    addTestSuite(testClass);
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

    private JcrTckSuites() {
    }
}
