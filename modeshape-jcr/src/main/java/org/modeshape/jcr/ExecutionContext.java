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

import java.math.BigDecimal;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.ThreadPoolFactory;
import org.modeshape.common.util.ThreadPools;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.UriFactory;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueTypeSystem;
import org.modeshape.jcr.value.basic.BasicPropertyFactory;
import org.modeshape.jcr.value.basic.BooleanValueFactory;
import org.modeshape.jcr.value.basic.DecimalValueFactory;
import org.modeshape.jcr.value.basic.DoubleValueFactory;
import org.modeshape.jcr.value.basic.JodaDateTimeValueFactory;
import org.modeshape.jcr.value.basic.LongValueFactory;
import org.modeshape.jcr.value.basic.NameValueFactory;
import org.modeshape.jcr.value.basic.ObjectValueFactory;
import org.modeshape.jcr.value.basic.PathValueFactory;
import org.modeshape.jcr.value.basic.ReferenceValueFactory;
import org.modeshape.jcr.value.basic.SimpleNamespaceRegistry;
import org.modeshape.jcr.value.basic.StringValueFactory;
import org.modeshape.jcr.value.basic.ThreadSafeNamespaceRegistry;
import org.modeshape.jcr.value.basic.UriValueFactory;
import org.modeshape.jcr.value.basic.UuidValueFactory;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreValueFactory;
import org.modeshape.jcr.value.binary.TransientBinaryStore;

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
public final class ExecutionContext implements ThreadPoolFactory, Cloneable, NamespaceRegistry.Holder {

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

    // This class is implemented with separate members for each factory so that the typical usage is optimized.
    private final TextDecoder decoder;
    private final TextEncoder encoder;
    private final StringFactory stringFactory;
    private final BinaryFactory binaryFactory;
    private final ValueFactory<Boolean> booleanFactory;
    private final DateTimeFactory dateFactory;
    private final ValueFactory<BigDecimal> decimalFactory;
    private final ValueFactory<Double> doubleFactory;
    private final ValueFactory<Long> longFactory;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final ReferenceFactory referenceFactory;
    private final ReferenceFactory weakReferenceFactory;
    private final UriFactory uriFactory;
    private final UuidFactory uuidFactory;
    private final ValueFactory<Object> objectFactory;
    private final TypeSystem typeSystem;

