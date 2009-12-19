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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.DnaIntLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceCapabilities;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Property;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local file system as content in a repository. This
 * source considers a workspace name to be the path to the directory on the file system that represents the root of that
 * workspace. New workspaces can be created, as long as the names represent valid paths to existing directories.
 */
@ThreadSafe
public class FileSystemSource implements RepositorySource, ObjectFactory {

    /**
     * An immutable {@link CustomPropertiesFactory} implementation that is used by default when none is provided. Note that this
     * implementation does restrict the properties that can be placed on file, folder and resource nodes.
     */
    protected static CustomPropertiesFactory DEFAULT_PROPERTIES_FACTORY = new StandardPropertiesFactory();

    /**
     * The first serialized version of this source. Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The initial {@link #getDefaultWorkspaceName() name of the default workspace} is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_WORKSPACE = "default";

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String CACHE_TIME_TO_LIVE_IN_MILLISECONDS = "cacheTimeToLiveInMilliseconds";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String WORKSPACE_ROOT = "workspaceRootPath";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    protected static final String MAX_PATH_LENGTH = "maxPathLength";
    protected static final String EXCLUSION_PATTERN = "exclusionPattern";
    protected static final String CUSTOM_PROPERTY_FACTORY = "customPropertyFactory";

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
     * This source does not support updates by default, but each instance may be configured to be read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = false;

    /**
     * This source supports creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = false;

    public static final int DEFAULT_RETRY_LIMIT = 0;
    public static final int DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS = 60 * 5; // 5 minutes
    public static final int DEFAULT_MAX_PATH_LENGTH = 255; // 255 for windows users
    public static final String DEFAULT_EXCLUSION_PATTERN = null;

    private volatile String name;
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;
    private volatile int cacheTimeToLiveInMilliseconds = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS * 1000;
    private volatile String defaultWorkspaceName = DEFAULT_NAME_OF_DEFAULT_WORKSPACE;
    private volatile String workspaceRootPath;
    private volatile String[] predefinedWorkspaces = new String[] {};
    private volatile UUID rootNodeUuid = UUID.randomUUID();
    private volatile int maxPathLength = DEFAULT_MAX_PATH_LENGTH;
    private volatile String exclusionPattern = DEFAULT_EXCLUSION_PATTERN;
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(
                                                                                                  SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  DEFAULT_SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  DEFAULT_SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES);
    private transient CachePolicy cachePolicy;
    private transient Map<String, File> availableWorkspaces;
    private volatile CustomPropertiesFactory customPropertiesFactory;

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
    public boolean getUpdatesAllowed() {
        return capabilities.supportsUpdates();
    }

    /**
     * Get the relative root directory for the workspaces. If this property is set, workspaces can be given as relative paths from
     * this directory and all workspace paths must be ancestors of this path.
     * 
     * @return the root directory for workspaces
     */
    public String getWorkspaceRootPath() {
        return workspaceRootPath;
    }

    /**
     * Sets the relative root directory for workspaces
     * 
     * @param workspaceRootPath the relative root directory for workspaces. If this value is non-null, all workspace paths will be
     *        treated as paths relative to this directory
     */
    public synchronized void setWorkspaceRootPath( String workspaceRootPath ) {
        this.workspaceRootPath = workspaceRootPath;
    }

    /**
     * Get the regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored
     * 
     * @return the regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored
     */
    public String getExclusionPattern() {
        return exclusionPattern;
    }

    /**
     * Sets the regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored
     * 
     * @param exclusionPattern the regular expression that, if matched by a file or folder, indicates that the file or folder
     *        should be ignored. If this pattern is {@code null}, no files will be excluded.
     */
    public synchronized void setExclusionPattern( String exclusionPattern ) {
        this.exclusionPattern = exclusionPattern;
    }

    /**
     * Get the UUID that is used for the root node of each workspace
     * 
     * @return the UUID that is used for the root node of each workspace
     */
    public UUID getRootNodeUuid() {
        return rootNodeUuid;
    }

