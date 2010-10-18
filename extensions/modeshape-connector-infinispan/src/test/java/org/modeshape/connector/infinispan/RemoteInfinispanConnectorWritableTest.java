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
package org.modeshape.connector.infinispan;

import java.io.File;
import java.io.IOException;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.xml.sax.SAXException;

/**
 * @author johnament
 */
public class RemoteInfinispanConnectorWritableTest extends WritableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        try {
            RemoteInfinispanTestHelper.createServer();
        } catch (IOException ex) {
            throw new RuntimeException("IOException ", ex);
        }
        String[] predefinedWorkspaceNames = new String[] {"remowritable"};
        RemoteInfinispanSource source = new RemoteInfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);
        source.setRemoteInfinispanServerList(String.format("%s:%s",
                                                           RemoteInfinispanTestHelper.HOST,
                                                           RemoteInfinispanTestHelper.PORT));
        return source;
    }

    @Override
    protected void cleanUpSourceResources() throws Exception {
        RemoteInfinispanTestHelper.releaseServer();
        File store = new File("target/infinispan-remote-remowritable/jcr");
        FileUtil.delete(store);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws SAXException
     * @throws IOException
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws IOException, SAXException {
    }

}
