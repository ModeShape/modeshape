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

import javax.jcr.RepositoryException;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit test for the node-types feature, which allows initial cnd files to be pre-configured in a repository
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrNodeTypesTest extends SingleUseAbstractTest {

    @Test
    public void shouldRegisterCustomNodeTypeAtStartup() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-node-types.json"));

        validateNodesWithCustomTypes();
    }

    @Test
    public void shouldRegisterValidNodeTypesOnly() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream(
                "config/repo-config-invalid-node-types.json"));

        validateNodesWithCustomTypes();
    }

    private void validateNodesWithCustomTypes() throws RepositoryException {
        JcrRootNode rootNode = session.getRootNode();
        rootNode.addNode("car", "car:Car");
        rootNode.addNode("aircraft", "air:Aircraft");

        session.save();

        assertEquals("car:Car", session.getNode("/car").getPrimaryNodeType().getName());
        assertEquals("air:Aircraft", session.getNode("/aircraft").getPrimaryNodeType().getName());
    }
}
