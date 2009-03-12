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
package org.jboss.dna.connector.svn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceCapabilities;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * A repository source that uses a SVN repository instance to manage the content. This source is capable of using an existing
 * {@link SVNRepository} instance or creating a new instance. This process is controlled entirely by the JavaBean properties of
 * the SVNRepositorySource instance. Like other {@link RepositorySource} classes, instances of SVNRepositorySource can be placed
 * into JNDI and do support the creation of {@link Referenceable JNDI referenceable} objects and resolution of references into
 * SVNRepositorySource. </p>
 * 
 * @author Serge Pagop
 */
public class SVNRepositorySource implements RepositorySource, ObjectFactory {

    private static final long serialVersionUID = 1L;
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
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = false;
    /**
     * This source supports creating workspaces.
     */
    protected static final boolean SUPPORTS_CREATING_WORKSPACES = false;
    /**
     * This source supports udpates by default, but each instance may be configured to {@link #setSupportsUpdates(boolean) be
     * read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = true;

    public static final int DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS = 60 * 5; // 5 minutes

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String SVN_REPOS_JNDI_NAME = "svnReposJndiName";
    protected static final String SVN_REPOS_FACTORY_JNDI_NAME = "svnReposFactoryJndiName";
    protected static final String SVN_URL = "svnURL";
    protected static final String SVN_USERNAME = "svnUsername";
    protected static final String SVN_PASSWORD = "svnPassword";
    protected static final String RETRY_LIMIT = "retryLimit";

    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private String name;
    private String svnURL;
    private String svnUsername;
    private String svnPassword;
    private CachePolicy defaultCachePolicy;

    private RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                         DEFAULT_SUPPORTS_UPDATES,
                                                                                         SUPPORTS_EVENTS,
                                                                                         SUPPORTS_CREATING_WORKSPACES);

    private transient Context jndiContext;
    private transient RepositoryContext repositoryContext;
    private transient SVNRepository svnRepository;

    /**
     * Create a repository source instance.
     */
    public SVNRepositorySource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#initialize(org.jboss.dna.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * Set the name of this source
     * 
     * @param name the name for this source
     */
    public synchronized void setName( String name ) {
        if (this.name == name || this.name != null && this.name.equals(name)) return; // unchanged
        this.name = name;
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

    public String getSVNURL() {
        return this.svnURL;
    }

    /**
     * Set the url for the subversion repository.
     * 
     * @param url - the url location.
     * @throws IllegalArgumentException If svn url is null or empty
     */
    public void setSVNURL( String url ) {
        CheckArg.isNotEmpty(url, "SVNURL");
        this.svnURL = url;
    }

    public String getSVNUsername() {
        return this.svnUsername;
    }

    /**
     * @param username
     */
    public void setSVNUsername( String username ) {
        this.svnUsername = username;
    }

    public String getSVNPassword() {
        return this.svnPassword;
    }

    /**
     * @param password
     */
    public void setSVNPassword( String password ) {
        this.svnPassword = password;
    }

    /**
     * Get whether this source supports updates.
     * 
     * @return true if this source supports updates, or false if this source only supports reading content.
     */
    public boolean getSupportsUpdates() {
        return capabilities.supportsUpdates();
    }

    /**
     * Set whether this source supports updates.
     * 
     * @param supportsUpdates true if this source supports updating content, or false if this source only supports reading
     *        content.
     */
    public synchronized void setSupportsUpdates( boolean supportsUpdates ) {
        capabilities = new RepositorySourceCapabilities(capabilities.supportsSameNameSiblings(), supportsUpdates,
                                                        capabilities.supportsEvents(), capabilities.supportsCreatingWorkspaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = SVNRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        SVNURL svnURL = null;
        if (this.svnRepository == null) {
            try {
                svnURL = SVNURL.parseURIDecoded(getSVNURL());
                String usedProtocol = this.getSVNURL().substring(0, this.getSVNURL().indexOf(":"));
                if (usedProtocol.equals(SVNProtocol.SVN.value()) || usedProtocol.equals(SVNProtocol.SVN_SSH.value())) {
                    SVNRepositoryFactoryImpl.setup();
                    this.svnRepository = SVNRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                } else if (usedProtocol.equals(SVNProtocol.HTTP.value()) || usedProtocol.equals(SVNProtocol.HTTPS.value())) {
                    DAVRepositoryFactory.setup();
                    this.svnRepository = DAVRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                } else if (usedProtocol.equals(SVNProtocol.FILE.value())) {
                    FSRepositoryFactory.setup();
                    this.svnRepository = FSRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                } else {
                    // protocol not supported by this connector
                    throw new RepositorySourceException(getSVNURL(),
                                                        "Protocol is not supported by this connector or there is problem in the svn url");
                }

            } catch (SVNException ex) {
                I18n msg = SVNRepositoryConnectorI18n.propertyIsRequired;
                throw new RepositorySourceException(getSVNURL(), msg.text(this.getSVNURL()), ex);
            }
        }
        boolean supportsUpdates = getSupportsUpdates();
        return new SVNRepositoryConnection(this.getName(), this.getDefaultCachePolicy(), supportsUpdates, this.svnRepository);
    }

    protected Context getContext() {
        return this.jndiContext;
    }

    protected synchronized void setContext( Context context ) {
        this.jndiContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SVNRepositorySource) {
            SVNRepositorySource that = (SVNRepositorySource)obj;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
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
        if (getSVNURL() != null) {
            ref.add(new StringRefAddr(SVN_URL, getSVNURL()));
        }
        if (getSVNUsername() != null) {
            ref.add(new StringRefAddr(SVN_USERNAME, getSVNUsername()));
        }
        if (getSVNPassword() != null) {
            ref.add(new StringRefAddr(SVN_PASSWORD, getSVNPassword()));
        }
        if (getDefaultCachePolicy() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CachePolicy policy = getDefaultCachePolicy();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(policy);
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = SVNRepositoryConnectorI18n.errorSerializingCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context,
     *      java.util.Hashtable)
     */
    public Object getObjectInstance( Object obj,
                                     Name name,
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
            String svnURL = (String)values.get(SVN_URL);
            String svnUsername = (String)values.get(SVN_USERNAME);
            String svnPassword = (String)values.get(SVN_PASSWORD);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);

            // Create the source instance ...
            SVNRepositorySource source = new SVNRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (svnURL != null) source.setSVNURL(svnURL);
            if (svnUsername != null) source.setSVNUsername(svnUsername);
            if (svnPassword != null) source.setSVNPassword(svnPassword);
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

}