    /**
     * Set the {@code jcr:uuid} property of the root node in each workspace to the given value.
     * 
     * @param rootNodeUuid the UUID to use for the root nodes of all workspaces
     */
    public synchronized void setRootNodeUuid( String rootNodeUuid ) {
        CheckArg.isNotNull(rootNodeUuid, "rootNodeUuid");
        this.rootNodeUuid = UUID.fromString(rootNodeUuid);
    }

    /**
     * Get the UUID that is used for the root node of each workspace
     * 
     * @return the UUID that is used for the root node of each workspace
     */
    public int getMaxPathLength() {
        return maxPathLength;
    }

    /**
     * Set the maximum absolute path length supported by this source.
     * <p>
     * The length of any path is calculated relative to the file system root, NOT the repository root. That is, if a workspace
     * {@code foo} is mapped to the {@code /tmp/foo/bar} directory on the file system, then the path {@code /node1/node2} in the
     * {@code foo} workspace has an effective length of 23 for the purposes of the {@code maxPathLength} calculation ({@code
     * /tmp/foo/bar} has length 11, {@code /node1/node2} has length 12, 11 + 12 = 23).
     * </p>
     * 
     * @param maxPathLength the maximum absolute path length supported by this source; must be non-negative
     */
    public synchronized void setMaxPathLength( int maxPathLength ) {
        CheckArg.isNonNegative(maxPathLength, "maxPathLength");
        this.maxPathLength = maxPathLength;
    }

