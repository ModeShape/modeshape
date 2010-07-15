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
package org.modeshape.graph;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.component.StandardClassLoaderFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.mimetype.MimeTypeDetectors;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.basic.BasicPropertyFactory;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;
import org.modeshape.graph.property.basic.StandardValueFactories;
import org.modeshape.graph.property.basic.ThreadSafeNamespaceRegistry;

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
public class ExecutionContext implements ClassLoaderFactory, Cloneable {

    public static final ExecutionContext DEFAULT_CONTEXT = new ExecutionContext();

    private final ClassLoaderFactory classLoaderFactory;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;
    private final MimeTypeDetector mimeTypeDetector;
    private final SecurityContext securityContext;
    /** The unique ID string, which is always generated so that it can be final and not volatile. */
    private final String id = UUID.randomUUID().toString();
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
    protected ExecutionContext( ExecutionContext original ) {
        CheckArg.isNotNull(original, "original");
        this.securityContext = original.getSecurityContext();
        this.namespaceRegistry = original.getNamespaceRegistry();
        this.valueFactories = original.getValueFactories();
        this.propertyFactory = original.getPropertyFactory();
        this.classLoaderFactory = original.getClassLoaderFactory();
        this.mimeTypeDetector = original.getMimeTypeDetector();
        this.data = original.getData();
        this.processId = original.getProcessId();
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
        this.classLoaderFactory = original.getClassLoaderFactory();
        this.mimeTypeDetector = original.getMimeTypeDetector();
        this.data = original.getData();
        this.processId = original.getProcessId();
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
     * @param mimeTypeDetector the {@link MimeTypeDetector} implementation, or null if the context should use a
     *        {@link MimeTypeDetectors} instance with an {@link ExtensionBasedMimeTypeDetector}
     * @param classLoaderFactory the {@link ClassLoaderFactory} implementation, or null if a {@link StandardClassLoaderFactory}
     *        instance should be used
     * @param data the custom data for this context, or null if there is no such data
     * @param processId the unique identifier of the process in which this context exists, or null if it should be generated
     */
    protected ExecutionContext( SecurityContext securityContext,
                                NamespaceRegistry namespaceRegistry,
                                ValueFactories valueFactories,
                                PropertyFactory propertyFactory,
                                MimeTypeDetector mimeTypeDetector,
                                ClassLoaderFactory classLoaderFactory,
                                Map<String, String> data,
                                String processId ) {
        assert securityContext != null;
        this.securityContext = securityContext;
        this.namespaceRegistry = namespaceRegistry != null ? namespaceRegistry : new ThreadSafeNamespaceRegistry(
                                                                                                                 new SimpleNamespaceRegistry());
        this.valueFactories = valueFactories == null ? new StandardValueFactories(this.namespaceRegistry) : valueFactories;
        this.propertyFactory = propertyFactory == null ? new BasicPropertyFactory(this.valueFactories) : propertyFactory;
        this.classLoaderFactory = classLoaderFactory == null ? new StandardClassLoaderFactory() : classLoaderFactory;
        this.mimeTypeDetector = mimeTypeDetector != null ? mimeTypeDetector : createDefaultMimeTypeDetector();
        this.data = data != null ? data : Collections.<String, String>emptyMap();
        this.processId = processId != null ? processId : UUID.randomUUID().toString();
    }

    private MimeTypeDetector createDefaultMimeTypeDetector() {
        MimeTypeDetectors detectors = new MimeTypeDetectors();
        detectors.addDetector(ExtensionBasedMimeTypeDetector.CONFIGURATION);
        return detectors;
    }

    /**
     * Get the class loader factory used by this context.
     * 
     * @return the class loader factory implementation; never null
     */
    protected ClassLoaderFactory getClassLoaderFactory() {
        return classLoaderFactory;
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
     * Return an object that can be used to determine the MIME type of some content, such as the content of a file.
     * 
     * @return the detector; never null
     */
    public MimeTypeDetector getMimeTypeDetector() {
        return this.mimeTypeDetector;
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
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.component.ClassLoaderFactory#getClassLoader(java.lang.String[])
     */
    public ClassLoader getClassLoader( String... classpath ) {
        return this.classLoaderFactory.getClassLoader(classpath);
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
        return data;
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
        return new ExecutionContext(this.getSecurityContext(), namespaceRegistry, null, null, this.getMimeTypeDetector(),
                                    this.getClassLoaderFactory(), this.getData(), getProcessId());
    }

    /**
     * Create a new execution context that is the same as this context, but which uses the supplied {@link MimeTypeDetector MIME
     * type detector}.
     * 
     * @param mimeTypeDetector the new MIME type detector implementation, or null if the context should use a
     *        {@link MimeTypeDetectors} instance with an {@link ExtensionBasedMimeTypeDetector}
     * @return the execution context that is identical with this execution context, but which uses the supplied detector
     *         implementation; never null
     */
    public ExecutionContext with( MimeTypeDetector mimeTypeDetector ) {
        // Don't supply the value factories or property factories, since they'll have to be recreated
        // to reference the supplied namespace registry ...
        return new ExecutionContext(this.getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    mimeTypeDetector, getClassLoaderFactory(), this.getData(), getProcessId());
    }

    /**
     * Create a new execution context that mirrors this context but that uses the supplied {@link ClassLoaderFactory class loader
     * factory}.
     * 
     * @param classLoaderFactory the new class loader factory implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied class loader
     *         factory implementation; never null
     */
    public ExecutionContext with( ClassLoaderFactory classLoaderFactory ) {
        // Don't supply the value factories or property factories, since they'll have to be recreated
        // to reference the supplied namespace registry ...
        return new ExecutionContext(this.getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    getMimeTypeDetector(), classLoaderFactory, this.getData(), getProcessId());
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
        return new ExecutionContext(this.getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    getMimeTypeDetector(), getClassLoaderFactory(), newData, getProcessId());
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
                                    getMimeTypeDetector(), getClassLoaderFactory(), newData, getProcessId());
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
                                    getMimeTypeDetector(), getClassLoaderFactory(), getData(), processId);
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
        namespaceRegistry.register(ModeShapeIntLexicon.Namespace.PREFIX, ModeShapeIntLexicon.Namespace.URI);
        // namespaceRegistry.register("dnadtd", "http://www.modeshape.org/dtd/1.0");
        // namespaceRegistry.register("dnaxml", "http://www.modeshape.org/xml/1.0");
    }

    /**
     * Default security context that confers no roles.
     */
    private static class NullSecurityContext implements SecurityContext {

        public String getUserName() {
            return null;
        }

        public boolean hasRole( String roleName ) {
            return false;
        }

        public void logout() {
        }

    }
}
