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
        startEngineUsing("modeshape_configuration_inmemory.xml", UnitTestsForModeShapeMultiUseTest.class);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        stopEngine();
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
