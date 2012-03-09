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

package org.modeshape.jcr.bus;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.util.Util;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of a {@link ChangeBus} which can run in a cluster, via JGroups. This bus wraps around another bus, to which
 * it delegates all "local" processing of events.
 *
 * @author Horia Chiorean
 */
@ThreadSafe
final class ClusteredRepositoryChangeBus implements ChangeBus {

    private static final Logger LOGGER = Logger.getLogger(ClusteredRepositoryChangeBus.class);

    /**
     * The wrapped standalone bus to which standard bus operations are delegated
     */
    private final ChangeBus delegate;

    /**
     * The listener for channel changes.
     */
    private final Listener listener = new Listener();

    /**
     * The component that will receive the JGroups messages and broadcast them to this bus' observers.
     */
    private final Receiver receiver = new Receiver();

    /**
     * Flag that dictates whether this bus has connected to the cluster.
     */
    private final AtomicBoolean isOpen = new AtomicBoolean(false);

    /**
     * Flag that dictates whether there are multiple participants in the cluster; if not, then the changes are propagated only to
     * the local observers.
     */
    private final AtomicBoolean multipleAddressesInCluster = new AtomicBoolean(false);

    /**
     * The name of the JGroups cluster.
     */
    private final String clusterName;

    /**
     * The configuration for JGroups
     */
    private String configuration;

    /**
     * The JGroups channel to which all {@link #notify(ChangeSet) change notifications} will be sent and from which all changes will
     * be received and sent to the observers.
     * <p>
     * It is important that the order of the {@link ChangeSet} instances are maintained across the cluster, and JGroups will do this
     * for us as long as we push all local changes into the channel and receive all local/remote changes from the channel.
     * </p>
     */
    private JChannel channel;

    ClusteredRepositoryChangeBus( String clusterName,
                                  ChangeBus delegate ) {
        CheckArg.isNotNull(clusterName, "clusterName");
        CheckArg.isNotNull(delegate, "delegate");

        this.clusterName = clusterName;
        this.delegate = delegate;
    }

    ClusteredRepositoryChangeBus( String clusterName,
                                  String configuration,
                                  RepositoryChangeBus delegate ) {
        this(clusterName, delegate);

        CheckArg.isNotNull(configuration, "configuration");
        this.configuration = configuration;
    }

    /**
     * Get the configuration for JGroups. This configuration may be a string that refers to a resource on the classpath, the path
     * of the local configuration, the URL to the configuration file, or the configuration specified using the newer-style XML
     * form or older-style string form.
     *
     * @return the location of the JGroups configuration, or null if no configuration has been defined
     */
    public String getConfiguration() {
        return configuration;
    }

    /**
     * Get the name of the JGroups cluster.
     *
     * @return the cluster name, or null if the cluster name was not yet defined
     * @see JChannel#connect(String)
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jcr.bus.ChangeBus#start()
     */
    @Override
    public synchronized void start() {
        if (clusterName == null) {
            throw new IllegalStateException(BusI18n.clusterNameRequired.text());
        }
        if (channel != null) {
            // Disconnect from any previous channel ...
            channel.removeChannelListener(listener);
            channel.setReceiver(null);
        }
        try {
            // Create the new channel by calling the delegate method ...
            channel = newChannel(configuration);
            assert channel != null;
            // Add a listener through which we'll know what's going on within the cluster ...
            channel.addChannelListener(listener);

            // Set the receiver through which we'll receive all of the changes ...
            channel.setReceiver(receiver);

            // Now connect to the cluster ...
            channel.connect(clusterName);

            //start the delegate
            delegate.start();
        } catch (Exception e) {
            throw new IllegalStateException(BusI18n.errorWhileStartingJGroups.text(configuration), e);
        }
    }

