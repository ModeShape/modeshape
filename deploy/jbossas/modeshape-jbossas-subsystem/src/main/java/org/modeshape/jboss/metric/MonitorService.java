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
package org.modeshape.jboss.metric;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;

/**
 * A service for obtaining the ModeShape monitoring repository statistics.
 */
public final class MonitorService implements Service<RepositoryMonitor> {

    /**
     * The injected repository instance associated with this service.
     */
    private final InjectedValue<JcrRepository> repoInjector = new InjectedValue<JcrRepository>();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public RepositoryMonitor getValue() throws IllegalStateException, IllegalArgumentException {
        try {
            return this.repoInjector.getValue().getRepositoryStatistics();
        } catch (Exception e) {
            // nothing to do
        }

        return RepositoryMonitor.EMPTY_MONITOR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start( final StartContext context ) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop( final StopContext context ) {
        // nothing to do
    }

    /**
     * @return the repository injector (never <code>null</code>)
     */
    public InjectedValue<JcrRepository> getJcrRepositoryInjector() {
        return this.repoInjector;
    }

}