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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
public class ClusteringService {

    private static final Logger LOGGER = Logger.getLogger(ClusteringService.class);

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
    private final AtomicBoolean isOpen = new AtomicBoolean(false);

    /**
     * The numbers of members in the cluster
     */
    private final AtomicInteger membersInCluster = new AtomicInteger(1);

    /**
     * The clustering configuration
     */
    private final RepositoryConfiguration.Clustering clusteringConfiguration;

    /**
     * The JGroups channel which will be used to send/receive event across the cluster
     */
    private Channel channel;

    /**
     * A list of message consumers which register themselves with this service.
     */
    private Set<MessageConsumer<Serializable>> consumers;

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
        //make sure the set is thread safe
        this.consumers = Collections.newSetFromMap(new ConcurrentHashMap<MessageConsumer<Serializable>, Boolean>());
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
     * Sends a message of a given type across a cluster.
     *
     * @param payload the main body of the message; must not be {@code null}
     * @param messageType the type of the message.
     * @return {@code true} if the send operation was successful, {@code false} otherwise
     */
    public boolean sendMessage( Serializable payload,
                                int messageType )  {
        if (!isOpen() || !multipleMembersInCluster()) {
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Process {0} from cluster {1} sending message {2} of type {3}", processId(), clusterName(), payload, messageType);
        }
        try {
            byte[] messageData = ClusterMessage.toByteArray(messageType, payload);
            Message jgMessage = new Message(null, null, messageData);
            channel.send(jgMessage);
            return true;
        } catch (Exception e) {
            // Something went wrong here
            throw new SystemFailureException(ClusteringI18n.errorSendingMessage.text(clusterName(), processId()), e);
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
                ClusterMessage<? extends Serializable> clusterMessage = ClusterMessage.fromByteArray(message.getBuffer(),
                                                                                                     getClass().getClassLoader());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Process {0} from cluster {1} received message {2}", processId(), clusterName(), clusterMessage);
                }

                for (MessageConsumer<Serializable> consumer : consumers) {
                    if (consumer.interestedIn(clusterMessage.getType())) {
                        consumer.consume(clusterMessage.getPayload());
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
            LOGGER.trace("Members of '{0}' cluster have changed: {1}, total count: {2}", clusterName(), newView, newView.getMembers().size());
            membersInCluster.set(newView.getMembers().size());
            if (membersInCluster.get() > 1) {
                LOGGER.debug(
                        "There are now multiple members of cluster '{0}'; changes will be propagated throughout the cluster",
                        clusterName());
            } else if (membersInCluster.get() == 1) {
                LOGGER.debug("There is only one member of cluster '{0}'; changes will be propagated locally only",
                             clusterName());
            }
        }
    }

    private static class ClusterMessage<T extends Serializable> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int type;
        private final T payload;

        private ClusterMessage( int type,
                                T payload ) {
            this.type = type;
            this.payload = payload;
        }

        int getType() {
            return type;
        }

        T getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ClusterMessage{");
            sb.append("type=").append(type);
            sb.append(", payload=").append(payload);
            sb.append('}');
            return sb.toString();
        }

        static <T extends Serializable> byte[] toByteArray( int type,
                                                            T payload ) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(output);
            try {
                stream.writeObject(new ClusterMessage<T>(type, payload));
            } finally {
                stream.close();
            }
            return output.toByteArray();
        }

        @SuppressWarnings( "unchecked" )
        static ClusterMessage<? extends Serializable> fromByteArray( byte[] data,
                                                                     ClassLoader classLoader ) throws IOException, ClassNotFoundException {
            if (classLoader == null) {
                classLoader = ClusterMessage.class.getClassLoader();
            }
            ObjectInputStreamWithClassLoader input = new ObjectInputStreamWithClassLoader(new ByteArrayInputStream(data),
                                                                                          classLoader);
            try {
                return (ClusterMessage<? extends Serializable>)input.readObject();
            } finally {
                input.close();
            }
        }

    }

    private class Listener implements ChannelListener {
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
