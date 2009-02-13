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

package org.jboss.dna.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceCapabilities;
import org.jboss.dna.graph.connector.RepositorySourceException;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local file system as content in a repository. This
 * source considers a workspace name to be the path to the directory on the file system that represents the root of that
 * workspace. New workspaces can be created, as long as the names represent valid paths to existing directories.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class FileSystemSource implements RepositorySource, ObjectFactory {

    /**
     * The first serialized version of this source. Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String CACHE_TIME_TO_LIVE_IN_MILLISECONDS = "cacheTimeToLiveInMilliseconds";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";

    /**
     * This source supports events.
     */
    protected static final boolean SUPPORTS_EVENTS = true;
    /**
     * This source supports same-name-siblings.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = true;
    /**
     * This source does support creating workspaces.
     */
    protected static final boolean DEFAULT_SUPPORTS_CREATING_WORKSPACES = true;
    /**
     * This source does not support udpates by default, but each instance may be configured to be read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = false;

    public static final int DEFAULT_RETRY_LIMIT = 0;
    public static final int DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS = 60 * 5; // 5 minutes

    private volatile String name;
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;
    private volatile int cacheTimeToLiveInMilliseconds = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS * 1000;
    private volatile String defaultWorkspace;
    private volatile String[] predefinedWorkspaces = new String[] {};
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  DEFAULT_SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  DEFAULT_SUPPORTS_CREATING_WORKSPACES);
    private transient CachePolicy cachePolicy;
    private transient CopyOnWriteArraySet<String> availableWorkspaceNames;

    /**
     * 
     */
    public FileSystemSource() {
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
     * @see org.jboss.dna.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for the source
     * 
     * @param name the new name for the source
     */
    public synchronized void setName( String name ) {
        if (name != null) {
            name = name.trim();
            if (name.length() == 0) name = null;
        }
        this.name = name;
    }

    /**
     * Get whether this source supports updates.
     * 
     * @return true if this source supports updates, or false if this source only supports reading content.
     */
    public boolean getSupportsUpdates() {
        return capabilities.supportsUpdates();
    }

    // /**
    // * Set whether this source supports updates.
    // *
    // * @param supportsUpdates true if this source supports updating content, or false if this source only supports reading
    // * content.
    // */
    // public synchronized void setSupportsUpdates( boolean supportsUpdates ) {
    // capabilities = new RepositorySourceCapabilities(SUPPORTS_SAME_NAME_SIBLINGS,
    // supportsUpdates,
    // SUPPORTS_EVENTS,
    // capabilities.supportsCreatingWorkspaces());
    // }

    /**
     * Get the file system path to the existing directory that should be used for the default workspace. If the default is
     * specified as a null String or is not a valid and resolvable path, this source will consider the default to be the current
     * working directory of this virtual machine, as defined by the <code>new File(".")</code>.
     * 
     * @return the file system path to the directory representing the default workspace, or null if the default should be the
     *         current working directory
     */
    public String getDirectoryForDefaultWorkspace() {
        return defaultWorkspace;
    }

    /**
     * Set the file system path to the existing directory that should be used for the default workspace. If the default is
     * specified as a null String or is not a valid and resolvable path, this source will consider the default to be the current
     * working directory of this virtual machine, as defined by the <code>new File(".")</code>.
     * 
     * @param pathToDirectoryForDefaultWorkspace the valid and resolvable file system path to the directory representing the
     *        default workspace, or null if the current working directory should be used as the default workspace
     */
    public synchronized void setDirectoryForDefaultWorkspace( String pathToDirectoryForDefaultWorkspace ) {
        this.defaultWorkspace = pathToDirectoryForDefaultWorkspace;
    }

    /**
     * Gets the names of the workspaces that are available when this source is created. Each workspace name corresponds to a path
     * to a directory on the file system.
     * 
     * @return the names of the workspaces that this source starts with, or null if there are no such workspaces
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    public synchronized String[] getPredefinedWorkspaceNames() {
        String[] copy = new String[predefinedWorkspaces.length];
        System.arraycopy(predefinedWorkspaces, 0, copy, 0, predefinedWorkspaces.length);
        return copy;
    }

    /**
     * Sets the names of the workspaces that are available when this source is created. Each workspace name corresponds to a path
     * to a directory on the file system.
     * 
     * @param predefinedWorkspaceNames the names of the workspaces that this source should start with, or null if there are no
     *        such workspaces
     * @see #setCreatingWorkspacesAllowed(boolean)
     * @see #getPredefinedWorkspaceNames()
     */
    public synchronized void setPredefinedWorkspaceNames( String[] predefinedWorkspaceNames ) {
        this.predefinedWorkspaces = predefinedWorkspaceNames;
    }

    /**
     * Get whether this source allows workspaces to be created dynamically.
     * 
     * @return true if this source allows workspaces to be created by clients, or false if the set of workspaces is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    public boolean isCreatingWorkspacesAllowed() {
        return capabilities.supportsCreatingWorkspaces();
    }

    /**
     * Set whether this source allows workspaces to be created dynamically.
     * 
     * @param allowWorkspaceCreation true if this source allows workspaces to be created by clients, or false if the set of
     *        workspaces is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #isCreatingWorkspacesAllowed()
     */
    public synchronized void setCreatingWorkspacesAllowed( boolean allowWorkspaceCreation ) {
        capabilities = new RepositorySourceCapabilities(capabilities.supportsSameNameSiblings(), capabilities.supportsUpdates(),
                                                        capabilities.supportsEvents(), allowWorkspaceCreation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        this.retryLimit = limit < 0 ? 0 : limit;
    }

    /**
     * Get the time in milliseconds that content returned from this source may used while in the cache.
     * 
     * @return the time to live, in milliseconds, or 0 if the time to live is not specified by this source
     */
    public int getCacheTimeToLiveInMilliseconds() {
        return cacheTimeToLiveInMilliseconds;
    }

    /**
     * Set the time in milliseconds that content returned from this source may used while in the cache.
     * 
     * @param cacheTimeToLive the time to live, in milliseconds; 0 if the time to live is not specified by this source; or a
     *        negative number for the default value
     */
    public synchronized void setCacheTimeToLiveInMilliseconds( int cacheTimeToLive ) {
        if (cacheTimeToLive < 0) cacheTimeToLive = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS;
        this.cacheTimeToLiveInMilliseconds = cacheTimeToLive;
        this.cachePolicy = cacheTimeToLiveInMilliseconds > 0 ? new FileSystemCachePolicy(cacheTimeToLiveInMilliseconds) : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#initialize(org.jboss.dna.graph.connector.RepositoryContext)
     */
    public synchronized void initialize( RepositoryContext context ) throws RepositorySourceException {
        // No need to do anything
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
        ref.add(new StringRefAddr(CACHE_TIME_TO_LIVE_IN_MILLISECONDS, Integer.toString(getCacheTimeToLiveInMilliseconds())));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDirectoryForDefaultWorkspace()));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
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
            Map<String, String> values = new HashMap<String, String>();
            Reference ref = (Reference)obj;
            Enumeration<?> en = ref.getAll();
            while (en.hasMoreElements()) {
                RefAddr subref = (RefAddr)en.nextElement();
                if (subref instanceof StringRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value != null) values.put(key, value.toString());
                }
            }
            String sourceName = values.get(SOURCE_NAME);
            String cacheTtlInMillis = values.get(CACHE_TIME_TO_LIVE_IN_MILLISECONDS);
            String retryLimit = values.get(RETRY_LIMIT);
            String defaultWorkspace = values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = values.get(ALLOW_CREATING_WORKSPACES);

            String combinedWorkspaceNames = values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            FileSystemSource source = new FileSystemSource();
            if (sourceName != null) source.setName(sourceName);
            if (cacheTtlInMillis != null) source.setCacheTimeToLiveInMilliseconds(Integer.parseInt(cacheTtlInMillis));
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            if (defaultWorkspace != null) source.setDirectoryForDefaultWorkspace(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        String sourceName = getName();
        if (sourceName == null || sourceName.trim().length() == 0) {
            I18n msg = FileSystemI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }

        boolean reportWarnings = false;
        if (this.availableWorkspaceNames == null) {
            // Set up the predefined workspace names ...
            this.availableWorkspaceNames = new CopyOnWriteArraySet<String>();
            for (String predefined : this.predefinedWorkspaces) {
                this.availableWorkspaceNames.add(predefined);
            }

            // Report the warnings for non-existant predefined workspaces
            reportWarnings = true;
            for (String path : this.availableWorkspaceNames) {
                // Look for the file at this path ...
                File file = new File(path);
                if (!file.exists()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceDoesNotExist, path, name);
                } else if (!file.isDirectory()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceIsNotDirectory, path, name);
                } else if (!file.canRead()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceCannotBeRead, path, name);
                }
            }
        }

        FilenameFilter filenameFilter = null;
        boolean supportsUpdates = getSupportsUpdates();
        File defaultWorkspace = new File(".");
        String path = getDirectoryForDefaultWorkspace();
        if (path != null) {
            // Look for the file at this path ...
            File file = new File(path);
            I18n warning = null;
            if (!file.exists()) {
                warning = FileSystemI18n.pathForDefaultWorkspaceDoesNotExist;
            } else if (!file.isDirectory()) {
                warning = FileSystemI18n.pathForDefaultWorkspaceIsNotDirectory;
            } else if (!file.canRead()) {
                warning = FileSystemI18n.pathForDefaultWorkspaceCannotBeRead;
            } else {
                // good to use!
                defaultWorkspace = file;
            }
            if (reportWarnings && warning != null) {
                Logger.getLogger(getClass()).warn(warning, path, name);
            }
        }
        this.availableWorkspaceNames.add(defaultWorkspace.getPath());
        return new FileSystemConnection(name, defaultWorkspace, availableWorkspaceNames, isCreatingWorkspacesAllowed(),
                                        cachePolicy, filenameFilter, supportsUpdates);
    }

    @Immutable
    /*package*/class FileSystemCachePolicy implements CachePolicy {
        private static final long serialVersionUID = 1L;
        private final int ttl;

        /*package*/FileSystemCachePolicy( int ttl ) {
            this.ttl = ttl;
        }

        public long getTimeToLive() {
            return ttl;
        }

    }

}
