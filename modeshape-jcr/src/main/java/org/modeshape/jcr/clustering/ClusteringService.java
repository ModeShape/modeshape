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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.RepositoryException;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.fork.ForkChannel;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

/**
 * ModeShape service which handles sending/receiving messages in a cluster via JGroups
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public final class ClusteringService {

    protected static final Logger LOGGER = Logger.getLogger(ClusteringService.class);

    /**
     * An approximation about the maximum delay in local time that we consider acceptable.
     */
    private static final long DEFAULT_MAX_CLOCK_DELAY_CLUSTER_MILLIS = TimeUnit.MINUTES.toMillis(10);

    /**
     * The listener for channel changes.
     */
    private final Listener listener;

    /**
     * The component that will receive the JGroups messages.
     */
    private final Receiver receiver;

    /**
     * Flag that dictates whether this service has connected to the cluster.
     */
    protected final AtomicBoolean isOpen;

    /**
     * The numbers of members in the cluster
     */
    protected final AtomicInteger membersInCluster;

    /**
     * The maximum accepted clock delay between cluster members
     */
    private final long maxAllowedClockDelayMillis;

    /**
     * The JGroups channel which will be used to send/receive event across the cluster
     */
    private Channel channel;

    /**
     * A list of message consumers which register themselves with this service.
     */
    private final Set<MessageConsumer<Serializable>> consumers;

    /**
     * Creates an empty, not started clustering service.
     */
    public ClusteringService() {
        this.listener = new Listener();
        this.receiver = new Receiver();
        this.isOpen = new AtomicBoolean(false);
        this.membersInCluster = new AtomicInteger(1);
        this.maxAllowedClockDelayMillis = DEFAULT_MAX_CLOCK_DELAY_CLUSTER_MILLIS;
        this.consumers = new CopyOnWriteArraySet<>();
    }

    /**
     * Starts a standalone clustering service which in turn will start & connect its own JGroup channel.
     *
     * @param clusterName the name of the cluster to which the JGroups channel should connect.
     * @param jgroupsConfig either the path or the XML content of a JGroups configuration file; may be null
     * @return this instance
     */
    public synchronized ClusteringService startStandalone(String clusterName, String jgroupsConfig) {
        if (StringUtil.isBlank(clusterName)) {
            clusterName = "modeshape-cluster";
        }
        try {
            // Create the new channel by calling the delegate method ...
            this.channel = newChannel(jgroupsConfig);
            // Add a listener through which we'll know what's going on within the cluster ...
            this.channel.addChannelListener(listener);

            // Set the receiver through which we'll receive all of the changes ...
            this.channel.setReceiver(receiver);

            // Now connect to the cluster ...
            this.channel.connect(clusterName);

            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts a new clustering service by forking a channel of an existing JGroups channel.
     *
     * @param mainChannel a {@link org.jgroups.Channel} instance; may not be null.
     * @return this instance
     */
    public synchronized ClusteringService startForked(Channel mainChannel) {
        CheckArg.isNotNull(mainChannel, "mainChannel");
        try {
            Protocol topProtocol = mainChannel.getProtocolStack().getTopProtocol();
            //add the fork at the top of the stack (the bottom should be either TCP/UDP) to preserve the default configuration
            this.channel = new ForkChannel(mainChannel, "modeshape-stack", "modeshape-fork-channel", true, ProtocolStack.ABOVE,
                                           topProtocol.getClass());
            // Add a listener through which we'll know what's going on within the cluster ...
            this.channel.addChannelListener(listener);

            // Set the receiver through which we'll receive all of the changes ...
            this.channel.setReceiver(receiver);

            // The name of the cluster is ignored for fork channels
            this.channel.connect("modeshape-fork-channel");

            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Channel newChannel( String jgroupsConfig ) throws Exception {

        if (StringUtil.isBlank(jgroupsConfig)) {
            return new JChannel();
        }

        ProtocolStackConfigurator configurator = null;
        //check if it points to a file accessible via the class loader
        InputStream stream = ClusteringService.class.getClassLoader().getResourceAsStream(jgroupsConfig);
        try {
            configurator = XmlConfigurator.getInstance(stream);
        } catch (IOException e) {
            LOGGER.debug(e, "Channel configuration is not a classpath resource");
            //check if the configuration is valid xml content
            stream = new ByteArrayInputStream(jgroupsConfig.getBytes());
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

        if (configurator == null) {
            throw new RepositoryException(ClusteringI18n.channelConfigurationError.text(jgroupsConfig));
        }
        return new JChannel(configurator);
    }

    /**
     * Adds a new message consumer to this service.
     * 
     * @param consumer a {@link MessageConsumer} instance.
     */
    @SuppressWarnings( "unchecked" )
    public synchronized void addConsumer( MessageConsumer<? extends Serializable> consumer ) {
        consumers.add((MessageConsumer<Serializable>)consumer);
    }

    /**
     * Shuts down and clears resources held by this service.
     */
    public synchronized void shutdown() {
        consumers.clear();
        if (channel != null) {
            // Mark this as not accepting any more ...
            isOpen.set(false);
            try {
                // Disconnect from the channel and close it ...
                channel.close();
                channel.removeChannelListener(listener);
                channel.setReceiver(null);
            } finally {
                channel = null;
            }
            membersInCluster.set(1);
        }
    }

    /**
     * Checks if this instance is open or not (open means the JGroups channel has been connected).
     * 
     * @return {@code true} if the service is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return isOpen.get();
    }

    /**
     * Checks if the cluster has multiple members.
     * 
     * @return {@code true} if the cluster has multiple members, {@code false} otherwise.
     */
    public boolean multipleMembersInCluster() {
        return membersInCluster.get() > 1;
    }

    /**
     * Returns the number of members in the cluster.
     * 
     * @return the number of active members
     */
    public int membersInCluster() {
        return membersInCluster.get();
    }

    /**
     * Returns the name of the cluster which has been configured for this service.
     * 
     * @return a {@code String} the name of the cluster; never {@code null}
     */
    public String clusterName() {
        return channel.getClusterName();
    }

    /**
     * Returns the maximum accepted delay in clock time between cluster members.
     * 
     * @return the number of milliseconds representing the maximum accepted delay.
     */
    public long getMaxAllowedClockDelayMillis() {
        return maxAllowedClockDelayMillis;
    }

    /**
     * Sends a message of a given type across a cluster.
     * 
     * @param payload the main body of the message; must not be {@code null}
     * @return {@code true} if the send operation was successful, {@code false} otherwise
     */
    public boolean sendMessage( Serializable payload ) {
        if (!isOpen() || !multipleMembersInCluster()) {
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending payload {0} in cluster {1} ", payload, clusterName());
        }
        try {
            byte[] messageData = toByteArray(payload);
            Message jgMessage = new Message(null, null, messageData);
            channel.send(jgMessage);
            return true;
        } catch (Exception e) {
            // Something went wrong here
            throw new SystemFailureException(ClusteringI18n.errorSendingMessage.text(clusterName()), e);
        }
    }

    private byte[] toByteArray( Object payload ) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(output);
        try {
            stream.writeObject(payload);
        } finally {
            stream.close();
        }
        return output.toByteArray();
    }

    protected Serializable fromByteArray( byte[] data,
                                          ClassLoader classLoader ) throws IOException, ClassNotFoundException {
        if (classLoader == null) {
            classLoader = ClusteringService.class.getClassLoader();
        }
        ObjectInputStreamWithClassLoader input = new ObjectInputStreamWithClassLoader(new ByteArrayInputStream(data), classLoader);
        try {
            return (Serializable)input.readObject();
        } finally {
            input.close();
        }
    }

    protected final class Receiver extends ReceiverAdapter {

        @Override
        public void block() {
            isOpen.set(false);
        }

        @Override
        public void receive( final org.jgroups.Message message ) {
            try {
                Serializable payload = fromByteArray(message.getBuffer(), getClass().getClassLoader());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cluster {0} received payload {1}", clusterName(), payload);
                }

                for (MessageConsumer<Serializable> consumer : consumers) {
                    if (consumer.getPayloadType().isAssignableFrom(payload.getClass())) {
                        consumer.consume(payload);
                    }
                }
            } catch (Exception e) {
                // Something went wrong here (this should not happen) ...
                String msg = ClusteringI18n.errorReceivingMessage.text(clusterName());
                throw new SystemFailureException(msg, e);
            }
        }

        @Override
        public void suspect( Address suspectedMbr ) {
            LOGGER.error(ClusteringI18n.memberOfClusterIsSuspect, clusterName(), suspectedMbr);
        }

        @Override
        public void viewAccepted( View newView ) {
            LOGGER.trace("Members of '{0}' cluster have changed: {1}, total count: {2}",
                         clusterName(),
                         newView,
                         newView.getMembers().size());
            membersInCluster.set(newView.getMembers().size());
            if (membersInCluster.get() > 1) {
                LOGGER.debug("There are now multiple members of cluster '{0}'; changes will be propagated throughout the cluster",
                             clusterName());
            } else if (membersInCluster.get() == 1) {
                LOGGER.debug("There is only one member of cluster '{0}'; changes will be propagated locally only", clusterName());
            }
        }
    }

    protected class Listener implements ChannelListener {
        @Override
        public void channelClosed( Channel channel ) {
            isOpen.set(false);
        }

        @Override
        public void channelConnected( Channel channel ) {
            isOpen.set(true);
        }

        @Override
        public void channelDisconnected( Channel channel ) {
            isOpen.set(false);
        }
    }

    /**
     * ObjectInputStream extension that allows a different class loader to be used when resolving types.
     */
    private static class ObjectInputStreamWithClassLoader extends ObjectInputStream {

        private ClassLoader cl;

        public ObjectInputStreamWithClassLoader( InputStream in,
                                                 ClassLoader cl ) throws IOException {
            super(in);
            this.cl = cl;
        }

        @Override
        protected Class<?> resolveClass( ObjectStreamClass desc ) throws IOException, ClassNotFoundException {
            if (cl == null) {
                return super.resolveClass(desc);
            }
            try {
                return Class.forName(desc.getName(), false, cl);
            } catch (ClassNotFoundException ex) {
                return super.resolveClass(desc);
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.cl = null;
        }
    }
}
