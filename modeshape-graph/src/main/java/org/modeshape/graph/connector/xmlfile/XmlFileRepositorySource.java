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
package org.modeshape.graph.connector.xmlfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.annotation.ReadOnly;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Observer;
import org.xml.sax.SAXException;

/**
 * A {@link RepositorySource} for a in-memory repository with content defined by an XML file. Note that any changes made to the
 * content are not currently persisted back to the XML file.
 */
@ThreadSafe
public class XmlFileRepositorySource implements RepositorySource, ObjectFactory {

    /**
     * The initial version is 1
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default name for the workspace used by this source, which is a blank string.
     */
    public static final String DEFAULT_WORKSPACE_NAME = "";

    protected static final RepositorySourceCapabilities CAPABILITIES = new RepositorySourceCapabilities(true, false, false,
                                                                                                        false, true);

    protected static final String CONTENT_ATTR = "content";
    protected static final String SOURCE_NAME_ATTR = "sourceName";
    protected static final String DEFAULT_WORKSPACE_NAME_ATTR = "defaultWorkspaceName";
    protected static final String RETRY_LIMIT_ATTR = "retryLimit";

    @Description( i18n = GraphI18n.class, value = "namePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "namePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "namePropertyCategory" )
    @GuardedBy( "sourcesLock" )
    private String name;

    @Description( i18n = GraphI18n.class, value = "contentPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "contentPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "contentPropertyCategory" )
    @GuardedBy( "this" )
    private String content;

    @Description( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private String defaultWorkspaceName = DEFAULT_WORKSPACE_NAME;

    @Description( i18n = GraphI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "retryLimitPropertyCategory" )
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);

    private transient InMemoryRepositorySource inMemorySource;
    private transient ExecutionContext defaultContext = new ExecutionContext();
    private transient RepositoryContext repositoryContext = new DefaultRepositoryContext();

    protected class DefaultRepositoryContext implements RepositoryContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
         */
        @SuppressWarnings( "synthetic-access" )
        public ExecutionContext getExecutionContext() {
            return defaultContext;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
         */
        public Subgraph getConfiguration( int depth ) {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
         */
        public Observer getObserver() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
         */
        public RepositoryConnectionFactory getRepositoryConnectionFactory() {
            return null;
        }
    }

    /**
     * Create a repository source instance.
     */
    public XmlFileRepositorySource() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context != null ? context : new DefaultRepositoryContext();
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * Get the name of the workspace that should be used by default.
     * 
     * @return the name of the default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }

    /**
     * Set the default workspace name.
     * 
     * @param defaultWorkspaceName the name of the workspace that should be used by default, or null if "" should be used
     */
    public void setDefaultWorkspaceName( String defaultWorkspaceName ) {
        this.defaultWorkspaceName = defaultWorkspaceName != null ? defaultWorkspaceName : DEFAULT_WORKSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Get the location where the initial content is defined.
     * 
     * @return the URL, file path, or classpath resource path to the file containing the content, or null if there is no such
     *         content
     */
    public String getContentLocation() {
        return content;
    }

    /**
     * Set the location where the initial content is defined.
     * 
     * @param uriOrFilePathOrResourcePath the URL, file path, or classpath resource path to the file containing the content
     */
    public void setContentLocation( String uriOrFilePathOrResourcePath ) {
        this.content = uriOrFilePathOrResourcePath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (inMemorySource == null) {
            // Initialize the source and load the content ...
            inMemorySource = new InMemoryRepositorySource();
            inMemorySource.setName(name);

            if (content != null && content.length() != 0) {
                try {
                    ExecutionContext context = repositoryContext != null ? repositoryContext.getExecutionContext() : defaultContext;
                    Graph graph = Graph.create(inMemorySource, context);
                    graph.useWorkspace(defaultWorkspaceName);
                    graph.importXmlFrom(content).into("/");
                } catch (IOException e) {
                    inMemorySource = null;
                    throw new RepositorySourceException(getName(), e);
                } catch (SAXException e) {
                    inMemorySource = null;
                    throw new RepositorySourceException(getName(), e);
                } catch (RepositorySourceException e) {
                    inMemorySource = null;
                    throw e;
                } catch (RuntimeException e) {
                    inMemorySource = null;
                    throw e;
                }
            }
        }
        return inMemorySource.getConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        // Null the reference to the in-memory repository; open connections still reference it and can continue to work ...
        this.inMemorySource = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME_ATTR, getName()));
        }
        if (getContentLocation() != null) {
            ref.add(new StringRefAddr(CONTENT_ATTR, getContentLocation().toString()));
        }
        if (getDefaultWorkspaceName() != null) {
            ref.add(new StringRefAddr(DEFAULT_WORKSPACE_NAME_ATTR, getDefaultWorkspaceName()));
        }
        ref.add(new StringRefAddr(RETRY_LIMIT_ATTR, Integer.toString(getRetryLimit())));
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
            String sourceName = (String)values.get(SOURCE_NAME_ATTR);
            String contentLocation = (String)values.get(CONTENT_ATTR);
            String defaultWorkspaceName = (String)values.get(DEFAULT_WORKSPACE_NAME_ATTR);
            String retryLimit = (String)values.get(RETRY_LIMIT_ATTR);

            // Create the source instance ...
            XmlFileRepositorySource source = new XmlFileRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (contentLocation != null) source.setContentLocation(contentLocation);
            if (defaultWorkspaceName != null) source.setDefaultWorkspaceName(defaultWorkspaceName);
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Description( i18n = GraphI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "updatesAllowedPropertyCategory" )
    @ReadOnly
    public boolean areUpdatesAllowed() {
        return getCapabilities().supportsUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "The \"" + name + "\" XML file repository";
    }
}
