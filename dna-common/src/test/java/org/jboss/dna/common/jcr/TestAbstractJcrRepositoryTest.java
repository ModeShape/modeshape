/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.jcr;

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
