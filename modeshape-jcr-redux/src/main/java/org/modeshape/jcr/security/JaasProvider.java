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
package org.modeshape.jcr.security;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ClassUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.JaasCredentials;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.core.JaasSecurityContext;

/**
 * An implementation of {@link AuthenticationProvider} that uses a supplied JAAS policy to perform all authentication and
 * <i>role-based</i> authorization.
 */
public class JaasProvider implements AuthenticationProvider {

    private final String policyName;
    private final SubjectResolver subjectResolver;

    /**
     * Create a JAAS provider for authentication and authorization, using the supplied name for the login configuration.
     * 
     * @param policyName
     * @exception LoginException if the caller-specified <code>name</code> does not appear in the <code>Configuration</code> and
     *            there is no <code>Configuration</code> entry for "<i>other</i>", or if the
     *            <i>auth.login.defaultCallbackHandler</i> security property was set, but the implementation class could not be
     *            loaded.
     *            <p>
     */
    public JaasProvider( String policyName ) throws LoginException {
        CheckArg.isNotNull(policyName, "policyName");
        this.policyName = policyName;

        // verify that the login context is valid ...
        new LoginContext(policyName);

        // Per MODE-1270, see if the JACC API is available (if so, we're running in an J2EE container
        // and need to provide a way to properly resolve the JAAS Subject)...
        JaasProvider.SubjectResolver subjectResolver = null;
        try {
            // Try to find the JACC PolicyContext class, which is entirely optional and provided only in J2EE containers
            // ...
            ClassUtil.loadClassStrict("javax.security.jacc.PolicyContext");
            subjectResolver = new JaccSubjectResolver();
            Logger.getLogger(getClass())
                  .debug("Enabling optional JACC approach for resolving the JAAS Subject (typically in J2EE containers)");
        } catch (ClassNotFoundException cnfe) {
            // Must not be able to load the class ...
            Logger.getLogger(getClass())
                  .debug("Failed to find 'javax.security.jacc.PolicyContext', so assuming not in a J2EE container.");
        }
        this.subjectResolver = subjectResolver;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.jcr.Credentials, java.lang.String,
     *      java.lang.String, org.modeshape.jcr.core.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate( final Credentials credentials,
                                          String repositoryName,
                                          String workspaceName,
                                          ExecutionContext repositoryContext,
                                          Map<String, Object> sessionAttributes ) {
        try {
            if (credentials == null) {
                // There are no credentials, so see if there is an authenticated Subject ...
                Subject subject = Subject.getSubject(AccessController.getContext());
                if (subject != null) {
                    // There is, so use this subject ...
                    return repositoryContext.with(new JaasSecurityContext(subject));
                }
                if (subjectResolver != null) {
                    // The Subject is still null (see MODE-1270), so try to resolve it ...
                    subject = subjectResolver.resolveSubject();
                    if (subject != null) {
                        return repositoryContext.with(new JaasSecurityContext(subject));
                    }
                }
                // There is no authenticated JAAS Subject and no credentials, so we can do nothing ...
                return null;
            }
            if (credentials instanceof SimpleCredentials) {
                SimpleCredentials simple = (SimpleCredentials)credentials;
                String[] attributeNames = simple.getAttributeNames();
                if (attributeNames != null && attributeNames.length != 0) {
                    sessionAttributes = new HashMap<String, Object>();
                    for (String attributeName : simple.getAttributeNames()) {
                        Object attributeValue = simple.getAttribute(attributeName);
                        sessionAttributes.put(attributeName, attributeValue);
                    }
                }
                return repositoryContext.with(new JaasSecurityContext(policyName, simple.getUserID(), simple.getPassword()));
            }
            LoginContext loginContext = null;
            if (credentials instanceof JaasCredentials) {
                // Call directly ...
                loginContext = ((JaasCredentials)credentials).getLoginContext();
            } else {
                // Look for a getter method ...
                try {
                    final Method method = credentials.getClass().getMethod("getLoginContext");
                    Object result = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            return method.invoke(credentials);
                        }
                    });
                    if (result instanceof LoginContext) {
                        loginContext = (LoginContext)result;
                    } else {
                        Logger.getLogger(JaasProvider.class).error(JcrI18n.credentialsMustReturnLoginContext,
                                                                   credentials.getClass().getName());
                    }
                } catch (NoSuchMethodException error) {
                    // Not an implementation of Credentials that we know what to do with ...
                } catch (PrivilegedActionException e) {
                    // Not an implementation of Credentials that we can call ...
                    Logger.getLogger(JaasProvider.class).warn(JcrI18n.noPrivilegeToGetLoginContextFromCredentials,
                                                              credentials.getClass().getName());
                }
            }
            if (loginContext != null) {
                Subject subject = loginContext.getSubject();
                if (subject == null) {
                    // Try authenticate first ...
                    loginContext.login();
                    // Authentication succeeded ...
                    subject = loginContext.getSubject();
                    if (subject == null && this.subjectResolver != null) {
                        // The Subject is still null (see MODE-1270), so try to resolve it ...
                        subject = this.subjectResolver.resolveSubject();
                        if (subject != null) {
                            return repositoryContext.with(new JaasSecurityContext(subject));
                        }
                    }
                    if (subject == null) {
                        // Still null; we don't know what to do about this, so fail ...
                        return null;
                    }
                }
                return repositoryContext.with(new JaasSecurityContext(loginContext));
            }
        } catch (javax.security.auth.login.LoginException error) {
            // We've already verified that the JAAS policy name exists, so this error can only mean no authentication
            // so we can just continue ...
        }

        return null;
    }

    /**
     * An extension point for the JaasProvider class that allows for custom logic for finding the current JAAS Subject, if not
     * already available via the {@code Subject.getSubject(AccessController.getContext())} method. That method returns null in
     * J2EE applications.
     * 
     * @see JaccSubjectResolver
     */
    public static interface SubjectResolver {
        /**
         * Get the current JAAS Subject.
         * 
         * @return the subject, or null if the Subject could not be resolved
         */
        Subject resolveSubject();
    }
}
