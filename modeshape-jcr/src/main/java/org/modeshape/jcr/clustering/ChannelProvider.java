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

package org.modeshape.jcr.clustering;

import org.jgroups.JChannel;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Interface used by ModeShape clustering to provide a JChannel instance based on an existing
 * {@link org.modeshape.jcr.RepositoryConfiguration.Clustering configuration}
 *
 * Implementing classes are expected to have a public, no-arg constructor.
 * 
 * @author Horia Chiorean
 */
public interface ChannelProvider {
    
    /**
     * A method that is used to instantiate the {@link JChannel} object with the supplied configuration.
     *
     * @param clusteringConfig the configuration; should not be null
     * @return the JChannel instance; never null
     *
     * @throws Exception if there is a problem creating the new channel object
     */
    public JChannel getChannel(RepositoryConfiguration.Clustering clusteringConfig) throws Exception;
}
