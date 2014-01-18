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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryConfiguration;

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
            throw new RepositoryException(ClusteringI18n.channelConfigurationError.text(channelConfiguration));
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
