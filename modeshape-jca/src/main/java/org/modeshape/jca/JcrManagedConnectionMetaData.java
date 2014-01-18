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
package org.modeshape.jca;

import javax.jcr.Repository;
import javax.jcr.Session;

import javax.resource.ResourceException;

import javax.resource.spi.ManagedConnectionMetaData;
import org.modeshape.common.annotation.Immutable;

/**
 * Implements Managed Connection Metadata.
 *
 * @author kulikov
 */
@Immutable
public class JcrManagedConnectionMetaData implements ManagedConnectionMetaData {

    private final Repository repository;
    private final Session session;

    /**
     * Constructs new object instance.
     *
     * @param repository JCR repository instance
     * @param session JSR session instance.
     */
    public JcrManagedConnectionMetaData(Repository repository, Session session) {
        this.repository = repository;
        this.session = session;
    }

    /**
     * Returns Product name of the underlying EIS instance connected through the
     * ManagedConnection.
     *
     * @return Product name of the EIS instance
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return repository.getDescriptor(Repository.REP_NAME_DESC);
    }

    /**
     * Returns Product version of the underlying EIS instance connected through
     * the ManagedConnection.
     *
     * @return Product version of the EIS instance
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return repository.getDescriptor(Repository.REP_VERSION_DESC);
    }

    /**
     * Returns maximum limit on number of active concurrent connections
     *
     * @return Maximum limit for number of active concurrent connections
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns name of the user associated with the ManagedConnection instance
     *
     * @return Name of the user
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getUserName() throws ResourceException {
        return session.getUserID();
    }
}
