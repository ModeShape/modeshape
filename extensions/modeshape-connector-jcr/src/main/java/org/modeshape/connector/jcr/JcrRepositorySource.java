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

package org.modeshape.connector.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local file system as content in a repository. This
 * source considers a workspace name to be the path to the directory on the file system that represents the root of that
 * workspace. New workspaces can be created, as long as the names represent valid paths to existing directories.
 */
@ThreadSafe
public class JcrRepositorySource implements RepositorySource, ObjectFactory {

    /**
     * The first serialized version of this source. Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String REPOSITORY_JNDI_NAME = "repositoryJndiName";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String CREDENTIALS = "credentials";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String RETRY_LIMIT = "retryLimit";

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * This source supports events.
     */
    protected static final boolean SUPPORTS_EVENTS = true;
    /**
     * This source supports same-name-siblings.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = true;
    /**
     * This source does support updates.
     */
    protected static final boolean SUPPORTS_UPDATES = true;
    /**
     * This source does not support creating new workspaces, since the JCR API does not provide a way of doing so.
     */
    protected static final boolean SUPPORTS_CREATING_WORKSPACES = false;
    /**
     * This source supports creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = false;

    @Description( i18n = JcrConnectorI18n.class, value = "namePropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "namePropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = JcrConnectorI18n.class, value = "repositoryJndiNamePropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "repositoryJndiNamePropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "repositoryJndiNamePropertyCategory" )
    private volatile String repositoryJndiName;

    @Description( i18n = JcrConnectorI18n.class, value = "usernamePropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "usernamePropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "usernamePropertyCategory" )
    private volatile String username;

    @Description( i18n = JcrConnectorI18n.class, value = "passwordPropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "passwordPropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "passwordPropertyCategory" )
    private volatile String password;

    private volatile Credentials credentials;
    private volatile CachePolicy defaultCachePolicy;

    @Description( i18n = JcrConnectorI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;

    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES);
    private transient Repository repository;
    private transient Context jndiContext;
    private transient RepositoryContext repositoryContext;

    /**
     * Create a new instance of a JCR repository source.
     */
    public JcrRepositorySource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for the source
     * 
     * @param name the new name for the source
     */
    public void setName( String name ) {
        if (name != null) {
            name = name.trim();
            if (name.length() == 0) name = null;
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Get whether this source supports updates.
     * 
     * @return true if this source supports updates, or false if this source only supports reading content.
     */
    @Description( i18n = JcrConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = JcrConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = JcrConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    public boolean getUpdatesAllowed() {
        return capabilities.supportsUpdates();
    }

    /**
     * @return dataSourceJndiName
     */
    public String getRepositoryJndiName() {
        return repositoryJndiName;
    }

    /**
     * Set the name in JNDI where this source can find the JCR Repository object.
     * 
     * @param repositoryJndiName the name in JNDI where the JCR Repository object can be found
     */
    public void setRepositoryJndiName( String repositoryJndiName ) {
        if (repositoryJndiName != null && repositoryJndiName.trim().length() == 0) repositoryJndiName = null;
        this.repositoryJndiName = repositoryJndiName;
    }

    /**
     * Get the username that should be used to access the repository.
     * 
     * @return the username, which may be null if the
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username Sets username to the specified value.
     */
    public synchronized void setUsername( String username ) {
        this.username = username;
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password Sets password to the specified value.
     */
    public synchronized void setPassword( String password ) {
        this.password = password;
    }

    /**
     * Get the JCR credentials that should be used.
     * 
     * @return the credentials, or null if no credentials should be used
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Set the JCR credentials that should be used.
     * 
     * @param credentials the credentials, or null if no fixed credentials should be used
     */
    public void setCredentials( Credentials credentials ) {
        this.credentials = credentials;
    }

    /**
     * Get the default cache policy for this source, or null if the global default cache policy should be used
     * 
     * @return the default cache policy, or null if this source has no explicit default cache policy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * @param defaultCachePolicy Sets defaultCachePolicy to the specified value.
     */
    public synchronized void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        if (this.defaultCachePolicy == defaultCachePolicy || this.defaultCachePolicy != null
            && this.defaultCachePolicy.equals(defaultCachePolicy)) return; // unchanged
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        retryLimit = limit < 0 ? 0 : limit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    protected RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    protected synchronized Repository getRepository() {
        return this.repository;
    }

    protected synchronized void setRepository( Repository repository ) {
        this.repository = repository;
    }

    protected synchronized Context getContext() {
        return this.jndiContext;
    }

    protected synchronized void setContext( Context context ) {
        this.jndiContext = context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        }
        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(REPOSITORY_JNDI_NAME, getRepositoryJndiName()));
        ref.add(new StringRefAddr(USERNAME, getUsername()));
        ref.add(new StringRefAddr(PASSWORD, getPassword()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        if (getCredentials() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Credentials credentials = getCredentials();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(credentials);
                ref.add(new BinaryRefAddr(CREDENTIALS, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = JcrConnectorI18n.errorSerializingObjectUsedInSource;
                throw new RepositorySourceException(getName(), msg.text(credentials.getClass().getName(), getName()), e);
            }
        }
        if (getDefaultCachePolicy() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CachePolicy policy = getDefaultCachePolicy();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(policy);
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = JcrConnectorI18n.errorSerializingObjectUsedInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        return ref;
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectInstance( Object obj,
                                     javax.naming.Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) throws Exception {
        if (obj instanceof Reference) {
            Map<String, Object> values = new HashMap<String, Object>();
            Reference ref = (Reference)obj;
            Enumeration<?> en = ref.getAll();
            while (en.hasMoreElements()) {
                RefAddr subref = (RefAddr)en.nextElement();
                if (subref instanceof StringRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value != null) values.put(key, value.toString());
                } else if (subref instanceof BinaryRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value instanceof byte[]) {
                        // Deserialize ...
                        ByteArrayInputStream bais = new ByteArrayInputStream((byte[])value);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        value = ois.readObject();
                        values.put(key, value);
                    }
                }
            }
            String sourceName = (String)values.get(SOURCE_NAME);
            String repositoryJndiName = (String)values.get(REPOSITORY_JNDI_NAME);
            String username = (String)values.get(USERNAME);
            String password = (String)values.get(PASSWORD);
            String retryLimit = (String)values.get(RETRY_LIMIT);
            Object credentials = values.get(CREDENTIALS);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);

            // Create the source instance ...
            JcrRepositorySource source = new JcrRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (repositoryJndiName != null) source.setRepositoryJndiName(repositoryJndiName);
            if (username != null) source.setUsername(username);
            if (password != null) source.setPassword(password);
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            if (credentials instanceof Credentials) {
                source.setCredentials((Credentials)credentials);
            }
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (name == null || name.trim().length() == 0) {
            I18n msg = JcrConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(name, msg.text("name"));
        }
        if (repositoryJndiName == null || repositoryJndiName.trim().length() == 0) {
            I18n msg = JcrConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("repositoryJndiName"));
        }
        if (this.repository == null) {
            Context context = getContext();
            if (context == null) {
                try {
                    context = new InitialContext();
                } catch (NamingException err) {
                    throw new RepositorySourceException(name, err);
                }
            }

            // Look for a cache manager in JNDI ...
            Repository repository = null;
            Object object = null;
            try {
                object = context.lookup(repositoryJndiName);
                if (object != null) repository = (Repository)object;
            } catch (ClassCastException err) {
                I18n msg = JcrConnectorI18n.objectFoundInJndiWasNotRepository;
                String className = object != null ? object.getClass().getName() : "null";
                throw new RepositorySourceException(getName(), msg.text(repositoryJndiName, this.getName(), className), err);
            } catch (Throwable err) {
                if (err instanceof RuntimeException) throw (RuntimeException)err;
                throw new RepositorySourceException(getName(), err);
            }
            if (repository == null) {
                I18n msg = JcrConnectorI18n.repositoryObjectNotFoundInJndi;
                throw new RepositorySourceException(getName(), msg.text(repositoryJndiName));
            }
            this.repository = repository;
        }
        assert this.repository != null;

        // Create the appropriate credentials ...
        Credentials credentials = getCredentials();
        if (credentials == null || (username != null || password != null)) {
            char[] passwd = this.password != null ? password.toCharArray() : new char[] {};
            credentials = new SimpleCredentials(username, passwd);
        }

        // Create the repositor connection ...
        return new JcrRepositoryConnection(this, repository, credentials);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
        // Null the repository context and Repository references ...
        this.repositoryContext = null;
        this.repository = null;
    }
}
