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
package org.modeshape.jcr.security;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jcr.Credentials;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;

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
                Logger.getLogger(AuthenticationProviders.class).error(e,
                                                                      JcrI18n.errorInAuthenticationProvider,
                                                                      provider.getClass().getName(),
                                                                      repositoryName,
                                                                      e.getMessage());
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
