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
package org.modeshape.test;

import static org.junit.Assert.fail;
import javax.jcr.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UnitTestsForModeShapeMultiUseTest extends ModeShapeMultiUseTest {

    private static boolean addedContent = false;

    @BeforeClass
    public static void beforeAll() throws Exception {
        ModeShapeMultiUseTest.beforeAll();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        ModeShapeMultiUseTest.afterAll();
    }

    protected void addContentIfEmpty( Session session ) throws Exception {
        if (session.getRootNode().getNodes().getSize() == 1L) {
            if (addedContent) {
                fail("Already added content and should see some existing content");
            }
            session.getRootNode().addNode("topLevel", "nt:unstructured");
            session.save();
            addedContent = true;
        }
    }

    @Test
    public void shouldAllowCreatingContentPart1() throws Exception {
        Session session = session();
        addContentIfEmpty(session);
    }

    /**
     * It doesn't matter which order these are called in; the first will always create content and the second should always see
     * that new content.
     * 
     * @throws Exception
     */
    @Test
    public void shouldAllowCreatingContentPart2() throws Exception {
        Session session = session();
        addContentIfEmpty(session);
    }

}
