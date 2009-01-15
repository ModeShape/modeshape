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

package org.jboss.dna.repository.util;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * A SessionFactory implementation that creates {@link Session} instances using {@link Repository} instances registered in JNDI.
 * <p>
 * This factory using a naming convention where the name supplied to the {@link #createSession(String)} contains both the name of
 * the repository and the name of the workspace. Typically, this is <i><code>repositoryName/workspaceName</code></i>, where
 * <code>repositoryName</code> is the JNDI name under which the Repository instance was bound, and <code>workspaceName</code>
 * is the name of the workspace. Note that this method looks for the last delimiter in the whole name to distinguish between the
 * repository and workspace names.
 * </p>
 * <p>
 * For example, if "<code>java:comp/env/repository/dataRepository/myWorkspace</code>" is passed to the
 * {@link #createSession(String)} method, this factory will look for a {@link Repository} instance registered in JDNI with the
 * name "<code>java:comp/env/repository/dataRepository</code>" and use it to {@link Repository#login(String) create a session}
 * to the workspace named "<code>myWorkspace</code>".
 * </p>
 * <p>
 * By default, this factory creates an anonymous JCR session. To use sessions with specific {@link Credentials}, simply
 * {@link #registerCredentials(String, Credentials) register} credentials for the appropriate repository/workspace name. For
 * security reasons, it is not possible to retrieve the Credentials once registered with this factory.
 * </p>
 * @author Randall Hauch
 */
public class JndiSessionFactory extends AbstractSessionFactory {

    private final Context context;

    /**
     * Create an instance of the factory by creating a new {@link InitialContext}. This is equivalent to calling
     * <code>new JndiSessionFactory(new InitialContext())</code>.
     * @throws NamingException if there is a problem creating the InitialContext.
     */
    public JndiSessionFactory() throws NamingException {
        this(new InitialContext());
    }

    /**
     * Create an instance of the factory by supplying the characters that may be used to delimit the workspace name from the
     * repository name. This constructor initializes the factory with a new {@link InitialContext}, and is equivalent to calling
     * <code>new JndiSessionFactory(new InitialContext(),workspaceDelimiters)</code>.
     * @param workspaceDelimiters the delimiters, or null/empty if the default delimiter of '/' should be used.
     * @throws NamingException if there is a problem creating the InitialContext.
     */
    public JndiSessionFactory( char... workspaceDelimiters ) throws NamingException {
        this(new InitialContext(), workspaceDelimiters);
    }

    /**
     * Create an instance of the factory using the supplied JNDI context.
     * @param context the naming context
     * @throws IllegalArgumentException if the context parameter is null
     */
    public JndiSessionFactory( Context context ) {
        this(context, DEFAULT_DELIMITERS);
    }

    /**
     * Create an instance of the factory by supplying naming context and the characters that may be used to delimit the workspace
     * name from the repository name.
     * @param context the naming context
     * @param workspaceDelimiters the delimiters, or null/empty if the default delimiter of '/' should be used.
     * @throws IllegalArgumentException if the context parameter is null
     */
    public JndiSessionFactory( Context context, char... workspaceDelimiters ) {
        super(workspaceDelimiters);
        CheckArg.isNotNull(context, "initial context");
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRegisterRepository( String name, Repository repository ) throws SystemFailureException {
        try {
            this.context.bind(name, repository);
        } catch (NamingException e) {
            throw new SystemFailureException(RepositoryI18n.unableToRegisterRepositoryInJndi.text(name));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doUnregisterRepository( String name ) throws SystemFailureException {
        try {
            this.context.unbind(name);
        } catch (NamingException e) {
            throw new SystemFailureException(RepositoryI18n.unableToUnregisterRepositoryInJndi.text(name));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Repository findRegisteredRepository( String name ) throws SystemFailureException {
        try {
            return (Repository)this.context.lookup(name);
        } catch (NamingException e) {
            throw new SystemFailureException(RepositoryI18n.unableToFindRepositoryInJndi.text(name));
        }
    }
}