    /**
     * Create an instance of an execution context that uses the {@link AccessController#getContext() current JAAS calling context}
     * , with default implementations for all other components (including default namespaces in the
     * {@link #getNamespaceRegistry() namespace registry}.
     */
    @SuppressWarnings( "synthetic-access" )
    public ExecutionContext() {
        this(new NullSecurityContext(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
             null, null, null, null, null, null, null);
        initializeDefaultNamespaces(this.getNamespaceRegistry());
        assert securityContext != null;
    }

    protected ExecutionContext( ExecutionContext context ) {
        this(context.securityContext, context.namespaceRegistry, context.propertyFactory, context.threadPools,
             context.binaryStore, context.data, context.processId, context.decoder, context.encoder, context.stringFactory,
             context.binaryFactory, context.booleanFactory, context.dateFactory, context.decimalFactory, context.doubleFactory,
             context.longFactory, context.nameFactory, context.pathFactory, context.referenceFactory,
             context.weakReferenceFactory, context.uriFactory, context.uuidFactory, context.objectFactory);
    }

    /**
     * Create an instance of the execution context by supplying all parameters.
     * 
     * @param securityContext the security context, or null if there is no associated authenticated user
     * @param namespaceRegistry the namespace registry implementation, or null if a thread-safe version of
     *        {@link SimpleNamespaceRegistry} instance should be used
     * @param propertyFactory the {@link PropertyFactory} implementation, or null if a {@link BasicPropertyFactory} instance
     *        should be used
     * @param threadPoolFactory the {@link ThreadPoolFactory} implementation, or null if a {@link ThreadPools} instance should be
     *        used
     * @param binaryStore the {@link BinaryStore} implementation, or null if a default {@link TransientBinaryStore} should be used
     * @param data the custom data for this context, or null if there is no such data
     * @param processId the unique identifier of the process in which this context exists, or null if it should be generated
     * @param decoder the decoder that should be used; if null, the {@link ValueFactory#DEFAULT_DECODER default decoder} is used.
     * @param encoder the encoder that should be used; if null, the {@link ValueFactory#DEFAULT_ENCODER default encoder} is used.
     * @param stringFactory the string factory that should be used; if null, a default implementation will be used
     * @param binaryFactory the binary factory that should be used; if null, a default implementation will be used
     * @param booleanFactory the boolean factory that should be used; if null, a default implementation will be used
     * @param dateFactory the date factory that should be used; if null, a default implementation will be used
     * @param decimalFactory the decimal factory that should be used; if null, a default implementation will be used
     * @param doubleFactory the double factory that should be used; if null, a default implementation will be used
     * @param longFactory the long factory that should be used; if null, a default implementation will be used
     * @param nameFactory the name factory that should be used; if null, a default implementation will be used
     * @param pathFactory the path factory that should be used; if null, a default implementation will be used
     * @param referenceFactory the strong reference factory that should be used; if null, a default implementation will be used
     * @param weakReferenceFactory the weak reference factory that should be used; if null, a default implementation will be used
     * @param uriFactory the URI factory that should be used; if null, a default implementation will be used
     * @param uuidFactory the UUID factory that should be used; if null, a default implementation will be used
     * @param objectFactory the object factory that should be used; if null, a default implementation will be used
     */
    protected ExecutionContext( SecurityContext securityContext,
                                NamespaceRegistry namespaceRegistry,
                                PropertyFactory propertyFactory,
                                ThreadPoolFactory threadPoolFactory,
                                BinaryStore binaryStore,
                                Map<String, String> data,
                                String processId,
                                TextDecoder decoder,
                                TextEncoder encoder,
                                StringFactory stringFactory,
                                BinaryFactory binaryFactory,
                                ValueFactory<Boolean> booleanFactory,
                                DateTimeFactory dateFactory,
                                ValueFactory<BigDecimal> decimalFactory,
                                ValueFactory<Double> doubleFactory,
                                ValueFactory<Long> longFactory,
                                NameFactory nameFactory,
                                PathFactory pathFactory,
                                ReferenceFactory referenceFactory,
                                ReferenceFactory weakReferenceFactory,
                                UriFactory uriFactory,
                                UuidFactory uuidFactory,
                                ValueFactory<Object> objectFactory ) {
        assert securityContext != null;
        this.securityContext = securityContext;

        if (binaryStore == null) binaryStore = TransientBinaryStore.get();
        if (namespaceRegistry == null) namespaceRegistry = new ThreadSafeNamespaceRegistry(new SimpleNamespaceRegistry());
        if (threadPoolFactory == null) threadPoolFactory = new ThreadPools();
        if (data == null) data = Collections.<String, String>emptyMap();
        if (processId == null) processId = UUID.randomUUID().toString();
        if (decoder == null) decoder = ValueFactory.DEFAULT_DECODER;
        if (encoder == null) encoder = ValueFactory.DEFAULT_ENCODER;

        // First assign the non-factory members ...
        this.binaryStore = binaryStore;
        this.namespaceRegistry = namespaceRegistry;
        this.threadPools = threadPoolFactory;
        this.data = data;
        this.processId = processId;
        this.decoder = decoder;
        this.encoder = encoder;
        this.valueFactories = new ContextFactories();

        // Create default factories if needed, or obtain new instances that use our ValueFactories (and
        // NamespaceRegistry.Holder) instances ...
        if (stringFactory == null) {
            stringFactory = new StringValueFactory(this, decoder, encoder);
        } else {
            stringFactory = stringFactory.with(valueFactories).with(this);
        }
        if (binaryFactory == null) {
            // The binary factory should NOT use the string factory that converts namespaces to prefixes ...
            StringValueFactory stringFactoryWithoutNamespaces = new StringValueFactory(decoder, encoder);
            binaryFactory = new BinaryStoreValueFactory(this.binaryStore, decoder, valueFactories, stringFactoryWithoutNamespaces);
        } else {
            binaryFactory = binaryFactory.with(binaryStore).with(valueFactories);
        }
        if (booleanFactory == null) {
            booleanFactory = new BooleanValueFactory(decoder, valueFactories);
        } else {
            booleanFactory = booleanFactory.with(valueFactories);
        }
        if (dateFactory == null) {
            dateFactory = new JodaDateTimeValueFactory(decoder, valueFactories);
        } else {
            dateFactory = dateFactory.with(valueFactories);
        }
        if (decimalFactory == null) {
            decimalFactory = new DecimalValueFactory(decoder, valueFactories);
        } else {
            decimalFactory = decimalFactory.with(valueFactories);
        }
        if (doubleFactory == null) {
            doubleFactory = new DoubleValueFactory(decoder, valueFactories);
        } else {
            doubleFactory = doubleFactory.with(valueFactories);
        }
        if (longFactory == null) {
            longFactory = new LongValueFactory(decoder, valueFactories);
        } else {
            longFactory = longFactory.with(valueFactories);
        }
        if (nameFactory == null) {
            nameFactory = new NameValueFactory(this, decoder, valueFactories);
        } else {
            nameFactory = nameFactory.with(valueFactories).with(this);
        }
        if (pathFactory == null) {
            pathFactory = new PathValueFactory(decoder, valueFactories);
        } else {
            pathFactory = pathFactory.with(valueFactories);
        }
        if (referenceFactory == null) {
            referenceFactory = new ReferenceValueFactory(decoder, valueFactories, false);
        } else {
            referenceFactory = referenceFactory.with(valueFactories);
        }
        if (weakReferenceFactory == null) {
            weakReferenceFactory = new ReferenceValueFactory(decoder, valueFactories, true);
        } else {
            weakReferenceFactory = weakReferenceFactory.with(valueFactories);
        }
        if (uuidFactory == null) {
            uuidFactory = new UuidValueFactory(decoder, valueFactories);
        } else {
            uuidFactory = uuidFactory.with(valueFactories);
        }
        if (uriFactory == null) {
            uriFactory = new UriValueFactory(this, decoder, valueFactories);
        } else {
            uriFactory = uriFactory.with(valueFactories).with(this);
        }
        if (objectFactory == null) {
            objectFactory = new ObjectValueFactory(decoder, valueFactories);
        } else {
            objectFactory = objectFactory.with(valueFactories);
        }

        // Now assign the factory members ...
        this.stringFactory = stringFactory;
        this.binaryFactory = binaryFactory;
        this.booleanFactory = booleanFactory;
        this.dateFactory = dateFactory;
        this.decimalFactory = decimalFactory;
        this.doubleFactory = doubleFactory;
        this.longFactory = longFactory;
        this.nameFactory = nameFactory;
        this.pathFactory = pathFactory;
        this.referenceFactory = referenceFactory;
        this.weakReferenceFactory = weakReferenceFactory;
        this.uuidFactory = uuidFactory;
        this.uriFactory = uriFactory;
        this.objectFactory = objectFactory;

        // Assign the things that depend on a ValueFactories implementation ...
        this.typeSystem = new ValueTypeSystem(valueFactories);
        this.propertyFactory = propertyFactory == null ? new BasicPropertyFactory(valueFactories) : propertyFactory;
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
        return securityContext;
    }

    /**
     * Get the (mutable) namespace registry for this context.
     * 
     * @return the namespace registry; never <code>null</code>
     */
    @Override
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * Get the factory for creating {@link Property} objects.
     * 
     * @return the property factory; never <code>null</code>
     */
    public final PropertyFactory getPropertyFactory() {
        return propertyFactory;
    }

    /**
     * Get the factories that should be used to create values for {@link Property properties}.
     * 
     * @return the property value factory; never null
     */
    public ValueFactories getValueFactories() {
        return valueFactories;
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

    @Override
    public ExecutorService getCachedTreadPool( String name ) {
        return this.threadPools.getCachedTreadPool(name);
    }

    @Override
    public ExecutorService getScheduledThreadPool( String name ) {
        return this.threadPools.getScheduledThreadPool(name);
    }

    @Override
    public void terminateAllPools( long maxWaitTime,
                                   TimeUnit timeUnit ) {
        this.threadPools.terminateAllPools(maxWaitTime, timeUnit);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPoolFactory, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied {@link PropertyFactory factory}.
     * 
     * @param propertyFactory the new propertyfactory implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied property factory
     *         implementation; never null
     */
    public ExecutionContext with( PropertyFactory propertyFactory ) {
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        if (newData == null) {
            if (this.data.isEmpty()) return this;
        } else {
            // Copy the data in the map ...
            newData = Collections.unmodifiableMap(new HashMap<String, String>(data));
        }
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, newData,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, newData,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
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
        return new ExecutionContext(securityContext, namespaceRegistry, propertyFactory, threadPools, binaryStore, data,
                                    processId, decoder, encoder, stringFactory, binaryFactory, booleanFactory, dateFactory,
                                    decimalFactory, doubleFactory, longFactory, nameFactory, pathFactory, referenceFactory,
                                    weakReferenceFactory, uriFactory, uuidFactory, objectFactory);
    }

    @Override
    public ExecutionContext clone() {
        return new ExecutionContext(this);
    }

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

    @SuppressWarnings( "synthetic-access" )
    protected final class ContextFactories implements ValueFactories {

        @Override
        public TypeSystem getTypeSystem() {
            return typeSystem;
        }

        /**
         * @return namespaceRegistry
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return namespaceRegistry;
        }

        @Override
        public BinaryFactory getBinaryFactory() {
            return binaryFactory;
        }

        @Override
        public ValueFactory<Boolean> getBooleanFactory() {
            return booleanFactory;
        }

        @Override
        public DateTimeFactory getDateFactory() {
            return dateFactory;
        }

        @Override
        public ValueFactory<BigDecimal> getDecimalFactory() {
            return decimalFactory;
        }

        @Override
        public ValueFactory<Double> getDoubleFactory() {
            return doubleFactory;
        }

        @Override
        public ValueFactory<Long> getLongFactory() {
            return longFactory;
        }

        @Override
        public NameFactory getNameFactory() {
            return nameFactory;
        }

        @Override
        public PathFactory getPathFactory() {
            return pathFactory;
        }

        @Override
        public ReferenceFactory getReferenceFactory() {
            return referenceFactory;
        }

        @Override
        public ReferenceFactory getWeakReferenceFactory() {
            return weakReferenceFactory;
        }

        @Override
        public StringFactory getStringFactory() {
            return stringFactory;
        }

        @Override
        public UriFactory getUriFactory() {
            return uriFactory;
        }

        @Override
        public UuidFactory getUuidFactory() {
            return uuidFactory;
        }

        @Override
        public ValueFactory<Object> getObjectFactory() {
            return objectFactory;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * <p>
         * This implementation always iterates over the instances return by the <code>get*Factory()</code> methods.
         * </p>
         */
        @Override
        public Iterator<ValueFactory<?>> iterator() {
            return new ValueFactoryIterator();
        }

        @Override
        public ValueFactory<?> getValueFactory( PropertyType type ) {
            CheckArg.isNotNull(type, "type");
            switch (type) {
                case BINARY:
                    return getBinaryFactory();
                case BOOLEAN:
                    return getBooleanFactory();
                case DATE:
                    return getDateFactory();
                case DECIMAL:
                    return getDecimalFactory();
                case DOUBLE:
                    return getDoubleFactory();
                case LONG:
                    return getLongFactory();
                case NAME:
                    return getNameFactory();
                case PATH:
                    return getPathFactory();
                case REFERENCE:
                    return getReferenceFactory();
                case WEAKREFERENCE:
                    return getWeakReferenceFactory();
                case STRING:
                    return getStringFactory();
                case URI:
                    return getUriFactory();
                case UUID:
                    return getUuidFactory();
                case OBJECT:
                    return getObjectFactory();
            }
            return getObjectFactory();
        }

        @Override
        public ValueFactory<?> getValueFactory( Object prototype ) {
            CheckArg.isNotNull(prototype, "prototype");
            PropertyType inferredType = PropertyType.discoverType(prototype);
            assert inferredType != null;
            return getValueFactory(inferredType);
        }

        protected class ValueFactoryIterator implements Iterator<ValueFactory<?>> {
            private final Iterator<PropertyType> propertyTypeIter = PropertyType.iterator();

            protected ValueFactoryIterator() {
            }

            @Override
            public boolean hasNext() {
                return propertyTypeIter.hasNext();
            }

            @Override
            public ValueFactory<?> next() {
                PropertyType nextType = propertyTypeIter.next();
                return getValueFactory(nextType);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

    }
}
