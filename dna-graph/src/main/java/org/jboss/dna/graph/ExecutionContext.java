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

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.Reflection;
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
 * namespace registry} but which has a different {@link #getSubject() JAAS subject}.
 * </p>
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class ExecutionContext implements ClassLoaderFactory, Cloneable {

    private final ClassLoaderFactory classLoaderFactory;
    private final LoginContext loginContext;
    private final AccessControlContext accessControlContext;
    private final Subject subject;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;
    private final MimeTypeDetector mimeTypeDetector;

    /**
     * Create an instance of an execution context that uses the {@link AccessController#getContext() current JAAS calling context}
     * , with default implementations for all other components (including default namespaces in the
     * {@link #getNamespaceRegistry() namespace registry}.
     */
    public ExecutionContext() {
        this(null, null, null, null, null, null, null);
        initializeDefaultNamespaces(this.getNamespaceRegistry());
    }

    /**
     * Create a copy of the supplied execution context.
     * 
     * @param original the original
     * @throws IllegalArgumentException if the original is null
     */
    protected ExecutionContext( ExecutionContext original ) {
        CheckArg.isNotNull(original, "original");
        this.loginContext = original.getLoginContext();
        this.accessControlContext = original.getAccessControlContext();
        this.subject = original.getSubject();
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
     * @param accessControlContext the access control context
     * @throws IllegalArgumentException if the original or access control context are is null
     */
    protected ExecutionContext( ExecutionContext original,
                                AccessControlContext accessControlContext ) {
        CheckArg.isNotNull(original, "original");
        CheckArg.isNotNull(accessControlContext, "accessControlContext");
        this.loginContext = null;
        this.accessControlContext = accessControlContext;
        this.subject = Subject.getSubject(this.accessControlContext);
        this.namespaceRegistry = original.getNamespaceRegistry();
        this.valueFactories = original.getValueFactories();
        this.propertyFactory = original.getPropertyFactory();
        this.classLoaderFactory = original.getClassLoaderFactory();
        this.mimeTypeDetector = original.getMimeTypeDetector();
    }

    /**
     * Create a copy of the supplied execution context, but use the supplied {@link LoginContext} instead.
     * 
     * @param original the original
     * @param loginContext the login context
     * @throws IllegalArgumentException if the original or login context are is null
     */
    protected ExecutionContext( ExecutionContext original,
                                LoginContext loginContext ) {
        CheckArg.isNotNull(original, "original");
        CheckArg.isNotNull(loginContext, "loginContext");
        this.loginContext = loginContext;
        this.accessControlContext = null;
        this.subject = this.loginContext.getSubject();
        this.namespaceRegistry = original.getNamespaceRegistry();
        this.valueFactories = original.getValueFactories();
        this.propertyFactory = original.getPropertyFactory();
        this.classLoaderFactory = original.getClassLoaderFactory();
        this.mimeTypeDetector = original.getMimeTypeDetector();
    }

    /**
     * Create an instance of the execution context by supplying all parameters.
     * 
     * @param loginContext the login context, or null if the {@link #getSubject() subject} is to be retrieved from the
     *        {@link AccessController#getContext() current calling context}.
     * @param accessControlContext the access control context, or null if a {@link LoginContext} is provided or if the
     *        {@link AccessController#getContext() current calling context} should be used
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
    protected ExecutionContext( LoginContext loginContext,
                                AccessControlContext accessControlContext,
                                NamespaceRegistry namespaceRegistry,
                                ValueFactories valueFactories,
                                PropertyFactory propertyFactory,
                                MimeTypeDetector mimeTypeDetector,
                                ClassLoaderFactory classLoaderFactory ) {
        this.loginContext = loginContext;
        this.accessControlContext = accessControlContext;
        if (loginContext == null) {
            this.subject = Subject.getSubject(accessControlContext == null ? AccessController.getContext() : accessControlContext);
        } else {
            this.subject = loginContext.getSubject();
        }
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
     * Get the {@link AccessControlContext JAAS access control context} for this context.
     * 
     * @return the access control context; may be <code>null</code>
     */
    public AccessControlContext getAccessControlContext() {
        return this.accessControlContext;
    }

    /**
     * Get the {@link LoginContext JAAS login context} for this context.
     * 
     * @return the login context; may be <code>null</code>
     */
    public LoginContext getLoginContext() {
        return this.loginContext;
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
     * Get the JAAS subject for which this context was created.
     * 
     * @return the subject; should never be null if JAAS is used, but will be null if there is no
     *         {@link #getAccessControlContext() access control context} or {@link #getLoginContext() login context}.
     */
    public Subject getSubject() {
        return this.subject;
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
        return new ExecutionContext(this.getLoginContext(), this.getAccessControlContext(), namespaceRegistry, null, null,
                                    this.getMimeTypeDetector(), this.getClassLoaderFactory());
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
        return new ExecutionContext(getLoginContext(), getAccessControlContext(), getNamespaceRegistry(), getValueFactories(),
                                    getPropertyFactory(), mimeTypeDetector, getClassLoaderFactory());
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
        return new ExecutionContext(getLoginContext(), getAccessControlContext(), getNamespaceRegistry(), getValueFactories(),
                                    getPropertyFactory(), getMimeTypeDetector(), classLoaderFactory);
    }

    /**
     * Creates an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link AccessControlContext access control context}.
     * 
     * @param accessControlContext the JAAS access control context that should be used
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws IllegalArgumentException if <code>accessControlContext</code> is <code>null</code>.
     */
    public ExecutionContext create( AccessControlContext accessControlContext ) {
        return new ExecutionContext(this, accessControlContext);
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied {@link LoginContext}. A
     * LoginContext has a variety of constructors, including contructors that take combinations of
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name}, {@link Subject subject},
     * {@link CallbackHandler callback handlers}, and a {@link Configuration JAAS configuration}.
     * 
     * @param loginContext the JAAS login context
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws IllegalArgumentException if the <code>loginContext</code> is null
     */
    public ExecutionContext create( LoginContext loginContext ) {
        return new ExecutionContext(this, loginContext);
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name}.
     * 
     * @param name the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws IllegalArgumentException if the <code>name</code> is null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         default callback handler JAAS property was not set or could not be loaded
     */
    public ExecutionContext with( String name ) throws LoginException {
        return new ExecutionContext(this, new LoginContext(name));
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name} and a {@link Subject JAAS subject}.
     * 
     * @param name the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param subject the subject to authenticate
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), if the default
     *         callback handler JAAS property was not set or could not be loaded, or if the <code>subject</code> is null or
     *         unknown
     */
    public ExecutionContext with( String name,
                                  Subject subject ) throws LoginException {
        return new ExecutionContext(this, new LoginContext(name, subject));
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name} and a {@link CallbackHandler JAAS
     * callback handler} (used to handle authentication callbacks).
     * 
     * @param name the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param callbackHandler the callback handler that will be used by {@link LoginModule}s to communicate with the user to
     *        authenticate
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         <code>callbackHandler</code> is null
     */
    public ExecutionContext with( String name,
                                  CallbackHandler callbackHandler ) throws LoginException {
        LoginContext loginContext = new LoginContext(name, callbackHandler);
        loginContext.login();

        return new ExecutionContext(this, loginContext);
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name} and a {@link CallbackHandler JAAS
     * callback handler} to create a new {@link LoginContext login context} with the given user ID and password.
     * 
     * @param name the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param userId the user ID to use for authentication
     * @param password the password to use for authentication
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         <code>callbackHandler</code> is null
     */
    public ExecutionContext with( String name,
                                  String userId,
                                  char[] password ) throws LoginException {
        return this.with(name, new UserPasswordCallbackHandler(userId, password));
    }

    /**
     * Create an {@link ExecutionContext} that is the same as this context, but which uses the supplied
     * {@link Configuration#getAppConfigurationEntry(String) application configuration name}, a {@link Subject JAAS subject}, and
     * a {@link CallbackHandler JAAS callback handler} (used to handle authentication callbacks).
     * 
     * @param name the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param subject the subject to authenticate
     * @param callbackHandler the callback handler that will be used by {@link LoginModule}s to communicate with the user to
     *        authenticate
     * @return the execution context that is identical with this execution context, but with a different security context; never
     *         null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), if the default
     *         callback handler JAAS property was not set or could not be loaded, if the <code>subject</code> is null or unknown,
     *         or if the <code>callbackHandler</code> is null
     */
    public ExecutionContext with( String name,
                                  Subject subject,
                                  CallbackHandler callbackHandler ) throws LoginException {
        LoginContext loginContext = new LoginContext(name, subject, callbackHandler);
        loginContext.login();

        return new ExecutionContext(this, loginContext);
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
        return "Execution context for " + getSubject();
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
     * A simple {@link CallbackHandler callback handler} implementation that attempts to provide a user ID and password to any
     * callbacks that it handles.
     */
    protected final class UserPasswordCallbackHandler implements CallbackHandler {

        private static final boolean LOG_TO_CONSOLE = false;

        private final String userId;
        private final char[] password;

        protected UserPasswordCallbackHandler( String userId,
                                               char[] password ) {
            this.userId = userId;
            this.password = password.clone();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
         */
        public void handle( Callback[] callbacks ) throws UnsupportedCallbackException, IOException {
            boolean userSet = false;
            boolean passwordSet = false;
            
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof TextOutputCallback) {

                    // display the message according to the specified type
                    TextOutputCallback toc = (TextOutputCallback)callbacks[i];
                    if (!LOG_TO_CONSOLE) {
                        continue;
                    }

                    switch (toc.getMessageType()) {
                        case TextOutputCallback.INFORMATION:
                            System.out.println(toc.getMessage());
                            break;
                        case TextOutputCallback.ERROR:
                            System.out.println("ERROR: " + toc.getMessage());
                            break;
                        case TextOutputCallback.WARNING:
                            System.out.println("WARNING: " + toc.getMessage());
                            break;
                        default:
                            throw new IOException("Unsupported message type: " + toc.getMessageType());
                    }

                } else if (callbacks[i] instanceof NameCallback) {

                    // prompt the user for a username
                    NameCallback nc = (NameCallback)callbacks[i];

                    if (LOG_TO_CONSOLE) {
                        // ignore the provided defaultName
                        System.out.print(nc.getPrompt());
                        System.out.flush();
                    }

                    nc.setName(this.userId);
                    userSet = true;

                } else if (callbacks[i] instanceof PasswordCallback) {

                    // prompt the user for sensitive information
                    PasswordCallback pc = (PasswordCallback)callbacks[i];
                    if (LOG_TO_CONSOLE) {
                        System.out.print(pc.getPrompt());
                        System.out.flush();
                    }
                    pc.setPassword(this.password);
                    passwordSet = true;

                } else {
                    /*
                     * Jetty uses its own callback for setting the password.  Since we're using Jetty for integration
                     * testing of the web project(s), we have to accomodate this.  Rather than introducing a direct
                     * dependency, we'll add code to handle the case of unexpected callback handlers with a setObject method.
                     */
                    try {
                        // Assume that a callback chain will ask for the user before the password
                        if (!userSet) {
                            new Reflection(callbacks[i].getClass()).invokeSetterMethodOnTarget("object", callbacks[i], this.userId);
                            userSet = true;
                        }
                        else if (!passwordSet) {
                            // Jetty also seems to eschew passing passwords as char arrays
                            new Reflection(callbacks[i].getClass()).invokeSetterMethodOnTarget("object", callbacks[i], new String(this.password));
                            passwordSet = true;
                        }
                        // It worked - need to continue processing the callbacks
                        continue;
                    } catch (Exception ex) {
                        // If the property cannot be set, fall through to the failure
                    }
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback: "
                                                           + callbacks[i].getClass().getName());
                }
            }

        }
    }
}
