/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph;

import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.connector.federation.FederatedLexicon;
import org.jboss.dna.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.basic.BasicPropertyFactory;
import org.jboss.dna.graph.property.basic.SimpleNamespaceRegistry;
import org.jboss.dna.graph.property.basic.StandardValueFactories;
import org.jboss.dna.graph.property.basic.ThreadSafeNamespaceRegistry;

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
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class ExecutionContext implements ClassLoaderFactory, Cloneable {

    private final ClassLoaderFactory classLoaderFactory;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;
    private final MimeTypeDetector mimeTypeDetector;
    private final SecurityContext securityContext;

    /**
     * Create an instance of an execution context that uses the {@link AccessController#getContext() current JAAS calling context}
     * , with default implementations for all other components (including default namespaces in the
     * {@link #getNamespaceRegistry() namespace registry}.
     */
    @SuppressWarnings( "synthetic-access" )
    public ExecutionContext() {
        this(new NullSecurityContext(), null, null, null, null, null);
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
     * @param mimeTypeDetector the {@link MimeTypeDetector} implementation, or null if an {@link ExtensionBasedMimeTypeDetector}
     *        instance should be used
     * @param classLoaderFactory the {@link ClassLoaderFactory} implementation, or null if a {@link StandardClassLoaderFactory}
     *        instance should be used
     */
    protected ExecutionContext( SecurityContext securityContext,
                                NamespaceRegistry namespaceRegistry,
                                ValueFactories valueFactories,
                                PropertyFactory propertyFactory,
                                MimeTypeDetector mimeTypeDetector,
                                ClassLoaderFactory classLoaderFactory ) {
        assert securityContext != null;
        this.securityContext = securityContext;
        this.namespaceRegistry = namespaceRegistry != null ? namespaceRegistry : new ThreadSafeNamespaceRegistry(
                                                                                                                 new SimpleNamespaceRegistry());
        this.valueFactories = valueFactories == null ? new StandardValueFactories(this.namespaceRegistry) : valueFactories;
        this.propertyFactory = propertyFactory == null ? new BasicPropertyFactory(this.valueFactories) : propertyFactory;
        this.classLoaderFactory = classLoaderFactory == null ? new StandardClassLoaderFactory() : classLoaderFactory;
        this.mimeTypeDetector = mimeTypeDetector != null ? mimeTypeDetector : new ExtensionBasedMimeTypeDetector();
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
    public PropertyFactory getPropertyFactory() {
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
     * @see org.jboss.dna.common.component.ClassLoaderFactory#getClassLoader(java.lang.String[])
     */
    public ClassLoader getClassLoader( String... classpath ) {
        return this.classLoaderFactory.getClassLoader(classpath);
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
                                    this.getClassLoaderFactory());
    }

    /**
     * Create a new execution context that is the same as this context, but which uses the supplied {@link MimeTypeDetector MIME
     * type detector}.
     * 
     * @param mimeTypeDetector the new MIME type detector implementation, or null if the default implementation should be used
     * @return the execution context that is identical with this execution context, but which uses the supplied detector
     *         implementation; never null
     */
    public ExecutionContext with( MimeTypeDetector mimeTypeDetector ) {
        // Don't supply the value factories or property factories, since they'll have to be recreated
        // to reference the supplied namespace registry ...
        return new ExecutionContext(this.getSecurityContext(), getNamespaceRegistry(), getValueFactories(), getPropertyFactory(),
                                    mimeTypeDetector, getClassLoaderFactory());
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
                                    getMimeTypeDetector(), classLoaderFactory);
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied {@link SecurityContext
     * security context}.
     * 
     * @param securityContext the new security context to use; may be null
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws IllegalArgumentException if the <code>name</code> is null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         default callback handler JAAS property was not set or could not be loaded
     */
    public ExecutionContext with( SecurityContext securityContext ) throws LoginException {
        return new ExecutionContext(this, securityContext);
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
        return "Execution context for " + getSecurityContext() == null ? "null" : getSecurityContext().getUserName();
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
        namespaceRegistry.register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        namespaceRegistry.register(FederatedLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        // namespaceRegistry.register("dnadtd", "http://www.jboss.org/dna/dtd/1.0");
        // namespaceRegistry.register("dnaxml", "http://www.jboss.org/dna/xml/1.0");
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
