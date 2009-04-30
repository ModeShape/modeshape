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
package org.jboss.dna.connector.jdbc;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.jdbc.provider.DataSourceDatabaseMetadataProvider;
import org.jboss.dna.common.jdbc.provider.DefaultDataSourceDatabaseMetadataProvider;
import org.jboss.dna.common.jdbc.provider.DefaultDriverDatabaseMetadataProvider;
import org.jboss.dna.common.jdbc.provider.DriverDatabaseMetadataProvider;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceCapabilities;
import org.jboss.dna.graph.connector.RepositorySourceException;

/**
 * A description of a JDBC resource that can be used to access database information.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class JdbcRepositorySource implements RepositorySource, ObjectFactory {
    private static final long serialVersionUID = 3380130639143030018L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * This source supports updates by default, but each instance may be configured to {@link #setSupportsUpdates(boolean) be
     * read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = true;
    /**
     * The default UUID that is used for root nodes in a JDBC connector.
     */
    public static final String DEFAULT_ROOT_NODE_UUID = "9f9a52c8-0a4d-40d0-ac58-7c77b24b3155";

    /**
     * This source supports events.
     */
    protected static final boolean SUPPORTS_EVENTS = true;
    /**
     * This source does not support same-name-siblings.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = false;

    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    protected String name;
    protected final Capabilities capabilities = new Capabilities();
    protected transient RepositoryContext repositoryContext;
    protected CachePolicy defaultCachePolicy;
    protected transient DriverDatabaseMetadataProvider driverProvider;
    protected transient DataSourceDatabaseMetadataProvider dataSourceProvider;
    protected transient UUID rootUuid = UUID.fromString(DEFAULT_ROOT_NODE_UUID);
    
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String DATA_SOURCE_JNDI_NAME = "dataSourceJndiName";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String URL = "url";
    protected static final String DRIVER_CLASS_NAME = "driverClassName";
    protected static final String RETRY_LIMIT = "retryLimit";

    /**
     * Get and optionally create driver based provider 
     * @param create create provider
     * @return driverProvider
     */
    protected DriverDatabaseMetadataProvider getDriverProvider(boolean create) {
        // lazy creation
        if (driverProvider == null) {
            driverProvider = new DefaultDriverDatabaseMetadataProvider();
        }
        return driverProvider;
    }

    /**
     * Get and optionally create data source based provider 
     * @param create create provider
     * @return dataSourceProvider
     */
    protected DataSourceDatabaseMetadataProvider getDataSourceProvider(boolean create) {
        // lazy creation
        if (dataSourceProvider == null && create) {
            dataSourceProvider = new DefaultDataSourceDatabaseMetadataProvider();
        }
        return dataSourceProvider;
    }
    
    /**
     * default constructor
     */
    public JdbcRepositorySource() {
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
     * @see org.jboss.dna.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        String errMsg = null;
        // check name
        if (getName() == null) {
            errMsg = JdbcMetadataI18n.propertyIsRequired.text("name");
            throw new RepositorySourceException(errMsg);
        }
        
        // create Jdbc connection using data source first
        try {
            if (dataSourceProvider != null) {
                // create wrapper for Jdbc connection
                return new JdbcConnection(getName(),
                                          getDefaultCachePolicy(),
                                          dataSourceProvider.getConnection(),
                                          rootUuid);
            }
        } catch (Exception e) {
            errMsg = JdbcMetadataI18n.unableToGetConnectionUsingDriver.text(getName(), getDriverClassName(), getDatabaseUrl());
            throw new RepositorySourceException(errMsg, e);
        }

        // create Jdbc connection using driver and database URL
        try {
            if (driverProvider != null) {
                // create wrapper for Jdbc connection
                return new JdbcConnection(getName(),
                                          getDefaultCachePolicy(),
                                          driverProvider.getConnection(),
                                          rootUuid);
            }
        } catch (Exception e) {
            errMsg = JdbcMetadataI18n.unableToGetConnectionUsingDataSource.text(getName(), getDataSourceName());
            throw new RepositorySourceException(errMsg, e);
        }
        
        // Either data source name or JDBC driver connection properties must be defined
        errMsg = JdbcMetadataI18n.oneOfPropertiesIsRequired.text(getName());
        throw new RepositorySourceException(errMsg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this source
     * 
     * @param name the name for this source
     */
    public void setName( String name ) {
        this.name = name;
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
     * @see org.jboss.dna.graph.connector.RepositorySource#initialize(org.jboss.dna.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
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
        capabilities.setSupportsUpdates(supportsUpdates);
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
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * @return rootNodeUuid
     */
    public String getRootNodeUuid() {
        return rootUuid != null? rootUuid.toString() : null;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     * @throws IllegalArgumentException if the string value cannot be converted to UUID
     */
    public void setRootNodeUuid( String rootNodeUuid ) {
        if (rootNodeUuid != null && rootNodeUuid.trim().length() == 0) rootNodeUuid = DEFAULT_ROOT_NODE_UUID;
        this.rootUuid = UUID.fromString(rootNodeUuid);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof JdbcRepositorySource) {
            JdbcRepositorySource that = (JdbcRepositorySource)obj;
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
     * Gets JDBC driver class name
     * 
     * @return the JDBC driver class name if any
     */
    public String getDriverClassName() {
        // get provider
        DriverDatabaseMetadataProvider provider = getDriverProvider(false);
        // return 
        return (provider != null)? provider.getDriverClassName() : null;
    }
  
    /**
     * Sets JDBC driver class name
     * 
     * @param driverClassName the JDBC driver class name
     */
    public void setDriverClassName( String driverClassName ) {
        if (driverClassName == null) {
            driverProvider = null;
        } else {
            // get/create provider
            DriverDatabaseMetadataProvider provider = getDriverProvider(true);
            // set
            provider.setDriverClassName(driverClassName);
        }
    }
    
    /**
     * Gets database URL as string
     * 
     * @return database URL as string
     */
    public String getDatabaseUrl() {
        // get provider
        DriverDatabaseMetadataProvider provider = getDriverProvider(false);
        // return 
        return (provider != null)? provider.getDatabaseUrl() : null;
    }

    /**
     * Sets the database URL as string
     * 
     * @param databaseUrl the database URL as string
     */
    public void setDatabaseUrl( String databaseUrl ) {
        if (databaseUrl == null) {
            driverProvider = null;
        } else {
            // get/create provider
            DriverDatabaseMetadataProvider provider = getDriverProvider(true);
            // set
            provider.setDatabaseUrl(databaseUrl);
        }
    }
    
    /**
     * Gets the user name
     * 
     * @return the user name
     */
    public String getUserName() {
        // get provider
        DriverDatabaseMetadataProvider provider = getDriverProvider(false);
        return (provider != null)? provider.getUserName() : null;
    }

    /**
     * Sets the user name
     * 
     * @param userName the user name
     */
    public void setUserName( String userName ) {
        if (userName == null) {
            driverProvider = null;
        } else {
            // get provider
            DriverDatabaseMetadataProvider provider = getDriverProvider(true);
            provider.setUserName(userName);
        }
    }
    
    /**
     * Get user's password
     * 
     * @return user's password
     */
    public String getPassword() {
        // get provider
        DriverDatabaseMetadataProvider provider = getDriverProvider(false);
        return (provider != null)? provider.getPassword() : null;
     }

    /**
     * Sets the user's password
     * 
     * @param password the user's password
     */
    public void setPassword( String password ) {
        if (password == null) {
            driverProvider = null;
        } else {
            // get provider
            DriverDatabaseMetadataProvider provider = getDriverProvider(true);
            provider.setPassword(password);
        }
    }
    
    /**
     * Sets data source JNDI name
     * 
     * @return data source JNDI name
     */
    public String getDataSourceName() {
        // get provider
        DataSourceDatabaseMetadataProvider provider = getDataSourceProvider(false);
        return (provider != null)? provider.getDataSourceName() : null;
    }

    /**
     * Sets data source JNDI name
     * 
     * @param dataSourceName the data source JNDI name
     */
    public void setDataSourceName( String dataSourceName ) {
        if (dataSourceName == null) {
            dataSourceProvider = null;
        } else {
            // get provider
            DataSourceDatabaseMetadataProvider provider = getDataSourceProvider(true);
            provider.setDataSourceName(dataSourceName);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        }
        
        if (getRootNodeUuid() != null) {
            ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid()));
        }
        if (getDataSourceName() != null) {
            ref.add(new StringRefAddr(DATA_SOURCE_JNDI_NAME, getDataSourceName()));
        }
        
        if (getUserName() != null) {
            ref.add(new StringRefAddr(USERNAME, getUserName()));
        }
        
        if (getPassword() != null) {
            ref.add(new StringRefAddr(PASSWORD, getPassword()));
        }
        
        if (getDatabaseUrl() != null) {
            ref.add(new StringRefAddr(URL, getDatabaseUrl()));
        }
        if (getDriverClassName() != null) {
            ref.add(new StringRefAddr(DRIVER_CLASS_NAME, getDriverClassName()));
        }
        
        if (getDefaultCachePolicy() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CachePolicy policy = getDefaultCachePolicy();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(policy);
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = JdbcMetadataI18n.errorSerializingCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        // return it
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
            // get individual properties
            String sourceName = (String)values.get(SOURCE_NAME);
            String rootNodeUuid = (String)values.get(ROOT_NODE_UUID);
            String dataSourceJndiName = (String)values.get(DATA_SOURCE_JNDI_NAME);
            String userName = (String)values.get(USERNAME);
            String password = (String)values.get(PASSWORD);
            String url = (String)values.get(URL);
            String driverClassName = (String)values.get(DRIVER_CLASS_NAME);
            
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);

            // Create the source instance ...
            JdbcRepositorySource source = new JdbcRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuid != null) source.setRootNodeUuid(rootNodeUuid);
            if (dataSourceJndiName != null) source.setDataSourceName(dataSourceJndiName);
            if (userName != null) source.setUserName(userName);
            if (password != null) source.setPassword(password);
            if (url != null) source.setDatabaseUrl(url);
            if (driverClassName != null) source.setDriverClassName(driverClassName);
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    @ThreadSafe
    protected class Capabilities extends RepositorySourceCapabilities {
        private final AtomicBoolean supportsUpdates = new AtomicBoolean(DEFAULT_SUPPORTS_UPDATES);

        /*package*/Capabilities() {
            super(DEFAULT_SUPPORTS_UPDATES, SUPPORTS_EVENTS);
        }

        /*package*/void setSupportsUpdates( boolean supportsUpdates ) {
            this.supportsUpdates.set(supportsUpdates);
        }

        @Override
        public boolean supportsUpdates() {
            return this.supportsUpdates.get();
        }
    }
}
