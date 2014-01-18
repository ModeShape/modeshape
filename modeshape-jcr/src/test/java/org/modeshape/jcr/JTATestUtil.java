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

/**
 * Helper class for all the tests which make use of JTA transactions, especially via JBOSS JTA.
 * 
 * @author Horia Chiorean
 */
public final class JTATestUtil {

    /**
     * If the following 2 props are not set, JBOSS JTA uses (for some reason) the user.dir system property as a root directory.
     * 
     * @see com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean
     */
    private static final String JBOSSJTA_OBJECT_STORE_DIR_PROP = "com.arjuna.ats.arjuna.objectstore.objectStoreDir";
    private static final String JBOSSJTA_DEFAULT_OBJECT_STORE_DIR_PROP = "com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean.default.objectStoreDir";

    private JTATestUtil() {
    }

    public static void setJBossJTADefaultStoreLocations() {
        String tempDir = System.getProperty("java.io.tmpdir");
        System.setProperty(JBOSSJTA_OBJECT_STORE_DIR_PROP, tempDir + "/ObjectStore");
        System.setProperty(JBOSSJTA_DEFAULT_OBJECT_STORE_DIR_PROP, tempDir + "/DefaultStore");
    }

    public static void clearJBossJTADefaultStoreLocation() {
        System.clearProperty(JBOSSJTA_DEFAULT_OBJECT_STORE_DIR_PROP);
        System.clearProperty(JBOSSJTA_OBJECT_STORE_DIR_PROP);
    }
}
