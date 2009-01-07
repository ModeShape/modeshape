/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.dna.repository.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.CheckArg;

/**
 * A SessionFactory implementation that creates {@link Session} instances using {@link Repository} instances.
 * <p>
 * This factory using a naming convention where the name supplied to the {@link #createSession(String)} contains both the name of
 * the repository and the name of the workspace. Typically, this is <i><code>repositoryName/workspaceName</code></i>, where
 * <code>repositoryName</code> is the name under which the Repository instance was bound, and <code>workspaceName</code> is
 * the name of the workspace. Note that this method looks for the last delimiter in the whole name to distinguish between the
 * repository and workspace names.
 * </p>
 * <p>
 * For example, if "<code>java:comp/env/repository/dataRepository/myWorkspace</code>" is passed to the
 * {@link #createSession(String)} method, this factory will look for a {@link Repository} instance registered with the name "<code>java:comp/env/repository/dataRepository</code>"
 * and use it to {@link Repository#login(String) create a session} to the workspace named "<code>myWorkspace</code>".
 * </p>
 * <p>
 * By default, this factory creates an anonymous JCR session. To use sessions with specific {@link Credentials}, simply
 * {@link #registerCredentials(String, Credentials) register} credentials for the appropriate repository/workspace name. For
 * security reasons, it is not possible to retrieve the Credentials once registered with this factory.
 * </p>
 * @author Randall Hauch
 */
public abstract class AbstractSessionFactory implements SessionFactory {

    protected static char[] DEFAULT_DELIMITERS = new char[] {'/'};

    private final char[] workspaceDelims;
    private final String workspaceDelimsRegexCharacterSet;
    private final Map<String, Credentials> credentials = new ConcurrentHashMap<String, Credentials>();

    /**
     * Create an instance of the factory with the default delimiters.
     */
    public AbstractSessionFactory() {
        this(null);
    }

    /**
     * Create an instance of the factory by supplying naming context and the characters that may be used to delimit the workspace
     * name from the repository name.
     * @param workspaceDelimiters the delimiters, or null/empty if the default delimiter of '/' should be used.
     * @throws IllegalArgumentException if the context parameter is null
     */
    public AbstractSessionFactory( char... workspaceDelimiters ) {
        this.workspaceDelims = (workspaceDelimiters == null || workspaceDelimiters.length == 0) ? DEFAULT_DELIMITERS : workspaceDelimiters;
        StringBuilder sb = new StringBuilder();
        for (char delim : this.workspaceDelims) {
            switch (delim) {
                case '\\':
                    sb.append("\\");
                    break;
                // case '[' : sb.append("\\["); break;
                case ']':
                    sb.append("\\]");
                    break;
                case '-':
                    sb.append("\\-");
                    break;
                case '^':
                    sb.append("\\^");
                    break;
                default:
                    sb.append(delim);
            }
        }
        this.workspaceDelimsRegexCharacterSet = sb.toString();
    }

    /**
     * Convenience method to bind a repository in JNDI. Repository instances can be bound into JNDI using any technique, so this
     * method need not be used. <i>Note that the name should not contain the workspace part.</i>
     * @param name the name of the repository, without the workspace name component.
     * @param repository the repository to be bound, or null if an existing repository should be unbound.
     */
    public void registerRepository( String name, Repository repository ) {
        assert name != null;
        // Remove all trailing delimiters ...
        name = name.replaceAll("[" + this.workspaceDelimsRegexCharacterSet + "]+$", "");
        if (repository != null) {
            this.doRegisterRepository(name, repository);
        } else {
            this.doUnregisterRepository(name);
        }
    }

    protected abstract void doRegisterRepository( String name, Repository repository ) throws SystemFailureException;

    protected abstract void doUnregisterRepository( String name ) throws SystemFailureException;

    protected abstract Repository findRegisteredRepository( String name ) throws SystemFailureException;

    /**
     * Register the credentials for the repository and workspace given by the supplied name, username and password. This is
     * equivalent to calling <code>registerCredentials(name, new SimpleCredentials(username,password))</code>, although if
     * <code>username</code> is null then this is equivalent to <code>registerCredentials(name,null)</code>.
     * @param name the name of the repository and workspace
     * @param username the username to use, or null if the existing credentials for the named workspace should be removed
     * @param password the password, may be null or empty
     * @return true if this overwrote existing credentials
     * @see #registerCredentials(String, Credentials)
     * @see #removeCredentials(String)
     */
    public boolean registerCredentials( String name, String username, char[] password ) {
        if (password == null && username != null) password = new char[] {};
        Credentials creds = username == null ? null : new SimpleCredentials(username, password);
        return registerCredentials(name, creds);
    }

    /**
     * Register the credentials to be used for the named repository and workspace. Use the same name as used to
     * {@link #createSession(String) create sessions}.
     * @param name the name of the repository and workspace
     * @param credentials the credentials to use, or null if the existing credentials for the named workspace should be removed
     * @return true if this overwrote existing credentials
     * @see #registerCredentials(String, String, char[])
     * @see #removeCredentials(String)
     */
    public boolean registerCredentials( String name, Credentials credentials ) {
        boolean foundExisting = false;
        name = name != null ? name.trim() : null;
        if (credentials == null) {
            foundExisting = this.credentials.remove(name) != null;
        } else {
            foundExisting = this.credentials.put(name, credentials) != null;
        }
        return foundExisting;
    }

    /**
     * Remove any credentials associated with the named repository and workspace. This is equivalent to calling
     * <code>registerCredentials(name,null)</code>.
     * @param name the name of the repository and workspace
     * @return true if existing credentials were found and removed, or false if no such credentials existed
     * @see #registerCredentials(String, Credentials)
     */
    public boolean removeCredentials( String name ) {
        return registerCredentials(name, null);
    }

    /**
     * {@inheritDoc}
     */
    public Session createSession( String name ) throws RepositoryException {
        CheckArg.isNotNull(name, "session name");
        name = name.trim();
        // Look up the Repository object in JNDI ...
        String repositoryName = getRepositoryName(name);
        Repository repository = findRegisteredRepository(repositoryName);

        // Determine the name of the workspace ...
        String workspaceName = getWorkspaceName(name);

        // Look up any credentials, which may be null ...
        Credentials creds = this.credentials.get(name);

        // Create a session to the specified workspace and using the credentials (either or both may be null) ...
        return repository.login(creds, workspaceName);
    }

    protected String getWorkspaceName( String name ) {
        assert name != null;
        int index = getIndexOfLastWorkspaceDelimiter(name);
        if (index == -1) return null;
        if ((index + 1) == name.length()) return null; // delim is the last character
        return name.substring(index + 1);
    }

    protected String getRepositoryName( String name ) {
        assert name != null;
        int index = getIndexOfLastWorkspaceDelimiter(name);
        if (index == -1) return name; // no delim
        if ((index + 1) == name.length()) return name.substring(0, index); // delim as last character
        return name.substring(0, index);
    }

    protected int getIndexOfLastWorkspaceDelimiter( String name ) {
        int index = -1;
        for (char delim : this.workspaceDelims) {
            int i = name.lastIndexOf(delim);
            index = Math.max(index, i);
        }
        return index;
    }

}
