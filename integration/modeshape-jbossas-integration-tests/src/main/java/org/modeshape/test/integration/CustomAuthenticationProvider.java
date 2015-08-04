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
package org.modeshape.test.integration;

import java.util.Map;
import javax.jcr.Credentials;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.jcr.security.SecurityContext;

/**
 * A simple {@link AuthenticationProvider} implementation used for Arquillian testing.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class CustomAuthenticationProvider implements AuthenticationProvider, SecurityContext {

    private String username;

    public void setUsername( String username ) {
        this.username = username;
    }

    @Override
    public ExecutionContext authenticate( Credentials credentials, String repositoryName, String workspaceName,
                                          ExecutionContext repositoryContext, Map<String, Object> sessionAttributes ) {
        return repositoryContext.with(this);
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public boolean hasRole( String roleName ) {
        return true;
    }

    @Override
    public void logout() {
    }
}
