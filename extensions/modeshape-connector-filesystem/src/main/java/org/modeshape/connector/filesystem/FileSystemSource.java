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

package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemRepository.FileSystemTransaction;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.AbstractRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local file system as content in a repository. This
 * source considers a workspace name to be the path to the directory on the file system that represents the root of that
 * workspace. New workspaces can be created, as long as the names represent valid paths to existing directories.
 */
@ThreadSafe
public class FileSystemSource extends AbstractRepositorySource implements ObjectFactory {

    /**
     * The first serialized version of this source. Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The initial {@link #getDefaultWorkspaceName() name of the default workspace} is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_WORKSPACE = "default";

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String WORKSPACE_ROOT = "workspaceRootPath";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    protected static final String MAX_PATH_LENGTH = "maxPathLength";
    protected static final String EXCLUSION_PATTERN = "exclusionPattern";
    protected static final String INCLUSION_PATTERN = "inclusionPattern";
    protected static final String FILENAME_FILTER = "filenameFilter";
    protected static final String CUSTOM_PROPERTY_FACTORY = "customPropertyFactory";
    protected static final String EAGER_FILE_LOADING = "eagerFileLoading";
    protected static final String DETERMINE_MIME_TYPE_USING_CONTENT = "determineMimeTypeUsingContent";
    protected static final String EXTRA_PROPERTIES = "extraProperties";

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
     * The default behavior for dealing with extra properties on 'nt:file', 'nt:folder' and 'nt:resource' nodes is to log them.
     */
    public static final String DEFAULT_EXTRA_PROPERTIES = "log";

    /**
     * This source supports creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = false;
    /**
     * This source by default does not eagerly read the file content into {@link Binary} values, but instead does it only when
     * necessary.
     */
    public static final boolean DEFAULT_EAGER_FILE_LOADING = false;

    /**
     * This source by default does not use the file content to determine the MIME type, but instead just uses the filename.
     */
    public static final boolean DEFAULT_DETERMINE_MIME_TYPE_USING_CONTENT = false;

    public static final int DEFAULT_MAX_PATH_LENGTH = 255; // 255 for windows users
    public static final String DEFAULT_EXCLUSION_PATTERN = null;
    public static final String DEFAULT_INCLUSION_PATTERN = null;
    public static final FilenameFilter DEFAULT_FILENAME_FILTER = null;
    private static final FilenameFilter ACCEPT_ALL_FILTER = new InclusionExclusionFilenameFilter();

    protected static Map<String, CustomPropertiesFactory> EXTRA_PROPERTIES_CLASSNAME_BY_KEY;

    static {
        Map<String, CustomPropertiesFactory> byKey = new HashMap<String, CustomPropertiesFactory>();
        byKey.put(DEFAULT_EXTRA_PROPERTIES, new LogProperties(Logger.getLogger(FileSystemSource.class)));
        byKey.put("store", new StoreProperties());
        byKey.put("error", new ThrowProperties());
        byKey.put("ignore", new IgnoreProperties());
        EXTRA_PROPERTIES_CLASSNAME_BY_KEY = Collections.unmodifiableMap(byKey);
    }

