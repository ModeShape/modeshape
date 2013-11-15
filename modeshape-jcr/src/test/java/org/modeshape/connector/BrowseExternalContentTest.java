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
package org.modeshape.connector;

import java.util.ArrayList;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Session;

public class BrowseExternalContentTest extends SingleUseAbstractTest {

    protected static final String TEXT_CONTENT = "Some text content";

    @Before
    public void before() throws Exception {
        tools = new JcrTools();
        startRepositoryWithConfiguration(getClass().getClassLoader()
                .getResourceAsStream("config/repo-config-federation-browse.json"));
//        registerNodeTypes("cnd/flex.cnd");
    }

    @Test
    public void shouldBrowseExternalWorkspace() throws Exception {
        Session session2 = session.getRepository().login("mock-source");
        assertTrue(session2 != null);
        
        Node node = session2.getNode("/");
        System.out.println("Root=" + node.getName());
        
        System.out.println("Level1------------");
        NodeIterator it = node.getNodes();
        
        
        while (it.hasNext()) {
            System.out.println(it.nextNode().getName());
        }
        
//        assertEquals(0, dirs.size());
    }
    
    
}
