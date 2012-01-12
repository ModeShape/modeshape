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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.jcr.sequencer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.infinispan.manager.CacheContainer;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrConstants;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Class which serves as base for various sequencer unit tests.
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractSequencerTest extends SingleUseAbstractTest {

    protected Node rootNode;
  
    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName, CacheContainer cacheContainer ) throws  Exception{
        return RepositoryConfiguration.read(getRepositoryConfigStream(), repositoryName).with(cacheContainer);
    }

    /**
     * Returns an input stream to a JSON file which will be used to configure the repository. By default, this is config/repot-config.json
     * @return a {@code InputStream} instance
     */
    protected InputStream getRepositoryConfigStream() {
        return resourceStream("config/repo-config.json");
    }

    /**
     * Creates a nt:file node, under the root node, at the given path and with the jcr:data property pointing at the filepath.
     * @param nodePath the path under the root node, where the nt:file will be created.
     * @param filePath a path relative to {@link Class#getResourceAsStream(String)} where a file is expected at runtime
     * @return the new node
     *
     * @throws RepositoryException if anything fails
     */
    protected Node createNodeWithContentFromFile( String nodePath, String filePath ) throws RepositoryException {
        Node file = rootNode.addNode(nodePath);
        Node content = file.addNode(JcrConstants.JCR_CONTENT);
        content.setProperty(JcrConstants.JCR_DATA, ((javax.jcr.Session)session).getValueFactory().createBinary(resourceStream(filePath)));
        session.save();
        return file;
    }
    
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = session.getRootNode();
    }

    protected Node getSequencedNode(Node parentNode, String path) throws  Exception{
       return getSequencedNode(parentNode, path, 2);
    }
    
    protected Node getSequencedNode(Node parentNode, String path, int maxWaitTimeSeconds) throws  Exception{
        //TODO author=Horia Chiorean date=12/14/11 description=Change this hack once there is a proper way (events) of retrieving the sequenced node
        long maxWaitTime = TimeUnit.SECONDS.toNanos(maxWaitTimeSeconds);
        long start = System.nanoTime();
        while (System.nanoTime() - start <= maxWaitTime) {
            try {
                return parentNode.getNode(path);
            } catch (Exception e) {
            }
        }
        return null;
    }



}