    @Description( i18n = FileSystemI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspaceName = DEFAULT_NAME_OF_DEFAULT_WORKSPACE;

    @Description( i18n = FileSystemI18n.class, value = "workspaceRootPathPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "workspaceRootPathPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "workspaceRootPathPropertyCategory" )
    private volatile String workspaceRootPath;

    @Description( i18n = FileSystemI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    @Description( i18n = FileSystemI18n.class, value = "maxPathLengthPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "maxPathLengthPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "maxPathLengthPropertyCategory" )
    private volatile int maxPathLength = DEFAULT_MAX_PATH_LENGTH;

    @Description( i18n = FileSystemI18n.class, value = "eagerFileLoadingPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "eagerFileLoadingPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "eagerFileLoadingPropertyCategory" )
    private volatile boolean eagerFileLoading = DEFAULT_EAGER_FILE_LOADING;

    @Description( i18n = FileSystemI18n.class, value = "determineMimeTypeUsingContentPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "determineMimeTypeUsingContentPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "determineMimeTypeUsingContentPropertyCategory" )
    private volatile boolean determineMimeTypeUsingContent = DEFAULT_DETERMINE_MIME_TYPE_USING_CONTENT;

    @Description( i18n = FileSystemI18n.class, value = "extraPropertiesPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "extraPropertiesPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "extraPropertiesPropertyCategory" )
    private volatile String extraProperties = DEFAULT_EXTRA_PROPERTIES;

    private volatile FilenameFilter filenameFilter = DEFAULT_FILENAME_FILTER;
    private volatile InclusionExclusionFilenameFilter inclusionExclusionFilenameFilter = new InclusionExclusionFilenameFilter();

    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(
                                                                                                  SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  DEFAULT_SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  DEFAULT_SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES);
    private transient FileSystemRepository repository;
    private volatile CustomPropertiesFactory customPropertiesFactory;

    private ExecutionContext defaultContext = new ExecutionContext();

    /**
     * 
     */
    public FileSystemSource() {
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
     * @return the regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored;
     *         may be null
     */
    @Description( i18n = FileSystemI18n.class, value = "exclusionPatternPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "exclusionPatternPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "exclusionPatternPropertyCategory" )
    public String getExclusionPattern() {
        return this.inclusionExclusionFilenameFilter.getExclusionPattern();
    }

    /**
     * Sets the regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored
     * <p>
     * Only one of FilenameFilter or Inclusion/Exclusion Pattern are used at a given time. If Inclusion/exclusion are set, then
     * FilenameFilter is ignored.
     * </p>
     * 
     * @param exclusionPattern the regular expression that, if matched by a file or folder, indicates that the file or folder
     *        should be ignored. If this pattern is {@code null}, no files will be excluded.
     */
    public synchronized void setExclusionPattern( String exclusionPattern ) {
        this.inclusionExclusionFilenameFilter.setExclusionPattern(exclusionPattern);
    }

    /**
     * Get the regular expression that, if matched by a file or folder, indicates that the file or folder should be included
     * 
     * @return the regular expression that, if matched by a file or folder, indicates that the file or folder should be included;
     *         may be null
     */
    @Description( i18n = FileSystemI18n.class, value = "inclusionPatternPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "inclusionPatternPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "inclusionPatternPropertyCategory" )
    public String getInclusionPattern() {
        return this.inclusionExclusionFilenameFilter.getInclusionPattern();
    }

    /**
     * Sets the regular expression that, if matched by a file or folder, indicates that the file or folder should be included
     * <p>
     * Only one of FilenameFilter or Inclusion/Exclusion Pattern are used at a given time. If Inclusion/exclusion are set, then
     * FilenameFilter is ignored.
     * </p>
     * 
     * @param inclusionPattern the regular expression that, if matched by a file or folder, indicates that the file or folder
     *        should be ignored. If this pattern is {@code null}, no files will be excluded.
     */
    public synchronized void setInclusionPattern( String inclusionPattern ) {
        this.inclusionExclusionFilenameFilter.setInclusionPattern(inclusionPattern);
    }

    /**
     * @return the {@FilenameFilter filename filter} (if any) that is used to restrict which content can be
     *         accessed by this connector; may be null
     */
    public FilenameFilter getFilenameFilter() {
        return this.filenameFilter;
    }

    /**
     * Sets the filename filter that is used to restrict which content can be accessed by this connector
     * <p>
     * Only one of FilenameFilter or Inclusion/Exclusion Pattern are used at a given time. If Inclusion/exclusion are set, then
     * FilenameFilter is ignored.
     * </p>
     * 
     * @param filenameFilter the filename filter that is used to restrict which content can be accessed by this connector. If this
     *        parameter is {@code null}, no files will be excluded.
     */
    public synchronized void setFilenameFilter( FilenameFilter filenameFilter ) {
        this.filenameFilter = filenameFilter;
    }

    /**
     * Sets the filename filter that is used to restrict which content can be accessed by this connector by specifying the name of
     * a class that implements the {@code FilenameFilter} interface and has a public, no-argument constructor.
     * <p>
     * Only one of the {@code exclusionPattern} and {@code filenameFilter} properties may be non-null at any one time. Calling
     * this method automatically sets the {@code exclusionPattern} property to {@code null}.
     * </p>
     * 
     * @param filenameFilterClassName the class name of the filter implementation or null if no filename filter should be used
     * @throws ClassNotFoundException if the the class for the {@code FilenameFilter} implementation cannot be located
     * @throws IllegalAccessException if the filename filter class or its nullary constructor is not accessible.
     * @throws InstantiationException if the filename filter represents an abstract class, an interface, an array class, a
     *         primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other
     *         reason.
     * @throws ClassCastException if the class named by {@code filenameFilterClassName} does not implement the {@code
     *         FilenameFilter} interface
     */
    public synchronized void setFilenameFilter( String filenameFilterClassName )
        throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (filenameFilterClassName == null) {
            this.filenameFilter = null;
            return;
        }

        Class<?> filenameFilterClass = Class.forName(filenameFilterClassName);

        this.filenameFilter = (FilenameFilter)filenameFilterClass.newInstance();
    }

    FilenameFilter filenameFilter( boolean hideFilesForCustomProperties ) {
        if (this.filenameFilter != null) return this.filenameFilter;
        if (this.getInclusionPattern() != null || this.getExclusionPattern() != null) return this.inclusionExclusionFilenameFilter;

        // Otherwise, create one that take into account the exclusion pattern ...
        FilenameFilter filenameFilter = null;
        final String filterPattern = this.inclusionExclusionFilenameFilter.getExclusionPattern();
        if (filterPattern != null) {
            filenameFilter = new FilenameFilter() {
                Pattern filter = Pattern.compile(filterPattern);

                public boolean accept( File dir,
                                       String name ) {
                    return !filter.matcher(name).matches();
                }
            };
        }

        if (hideFilesForCustomProperties) {
            // And the properties factory ...
            CustomPropertiesFactory customPropsFactory = customPropertiesFactory();
            if (customPropsFactory instanceof BasePropertiesFactory) {
                filenameFilter = ((BasePropertiesFactory)customPropsFactory).getFilenameFilter(filenameFilter);
            }
        }

        // If there are no criteria that would allow us to build a filter, then accept any file.
        if (filenameFilter == null) filenameFilter = ACCEPT_ALL_FILTER;

        return filenameFilter;
    }

    /**
     * Get the maximum path length (in characters) allowed by the underlying file system
     * 
     * @return the maximum path length (in characters) allowed by the underlying file system
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
    @Description( i18n = FileSystemI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = FileSystemI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = FileSystemI18n.class, value = "updatesAllowedPropertyCategory" )
    @Override
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
     * Get whether this source should use file content (and file name) to determine the MIME type.
     * 
     * @return true if the file content should be used to determine the MIME type, or false if only the filename should be used
     */
    public boolean isContentUsedToDetermineMimeType() {
        return determineMimeTypeUsingContent;
    }

    /**
     * Set whether this source should use file content (and file name) to determine the MIME type.
     * 
     * @param contentUsedToDetermineMimeType true if the file content should be used to determine the MIME type
     */
    public void setContentUsedToDetermineMimeType( boolean contentUsedToDetermineMimeType ) {
        determineMimeTypeUsingContent = contentUsedToDetermineMimeType;
    }

    /**
     * Get the desired behavior for handling extra properties on "nt:foldeR", "nt:file", and "nt:resource" nodes.
     * 
     * @return one of "log", "ignore", "error", or "store"
     * @see #getCustomPropertiesFactory()
     */
    public String getExtraPropertiesBehavior() {
        return extraProperties;
    }

    /**
     * Set the desired behavior for handling extra properties on "nt:foldeR", "nt:file", and "nt:resource" nodes.
     * 
     * @param behavior "log", "ignore", "error", or "store"
     * @see #setCustomPropertiesFactory(CustomPropertiesFactory)
     * @see #setCustomPropertiesFactory(String)
     */
    public void setExtraPropertiesBehavior( String behavior ) {
        if (behavior != null) behavior = behavior.trim().toLowerCase();
        if (EXTRA_PROPERTIES_CLASSNAME_BY_KEY.containsKey(behavior)) {
            this.extraProperties = behavior;
        } else {
            this.extraProperties = DEFAULT_EXTRA_PROPERTIES;
        }
    }

    /**
     * Get the factory that is used to create custom properties on "nt:folder", "nt:file", and "nt:resource" nodes.
     * 
     * @return the factory, or null if no custom properties are to be created
     */
    public synchronized CustomPropertiesFactory getCustomPropertiesFactory() {
        return customPropertiesFactory;
    }

    CustomPropertiesFactory customPropertiesFactory() {
        if (customPropertiesFactory == null) {
            customPropertiesFactory = EXTRA_PROPERTIES_CLASSNAME_BY_KEY.get(extraProperties);
            if (customPropertiesFactory == null) {
                customPropertiesFactory = EXTRA_PROPERTIES_CLASSNAME_BY_KEY.get(DEFAULT_EXTRA_PROPERTIES);
            }
            assert customPropertiesFactory != null;
        }
        return customPropertiesFactory;
    }

    /**
     * Set the factory that is used to create custom properties on "nt:folder", "nt:file", and "nt:resource" nodes.
     * 
     * @param customPropertiesFactory the factory reference, or null if no custom properties will be created
     * @see #setExtraPropertiesBehavior(String)
     */
    public synchronized void setCustomPropertiesFactory( CustomPropertiesFactory customPropertiesFactory ) {
        this.customPropertiesFactory = customPropertiesFactory;
    }

    /**
     * Set the factory that is used to create custom properties on "nt:folder", "nt:file", and "nt:resource" nodes by specifying
     * the name of a class that implements the {@code CustomPropertiesFactory} interface and has a public, no-argument
     * constructor.
     * 
     * @param customPropertiesFactoryClassName the class name of the factory implementation or null if no custom properties will
     *        be created
     * @throws ClassNotFoundException if the the class for the {@code CustomPropertiesFactory} implementation cannot be located
     * @throws IllegalAccessException if the custom properties factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the custom properties factory represents an abstract class, an interface, an array class,
     *         a primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other
     *         reason.
     * @throws ClassCastException if the class named by {@code customPropertiesFactoryClassName} does not implement the {@code
     *         CustomPropertiesFactory} interface
     * @see #setExtraPropertiesBehavior(String)
     */
    public synchronized void setCustomPropertiesFactory( String customPropertiesFactoryClassName )
        throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (customPropertiesFactoryClassName == null) {
            this.customPropertiesFactory = null;
            return;
        }

        Class<?> customPropertiesFactoryClass = Class.forName(customPropertiesFactoryClassName);
        this.customPropertiesFactory = (CustomPropertiesFactory)customPropertiesFactoryClass.newInstance();
    }

    /**
     * Optional flag that defines whether the connector should eagerly read file content even before it is needed, guaranteeing
     * access to the content. A value of "true" may result in the file content being loaded even when it is not needed, and may
     * increase the memory footprint; a value of "false" will delay reading the file content until it is needed, but changes to
     * the underlying files may leak into the JCR sessions. The default value is "false".
     * 
     * @return 'true' if the file is to be read eagerly and preemptively, or false if the file content is to be loaded lazily.
     */
    public boolean isEagerFileLoading() {
        return eagerFileLoading;
    }

    /**
     * @param eagerFileLoading Sets eagerFileLoading to the specified value.
     */
    public void setEagerFileLoading( boolean eagerFileLoading ) {
        this.eagerFileLoading = eagerFileLoading;
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
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        ref.add(new StringRefAddr(MAX_PATH_LENGTH, String.valueOf(maxPathLength)));
        ref.add(new StringRefAddr(EXTRA_PROPERTIES, String.valueOf(extraProperties)));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
        }
        if (getCustomPropertiesFactory() != null) {
            ref.add(new StringRefAddr(CUSTOM_PROPERTY_FACTORY, getCustomPropertiesFactory().getClass().getName()));
        }
        if (this.inclusionExclusionFilenameFilter.getExclusionPattern() != null) {
            ref.add(new StringRefAddr(EXCLUSION_PATTERN, this.inclusionExclusionFilenameFilter.getExclusionPattern()));
        }
        if (this.inclusionExclusionFilenameFilter.getInclusionPattern() != null) {
            ref.add(new StringRefAddr(INCLUSION_PATTERN, this.inclusionExclusionFilenameFilter.getInclusionPattern()));
        }
        if (filenameFilter != null) {
            ref.add(new StringRefAddr(FILENAME_FILTER, filenameFilter.getClass().getName()));
        }
        ref.add(new StringRefAddr(EAGER_FILE_LOADING, Boolean.toString(isEagerFileLoading())));
        ref.add(new StringRefAddr(DETERMINE_MIME_TYPE_USING_CONTENT, Boolean.toString(isContentUsedToDetermineMimeType())));

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
            Map<String, Object> values = valuesFrom((Reference)obj);

            String sourceName = (String)values.get(SOURCE_NAME);
            String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = (String)values.get(ALLOW_CREATING_WORKSPACES);
            String exclusionPattern = (String)values.get(EXCLUSION_PATTERN);
            String inclusionPattern = (String)values.get(INCLUSION_PATTERN);
            String filenameFilterClassName = (String)values.get(FILENAME_FILTER);
            String maxPathLength = (String)values.get(DEFAULT_MAX_PATH_LENGTH);
            String customPropertiesFactoryClassName = (String)values.get(CUSTOM_PROPERTY_FACTORY);
            String extraPropertiesBehavior = (String)values.get(EXTRA_PROPERTIES);
            String eagerFileLoading = (String)values.get(EAGER_FILE_LOADING);
            String useContentForMimeType = (String)values.get(DETERMINE_MIME_TYPE_USING_CONTENT);

            String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            FileSystemSource source = new FileSystemSource();
            if (sourceName != null) source.setName(sourceName);
            if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (exclusionPattern != null) source.setExclusionPattern(exclusionPattern);
            if (inclusionPattern != null) source.setInclusionPattern(inclusionPattern);
            if (filenameFilterClassName != null) source.setFilenameFilter(filenameFilterClassName);
            if (maxPathLength != null) source.setMaxPathLength(Integer.valueOf(maxPathLength));
            if (extraPropertiesBehavior != null) source.setExtraPropertiesBehavior(extraPropertiesBehavior);
            if (customPropertiesFactoryClassName != null) source.setCustomPropertiesFactory(customPropertiesFactoryClassName);
            if (eagerFileLoading != null) source.setEagerFileLoading(Boolean.parseBoolean(eagerFileLoading));
            if (useContentForMimeType != null) source.setContentUsedToDetermineMimeType(Boolean.parseBoolean(useContentForMimeType));
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
        String sourceName = getName();
        if (sourceName == null || sourceName.trim().length() == 0) {
            I18n msg = FileSystemI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }

        if (repository == null) {
            repository = new FileSystemRepository(this);

            ExecutionContext context = repositoryContext != null ? repositoryContext.getExecutionContext() : defaultContext;
            FileSystemTransaction txn = repository.startTransaction(context, false);
            try {
                // Create the set of initial workspaces ...
                for (String initialName : getPredefinedWorkspaceNames()) {
                    repository.createWorkspace(txn, initialName, CreateConflictBehavior.DO_NOT_CREATE, null);
                }
            } finally {
                txn.commit();
            }

        }
        return new Connection<PathNode, FileSystemWorkspace>(this, repository);
    }
}
