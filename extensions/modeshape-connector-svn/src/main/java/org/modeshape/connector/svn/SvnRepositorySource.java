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
package org.modeshape.connector.svn;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.annotation.ReadOnly;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.svn.SvnRepository.SvnTransaction;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.AbstractRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * The {@link RepositorySource} for the connector that exposes an area of the local/remote svn repository as content in a
 * repository. This source considers a workspace name to be the path to the directory on the repository's root directory location
 * that represents the root of that workspace. New workspaces can be created, as long as the names represent valid paths to
 * existing directories.
 */
@ThreadSafe
public class SvnRepositorySource extends AbstractRepositorySource implements ObjectFactory {

    /**
     * The first serialized version of this source. Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String SVN_REPOSITORY_ROOT_URL = "repositoryRootURL";
    protected static final String SVN_USERNAME = "username";
    protected static final String SVN_PASSWORD = "password";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
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
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = false;
    /**
     * This source does support creating workspaces.
     */
    protected static final boolean DEFAULT_SUPPORTS_CREATING_WORKSPACES = true;
    /**
     * This source supports udpates by default, but each instance may be configured to be read-only or updateable}.
     */
    public static final boolean DEFAULT_SUPPORTS_UPDATES = false;
    /**
     * The default name of the {@link #defaultWorkspace} property.
     */
    public static final String DEFAULT_WORKSPACE_NAME = "trunk";

    /**
     * This source supports creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = false;

    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "repositoryRootUrlPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "repositoryRootUrlPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "repositoryRootUrlPropertyCategory" )
    private volatile String repositoryRootUrl;

    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "usernamePropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "usernamePropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "usernamePropertyCategory" )
    private volatile String username;

    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "passwordPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "passwordPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "passwordPropertyCategory" )
    private volatile String password;

    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace = DEFAULT_WORKSPACE_NAME;

    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(
                                                                                                  SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  DEFAULT_SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  DEFAULT_SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES);

    private transient SvnRepository repository;

    private ExecutionContext defaultContext = new ExecutionContext();

    /**
     * Create a repository source instance.
     */
    public SvnRepositorySource() {
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
     * @return the url
     */
    public String getRepositoryRootUrl() {
        return this.repositoryRootUrl;
    }

    /**
     * Set the url for the subversion repository.
     * 
     * @param url - the url location.
     * @throws IllegalArgumentException If svn url is null or empty
     */
    public synchronized void setRepositoryRootUrl( String url ) {
        CheckArg.isNotEmpty(url, "RepositoryRootUrl");
        this.repositoryRootUrl = url;
    }

    public String getUsername() {
        return this.username;
    }

    /**
     * @param username
     */
    public synchronized void setUsername( String username ) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    /**
     * @param password
     */
    public synchronized void setPassword( String password ) {
        this.password = password;
    }

    /**
     * Get whether this source supports updates.
     * 
     * @return true if this source supports updates, or false if this source only supports reading content.
     */
    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    @ReadOnly
    public boolean getSupportsUpdates() {
        return capabilities.supportsUpdates();
    }

    /**
     * Get the path to the existing directory that should be used for the default workspace. This path should be relative to the
     * {@link #getRepositoryRootUrl() repository root URL}. If the default is specified as a null String or is not a valid and
     * resolvable path, this source will consider the default to be the current working directory of this virtual machine, as
     * defined by the <code>new File(".")</code>.
     * 
     * @return the file system path to the directory representing the default workspace, or null if the default should be the
     *         current working directory
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspace;
    }

    /**
     * Set the file system path to the existing directory that should be used for the default workspace. This path should be
     * relative to the {@link #getRepositoryRootUrl() repository root URL}. If the default is specified as a null String or is not
     * a valid and resolvable path, this source will consider the default to be the current working directory of this virtual
     * machine, as defined by the <code>new File(".")</code>.
     * 
     * @param pathToDirectoryForDefaultWorkspace the valid and resolvable file system path to the directory representing the
     *        default workspace, or null if the current working directory should be used as the default workspace
     */
    public synchronized void setDefaultWorkspaceName( String pathToDirectoryForDefaultWorkspace ) {
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
    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyCategory" )
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
    @Description( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = SvnRepositoryConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SvnRepositorySource) {
            SvnRepositorySource that = (SvnRepositorySource)obj;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
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
        if (getRepositoryRootUrl() != null) {
            ref.add(new StringRefAddr(SVN_REPOSITORY_ROOT_URL, getRepositoryRootUrl()));
        }
        if (getUsername() != null) {
            ref.add(new StringRefAddr(SVN_USERNAME, getUsername()));
        }
        if (getPassword() != null) {
            ref.add(new StringRefAddr(SVN_PASSWORD, getPassword()));
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuidObject().toString()));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
        }
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
        if (!(obj instanceof Reference)) return null;

        Map<String, Object> values = valuesFrom((Reference)obj);

        String sourceName = (String)values.get(SOURCE_NAME);
        String repositoryRootUrl = (String)values.get(SVN_REPOSITORY_ROOT_URL);
        String username = (String)values.get(SVN_USERNAME);
        String password = (String)values.get(SVN_PASSWORD);
        String retryLimit = (String)values.get(RETRY_LIMIT);
        String rootNodeUuid = (String)values.get(ROOT_NODE_UUID);
        String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
        String createWorkspaces = (String)values.get(ALLOW_CREATING_WORKSPACES);

        String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
        String[] workspaceNames = null;
        if (combinedWorkspaceNames != null) {
            List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
            workspaceNames = paths.toArray(new String[paths.size()]);
        }
        // Create the source instance ...
        SvnRepositorySource source = new SvnRepositorySource();
        if (sourceName != null) source.setName(sourceName);
        if (repositoryRootUrl != null && repositoryRootUrl.length() > 0) source.setRepositoryRootUrl(repositoryRootUrl);
        if (username != null) source.setUsername(username);
        if (password != null) source.setPassword(password);
        if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
        if (rootNodeUuid != null) source.setRootNodeUuidObject(rootNodeUuid);
        if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
        if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
        if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {

        String sourceName = getName();
        if (sourceName == null || sourceName.trim().length() == 0) {
            I18n msg = SvnRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }

        String sourceUsername = getUsername();
        if (sourceUsername == null || sourceUsername.trim().length() == 0) {
            I18n msg = SvnRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getUsername(), msg.text("username"));
        }

        String sourcePassword = getPassword();
        if (sourcePassword == null) {
            I18n msg = SvnRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getPassword(), msg.text("password"));
        }

        String repositoryRootURL = getRepositoryRootUrl();
        if (repositoryRootURL == null || repositoryRootURL.trim().length() == 0) {
            I18n msg = SvnRepositoryConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getRepositoryRootUrl(), msg.text("repositoryRootURL"));
        }
        repositoryRootURL = repositoryRootURL.trim();
        if (repositoryRootURL.endsWith("/")) repositoryRootURL = repositoryRootURL + "/";

        if (repository == null) {
            repository = new SvnRepository(this);

            ExecutionContext context = repositoryContext != null ? repositoryContext.getExecutionContext() : defaultContext;
            SvnTransaction txn = repository.startTransaction(context, false);
            try {
                // Create the set of initial workspaces ...
                for (String initialName : getPredefinedWorkspaceNames()) {
                    repository.createWorkspace(txn, initialName, CreateConflictBehavior.DO_NOT_CREATE, null);
                }
            } finally {
                txn.commit();
            }

        }
        return new Connection<PathNode, SvnWorkspace>(this, repository);
    }
}
