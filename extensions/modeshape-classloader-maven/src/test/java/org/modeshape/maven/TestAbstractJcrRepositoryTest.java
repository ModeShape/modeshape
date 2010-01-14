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

package org.modeshape.maven;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class TestAbstractJcrRepositoryTest extends AbstractJcrRepositoryTest {

    @Test
    public void shouldBeAbleToStartAndShutdownRepository() throws Exception {
        for (int i = 0; i != 3; ++i) {
            startRepository();
            Session session = getRepository().login(getTestCredentials());
            assertThat(session, is(notNullValue()));
            assertThat(session.getRootNode(), is(notNullValue()));
            // Create a child node ...
            Node node = session.getRootNode().addNode("testnode", "nt:unstructured");
            assertThat(node, is(notNullValue()));
            assertThat(node.getName(), is("testnode"));
            assertThat(node.getPath(), is("/testnode"));
            // Save and close the session ...
            session.save();
            session.logout();
            shutdownRepository();
        }
    }

    @Test
    public void shouldAllowDataPersistedInOneSessionBeAccessibleInOtherSessions() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());
        assertThat(session, is(notNullValue()));
        assertThat(session.getRootNode(), is(notNullValue()));
        // Create a child node ...
        Node node = session.getRootNode().addNode("testnode", "nt:unstructured");
        assertThat(node, is(notNullValue()));
        assertThat(node.getName(), is("testnode"));
        assertThat(node.getPath(), is("/testnode"));
        // Save and close the session ...
        session.save();
        session.logout();

        for (int i = 0; i != 3; ++i) {
            // Create another session ...
            session = getRepository().login(getTestCredentials());
            assertThat(session, is(notNullValue()));
            assertThat(session.getRootNode(), is(notNullValue()));
            // Look for the child node ...
            node = session.getRootNode().getNode("testnode");
            assertThat(node, is(notNullValue()));
            assertThat(node.getName(), is("testnode"));
            assertThat(node.getPath(), is("/testnode"));
            // Close the session ...
            session.logout();
        }

    }

}
