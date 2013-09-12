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
package org.modeshape.jcr.sequencer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.URL;
import javax.jcr.Node;
import javax.jcr.Property;
import org.junit.Test;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.TestSequencersHolder;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Session;

public class ManualSequencingTest extends SingleUseAbstractTest {

    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     Environment environment ) throws Exception {
        return RepositoryConfiguration.read("config/repo-config-manual-sequencing.json");
    }

    private JcrTools tools = new JcrTools();

    @Test
    public void shouldManuallySequence() throws Exception {
        URL url = getClass().getClassLoader().getResource("log4j.properties");
        assertNotNull(url);
        Session session = session();
        tools.uploadFile(session, "/files/log4j.properties", url);

        Node propFile = session.getNode("/files/log4j.properties");
        Property content = propFile.getProperty("jcr:content/jcr:data");
        assertNotNull(content);

        Node output = session.getRootNode().addNode("output");
        assertFalse(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));

        session.sequence("Counting sequencer", content, output);
        assertTrue(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));

        session.refresh(false);

        assertFalse(session.getRootNode().hasNode("files"));
        assertFalse(session.getRootNode().hasNode("output"));
    }

}
