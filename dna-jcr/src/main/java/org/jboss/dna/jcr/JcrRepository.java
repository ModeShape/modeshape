/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.ExecutionContextFactory;
import org.jboss.dna.spi.connector.RepositoryConnectionFactory;

/**
 * Creates JCR {@link Session sessions} to an underlying repository (which may be a federated repository).
 * <p>
 * This JCR repository must be configured with the ability to connect to a repository via a supplied
 * {@link RepositoryConnectionFactory repository connection factory} and repository source name. An
 * {@link ExecutionContextFactory execution context factory} must also be supplied to enable working with the underlying DNA graph
 * implementation to which this JCR implementation delegates.
 * </p>
 * <p>
 * If {@link Credentials credentials} are used to login, implementations <em>must</em> also implement one of the following
 * methods:
 * 
 * <pre>
 * public {@link AccessControlContext} getAccessControlContext();
 * public {@link LoginContext} getLoginContext();
 * </pre>
 * 
 * Note, {@link Session#getAttributeNames() attributes} on credentials are not supported. JCR {@link SimpleCredentials} are also
 * not supported.
 * </p>
 * 
 * @author John Verhaeg
 * @author Randall Hauch
 */
@ThreadSafe
public class JcrRepository implements Repository {

    private final Map<String, String> descriptors;
    private final ExecutionContextFactory executionContextFactory;
    private final RepositoryConnectionFactory connectionFactory;

    /**
     * Creates a JCR repository that uses the supplied {@link RepositoryConnectionFactory repository connection factory} to
     * establish {@link Session sessions} to the underlying repository source upon {@link #login() login}.
     * 
     * @param executionContextFactory An execution context factory.
     * @param connectionFactory A repository connection factory.
     * @throws IllegalArgumentException If <code>executionContextFactory</code> or <code>connectionFactory</code> is
     *         <code>null</code>.
     */
    public JcrRepository( ExecutionContextFactory executionContextFactory,
                          RepositoryConnectionFactory connectionFactory ) {
        this(null, executionContextFactory, connectionFactory);
    }

    /**
     * Creates a JCR repository that uses the supplied {@link RepositoryConnectionFactory repository connection factory} to
     * establish {@link Session sessions} to the underlying repository source upon {@link #login() login}.
     * 
     * @param descriptors The {@link #getDescriptorKeys() descriptors} for this repository; may be <code>null</code>.
     * @param executionContextFactory An execution context factory.
     * @param connectionFactory A repository connection factory.
     * @throws IllegalArgumentException If <code>executionContextFactory</code> or <code>connectionFactory</code> is
     *         <code>null</code>.
     */
    public JcrRepository( Map<String, String> descriptors,
                          ExecutionContextFactory executionContextFactory,
                          RepositoryConnectionFactory connectionFactory ) {
        ArgCheck.isNotNull(executionContextFactory, "executionContextFactory");
        ArgCheck.isNotNull(connectionFactory, "connectionFactory");
        this.executionContextFactory = executionContextFactory;
        this.connectionFactory = connectionFactory;
        Map<String, String> modifiableDescriptors;
        if (descriptors == null) {
            modifiableDescriptors = new HashMap<String, String>();
        } else {
            modifiableDescriptors = new HashMap<String, String>(descriptors);
        }
        // Initialize required JCR descriptors.
        modifiableDescriptors.put(Repository.LEVEL_1_SUPPORTED, "true");
        modifiableDescriptors.put(Repository.LEVEL_2_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_LOCKING_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.QUERY_XPATH_DOC_ORDER, "true");
        modifiableDescriptors.put(Repository.QUERY_XPATH_POS_INDEX, "true");
        // Vendor-specific descriptors (REP_XXX) will only be initialized if not already present, allowing for customer branding.
        if (!modifiableDescriptors.containsKey(Repository.REP_NAME_DESC)) {
            modifiableDescriptors.put(Repository.REP_NAME_DESC, JcrI18n.REP_NAME_DESC.text());
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VENDOR_DESC)) {
            modifiableDescriptors.put(Repository.REP_VENDOR_DESC, JcrI18n.REP_VENDOR_DESC.text());
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VENDOR_URL_DESC)) {
            modifiableDescriptors.put(Repository.REP_VENDOR_URL_DESC, "http://www.jboss.org/dna");
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VERSION_DESC)) {
            modifiableDescriptors.put(Repository.REP_VERSION_DESC, "0.2");
        }
        modifiableDescriptors.put(Repository.SPEC_NAME_DESC, JcrI18n.SPEC_NAME_DESC.text());
        modifiableDescriptors.put(Repository.SPEC_VERSION_DESC, "1.0");
        this.descriptors = Collections.unmodifiableMap(modifiableDescriptors);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>key</code> is <code>null</code>.
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor( String key ) {
        ArgCheck.isNotEmpty(key, "key");
        return descriptors.get(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return descriptors.keySet().toArray(new String[descriptors.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login()
     */
    public synchronized Session login() throws RepositoryException {
        return login(null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public synchronized Session login( Credentials credentials ) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public synchronized Session login( String workspaceName ) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>credentials</code> is not <code>null</code> but:
     *         <ul>
     *         <li>provides neither a <code>getLoginContext()</code> nor a <code>getAccessControlContext()</code> method.</li>
     *         <li>provides a <code>getLoginContext()</code> method that doesn't return a {@link LoginContext}.
     *         <li>provides a <code>getLoginContext()</code> method that returns a <code>null</code> {@link LoginContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that doesn't return an {@link AccessControlContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that returns a <code>null</code> {@link AccessControlContext}.
     *         </ul>
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    public synchronized Session login( Credentials credentials,
                                       String workspaceName ) throws RepositoryException {
        // Ensure credentials are either null or provide a JAAS method
        ExecutionContext execContext;
        if (credentials == null) {
            execContext = executionContextFactory.create();
        } else {
            try {
                // Check if credentials provide a login context
                try {
                    Method method = credentials.getClass().getMethod("getLoginContext");
                    if (method.getReturnType() != LoginContext.class) {
                        throw new IllegalArgumentException(JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                    }
                    LoginContext loginContext = (LoginContext)method.invoke(credentials);
                    if (loginContext == null) {
                        throw new IllegalArgumentException(JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                    }
                    execContext = executionContextFactory.create(loginContext);
                } catch (NoSuchMethodException error) {
                    // Check if credentials provide an access control context
                    try {
                        Method method = credentials.getClass().getMethod("getAccessControlContext");
                        if (method.getReturnType() != AccessControlContext.class) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnAccessControlContext.text(credentials.getClass()));
                        }
                        AccessControlContext accessControlContext = (AccessControlContext)method.invoke(credentials);
                        if (accessControlContext == null) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnAccessControlContext.text(credentials.getClass()));
                        }
                        execContext = executionContextFactory.create(accessControlContext);
                    } catch (NoSuchMethodException error2) {
                        throw new IllegalArgumentException(JcrI18n.credentialsMustProvideJaasMethod.text(credentials.getClass()),
                                                           error2);
                    }
                }
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new RepositoryException(error);
            }
        }
        // Authenticate
        try {
            assert execContext != null;
            assert execContext.getLoginContext() != null;
            execContext.getLoginContext().login();
        } catch (javax.security.auth.login.LoginException error) {
            throw new LoginException(error);
        }
        // Ensure valid workspace name
        if (workspaceName == null) workspaceName = JcrI18n.defaultWorkspaceName.text();
        // Create session
        return new JcrSession(this, execContext, workspaceName, connectionFactory.createConnection(workspaceName),
                              new WeakHashMap<UUID, WeakReference<Node>>());
    }
}
