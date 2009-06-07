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
package org.jboss.dna.web.jcr.rest;

import java.util.Collection;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.web.jcr.rest.spi.RepositoryProvider;

/**
 * Factory that provides implementations of the {@link RepositoryProvider repository provider SPI} by wrapping a
 * {@link RepositoryProvider}.
 * <p>
 * The repository factory implements a lifecycle for the repository providers. It is first {@link #initialize(ServletContext)
 * initialized} by {@link DnaJcrDeployer}, a servlet context listener that must be configured in the DNA JCR REST web
 * configuration (web.xml). The repository factory looks in the context for a parameter with the name of {@link #PROVIDER_KEY}.
 * This is assumed to be the FQN of the {@link RepositoryProvider repository provider}, which the factory will then instantiate.
 * </p>
 * <p>
 * The repository factory is then able to respond to multiple requests to {@link #getJcrRepositoryNames() list the repository
 * names} and {@link #getSession(HttpServletRequest, String, String) return active JCR sessions} until the {@link #shutdown()
 * shutdown method} is called.
 * </p>
 * <p>
 * The {@link #shutdown() shutdown method} is a simple proxy to the {@link RepositoryProvider#shutdown()} repository provider's
 * shutdown method}.
 * </p>
 */
@ThreadSafe
public class RepositoryFactory {

    /** The FQN of the repository provider class. Currently set to {@value} . */
    public static final String PROVIDER_KEY = "org.jboss.dna.web.jcr.rest.REPOSITORY_PROVIDER";

    private static RepositoryProvider provider;

    private RepositoryFactory() {

    }

    /**
     * Initializes the repository factory. For more details, please see the {@link RepositoryFactory class-level documentation}.
     * 
     * @param context the servlet context; may not be null
     * @see RepositoryFactory
     */
    static void initialize( ServletContext context ) {
        CheckArg.isNotNull(context, "context");
        String className = context.getInitParameter(PROVIDER_KEY);

        try {
            Class<? extends RepositoryProvider> providerClass = Class.forName(className).asSubclass(RepositoryProvider.class);
            provider = providerClass.newInstance();

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        provider.startup(context);
    }

    public static Session getSession( HttpServletRequest request,
                                      String repositoryName,
                                      String workspaceName ) throws RepositoryException {
        return provider.getSession(request, repositoryName, workspaceName);
    }

    public static Collection<String> getJcrRepositoryNames() {
        return provider.getJcrRepositoryNames();
    }

    static void shutdown() {
        provider.shutdown();
    }
}
