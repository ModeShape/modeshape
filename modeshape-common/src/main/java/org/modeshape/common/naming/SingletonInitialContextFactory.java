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
package org.modeshape.common.naming;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;
import org.modeshape.common.SystemFailureException;

/**
 * An {@link InitialContextFactory} that provides a singleton {@link Context JNDI naming context}. Because it is a singleton, it
 * is useful in unit tests that {@link Context#lookup(String) looks up} objects where it can be used within service registration
 * logic while also being easily accessible in the test case itself.
 * <p>
 * For example, the following code sample shows how this InitialContextFactory implementation can be specified via the standard "
 * <code>{@link Context#INITIAL_CONTEXT_FACTORY java.naming.factory.initial}</code>" property:
 * 
 * <pre>
 * Hashtable&lt;String, Object&gt; jndiContext = new Hashtable&lt;String, Object&gt;();
 * jndiContext.put(&quot;java.naming.factory.initial&quot;, &quot;org.modeshape.common.naming.SingleonInitialContextFactory&quot;);
 * jndiContext.put(&quot;java.naming.provider.url&quot;, &quot;localhost&quot;);
 * InitialContext initialContext = new InitialContext(jndiContext);
 * </pre>
 * 
 * while the following sample shows how the same {@link Context} instance will be subsequently returned from accessed within other
 * code (e.g., a test case):
 * 
 * <pre>
 * Context context = SingletoneInitialContextFactory.create();
 * </pre>
 * 
 * </p>
 * 
 * @author Luca Stancapiano
 * @author Randall Hauch
 */
public class SingletonInitialContextFactory implements InitialContextFactory {

    private static SingletonInitialContext SINGLETON;

    /**
     * No-arg constructor
     */
    public SingletonInitialContextFactory() {
    }

    /**
     * {@inheritDoc}
     */
    public Context getInitialContext( Hashtable<?, ?> environment ) {
        return getInstance(environment);
    }

    /**
     * Return the {@link Context} singleton instance. If no such context has been configured, this method will configured the
     * singletone using the supplied environment.
     * 
     * @param environment the environment for the naming context; may be null or empty
     * @return the singleton context; never null
     * @see #getInitialContext(Hashtable)
     * @see #getInstance(Hashtable)
     */
    public static synchronized Context getInstance( Hashtable<?, ?> environment ) {
        if (SINGLETON == null) SINGLETON = new SingletonInitialContext(environment);
        return SINGLETON;
    }

    /**
     * Return the previously-configured {@link Context} singleton instance. If no such context has been configured, this method
     * throws a {@link SystemFailureException}.
     * 
     * @return the singleton context; never null
     * @throws SystemFailureException if the singleton context has not yet been configured.
     * @see #getInitialContext(Hashtable)
     * @see #getInstance(Hashtable)
     */
    public static synchronized Context getInstance() {
        if (SINGLETON == null) {
            throw new SystemFailureException();
        }
        return SINGLETON;
    }

    /**
     * Set the {@link Context#INITIAL_CONTEXT_FACTORY} system property to the name of this context's
     * {@link SingletonInitialContextFactory factory class}.
     */
    static public void initialize() {
        System.setProperty("java.naming.factory.initial", SingletonInitialContextFactory.class.getName());
    }

    /**
     * Release any existing singleton {@link Context naming context}. Any subsequent calls to {@link #getInstance(Hashtable)} or
     * {@link #getInitialContext(Hashtable)} will return a new singleton instance.
     */
    public static synchronized void tearDown() {
        SINGLETON = null;
    }
}
