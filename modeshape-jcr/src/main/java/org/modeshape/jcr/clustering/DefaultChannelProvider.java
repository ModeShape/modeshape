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
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.modeshape.jcr.RepositoryConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A standard {@link ChannelProvider} implementation which creates a default {@link JChannel} instance using the provided configuration.
 *
 * @author Horia Chiorean
 */
public class DefaultChannelProvider implements ChannelProvider {

    @Override
    public JChannel getChannel( RepositoryConfiguration.Clustering clusteringConfig ) throws Exception{
        String channelConfiguration = clusteringConfig.getChannelConfiguration();

        if (channelConfiguration == null || channelConfiguration.trim().length() == 0) {
            return new JChannel();
        }
        // Try the XML configuration first ...
        ProtocolStackConfigurator configurator = null;
        InputStream stream = new ByteArrayInputStream(channelConfiguration.getBytes());
        try {
            configurator = XmlConfigurator.getInstance(stream);
        } catch (IOException e) {
            // ignore, since the configuration may be of another form ...
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore this
            }
        }
        if (configurator != null) {
            return new JChannel(configurator);
        }
        // Otherwise, just try the regular configuration ...
        return new JChannel(channelConfiguration);
    }
}