    /**
     * A method that is used to instantiate the {@link JChannel} object with the supplied configuration.
     *
     * @param configuration the configuration; may be null or empty string, if the default configuration should be used
     * @return the JChannel instance; never null
     * @throws Exception if there is a problem creating the new channel object
     */
    private JChannel newChannel( String configuration ) throws Exception {
        if (configuration == null || configuration.trim().length() == 0) {
            return new JChannel();
        }
        // Try the XML configuration first ...
        ProtocolStackConfigurator configurator = null;
        InputStream stream = new ByteArrayInputStream(configuration.getBytes());
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
        return new JChannel(configuration);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jcr.bus.ChangeBus#hasObservers()
     */
    @Override
    public boolean hasObservers() {
        return delegate.hasObservers();
    }

    /**
     * Return whether this bus has been {@link #start() started} and not yet {@link #shutdown() shut down}.
     *
     * @return true if {@link #start()} has been called but {@link #shutdown()} has not, or false otherwise
     */
    public boolean isStarted() {
        return channel != null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jcr.bus.ChangeBus#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        if (channel != null) {
            // Mark this as not accepting any more ...
            isOpen.set(false);
            try {
                // Disconnect from the channel and close it ...
                channel.removeChannelListener(listener);
                channel.setReceiver(null);
                channel.close();
            } finally {
                channel = null;
                // Now that we're not receiving any more messages, shut down the delegate
                delegate.shutdown();
            }
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null) {
            return; // do nothing
        }
        if (!isOpen.get()) {
            // The channel is not open ...
            return;
        }
        if (!multipleAddressesInCluster.get()) {
            // We are in clustered mode, but there is only one participant in the cluster (us).
            // So short-circuit the cluster and just notify the local observers ...
            if (hasObservers()) {
                delegate.notify(changeSet);
                logReceivedOperation(changeSet);

            }
            return;
        }

        // There are multiple participants in the cluster, so send all changes out to JGroups,
        // letting JGroups do the ordering of messages...
        try {
            logSendOperation(changeSet);
            byte[] data = serialize(changeSet);
            Message message = new Message(null, null, data);
            channel.send(message);
        } catch (IllegalStateException e) {
            LOGGER.warn(BusI18n.unableToNotifyChanges,
                        clusterName,
                        changeSet.size(),
                        changeSet.getWorkspaceName(),
                        changeSet.getUserId(),
                        changeSet.getProcessKey(),
                        changeSet.getTimestamp());
        } catch (Exception e) {
            // Something went wrong here (this should not happen) ...
            String msg = BusI18n.errorSerializingChanges.text(clusterName,
                                                              changeSet.size(),
                                                              changeSet.getWorkspaceName(),
                                                              changeSet.getUserId(),
                                                              changeSet.getProcessKey(),
                                                              changeSet.getTimestamp(),
                                                              changeSet);
            throw new SystemFailureException(msg, e);
        }
    }

    private void logSendOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending to cluster '{0}' {1} changes on workspace {2} made by {3} from process '{4}' at {5}",
                         clusterName,
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserData(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());
        }
    }

    private void logReceivedOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Received on cluster '{0}' {1} changes on workspace {2} made by {3} from process '{4}' at {5}",
                         getClusterName(),
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserId(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());

        }
    }

    @Override
    public boolean register( ChangeSetListener observer ) {
        return delegate.register(observer);
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        return delegate.unregister(observer);
    }

    protected static byte[] serialize( ChangeSet changes ) throws Exception {
        return Util.objectToByteBuffer(changes);
    }

    protected static ChangeSet deserialize( byte[] data ) throws Exception {
        return (ChangeSet)Util.objectFromByteBuffer(data);
    }

    private class Receiver extends ReceiverAdapter {

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.MembershipListener#block()
         */
        @Override
        public void block() {
            isOpen.set(false);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.MessageListener#receive(org.jgroups.Message)
         */
        @Override
        public void receive( Message message ) {
            if (!hasObservers()) {
                return;
            }
            // We have at least one observer ...
            try {
                // Deserialize the changes ...
                ChangeSet changes = deserialize(message.getBuffer());
                // and broadcast them
                delegate.notify(changes);
                logReceivedOperation(changes);
            } catch (Exception e) {
                // Something went wrong here (this should not happen) ...
                String msg = BusI18n.errorDeserializingChanges.text(getClusterName());
                throw new SystemFailureException(msg, e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.MembershipListener#suspect(org.jgroups.Address)
         */
        @Override
        public void suspect( Address suspectedMbr ) {
            LOGGER.error(BusI18n.memberOfClusterIsSuspect, getClusterName(), suspectedMbr);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.MembershipListener#viewAccepted(org.jgroups.View)
         */
        @Override
        public void viewAccepted( View newView ) {
            LOGGER.trace("Members of '{0}' cluster have changed: {1}", getClusterName(), newView);
            if (newView.getMembers().size() > 1) {
                if (multipleAddressesInCluster.compareAndSet(false, true)) {
                    LOGGER.debug(
                            "There are now multiple members of cluster '{0}'; changes will be propagated throughout the cluster",
                            getClusterName());
                }
            } else {
                if (multipleAddressesInCluster.compareAndSet(true, false)) {
                    LOGGER.debug("There is only one member of cluster '{0}'; changes will be propagated locally only",
                                 getClusterName());
                }
            }
        }
    }

    private class Listener implements ChannelListener {
        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.ChannelListener#channelClosed(org.jgroups.Channel)
         */
        @Override
        public void channelClosed( Channel channel ) {
            isOpen.set(false);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.ChannelListener#channelConnected(org.jgroups.Channel)
         */
        @Override
        public void channelConnected( Channel channel ) {
            isOpen.set(true);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.jgroups.ChannelListener#channelDisconnected(org.jgroups.Channel)
         */
        @Override
        public void channelDisconnected( Channel channel ) {
            isOpen.set(false);
        }
    }
}
