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
package org.modeshape.jcr;

import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.ThreadPoolFactory;
import org.modeshape.common.util.ThreadPools;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.BasicPropertyFactory;
import org.modeshape.jcr.value.basic.SimpleNamespaceRegistry;
import org.modeshape.jcr.value.basic.StandardValueFactories;
import org.modeshape.jcr.value.basic.ThreadSafeNamespaceRegistry;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.TransientBinaryStore;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * An ExecutionContext is a representation of the environment or context in which a component or operation is operating. Some
 * components require this context to be passed into individual methods, allowing the context to vary with each method invocation.
 * Other components require the context to be provided before it's used, and will use that context for all its operations (until
 * it is given a different one).
 * <p>
 * ExecutionContext instances are {@link Immutable immutable}, so components may hold onto references to them without concern of
 * those contexts changing. Contexts may be used to create other contexts that vary the environment and/or security context. For
 * example, an ExecutionContext could be used to create another context that references the same {@link #getNamespaceRegistry()
 * namespace registry} but which has a different {@link #getSecurityContext() security context}.
 * </p>
 */
@Immutable
public class ExecutionContext implements ThreadPoolFactory, Cloneable {

    public static final ExecutionContext DEFAULT_CONTEXT = new ExecutionContext();

    private static String sha1( String name ) {
        try {
            byte[] sha1 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, name.getBytes());
            return SecureHash.asHexString(sha1);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

    private final ThreadPoolFactory threadPools;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;
    private final SecurityContext securityContext;
    private final BinaryStore binaryStore;
    /** The unique ID string, which is always generated so that it can be final and not volatile. */
    private final String id = sha1(UUID.randomUUID().toString()).substring(0, 9);
    private final String processId;
    private final Map<String, String> data;

    /**
     * Create an instance of an execution context that uses the {@link AccessController#getContext() current JAAS calling context}
     * , with default implementations for all other components (including default namespaces in the
     * {@link #getNamespaceRegistry() namespace registry}.
     */
    @SuppressWarnings( "synthetic-access" )
    public ExecutionContext() {
        this(new NullSecurityContext(), null, null, null, null, null, null, null);
        initializeDefaultNamespaces(this.getNamespaceRegistry());
        assert securityContext != null;

    }

    /**
     * Create a copy of the supplied execution context.
     * 
     * @param original the original
     * @throws IllegalArgumentException if the original is null
     */
    public ExecutionContext( ExecutionContext original ) {
        CheckArg.isNotNull(original, "original");
        this.securityContext = original.getSecurityContext();
        this.namespaceRegistry = original.getNamespaceRegistry();
        this.valueFactories = original.getValueFactories();
        this.propertyFactory = original.getPropertyFactory();
        this.threadPools = original.getThreadPoolFactory();
        this.data = original.getData();
        this.processId = original.getProcessId();
        this.binaryStore = TransientBinaryStore.get();
    }

    /**
     * Create a copy of the supplied execution context, but use the supplied {@link AccessControlContext} instead.
     * 
     * @param original the original
     * @param securityContext the security context
     * @throws IllegalArgumentException if the original or access control context are is null
     */
    protected ExecutionContext( ExecutionContext original,
                                SecurityContext securityContext ) {
        CheckArg.isNotNull(original, "original");
        CheckArg.isNotNull(securityContext, "securityContext");
        this.securityContext = securityContext;
        this.namespaceRegistry = original.getNamespaceRegistry();
        this.valueFactories = original.getValueFactories();
        this.propertyFactory = original.getPropertyFactory();
        this.threadPools = original.getThreadPoolFactory();
        this.data = original.getData();
        this.processId = original.getProcessId();
        this.binaryStore = original.getBinaryStore();
    }

    /**
     * Create an instance of the execution context by supplying all parameters.
     * 
     * @param securityContext the security context, or null if there is no associated authenticated user
     * @param namespaceRegistry the namespace registry implementation, or null if a thread-safe version of
     *        {@link SimpleNamespaceRegistry} instance should be used
     * @param valueFactories the {@link ValueFactories} implementation, or null if a {@link StandardValueFactories} instance
     *        should be used
     * @param propertyFactory the {@link PropertyFactory} implementation, or null if a {@link BasicPropertyFactory} instance
     *        should be used
     * @param threadPoolFactory the {@link ThreadPoolFactory} implementation, or null if a {@link ThreadPools} instance should be
     *        used
     * @param binaryStore the {@link BinaryStore} implementation, or null if a default {@link TransientBinaryStore} should be used
     * @param data the custom data for this context, or null if there is no such data
     * @param processId the unique identifier of the process in which this context exists, or null if it should be generated
     */
    protected ExecutionContext( SecurityContext securityContext,
                                NamespaceRegistry namespaceRegistry,
                                ValueFactories valueFactories,
                                PropertyFactory propertyFactory,
                                ThreadPoolFactory threadPoolFactory,
                                BinaryStore binaryStore,
                                Map<String, String> data,
                                String processId ) {
        assert securityContext != null;
        this.securityContext = securityContext;
        if (binaryStore == null) binaryStore = TransientBinaryStore.get();
        this.binaryStore = binaryStore;
        this.namespaceRegistry = namespaceRegistry != null ? namespaceRegistry : new ThreadSafeNamespaceRegistry(
                                                                                                                 new SimpleNamespaceRegistry());
        this.valueFactories = valueFactories == null ? new StandardValueFactories(this.namespaceRegistry, binaryStore) : valueFactories;
        this.propertyFactory = propertyFactory == null ? new BasicPropertyFactory(this.valueFactories) : propertyFactory;
        this.threadPools = threadPoolFactory == null ? new ThreadPools() : threadPoolFactory;
        this.data = data != null ? data : Collections.<String, String>emptyMap();
        this.processId = processId != null ? processId : UUID.randomUUID().toString();
    }

    protected ThreadPoolFactory getThreadPoolFactory() {
        return threadPools;
    }

    /**
     * Return a logger associated with this context. This logger records only those activities within the context and provide a
     * way to capture the context-specific activities. All log messages are also sent to the system logger, so classes that log
     * via this mechanism should <i>not</i> also {@link Logger#getLogger(Class) obtain a system logger}.
     * 
     * @param clazz the class that is doing the logging
     * @return the logger, named after <code>clazz</code>; never null
     * @see #getLogger(String)
     */
    public Logger getLogger( Class<?> clazz ) {
        return Logger.getLogger(clazz);
    }

    /**
     * Return a logger associated with this context. This logger records only those activities within the context and provide a
     * way to capture the context-specific activities. All log messages are also sent to the system logger, so classes that log
     * via this mechanism should <i>not</i> also {@link Logger#getLogger(Class) obtain a system logger}.
     * 
     * @param name the name for the logger
     * @return the logger, named after <code>clazz</code>; never null
     * @see #getLogger(Class)
     */
    public Logger getLogger( String name ) {
        return Logger.getLogger(name);
    }

    /**
     * Get the {@link SecurityContext security context} for this context.
     * 
     * @return the security context; never <code>null</code>
     */
    public SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    /**
     * Get the (mutable) namespace registry for this context.
     * 
     * @return the namespace registry; never <code>null</code>
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return this.namespaceRegistry;
    }

    /**
     * Get the factory for creating {@link Property} objects.
     * 
     * @return the property factory; never <code>null</code>
     */
    public final PropertyFactory getPropertyFactory() {
        return this.propertyFactory;
    }

    /**
     * Get the factories that should be used to create values for {@link Property properties}.
     * 
     * @return the property value factory; never null
     */
    public ValueFactories getValueFactories() {
        return this.valueFactories;
    }

    /**
     * Get the binary store that should be used to store binary values. This store is used by the {@link BinaryFactory} in the
     * {@link #getValueFactories()}.
     * 
     * @return the binary store; never null
     */
    public BinaryStore getBinaryStore() {
        return binaryStore;
    }

    @Override
    public ExecutorService getThreadPool( String name ) {
        return this.threadPools.getThreadPool(name);
    }

    @Override
    public void releaseThreadPool( ExecutorService pool ) {
        this.threadPools.releaseThreadPool(pool);
    }

    public ExecutorService getCachedTreadPool( String name ) {
        return this.threadPools.getCachedTreadPool(name);
    }

    public ExecutorService getScheduledThreadPool( String name ) {
        return this.threadPools.getScheduledThreadPool(name);
    }

    @Override
    public void terminateAllPools( long maxWaitTimeMillis ) {
        this.threadPools.terminateAllPools(maxWaitTimeMillis);
    }

    /**
     * Get the unique identifier for this context. Each context will always have a unique identifier.
     * 
     * @return the unique identifier string; never null and never empty
     */
    public String getId() {
        return id;
    }

    /**
     * Get the identifier for the process in which this context exists. Multiple contexts running in the same "process" will all
     * have the same identifier.
     * 
     * @return the identifier for the process; never null and never empty
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * Get the immutable map of custom data that is affiliated with this context.
     * 
     * @return the custom data; never null but possibly empty
     */
    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied binary store.
     * 
     * @param binaryStore the binary store that should be used, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied binary store;
     *         never null
     */
    public ExecutionContext with( BinaryStore binaryStore ) {
        if (binaryStore == null) binaryStore = TransientBinaryStore.get();
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), valueFactories, getPropertyFactory(),
                                    getThreadPoolFactory(), binaryStore, getData(), getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied namespace registry. The resulting
     * context's {@link #getValueFactories() value factories} and {@link #getPropertyFactory() property factory} all make use of
     * the new namespace registry.
     * 
     * @param namespaceRegistry the new namespace registry implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied registry; never
     *         null
     */
    public ExecutionContext with( NamespaceRegistry namespaceRegistry ) {
        // Don't supply the value factories or property factories, since they'll have to be recreated
        // to reference the supplied namespace registry ...
        return new ExecutionContext(getSecurityContext(), namespaceRegistry, null, getPropertyFactory(),
                                    getThreadPoolFactory(), getBinaryStore(), getData(), getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied {@link ThreadPoolFactory thread pool
     * factory}.
     * 
     * @param threadPoolFactory the new thread pool factory implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied thread pool
     *         factory implementation; never null
     */
    public ExecutionContext with( ThreadPoolFactory threadPoolFactory ) {
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    threadPoolFactory, getBinaryStore(), getData(), getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied {@link PropertyFactory factory}.
     * 
     * @param propertyFactory the new propertyfactory implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied property factory
     *         implementation; never null
     */
    public ExecutionContext with( PropertyFactory propertyFactory ) {
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), getValueFactories(), propertyFactory,
                                    getThreadPoolFactory(), getBinaryStore(), getData(), getProcessId());
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied {@link SecurityContext
     * security context}.
     * 
     * @param securityContext the new security context to use; may be null
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws IllegalArgumentException if the <code>name</code> is null
     */
    public ExecutionContext with( SecurityContext securityContext ) {
        return new ExecutionContext(this, securityContext);
    }

    /**
     * Create a new execution context that mirrors this context but that contains the supplied data. Note that the supplied map is
     * always copied to ensure that it is immutable.
     * 
     * @param data the data that is to be affiliated with the resulting context or null if the resulting context should have no
     *        data
     * @return the execution context that is identical with this execution context, but which uses the supplied data; never null
     * @since 2.0
     */
    public ExecutionContext with( Map<String, String> data ) {
        Map<String, String> newData = data;
        if (data == null) {
            if (this.data.isEmpty()) return this;
        } else {
            // Copy the data in the map ...
            newData = Collections.unmodifiableMap(new HashMap<String, String>(data));
        }
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    getThreadPoolFactory(), getBinaryStore(), newData, getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that contains the supplied key-value pair in the new context's
     * data.
     * 
     * @param key the key for the new data that is to be affiliated with the resulting context
     * @param value the data value to be affiliated with the supplied key in the resulting context, or null if an existing data
     *        affiliated with the key should be removed in the resulting context
     * @return the execution context that is identical with this execution context, but which uses the supplied data; never null
     * @since 2.0
     */
    public ExecutionContext with( String key,
                                  String value ) {
        Map<String, String> newData = data;
        if (value == null) {
            // Remove the value with the key ...
            if (this.data.isEmpty() || !this.data.containsKey(key)) {
                // nothing to remove
                return this;
            }
            newData = new HashMap<String, String>(data);
            newData.remove(key);
            newData = Collections.unmodifiableMap(newData);
        } else {
            // We are to add the value ...
            newData = new HashMap<String, String>(data);
            newData.put(key, value);
            newData = Collections.unmodifiableMap(newData);
        }
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    getThreadPoolFactory(), getBinaryStore(), newData, getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that contains the supplied process identifier.
     * 
     * @param processId the identifier of the process
     * @return the execution context that is identical with this execution context, but which uses the supplied process
     *         identifier; never null
     * @since 2.1
     */
    public ExecutionContext with( String processId ) {
        return new ExecutionContext(getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    getThreadPoolFactory(), getBinaryStore(), getData(), processId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public ExecutionContext clone() {
        return new ExecutionContext(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Execution context for ");
        if (getSecurityContext() == null) sb.append("null");
        else sb.append(getSecurityContext().getUserName());
        sb.append(" (").append(id).append(')');
        return sb.toString();
    }

    /**
     * Method that initializes the default namespaces for namespace registries.
     * 
     * @param namespaceRegistry the namespace registry
     */
    protected void initializeDefaultNamespaces( NamespaceRegistry namespaceRegistry ) {
        if (namespaceRegistry == null) return;
        namespaceRegistry.register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        namespaceRegistry.register(JcrMixLexicon.Namespace.PREFIX, JcrMixLexicon.Namespace.URI);
        namespaceRegistry.register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
        namespaceRegistry.register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
    }

    /**
     * Default security context that confers no roles.
     */
    private static class NullSecurityContext implements SecurityContext {

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public String getUserName() {
            return null;
        }

        @Override
        public boolean hasRole( String roleName ) {
            return false;
        }

        @Override
        public void logout() {
        }

    }
}
