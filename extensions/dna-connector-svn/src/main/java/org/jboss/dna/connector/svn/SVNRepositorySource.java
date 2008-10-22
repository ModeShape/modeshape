/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
import java.util.UUID;
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
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryContext;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceCapabilities;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.Property;
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
    public static final String DEFAULT_UUID_PROPERTY_NAME = DnaLexicon.UUID.getString();

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String UUID_PROPERTY_NAME = "uuidPropertyName";
    protected static final String SVN_REPOS_JNDI_NAME = "svnReposJndiName";
    protected static final String SVN_REPOS_FACTORY_JNDI_NAME = "svnReposFactoryJndiName";
    protected static final String SVN_URL = "svnURL";
    protected static final String SVN_USERNAME = "svnUsername";
    protected static final String SVN_PASSWORD = "svnPassword";
    protected static final String RETRY_LIMIT = "retryLimit";

    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private String name;
    private UUID rootNodeUuid = UUID.randomUUID();
    private String uuidPropertyName = DEFAULT_UUID_PROPERTY_NAME;
    private String svnURL;
    private String svnUsername;
    private String svnPassword;
    private CachePolicy defaultCachePolicy;

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
     * @see org.jboss.dna.graph.connectors.RepositorySource#initialize(org.jboss.dna.graph.connectors.RepositoryContext)
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
     * @see org.jboss.dna.graph.connectors.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#setRetryLimit(int)
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

    /**
     * Get the UUID of the root node for the cache. If the cache exists, this UUID is not used but is instead set to the UUID of
     * the existing root node.
     * 
     * @return the UUID of the root node for the cache.
     */
    public String getRootNodeUuid() {
        return this.rootNodeUuid.toString();
    }

    /**
     * Get the UUID of the root node for the cache. If the cache exists, this UUID is not used but is instead set to the UUID of
     * the existing root node.
     * 
     * @return the UUID of the root node for the cache.
     */
    public UUID getRootNodeUuidObject() {
        return this.rootNodeUuid;
    }

    /**
     * Set the UUID of the root node in this repository. If the cache exists, this UUID is not used but is instead set to the UUID
     * of the existing root node.
     * 
     * @param rootNodeUuid the UUID of the root node for the cache, or null if the UUID should be randomly generated
     */
    public synchronized void setRootNodeUuid( String rootNodeUuid ) {
        UUID uuid = null;
        if (rootNodeUuid == null) uuid = UUID.randomUUID();
        else uuid = UUID.fromString(rootNodeUuid);
        if (this.rootNodeUuid.equals(uuid)) return; // unchanged
        this.rootNodeUuid = uuid;
    }

    /**
     * Get the {@link Property#getName() property name} where the UUID is stored for each node.
     * 
     * @return the name of the UUID property; never null
     */
    public String getUuidPropertyName() {
        return this.uuidPropertyName;
    }

    /**
     * Set the {@link Property#getName() property name} where the UUID is stored for each node.
     * 
     * @param uuidPropertyName the name of the UUID property, or null if the {@link #DEFAULT_UUID_PROPERTY_NAME default name}
     *        should be used
     */
    public synchronized void setUuidPropertyName( String uuidPropertyName ) {
        if (uuidPropertyName == null || uuidPropertyName.trim().length() == 0) uuidPropertyName = DEFAULT_UUID_PROPERTY_NAME;
        if (this.uuidPropertyName.equals(uuidPropertyName)) return; // unchanged
        this.uuidPropertyName = uuidPropertyName;
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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return new Capabilities();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = SVNRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        if (getUuidPropertyName() == null) {
            I18n msg = SVNRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getUuidPropertyName(), msg.text("uuidPropertyName"));
        }
        SVNURL svnURL = null;
        if (this.svnRepository == null) {
            try {
                svnURL = SVNURL.parseURIDecoded(getSVNURL());
                String usedProtocol = this.getSVNURL().substring(0, this.getSVNURL().lastIndexOf(":"));
                if (usedProtocol.equals(SVNProtocol.SVN.value()) || usedProtocol.equals(SVNProtocol.SVN_SSH.value())) {
                    SVNRepositoryFactoryImpl.setup();
                    this.svnRepository = SVNRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                }
                if (usedProtocol.equals(SVNProtocol.HTTP.value()) || usedProtocol.equals(SVNProtocol.HTTPS.value())) {
                    DAVRepositoryFactory.setup();
                    this.svnRepository = DAVRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                }
                if (usedProtocol.equals(SVNProtocol.FILE.value())) {
                    FSRepositoryFactory.setup();
                    this.svnRepository = FSRepositoryFactory.create(svnURL);
                    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.getSVNUsername(),
                                                                                                         this.getSVNPassword());
                    this.svnRepository.setAuthenticationManager(authManager);
                }

            } catch (SVNException ex) {
                I18n msg = SVNRepositoryConnectorI18n.propertyIsRequired;
                throw new RepositorySourceException(getSVNURL(), msg.text(this.getSVNURL()), ex);
            }
        }
        return new SVNRepositoryConnection(this.getName(), this.getDefaultCachePolicy(), this.getUuidPropertyName(),
                                           this.svnRepository);
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
        if (getRootNodeUuid() != null) {
            ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid().toString()));
        }
        if (getUuidPropertyName() != null) {
            ref.add(new StringRefAddr(UUID_PROPERTY_NAME, getUuidPropertyName()));
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
            String rootNodeUuidString = (String)values.get(ROOT_NODE_UUID);
            String uuidPropertyName = (String)values.get(UUID_PROPERTY_NAME);
            String svnURL = (String)values.get(SVN_URL);
            String svnUsername = (String)values.get(SVN_USERNAME);
            String svnPassword = (String)values.get(SVN_PASSWORD);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);

            // Create the source instance ...
            SVNRepositorySource source = new SVNRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(rootNodeUuidString);
            if (uuidPropertyName != null) source.setUuidPropertyName(uuidPropertyName);
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

    protected class Capabilities implements RepositorySourceCapabilities {
        public boolean supportsSameNameSiblings() {
            return false;
        }

        public boolean supportsUpdates() {
            return true;
        }
    }
}
