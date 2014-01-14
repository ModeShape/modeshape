/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryConfiguration;

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
     * The id of the process which started this service
     */
    private final String processId;

    /**
     * The listener for channel changes.
     */
    private final Listener listener = new Listener();

    /**
     * The component that will receive the JGroups messages.
     */
    private final Receiver receiver = new Receiver();

    /**
     * Flag that dictates whether this service has connected to the cluster.
     */
    protected final AtomicBoolean isOpen = new AtomicBoolean(false);

    /**
     * The numbers of members in the cluster
     */
    protected final AtomicInteger membersInCluster = new AtomicInteger(1);

    /**
     * The clustering configuration
     */
    private final RepositoryConfiguration.Clustering clusteringConfiguration;

    /**
     * The maximum accepted clock delay between cluster members
     */
    private final long maxAllowedClockDelayMillis = DEFAULT_MAX_CLOCK_DELAY_CLUSTER_MILLIS;

    /**
     * The JGroups channel which will be used to send/receive event across the cluster
     */
    private Channel channel;

    /**
     * A list of message consumers which register themselves with this service.
     */
    protected Set<MessageConsumer<Serializable>> consumers;

    /**
     * Creates a new service
     * 
     * @param clusteringConfiguration the clustering configuration
     * @param processId the id of the process which started this bus
     */
    public ClusteringService( String processId,
                              RepositoryConfiguration.Clustering clusteringConfiguration ) {
        this.processId = processId;
        this.clusteringConfiguration = clusteringConfiguration;
        assert clusteringConfiguration.isEnabled();
        // make sure the set is thread safe
        this.consumers = new CopyOnWriteArraySet<MessageConsumer<Serializable>>();
    }

    /**
     * Starts the clustering service.
     * 
     * @throws Exception if anything unexpected fails
     */
    public synchronized void start() throws Exception {
        if (channel != null) {
            // we've already been started
            return;
        }

        String clusterName = clusteringConfiguration.getClusterName();
        if (clusterName == null) {
            throw new IllegalStateException(ClusteringI18n.clusterNameRequired.text());
        }
        // Create the new channel by calling the delegate method ...
        channel = newChannel();
        // Add a listener through which we'll know what's going on within the cluster ...
        channel.addChannelListener(listener);

        // Set the receiver through which we'll receive all of the changes ...
        channel.setReceiver(receiver);

        // Now connect to the cluster ...
        channel.connect(clusterName);
    }

    private Channel newChannel() throws Exception {
        // Try to get the channel directly from the configuration (and its environment) ...
        Channel channel = clusteringConfiguration.getChannel();
        if (channel != null) {
            return channel;
        }

        String lookupClassName = clusteringConfiguration.getChannelProviderClassName();
        assert lookupClassName != null;

        Class<?> lookupClass = Class.forName(lookupClassName);
        if (!ChannelProvider.class.isAssignableFrom(lookupClass)) {
            throw new IllegalArgumentException(
                                               "Invalid channel lookup class configured. Expected a subclass of org.modeshape.jcr.clustering.ChannelProvider. Actual class:"
                                               + lookupClass);
        }
        return ((ChannelProvider)lookupClass.newInstance()).getChannel(clusteringConfiguration);
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
        return clusteringConfiguration.getClusterName();
    }

    /**
     * Returns the id of the process which started this service.
     * 
     * @return a {@code String} the id of the process; never {@code null}
     */
    public String processId() {
        return processId;
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
            LOGGER.debug("Process {0} from cluster {1} sending payload {2}", processId(), clusterName(), payload);
        }
        try {
            byte[] messageData = toByteArray(payload);
            Message jgMessage = new Message(null, null, messageData);
            channel.send(jgMessage);
            return true;
        } catch (Exception e) {
            // Something went wrong here
            throw new SystemFailureException(ClusteringI18n.errorSendingMessage.text(clusterName(), processId()), e);
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
                    LOGGER.debug("Process {0} from cluster {1} received payload {2}", processId(), clusterName(), payload);
                }

                for (MessageConsumer<Serializable> consumer : consumers) {
                    if (consumer.getPayloadType().isAssignableFrom(payload.getClass())) {
                        consumer.consume(payload);
                    }
                }
            } catch (Exception e) {
                // Something went wrong here (this should not happen) ...
                String msg = ClusteringI18n.errorReceivingMessage.text(clusterName(), processId());
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
