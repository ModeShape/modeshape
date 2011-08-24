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

import java.security.PrivilegedActionException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jcr.Credentials;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.core.ExecutionContext;

/**
 * An implementation of {@link AuthenticationProvider} that represents an ordered list of other {@link AuthenticationProvider}
 * implementations.
 */
public class AuthenticationProviders implements AuthenticationProvider {

    private final List<AuthenticationProvider> providers;

    public AuthenticationProviders() {
        this.providers = new CopyOnWriteArrayList<AuthenticationProvider>();
    }

    public AuthenticationProviders( List<AuthenticationProvider> providers ) {
        this.providers = new CopyOnWriteArrayList<AuthenticationProvider>(providers);
    }

    public AuthenticationProviders( AuthenticationProvider... providers ) {
        this.providers = new CopyOnWriteArrayList<AuthenticationProvider>(providers);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.jcr.Credentials, java.lang.String,
     *      java.lang.String, org.modeshape.jcr.core.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate( Credentials credentials,
                                          String repositoryName,
                                          String workspaceName,
                                          ExecutionContext repositoryContext,
                                          Map<String, Object> sessionAttributes ) {
        ExecutionContext result = null;
        for (AuthenticationProvider provider : providers) {
            try {
                // The session attributes from prior, failed authenticators should be cleared ...
                sessionAttributes.clear();
                result = provider.authenticate(credentials, repositoryName, workspaceName, repositoryContext, sessionAttributes);
                if (result != null) return result;
            } catch (Exception e) {
                // This should not happen, so log it ...
                if (e instanceof PrivilegedActionException) {
                    e = ((PrivilegedActionException)e).getException();
                    Logger.getLogger(AuthenticationProviders.class).error(e,
                                                                          JcrI18n.mustBeInPrivilegedAction,
                                                                          repositoryName,
                                                                          workspaceName,
                                                                          provider.getClass().getName());
                } else {
                    Logger.getLogger(AuthenticationProviders.class).error(e,
                                                                          JcrI18n.errorInAuthenticationProvider,
                                                                          provider.getClass().getName(),
                                                                          repositoryName,
                                                                          e.getMessage());
                }
            }
        }
        return null;
    }

    public AuthenticationProviders with( AuthenticationProvider provider ) {
        List<AuthenticationProvider> providers = new CopyOnWriteArrayList<AuthenticationProvider>(this.providers);
        providers.add(provider);
        return new AuthenticationProviders(providers);
    }

    public static AuthenticationProviders with( AuthenticationProvider... providers ) {
        List<AuthenticationProvider> providerList = new CopyOnWriteArrayList<AuthenticationProvider>();
        for (AuthenticationProvider provider : providers) {
            if (provider != null) providerList.add(provider);
        }
        return new AuthenticationProviders(providerList);
    }

}
