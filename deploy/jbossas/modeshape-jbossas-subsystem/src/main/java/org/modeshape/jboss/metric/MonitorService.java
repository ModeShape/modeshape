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
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start( final StartContext context ) {
        // nothing to do
    }

    /**
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