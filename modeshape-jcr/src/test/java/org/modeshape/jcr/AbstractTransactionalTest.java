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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.modeshape.common.junit.SkipTestRule;

/**
 * Abstract test class which should be extended whenever there are tests which use JBoss JTA, to properly configure the output
 * directory locations of the JTA: "ObjectStore" and "PutObjectStoreDirHere" for some very annoying reason, are created inside
 * the current working dir, which is the root module dir during the test run.
 *
 * @author Horia Chiorean
 */
public abstract class AbstractTransactionalTest {

    @Rule
    public TestRule skipTestRule = new SkipTestRule();

    @BeforeClass
    public static void beforeSuite() {
        JTATestUtil.setJBossJTADefaultStoreLocations();
    }

    @AfterClass
    public static void afterSuite() {
        JTATestUtil.clearJBossJTADefaultStoreLocation();
    }
}