    /**
     * Get the name of the default workspace.
     * 
     * @return the name of the workspace that should be used by default; never null
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }

    /**
     * Set the name of the workspace that should be used when clients don't specify a workspace.
     * 
     * @param nameOfDefaultWorkspace the name of the workspace that should be used by default, or null if the
     *        {@link #DEFAULT_NAME_OF_DEFAULT_WORKSPACE default name} should be used
     */
    public synchronized void setDefaultWorkspaceName( String nameOfDefaultWorkspace ) {
        this.defaultWorkspaceName = nameOfDefaultWorkspace != null ? nameOfDefaultWorkspace : DEFAULT_NAME_OF_DEFAULT_WORKSPACE;
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
                                                        capabilities.supportsEvents(), allowWorkspaceCreation,
                                                        capabilities.supportsReferences());
    }

    /**
     * Get whether this source allows updates.
     * 
     * @return true if this source allows updates by clients, or false if no updates are allowed
     * @see #setUpdatesAllowed(boolean)
     */
    public boolean areUpdatesAllowed() {
        return capabilities.supportsUpdates();
    }

    /**
     * Set whether this source allows updates to data within workspaces
     * 
     * @param allowUpdates true if this source allows updates to data within workspaces clients, or false if updates are not
     *        allowed.
     * @see #areUpdatesAllowed()
     */
    public synchronized void setUpdatesAllowed( boolean allowUpdates ) {
        capabilities = new RepositorySourceCapabilities(capabilities.supportsSameNameSiblings(), allowUpdates,
                                                        capabilities.supportsEvents(), capabilities.supportsCreatingWorkspaces(),
                                                        capabilities.supportsReferences());
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
     * Get the factory that is used to create custom properties on "nt:folder", "nt:file", and "nt:resource" nodes.
     * 
     * @return the factory, or null if no custom properties are to be created
     */
    public synchronized CustomPropertiesFactory getCustomPropertiesFactory() {
        return customPropertiesFactory;
    }

    /**
     * Set the factory that is used to create custom properties on "nt:folder", "nt:file", and "nt:resource" nodes.
     * 
     * @param customPropertiesFactory the factory reference, or null if no custom properties will be created
     */
    public synchronized void setCustomPropertiesFactory( CustomPropertiesFactory customPropertiesFactory ) {
        this.customPropertiesFactory = customPropertiesFactory;
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
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        ref.add(new StringRefAddr(EXCLUSION_PATTERN, exclusionPattern));
        ref.add(new StringRefAddr(MAX_PATH_LENGTH, String.valueOf(maxPathLength)));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
        }
        if (getCustomPropertiesFactory() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CustomPropertiesFactory factory = getCustomPropertiesFactory();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(factory);
                ref.add(new BinaryRefAddr(CUSTOM_PROPERTY_FACTORY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = FileSystemI18n.errorSerializingCustomPropertiesFactory;
                throw new RepositorySourceException(getName(), msg.text(factory.getClass().getName(), getName()), e);
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
            String cacheTtlInMillis = (String)values.get(CACHE_TIME_TO_LIVE_IN_MILLISECONDS);
            String retryLimit = (String)values.get(RETRY_LIMIT);
            String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = (String)values.get(ALLOW_CREATING_WORKSPACES);
            String exclusionPattern = (String)values.get(EXCLUSION_PATTERN);
            String maxPathLength = (String)values.get(DEFAULT_MAX_PATH_LENGTH);
            Object customPropertiesFactory = values.get(CUSTOM_PROPERTY_FACTORY);

            String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
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
            if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (exclusionPattern != null) source.setExclusionPattern(exclusionPattern);
            if (maxPathLength != null) source.setMaxPathLength(Integer.valueOf(maxPathLength));
            if (customPropertiesFactory != null) source.setCustomPropertiesFactory((CustomPropertiesFactory)customPropertiesFactory);
            return source;
        }
        return null;
    }

    private String pathFor( String workspaceName ) {
        String path = workspaceName;
        if (this.workspaceRootPath != null) {
            if (this.workspaceRootPath.charAt(workspaceRootPath.length() - 1) == File.separatorChar) {
                path = this.workspaceRootPath + workspaceName;
            }
            path = this.workspaceRootPath + File.separatorChar + workspaceName;
        }

        return path;
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

        if (this.availableWorkspaces == null) {
            // Set up the predefined workspace names ...
            this.availableWorkspaces = new ConcurrentHashMap<String, File>();
            for (String predefined : this.predefinedWorkspaces) {
                // Look for the file at this path ...
                File file = new File(pathFor(predefined));
                if (!file.exists()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceDoesNotExist, predefined, name);
                } else if (!file.isDirectory()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceIsNotDirectory, predefined, name);
                } else if (!file.canRead()) {
                    Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceCannotBeRead, predefined, name);
                }

                this.availableWorkspaces.put(predefined, file);
            }
        }

        if (defaultWorkspaceName != null) {
            // Look for the file at this path ...
            File file = new File(pathFor(defaultWorkspaceName));
            if (!file.exists()) {
                Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceDoesNotExist,
                                                  defaultWorkspaceName,
                                                  name);
            } else if (!file.isDirectory()) {
                Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceIsNotDirectory,
                                                  defaultWorkspaceName,
                                                  name);
            } else if (!file.canRead()) {
                Logger.getLogger(getClass()).warn(FileSystemI18n.pathForPredefinedWorkspaceCannotBeRead,
                                                  defaultWorkspaceName,
                                                  name);
            }

            this.availableWorkspaces.put(defaultWorkspaceName, file);
        }

        FilenameFilter filenameFilter = null;
        if (exclusionPattern != null) {
            final String filterPattern = exclusionPattern;
            filenameFilter = new FilenameFilter() {
                Pattern filter = Pattern.compile(filterPattern);

                public boolean accept( File dir,
                                       String name ) {
                    return !filter.matcher(name).matches();
                }
            };
        }

        CustomPropertiesFactory propFactory = customPropertiesFactory != null ? customPropertiesFactory : DEFAULT_PROPERTIES_FACTORY;
        return new FileSystemConnection(name, defaultWorkspaceName, availableWorkspaces, isCreatingWorkspacesAllowed(),
                                        cachePolicy, rootNodeUuid, workspaceRootPath, maxPathLength, filenameFilter,
                                        getUpdatesAllowed(), propFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        this.availableWorkspaces = null;
    }

    protected static class StandardPropertiesFactory implements CustomPropertiesFactory {
        private static final long serialVersionUID = 1L;
        private final Collection<Property> empty = Collections.emptyList();

        /**
         * Only certain properties are tolerated when writing content (dna:resource or jcr:resource) nodes. These properties are
         * implicitly stored (primary type, data) or silently ignored (encoded, mimetype, last modified). The silently ignored
         * properties must be accepted to stay compatible with the JCR specification.
         */
        private final Set<Name> ALLOWABLE_PROPERTIES_FOR_CONTENT = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                                 Arrays.asList(new Name[] {
                                                                                                                     JcrLexicon.PRIMARY_TYPE,
                                                                                                                     JcrLexicon.DATA,
                                                                                                                     JcrLexicon.ENCODED,
                                                                                                                     JcrLexicon.MIMETYPE,
                                                                                                                     JcrLexicon.LAST_MODIFIED,
                                                                                                                     JcrLexicon.UUID,
                                                                                                                     DnaIntLexicon.NODE_DEFINITON})));
        /**
         * Only certain properties are tolerated when writing files (nt:file) or folders (nt:folder) nodes. These properties are
         * implicitly stored in the file or folder (primary type, created).
         */
        private final Set<Name> ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                                        Arrays.asList(new Name[] {
                                                                                                                            JcrLexicon.PRIMARY_TYPE,
                                                                                                                            JcrLexicon.CREATED,
                                                                                                                            JcrLexicon.UUID,
                                                                                                                            DnaIntLexicon.NODE_DEFINITON})));

        public Collection<Property> getDirectoryProperties( ExecutionContext context,
                                                            Location location,
                                                            File directory ) {
            return empty;
        }

        public Collection<Property> getFileProperties( ExecutionContext context,
                                                       Location location,
                                                       File file ) {
            return empty;
        }

        public Collection<Property> getResourceProperties( ExecutionContext context,
                                                           Location location,
                                                           File file,
                                                           String mimeType ) {
            return empty;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.filesystem.CustomPropertiesFactory#recordDirectoryProperties(org.jboss.dna.graph.ExecutionContext,
         *      java.lang.String, org.jboss.dna.graph.Location, java.io.File, java.util.Map)
         */
        public Set<Name> recordDirectoryProperties( ExecutionContext context,
                                                    String sourceName,
                                                    Location location,
                                                    File file,
                                                    Map<Name, Property> properties ) throws RepositorySourceException {
            ensureValidProperties(context, sourceName, properties.values(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.filesystem.CustomPropertiesFactory#recordFileProperties(org.jboss.dna.graph.ExecutionContext,
         *      java.lang.String, org.jboss.dna.graph.Location, java.io.File, java.util.Map)
         */
        public Set<Name> recordFileProperties( ExecutionContext context,
                                               String sourceName,
                                               Location location,
                                               File file,
                                               Map<Name, Property> properties ) throws RepositorySourceException {
            ensureValidProperties(context, sourceName, properties.values(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.filesystem.CustomPropertiesFactory#recordResourceProperties(org.jboss.dna.graph.ExecutionContext,
         *      java.lang.String, org.jboss.dna.graph.Location, java.io.File, java.util.Map)
         */
        public Set<Name> recordResourceProperties( ExecutionContext context,
                                                   String sourceName,
                                                   Location location,
                                                   File file,
                                                   Map<Name, Property> properties ) throws RepositorySourceException {
            ensureValidProperties(context, sourceName, properties.values(), ALLOWABLE_PROPERTIES_FOR_CONTENT);
            return null;
        }

        /**
         * Checks that the collection of {@code properties} only contains properties with allowable names.
         * 
         * @param context
         * @param sourceName
         * @param properties
         * @param validPropertyNames
         * @throws RepositorySourceException if {@code properties} contains a
         * @see #ALLOWABLE_PROPERTIES_FOR_CONTENT
         * @see #ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER
         */
        protected void ensureValidProperties( ExecutionContext context,
                                              String sourceName,
                                              Collection<Property> properties,
                                              Set<Name> validPropertyNames ) {
            List<String> invalidNames = new LinkedList<String>();
            NamespaceRegistry registry = context.getNamespaceRegistry();

            for (Property property : properties) {
                if (!validPropertyNames.contains(property.getName())) {
                    invalidNames.add(property.getName().getString(registry));
                }
            }

            if (!invalidNames.isEmpty()) {
                throw new RepositorySourceException(sourceName, FileSystemI18n.invalidPropertyNames.text(invalidNames.toString()));
            }
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
