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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.locking.LockService;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.fork.ForkChannel;
import org.jgroups.protocols.CENTRAL_LOCK;
import org.jgroups.protocols.FORK;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;

/**
 * ModeShape service which handles sending/receiving messages in a cluster via JGroups. This service is also a
 * {@link org.modeshape.jcr.locking.LockingService} when running in a cluster, relying on JGroups' {@link CENTRAL_LOCK} protocol.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public abstract class ClusteringService implements org.modeshape.jcr.locking.LockingService {

    protected static final Logger LOGGER = Logger.getLogger(ClusteringService.class);

    /**
     * An approximation about the maximum delay in local time that we consider acceptable.
     */
    private static final long DEFAULT_MAX_CLOCK_DELAY_CLUSTER_MILLIS = TimeUnit.MINUTES.toMillis(10);

    /**
     * The listener for channel changes.
     */
    protected final Listener listener;

    /**
     * The component that will receive the JGroups messages.
     */
    protected final Receiver receiver;

    /**
     * The name of the cluster (in standalone mode) or the ID of the fork stack (in forked mode)
     */
    protected final String clusterName;

    /**
     * The JGroups channel which will be used to send/receive event across the cluster
     */
    protected Channel channel;

    /**
     * The service used for cluster-wide locking
     */
    protected LockService lockService;

    /**
     * The maximum accepted clock delay between cluster members
     */
    private final long maxAllowedClockDelayMillis;

    /**
     * The numbers of members in the cluster
     */
    private final AtomicInteger membersInCluster;

    /**
     * Flag that dictates whether this service has connected to the cluster.
     */
    private final AtomicBoolean isOpen;

    /**
     * A list of message consumers which register themselves with this service.
     */
    private final Set<MessageConsumer<Serializable>> consumers;

    /**
     * A map of cluster locks held via this service
     */
    private final ConcurrentMap<String, Lock> locksByName;

    /**
     * The default lock timeout
     */
    private volatile long lockTimeoutMillis = 0;

    protected ClusteringService( String clusterName ) {
        assert clusterName != null;
        this.clusterName = clusterName;

        this.listener = new Listener();
        this.receiver = new Receiver();
        this.isOpen = new AtomicBoolean(false);
        this.membersInCluster = new AtomicInteger(1);
        this.maxAllowedClockDelayMillis = DEFAULT_MAX_CLOCK_DELAY_CLUSTER_MILLIS;
        this.consumers = new CopyOnWriteArraySet<>();     
        this.locksByName = new ConcurrentHashMap<>();
    }

    /**
     * Performs a shutdown/startup sequence.
     * 
     * @throws java.lang.Exception if anything unexpected fails
     */
    public void restart() throws Exception {
        shutdown();
        init();
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
     * 
     * @return {@code true} if the service has been shutdown or {@code false} if it had already been shut down.
     */
    public synchronized boolean shutdown() {
        if (channel == null) {
            return false;
        }
        Address address = channel.getAddress();
        LOGGER.debug("{0} Shutting down clustering service...", address);
        consumers.clear();

        // Mark this as not accepting any more ...
        isOpen.set(false);
        try {
            LOGGER.debug("{0} releasing all held clustering locks...", address);
            unlock(locksByName.keySet().toArray(new String[locksByName.size()]));
            locksByName.clear();
        } catch (Throwable t) {
            LOGGER.debug(t, "Cannot release all locks...");
        }
        
        try {
            // Disconnect from the channel and close it ...
            channel.disconnect();
            channel.removeChannelListener(listener);
            channel.setReceiver(null);
            channel.close();
            LOGGER.debug("{0} Successfully closed main channel", address);
        } finally {
            channel = null;
        }
        membersInCluster.set(1);
        return true;
    }
    
    @Override
    public boolean tryLock( long time,
                            TimeUnit unit,
                            String... names) {
        Map<String, Lock> successfullyAcquiredLocks = new HashMap<>();
        for (String name : names) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{0} attempting to lock {1}", channel.getAddress(), name);
            }
            AtomicBoolean successfullyLocked = new AtomicBoolean(false);
            locksByName.compute(name, (lockName, existingLock) -> {
                if (existingLock != null) {
                    if (existingLock.tryLock()) {
                        LOGGER.debug("{0} locked successfully", name);
                        successfullyLocked.compareAndSet(false, true);
                    } else {
                        LOGGER.debug("Unable to acquire lock on {0}. Reverting back the already obtained locks: {1}", name,
                                     successfullyAcquiredLocks);
                        successfullyAcquiredLocks.values().forEach(Lock::unlock);
                    }
                    // we don't want to replace the old lock
                    return existingLock;
                } else {
                    Lock lock = lockService.getLock(name);
                    try {
                        if (attemptToLock(time, unit, name, lock)) {
                            successfullyLocked.compareAndSet(false, true);
                            successfullyAcquiredLocks.putIfAbsent(name, lock);
                            // we successfully acquired a new lock so we'll want to map it
                            return lock;
                        } else {
                            successfullyAcquiredLocks.values().forEach(Lock::unlock);
                        }
                    } catch (InterruptedException e) {
                        LOGGER.debug("Thread " + Thread.currentThread().getName()
                                     + " received interrupt request while waiting to acquire lock '{0}'", name);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        LOGGER.error(e, JcrI18n.unexpectedLockingError, name);
                    }
                    // there is no previous lock and the locking was unsuccessful so return null
                    return null;
                }                 
            });
            
            if (!successfullyLocked.get()) {
                // we were unable to obtain a lock so just return
                return false;               
            }
        }
        return true;        
    }

    public boolean attemptToLock(long time, TimeUnit unit, String name, Lock lock) throws InterruptedException {
        if (time > 0) {
            // even though JG has a tryLock(timeunit) method, sometimes it seems to deadlock when using it
            // so this is a workaround for that
            long timeUnitMillis = TimeUnit.MILLISECONDS.convert(time, unit);
            int threadWaitTimeMillis = 100;
            long repeatCycles = timeUnitMillis / threadWaitTimeMillis;
            while (repeatCycles-- > 0) {
                LOGGER.debug("Attempt {0} at obtaining cluster lock {1}", repeatCycles, name);
                boolean success = lock.tryLock();
                if (success) {
                    return true;
                }
                LOGGER.debug("...attempt failed. Sleeping for {0} millis before retrying...", threadWaitTimeMillis);
                Thread.sleep(threadWaitTimeMillis);
            }
            return false;
        } else {
            return lock.tryLock();     
        }
    }

    @Override
    public boolean tryLock(String... names) {
        return tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS, names);
    }

    @Override
    public void setLockTimeout(long lockTimeoutMillis) {
        CheckArg.isNonNegative(lockTimeoutMillis, "lockTimeoutMillis");
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    @Override
    public List<String> unlock(String... names) {
        return Arrays.stream(names).map(name -> {
            if (LOGGER.isDebugEnabled() && channel != null) {
                LOGGER.debug("{0} attempting to unlock {1}", channel.getAddress(), name);
            }
            Lock oldLock = locksByName.computeIfPresent(name, (lockName, lock) -> {
                try {
                    if (lock.tryLock()) {
                        // this is the only way to tell in JG atm if a lock is actually unlocked successfully or not....
                        lock.unlock();
                        LOGGER.debug("Unlocked {0}", name);
                        return null;
                    } else {
                        return lock;
                    }
                } catch (Exception e) {
                    LOGGER.error(e, JcrI18n.unexpectedLockingError, name);
                    return lock;
                }
            });
            return oldLock != null ? name : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
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
            LOGGER.debug("{0} sending payload {1} in cluster {2} ", channel.getAddress(), payload, clusterName());
        }
        try {
            byte[] messageData = toByteArray(payload);
            Message jgMessage = new Message(null, channel.getAddress(), messageData);
            channel.send(jgMessage);
            return true;
        } catch (Exception e) {
            // Something went wrong here
            throw new SystemFailureException(ClusteringI18n.errorSendingMessage.text(clusterName()), e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusteringService[cluster_name='");
        sb.append(clusterName()).append("', address=").append(getChannel().getAddress()).append("]");
        return sb.toString();
    }

    /**
     * Starts a standalone clustering service which in turn will start & connect its own JGroup channel.
     * 
     * @param clusterName the name of the cluster to which the JGroups channel should connect; may not be null
     * @param jgroupsConfig either the path or the XML content of a JGroups configuration file; may not be null
     * @return a {@link org.modeshape.jcr.clustering.ClusteringService} instance, never null
     */
    public static ClusteringService startStandalone( String clusterName,
                                                     String jgroupsConfig ) {
        ClusteringService clusteringService = new StandaloneClusteringService(clusterName, jgroupsConfig);
        clusteringService.init();
        return clusteringService;
    }
    
    /**
     * Starts a standalone clustering service which uses the supplied channel.
     * 
     *
     * @param clusterName the name of the cluster to which the JGroups channel should connect; may not be null
     * @param channel a  {@link Channel} instance, may not be {@code null}
     * @return a {@link org.modeshape.jcr.clustering.ClusteringService} instance, never null
     */
    public static ClusteringService startStandalone(String clusterName, Channel channel) {
        ClusteringService clusteringService = new StandaloneClusteringService(clusterName, channel);
        clusteringService.init();
        return clusteringService;
    }

    /**
     * Starts a new clustering service by forking a channel of an existing JGroups channel.
     * 
     * @param mainChannel a {@link Channel} instance; may not be null.
     * @return a {@link org.modeshape.jcr.clustering.ClusteringService} instance, never null
     */
    public static ClusteringService startForked( Channel mainChannel ) {
        if (!mainChannel.isConnected()) {
            throw new IllegalStateException(ClusteringI18n.channelNotConnected.text());                        
        }
        ClusteringService clusteringService = new ForkedClusteringService(mainChannel);
        clusteringService.init();
        return clusteringService;
    }

    private byte[] toByteArray( Object payload ) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(output)) {
            stream.writeObject(payload);
        }
        return output.toByteArray();
    }

    protected Serializable fromByteArray( byte[] data,
                                          ClassLoader classLoader ) throws IOException, ClassNotFoundException {
        if (classLoader == null) {
            classLoader = ClusteringService.class.getClassLoader();
        }
        try (ObjectInputStreamWithClassLoader input = new ObjectInputStreamWithClassLoader(new ByteArrayInputStream(data),
                                                                                           classLoader)) {
            return (Serializable)input.readObject();
        }
    }

    protected Channel getChannel() {
        return channel;
    }

    protected abstract void init();

    @SuppressWarnings( "synthetic-access" )
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
                    LOGGER.debug("{0} from cluster {1} received payload {2}", channel.getAddress(), clusterName(), payload);
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
            LOGGER.trace("{0} Members of '{1}' cluster have changed: {2}, total count: {3}", channel.getAddress(), clusterName(), 
                         newView, newView.getMembers().size());
            membersInCluster.set(newView.getMembers().size());
            if (LOGGER.isDebugEnabled()) {
                if (membersInCluster.get() > 1) {
                    LOGGER.debug(
                            "{0} There are now multiple members of cluster '{1}'; changes will be propagated throughout the cluster",
                            channel.getAddress(), clusterName());
                } else if (membersInCluster.get() == 1) {
                    LOGGER.debug("{0} There is only one member of cluster '{1}'; changes will be propagated locally only",
                                 channel.getAddress(), clusterName());
                }
            }
        }
    }

    @SuppressWarnings( "synthetic-access" )
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

    private static class StandaloneClusteringService extends ClusteringService {
        private final String jgroupsConfig;
        
        protected StandaloneClusteringService( String clusterName,
                                               String jgroupsConfig ) {
            super(clusterName);
            this.jgroupsConfig = jgroupsConfig;
            this.channel = null;
        }

        protected StandaloneClusteringService( String clusterName, Channel channel ) {
            super(clusterName);
            this.jgroupsConfig = null;
            this.channel = channel;
        }

        @Override
        protected void init() {
            try {
                if (this.channel == null) {
                    this.channel = newChannel(jgroupsConfig);
                }

                ProtocolStack protocolStack = channel.getProtocolStack();
                Protocol centralLock = protocolStack.findProtocol(CENTRAL_LOCK.class);
                if (centralLock == null) {
                    // add the locking protocol
                    CENTRAL_LOCK lockingProtocol = new CENTRAL_LOCK();
                    // we have to call init because the channel has already been created
                    lockingProtocol.init();
                    protocolStack.addProtocol(lockingProtocol);
                }
                this.lockService = new LockService(this.channel);

                // Add a listener through which we'll know what's going on within the cluster ...
                this.channel.addChannelListener(listener);

                // Set the receiver through which we'll receive all of the changes ...
                this.channel.setReceiver(receiver);

                // Now connect to the cluster ...
                this.channel.connect(clusterName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private JChannel newChannel( String jgroupsConfig ) throws Exception {
            if (StringUtil.isBlank(jgroupsConfig)) {
                return new JChannel();
            }

            ProtocolStackConfigurator configurator = null;
            // check if it points to a file accessible via the class loader
            InputStream stream = ClusteringService.class.getClassLoader().getResourceAsStream(jgroupsConfig);
            if (stream == null) {
                LOGGER.debug("Unable to locate configuration file '{0}' using the clustering service class loader.", jgroupsConfig);
                try {
                    stream = new FileInputStream(jgroupsConfig);
                } catch (FileNotFoundException e) {
                    throw new RepositoryException(ClusteringI18n.missingConfigurationFile.text(jgroupsConfig));
                }                 
            }
            try {
                configurator = XmlConfigurator.getInstance(stream);
            } catch (IOException e) {
                LOGGER.debug(e, "Channel configuration is not a classpath resource");
                // check if the configuration is valid xml content
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
    }

    private static class ForkedClusteringService extends ClusteringService {
        private final static String FORK_CHANNEL_NAME = "modeshape-fork-channel";
        private final static Map<String, List<String>> FORK_STACKS_BY_CHANNEL_NAME = new HashMap<>();
        private final Channel mainChannel;

        protected ForkedClusteringService( Channel mainChannel ) {
            super(mainChannel.getClusterName());
            this.mainChannel = mainChannel;
        }

        @Override
        protected void init() {
            try {
                ProtocolStack stack = mainChannel.getProtocolStack();
                Protocol topProtocol = stack.getTopProtocol();
                String forkStackId = this.clusterName;

                boolean alreadyHasForkProtocol = stack.findProtocol(FORK.class) != null;
                if (!alreadyHasForkProtocol) {
                    // this is workaround for this bug: https://issues.jboss.org/browse/JGRP-1984
                    FORK fork = new FORK();
                    fork.setProtocolStack(stack);
                    stack.insertProtocol(fork, ProtocolStack.ABOVE, topProtocol.getClass());
                }

                // add the fork at the top of the stack to preserve the default configuration
                // and use the name of the cluster as the stack id
                this.channel = new ForkChannel(mainChannel, forkStackId, FORK_CHANNEL_NAME, new CENTRAL_LOCK());

                // always add central lock to the stack
                this.lockService = new LockService(this.channel);

                // Add a listener through which we'll know what's going on within the cluster ...
                this.channel.addChannelListener(listener);

                // Set the receiver through which we'll receive all of the changes ...
                this.channel.setReceiver(receiver);

                // Now connect to the cluster ...
                this.channel.connect(FORK_CHANNEL_NAME);

                // and add the id of the fork only if we added the FORK protocol. Otherwise, the protocol was already there to
                // begin with, so we shouldn't remove it.
                if (!alreadyHasForkProtocol) {
                    String mainChannelName = mainChannel.getName();
                    List<String> existingForksForChannel = FORK_STACKS_BY_CHANNEL_NAME.get(mainChannelName);
                    if (existingForksForChannel == null) {
                        existingForksForChannel = new ArrayList<>();
                        FORK_STACKS_BY_CHANNEL_NAME.put(mainChannelName, existingForksForChannel);
                    }
                    existingForksForChannel.add(forkStackId);
                }

            } catch (RuntimeException rt) {
                throw rt;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized boolean shutdown() {
            if (super.shutdown()) {
                String mainChannelName = mainChannel.getName();
                List<String> forksForChannel = FORK_STACKS_BY_CHANNEL_NAME.get(mainChannelName);
                if (forksForChannel != null) {
                    forksForChannel.remove(clusterName);
                    if (forksForChannel.isEmpty()) {
                        FORK_STACKS_BY_CHANNEL_NAME.remove(mainChannelName);
                        Protocol removed = this.mainChannel.getProtocolStack().removeProtocol(FORK.class);
                        if (removed != null) {
                            LOGGER.debug("FORK protocol removed from original channel stack for channel {0}", mainChannelName);
                        } else {
                            LOGGER.debug("FORK protocol not found in original channel stack for channel {0}", mainChannelName);
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }
}
