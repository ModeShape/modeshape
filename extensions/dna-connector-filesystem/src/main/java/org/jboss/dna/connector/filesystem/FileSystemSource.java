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

package org.jboss.dna.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryContext;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceCapabilities;
import org.jboss.dna.graph.connectors.RepositorySourceException;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local file system as content in a repository.
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
    protected static final String FILE_SYSTEM_PATHS = "fileSystemPaths";

    /**
     * This source supports events.
     */
    protected static final boolean SUPPORTS_EVENTS = true;
    /**
     * This source supports same-name-siblings.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = true;
    /**
     * This source supports udpates by default, but each instance may be configured to {@link #setSupportsUpdates(boolean) be
     * read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = true;

    public static final int DEFAULT_RETRY_LIMIT = 0;
    public static final int DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS = 60 * 5; // 5 minutes

    private String name;
    private int retryLimit = DEFAULT_RETRY_LIMIT;
    private int cacheTimeToLiveInMilliseconds = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS * 1000;
    private String[] fileSystemPaths;
    private final Capabilities capabilities = new Capabilities();
    private transient CachePolicy cachePolicy;

    /**
     * 
     */
    public FileSystemSource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getName()
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
     * Get the file system paths to each directory or file that should be exposed immediately the root node nodes in this
     * connector. If not specified, all of the file system's root will be used.
     * 
     * @return the paths in the file system path to the top-level files and/or directories, or null if not yet set and the file
     *         system's roots should be used
     */
    public String[] getFileSystemPaths() {
        return fileSystemPaths;
    }

    /**
     * Set the file system paths to each directory or file that should be exposed immediately the root node nodes in this
     * connector. If not specified, all of the file system's root will be used.
     * 
     * @param fileSystemPaths the paths in the file system path to the top-level files and/or directories, or null if not yet set
     *        and the file system's roots should be used
     */
    public synchronized void setFileSystemPaths( String[] fileSystemPaths ) {
        this.fileSystemPaths = fileSystemPaths;
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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        if (limit < 0) limit = 0;
        this.retryLimit = limit;
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
     * @see org.jboss.dna.graph.connectors.RepositorySource#initialize(org.jboss.dna.graph.connectors.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        // No need to do anything
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
        String[] paths = getFileSystemPaths();
        if (paths != null && paths.length != 0) {
            ref.add(new StringRefAddr(FILE_SYSTEM_PATHS, StringUtil.combineLines(paths)));
        }
        ref.add(new StringRefAddr(CACHE_TIME_TO_LIVE_IN_MILLISECONDS, Integer.toString(getCacheTimeToLiveInMilliseconds())));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
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
            String combinedPaths = values.get(FILE_SYSTEM_PATHS);
            String[] fileSystemPaths = null;
            if (combinedPaths != null) {
                List<String> paths = StringUtil.splitLines(combinedPaths);
                fileSystemPaths = paths.toArray(new String[paths.size()]);
            }
            String cacheTtlInMillis = values.get(CACHE_TIME_TO_LIVE_IN_MILLISECONDS);
            String retryLimit = values.get(RETRY_LIMIT);

            // Create the source instance ...
            FileSystemSource source = new FileSystemSource();
            if (sourceName != null) source.setName(sourceName);
            if (fileSystemPaths != null) source.setFileSystemPaths(fileSystemPaths);
            if (cacheTtlInMillis != null) source.setCacheTimeToLiveInMilliseconds(Integer.parseInt(cacheTtlInMillis));
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        String sourceName = getName();
        if (sourceName == null || sourceName.trim().length() == 0) {
            I18n msg = FileSystemI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        Map<String, File> rootsByName = new HashMap<String, File>();
        String[] fileSystemPaths = getFileSystemPaths();
        if (fileSystemPaths != null && fileSystemPaths.length != 0) {
            // Find each of the paths ...
            List<String> pathsThatDontExist = new ArrayList<String>();
            for (String fileSystemPath : fileSystemPaths) {
                File root = new File(fileSystemPath);
                if (!root.exists()) {
                    pathsThatDontExist.add(fileSystemPath);
                } else {
                    rootsByName.put(root.getName(), root);
                }
            }
            if (!pathsThatDontExist.isEmpty()) {
                int count = pathsThatDontExist.size();
                I18n msg = count == 1 ? FileSystemI18n.fileSystemPathDoesNotExist : FileSystemI18n.fileSystemPathsDoNotExist;
                throw new RepositorySourceException(getName(), msg.text(getName(), pathsThatDontExist, count));
            }
        } else {
            // No file system paths specified, so get all of the file system's roots ...
            for (File root : File.listRoots()) {
                rootsByName.put(root.getName(), root);
            }
        }
        FilenameFilter filenameFilter = null;
        boolean supportsUpdates = getSupportsUpdates();
        return new FileSystemConnection(name, rootsByName, cachePolicy, filenameFilter, supportsUpdates);
    }

    @ThreadSafe
    protected class Capabilities extends RepositorySourceCapabilities {
        private final AtomicBoolean supportsUpdates = new AtomicBoolean(DEFAULT_SUPPORTS_UPDATES);

        /*package*/Capabilities() {
            super(SUPPORTS_SAME_NAME_SIBLINGS, DEFAULT_SUPPORTS_UPDATES, SUPPORTS_EVENTS);
        }

        /*package*/void setSupportsUpdates( boolean supportsUpdates ) {
            this.supportsUpdates.set(supportsUpdates);
        }

        @Override
        public boolean supportsUpdates() {
            return this.supportsUpdates.get();
        }
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
