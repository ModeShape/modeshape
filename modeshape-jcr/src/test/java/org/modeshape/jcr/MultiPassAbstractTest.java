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

import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Abstract test class for tests that repeatedly starting/stopping repositories.
 * <p>
 * Subclasses should call {@link #startRunStop(RepositoryOperation, String)} one or more times in each test, where the supplied
 * {@link RepositoryOperation} implementation does the work on a running repository.
 * </p>
 * 
 * @author rhauch
 * @author hchiorean
 */
public abstract class MultiPassAbstractTest extends AbstractTransactionalTest {

    protected void startRunStop( RepositoryOperation operation,
                                 String repositoryConfigFile ) throws Exception {
        URL configUrl = getClass().getClassLoader().getResource(repositoryConfigFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = null;

        try {
            repository = new JcrRepository(config);
            repository.start();

            operation.setRepository(repository).call();
        } finally {
            if (repository != null) {
                TestingUtil.killRepositoryAndContainer(repository);
            }
        }
    }

    protected abstract class RepositoryOperation implements Callable<Void> {
        protected JcrRepository repository;

        protected RepositoryOperation setRepository( JcrRepository repository ) {
            this.repository = repository;
            return this;
        }
    }
}
