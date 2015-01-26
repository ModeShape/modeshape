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

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.Environment;

/**
 * Base class for {@link org.modeshape.jcr.security.AuthenticationProvider} implementations which leverage the active
 * {@link org.modeshape.jcr.Environment} instance to perform custom authentication.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class EnvironmentAuthenticationProvider implements AuthenticationProvider {
    
    private Environment environment;
    private String securityDomain;
    
    /**
     * No-arg constructor, required in order for these providers to be created via reflection at repository start.
     */                                                               
    public EnvironmentAuthenticationProvider() {
    }

    /**
     * Sets the active repository environment for this provider.
     * 
     * @param environment a {@link org.modeshape.jcr.Environment} instance, never {@code null}
     */
    public void setEnvironment( Environment environment ) {
        CheckArg.isNotNull(environment, "environment");
        this.environment = environment;
    }

    /**
     * Sets the name of the security domain in which authentication will be attempted.
     * 
     * @param securityDomain the name of a valid security domain, never {@code null}
     */
    public void setSecurityDomain( String securityDomain ) {
        CheckArg.isNotNull(securityDomain, "securityDomain");
        this.securityDomain = securityDomain;
    }
    
    protected Environment environment() {
        return environment;
    }

    protected String securityDomain() {
        return securityDomain;
    }

    /**
     * Initializes this provider instance, after the {@link org.modeshape.jcr.Environment) and the security domain have been set
     */
    public abstract void initialize();
}
