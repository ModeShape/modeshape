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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.bus.BusI18n;

/**
 * A standard {@link ChannelProvider} implementation which creates a default {@link JChannel} instance using the provided configuration.
 *
 * @author Horia Chiorean
 */
public class DefaultChannelProvider implements ChannelProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultChannelProvider.class);

    @Override
    public JChannel getChannel( RepositoryConfiguration.Clustering clusteringConfig ) throws Exception{
        String channelConfiguration = clusteringConfig.getChannelConfiguration();

        if (channelConfiguration == null || channelConfiguration.trim().length() == 0) {
            return new JChannel();
        }

        ProtocolStackConfigurator configurator = createConfigurator(channelConfiguration);
        if (configurator == null) {
            throw new RepositoryException(BusI18n.channelConfigurationError.text(channelConfiguration));
        }

        return new JChannel(configurator);
    }

    private ProtocolStackConfigurator createConfigurator( String channelConfiguration ) {
        ProtocolStackConfigurator configurator = null;
        //check if it points to a file accessible via the class loader
        InputStream stream = DefaultChannelProvider.class.getClassLoader().getResourceAsStream(channelConfiguration);
        try {
            configurator = XmlConfigurator.getInstance(stream);
        } catch (IOException e) {
            LOGGER.debug(e, "Channel configuration is not a classpath resource");
            //check if the configuration is valid xml content
            stream = new ByteArrayInputStream(channelConfiguration.getBytes());
            try {
                configurator = XmlConfigurator.getInstance(stream);
            } catch (IOException e1) {
                LOGGER.debug(e, "Channel configuration is not valid XML content");
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ignore this
                }
            }
        }

        return configurator;
    }
}
