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

package org.modeshape.jcr.clustering;

import org.jgroups.JChannel;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Interface used by ModeShape clustering to provide a JChannel instance based on an existing
 * {@link org.modeshape.jcr.RepositoryConfiguration.Clustering configuration} Implementing classes are expected to have a public,
 * no-arg constructor.
 * 
 * @author Horia Chiorean
 */
public interface ChannelProvider {

    /**
     * A method that is used to instantiate the {@link JChannel} object with the supplied configuration.
     * 
     * @param clusteringConfig the configuration; should not be null
     * @return the JChannel instance; never null
     * @throws Exception if there is a problem creating the new channel object
     */
    public JChannel getChannel( RepositoryConfiguration.Clustering clusteringConfig ) throws Exception;
}
