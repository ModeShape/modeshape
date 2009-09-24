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
package org.jboss.dna.eclipse.jcr.rest.client.wizards;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.PUBLISH_DIALOG_HELP_CONTEXT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.actions.NewServerAction;
import org.jboss.dna.eclipse.jcr.rest.client.jobs.PublishJob.Type;
import org.jboss.dna.eclipse.jcr.rest.client.preferences.PublishingFileFilter;
import org.jboss.dna.web.jcr.rest.client.IServerRegistryListener;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>PublishPage</code> is a UI for publishing or unpublishing one or more files to a DNA repository.
 */
public final class PublishPage extends WizardPage implements IServerRegistryListener, ModifyListener {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The key in the wizard <code>IDialogSettings</code> for the recurse flag.
     */
    private static final String RECURSE_KEY = "recurse"; //$NON-NLS-1$

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    /**
     * Indicates if the file filter should be used.
     */
    private static boolean filterFiles = true;

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param container the project or folder whose files are being requested
     * @param recurse the flag indicating if child containers should be traversed
     * @param filter the file filter or <code>null</code> if not used
     * @return the list of files contained in the specified container (never <code>null</code>)
     * @throws CoreException if there is a problem finding the files
     */
    private static List<IFile> findFiles( IContainer container,
                                          boolean recurse,
                                          PublishingFileFilter filter ) throws CoreException {
        List<IFile> result = new ArrayList<IFile>();

        for (IResource member : container.members()) {
            if (recurse && (member instanceof IContainer)) {
                // don't select closed projects
                if ((member instanceof IProject) && !((IProject)member).isOpen()) {
                    continue;
                }

                result.addAll(findFiles((IContainer)member, recurse, filter));
            } else if ((member instanceof IFile) && ((IFile)member).getLocation().toFile().exists()) {
                if ((filter == null) || filter.accept(member)) {
                    result.add((IFile)member);
                }
            }
        }

        return result;
    }

    /**
     * Processes the specified list of files and for (1) each file found adds it to the result and (2) for each project or folder
     * adds all contained files. For projects and folders processing will be recursive based on saved wizard settings.
     * 
     * @param resources the resources being processed (never <code>null</code>)
     * @param recurse the flag indicating if child containers should be traversed
     * @param filter the file filter or <code>null</code> if not used
     * @return the files being published or unpublished (never <code>null</code>)
     * @throws CoreException if there is a problem processing the resources
     */
    private static List<IFile> processResources( List<IResource> resources,
                                                 boolean recurse,
                                                 PublishingFileFilter filter ) throws CoreException {
        List<IFile> result = new ArrayList<IFile>();

        // Project Map - the outer map. Its keys are IProjects and its values are a Parent Map
        // Parent Map - the inner map. Its keys are IContainers (IProject, IFolder) and its values are a list of files
        Map<IProject, Map<IContainer, List<IFile>>> projectMap = new HashMap<IProject, Map<IContainer, List<IFile>>>();

        // Step 1: Process resources
        // - For each file make sure there is a project entry and parent entry then add the file to the Parent Map.
        // - For each folder make sure there is a project entry then add folder entry.
        // - For each project make sure there is a project entry.
        //
        // Step 2: Process maps
        // - In the Project Map, when the recurse flag is set, entries for projects that have a null value (parent map) will be
        // traversed finding all child files and them to results.
        // - In the internal parent map, when the recurse flag is set, entries for parents that have a null value (child
        // collection) will be traversed finding all child files and add them to results.
        //
        // Step 3: Add files from Step 1 to results

        // Step 1 (see above for processing description)
        for (IResource resource : resources) {
            IFile file = null;
            IProject project = null;
            List<IFile> files = null;
            Map<IContainer, List<IFile>> parentMap = null;

            // see if resource is filtered
            if ((filter != null) && !filter.accept(resource)) {
                continue;
            }

            if (resource instanceof IFile) {
                IContainer parent = null; // project or folder
                file = (IFile)resource;
                parent = file.getParent();
                project = file.getProject();

                // make sure there is a project entry
                if (!projectMap.containsKey(project)) {
                    projectMap.put(project, null);
                }

                parentMap = projectMap.get(project);

                // make sure there is a parent entry
                if (parentMap == null) {
                    parentMap = new HashMap<IContainer, List<IFile>>();
                    projectMap.put(project, parentMap);
                }

                files = parentMap.get(parent);

                // make sure there is a files collection
                if (files == null) {
                    files = new ArrayList<IFile>();
                    parentMap.put(parent, files);
                }

                // add file
                files.add(file);
            } else if (resource instanceof IFolder) {
                IFolder folder = (IFolder)resource;
                project = folder.getProject();

                // make sure there is a project entry
                if (!projectMap.containsKey(project)) {
                    projectMap.put(project, null);
                }

                parentMap = projectMap.get(project);

                // make sure there is a folder entry
                if (parentMap == null) {
                    parentMap = new HashMap<IContainer, List<IFile>>();
                    projectMap.put(project, parentMap);
                }

                // add folder only if not already there
                if (!parentMap.containsKey(folder)) {
                    parentMap.put(folder, null);
                }
            } else if (resource instanceof IProject) {
                project = (IProject)resource;

                // if map does not have entry create one
                if (!projectMap.containsKey(project)) {
                    projectMap.put(project, null);
                }
            }
        }

        // Step 2 (see above for processing description)
        // Process projects that have nothing under them selected
        for (IProject project : projectMap.keySet()) {
            Map<IContainer, List<IFile>> parentMap = projectMap.get(project);

            if (parentMap == null) {
                result.addAll(findFiles(project, recurse, filter));
            } else {
                // process folders with no folder entries
                for (IContainer folder : parentMap.keySet()) {
                    List<IFile> files = parentMap.get(folder);

                    if (files == null) {
                        result.addAll(findFiles(folder, recurse, filter));
                    }
                }
            }
        }

        // Step 3 (see above for processing description)
        for (IProject project : projectMap.keySet()) {
            Map<IContainer, List<IFile>> parentMap = projectMap.get(project);

            if (parentMap != null) {
                for (Entry<IContainer, List<IFile>> entry : parentMap.entrySet()) {
                    if (entry.getValue() != null) {
                        result.addAll(entry.getValue());
                    }
                }
            }
        }

        return result;
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The repository chooser control.
     */
    private Combo cbxRepository;

    /**
     * The server chooser control.
     */
    private Combo cbxServer;

    /**
     * The workspace chooser control.
     */
    private Combo cbxWorkspace;

    /**
     * The files being published or unpublished (never <code>null</code>).
     */
    private List<IFile> files;

    /**
     * The filter used to determine if a file should be included in publishing operations (may be <code>null</code>).
     */
    private PublishingFileFilter filter;

    /**
     * The control containing all the files being published or unpublished.
     */
    private org.eclipse.swt.widgets.List lstResources;

    /**
     * The flag indicating if child containers should be traversed.
     */
    private boolean recurse;

    /**
     * A collection of repositories for the selected server (never <code>null</code>).
     */
    private List<Repository> repositories;

    /**
     * The repository where the workspace is located.
     */
    private Repository repository;

    /**
     * The collection of resources selected by the user to be published or unpublished.
     */
    private final List<IResource> resources;

    /**
     * The server where the repository is located.
     */
    private Server server;

    /**
     * A collection of servers from the server registry (never <code>null</code>).
     */
    private List<Server> servers;

    /**
     * The current validation status.
     */
    private Status status;

    /**
     * Indicates if publishing or unpublishing is being done.
     */
    private final Type type;

    /**
     * The workspace where the resources are being published/unpublished (may be <code>null</code>).
     */
    private Workspace workspace;

    /**
     * A collection of workspaces for the selected server repository (never <code>null</code>).
     */
    private List<Workspace> workspaces;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param type indicates if publishing or unpublishing is being done
     * @param resources the resources being published or unpublished (never <code>null</code>)
     * @throws CoreException if there is a problem processing the input resources
     */
    public PublishPage( Type type,
                        List<IResource> resources ) throws CoreException {
        super(PublishPage.class.getSimpleName());
        setTitle((type == Type.PUBLISH) ? RestClientI18n.publishPagePublishTitle.text()
                                       : RestClientI18n.publishPageUnpublishTitle.text());
        setPageComplete(false);

        this.type = type;
        this.resources = resources;

        // filter should not be cached as preferences may change
        this.filter = (filterFiles ? new PublishingFileFilter() : null);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    private void constructLocationPanel( Composite parent ) {
        Group pnl = new Group(parent, SWT.NONE);
        pnl.setText(RestClientI18n.publishPageLocationGroupTitle.text());
        pnl.setLayout(new GridLayout(2, false));
        pnl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // row 1: label combobox button
        // row 2: label combobox
        // row 3: label combobox

        { // row 1: server row
            Composite pnlServer = new Composite(pnl, SWT.NONE);
            GridLayout layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            pnlServer.setLayout(layout);
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gd.horizontalSpan = 2;
            pnlServer.setLayoutData(gd);

            Label lblServer = new Label(pnlServer, SWT.LEFT);
            lblServer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            lblServer.setText(RestClientI18n.publishPageServerLabel.text());

            this.cbxServer = new Combo(pnlServer, SWT.DROP_DOWN | SWT.READ_ONLY);
            this.cbxServer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            this.cbxServer.setToolTipText(RestClientI18n.publishPageServerToolTip.text());

            final IAction action = new NewServerAction(this.getShell(), getServerManager());
            final Button btnNewServer = new Button(pnlServer, SWT.PUSH);
            btnNewServer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            btnNewServer.setText(RestClientI18n.publishPageNewServerButton.text());
            btnNewServer.setToolTipText(action.getToolTipText());
            btnNewServer.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    action.run();
                }
            });

            // update page message first time selected to get rid of initial message by forcing validation
            btnNewServer.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    updateInitialMessage();
                    btnNewServer.removeSelectionListener(this);
                }
            });
        }

        { // row 2: repository row
            Label lblRepository = new Label(pnl, SWT.LEFT);
            lblRepository.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            lblRepository.setText(RestClientI18n.publishPageRepositoryLabel.text());

            this.cbxRepository = new Combo(pnl, SWT.DROP_DOWN | SWT.READ_ONLY);
            this.cbxRepository.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            this.cbxRepository.setToolTipText(RestClientI18n.publishPageRepositoryToolTip.text());
        }

        { // row 3: workspace row
            Label lblWorkspace = new Label(pnl, SWT.LEFT);
            lblWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            lblWorkspace.setText(RestClientI18n.publishPageWorkspaceLabel.text());

            this.cbxWorkspace = new Combo(pnl, SWT.DROP_DOWN | SWT.READ_ONLY);
            this.cbxWorkspace.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            if (type == Type.PUBLISH) {
                this.cbxWorkspace.setToolTipText(RestClientI18n.publishPageWorkspacePublishToolTip.text());
            } else {
                this.cbxWorkspace.setToolTipText(RestClientI18n.publishPageWorkspaceUnpublishToolTip.text());
            }
        }
    }

    private void constructResourcesPanel( Composite parent ) {
        Composite pnl = new Composite(parent, SWT.NONE);
        pnl.setLayout(new GridLayout());
        pnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // pnl layout:
        // row 1: lbl
        // row 2: lstResources
        // row 3: chkbox

        { // row 1
            Label lbl = new Label(pnl, SWT.LEFT);
            lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

            if (type == Type.PUBLISH) {
                lbl.setText(RestClientI18n.publishPagePublishResourcesLabel.text());
            } else {
                lbl.setText(RestClientI18n.publishPageUnpublishResourcesLabel.text());
            }
        }

        { // row 2
            this.lstResources = new org.eclipse.swt.widgets.List(pnl, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
            gd.horizontalSpan = 2;
            this.lstResources.setLayoutData(gd);
            final org.eclipse.swt.widgets.List finalLst = this.lstResources;

            // update page message first time selected to get rid of initial message by forcing validation
            this.lstResources.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    // do the very first time to get rid of initial message then remove listener
                    updateInitialMessage();
                    finalLst.removeSelectionListener(this);
                }
            });

            // load list with initial files
            loadFiles();
        }

        { // row 3
            final Button chkRecurse = new Button(pnl, SWT.CHECK);
            chkRecurse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            chkRecurse.setText(RestClientI18n.publishPageRecurseCheckBox.text());
            chkRecurse.setToolTipText(RestClientI18n.publishPageRecurseCheckBoxToolTip.text());
            chkRecurse.setSelection(this.recurse);
            chkRecurse.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    handleRecurseChanged(chkRecurse.getSelection());
                }
            });

            // update page message first time selected to get rid of initial message by forcing validation
            chkRecurse.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    updateInitialMessage();
                    chkRecurse.removeSelectionListener(this);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl( Composite parent ) {
        Composite pnlMain = new Composite(parent, SWT.NONE);
        pnlMain.setLayout(new GridLayout());
        pnlMain.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        constructLocationPanel(pnlMain);
        constructResourcesPanel(pnlMain);
        setControl(pnlMain);

        // add combobox listeners
        this.cbxRepository.addModifyListener(this);
        this.cbxServer.addModifyListener(this);
        this.cbxWorkspace.addModifyListener(this);

        // register with the help system
        IWorkbenchHelpSystem helpSystem = Activator.getDefault().getWorkbench().getHelpSystem();
        helpSystem.setHelp(pnlMain, PUBLISH_DIALOG_HELP_CONTEXT);

        // register to receive server registry events (this will populate the UI)
        getServerManager().addRegistryListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    @Override
    public void dispose() {
        getServerManager().removeRegistryListener(this);
        super.dispose();
    }

    /**
     * @return the files to publish or unpublish (never <code>null</code>)
     */
    public List<IFile> getFiles() {
        return this.files;
    }

    /**
     * Updates the initial page message.
     */
    void updateInitialMessage() {
        String msg = ((this.type == Type.PUBLISH) ? RestClientI18n.publishPagePublishOkStatusMsg.text()
                                                 : RestClientI18n.publishPageUnpublishOkStatusMsg.text());

        if (msg.equals(getMessage())) {
            updateState();
        }
    }

    /**
     * @return the server manager obtained from the wizard
     */
    private ServerManager getServerManager() {
        return ((PublishWizard)getWizard()).getServerManager();
    }

    /**
     * @return thw workspace to use when publishing or unpublishing (page must be complete)
     */
    public Workspace getWorkspace() {
        assert isPageComplete();
        return this.workspace;
    }

    /**
     * Saves the recurse setting and reloads the files to be published or unpublished.
     * 
     * @param selected the flag indicating the new recurse setting
     */
    void handleRecurseChanged( boolean selected ) {
        this.recurse = selected;
        saveRecurseSetting();

        try {
            this.files = processResources(this.resources, isRecursing(), filter);
            loadFiles();
        } catch (CoreException e) {
            Activator.getDefault().log(new Status(Severity.ERROR, RestClientI18n.publishPageRecurseProcessingErrorMsg.text(), e));

            if (getControl().isVisible()) {
                MessageDialog.openError(getShell(),
                                        RestClientI18n.errorDialogTitle.text(),
                                        RestClientI18n.publishPageRecurseProcessingErrorMsg.text());
            }
        }
    }

    /**
     * Handler for when the repository control value is modified
     */
    void handleRepositoryModified() {
        int index = this.cbxRepository.getSelectionIndex();

        // make sure there is a selection
        if (index != -1) {
            this.repository = this.repositories.get(index);
        }

        // clear loaded workspaces
        refreshWorkspaces();

        // update page state
        updateState();
    }

    /**
     * Handler for when the server control value is modified
     */
    void handleServerModified() {
        int index = this.cbxServer.getSelectionIndex();

        // make sure there is a selection
        if (index != -1) {
            this.server = this.servers.get(index);
        }

        // need to reload repositories since server changed
        refreshRepositories();

        // update page state
        updateState();
    }

    /**
     * Handler for when the workspace control value is modified
     */
    void handleWorkspaceModified() {
        int index = this.cbxWorkspace.getSelectionIndex();

        // make sure there is a selection
        if (index != -1) {
            this.workspace = this.workspaces.get(index);
        }

        updateState();
    }

    /**
     * @return the flag indicating if resources found recursively under projects and folders should also be published or
     *         unpublished
     */
    protected boolean isRecursing() {
        boolean recurse = true;

        if (getDialogSettings().get(RECURSE_KEY) != null) {
            recurse = getDialogSettings().getBoolean(RECURSE_KEY);
        }

        return recurse;
    }

    /**
     * Populates the list of files to be published based on the recurse flag and the list of workspace selected resources.
     * Pre-condition is that {@link #processResources(List, boolean, PublishingFileFilter)} has been called.
     */
    private void loadFiles() {
        this.lstResources.removeAll();

        for (IResource resource : this.files) {
            this.lstResources.add(resource.getFullPath().toString());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
     */
    @Override
    public void modifyText( ModifyEvent e ) {
        if (e.widget == this.cbxServer) {
            handleServerModified();
        } else if (e.widget == this.cbxRepository) {
            handleRepositoryModified();
        } else if (e.widget == this.cbxRepository) {
            handleWorkspaceModified();
        } else {
            assert false; // should not happen
        }
    }

    /**
     * Refreshes the repository-related fields and controls based on the server registry. This in turn causes the workspaces to
     * also to be refreshed.
     */
    private void refreshRepositories() {
        this.repository = null;

        if (this.server == null) {
            this.repositories = Collections.emptyList();
        } else {
            try {
                this.repositories = new ArrayList<Repository>(getServerManager().getRepositories(this.server));
            } catch (Exception e) {
                this.repositories = Collections.emptyList();
                String msg = RestClientI18n.serverManagerGetRepositoriesExceptionMsg.text(this.server.getShortDescription());
                Activator.getDefault().log(new Status(Severity.ERROR, msg, e));

                if (getControl().isVisible()) {
                    MessageDialog.openError(getShell(), RestClientI18n.errorDialogTitle.text(), msg);
                }
            }
        }

        // clear items
        this.cbxRepository.removeAll();

        // reload
        if (this.repositories.isEmpty()) {
            // disable control if necessary
            if (this.cbxRepository.getEnabled()) {
                this.cbxRepository.setEnabled(false);
            }
        } else if (this.repositories.size() == 1) {
            this.repository = this.repositories.get(0);
            this.cbxRepository.add(this.repository.getName());
            this.cbxRepository.select(0);

            // enable control if necessary
            if (!this.cbxRepository.getEnabled()) {
                this.cbxRepository.setEnabled(true);
            }
        } else {
            // add an item for each repository
            for (Repository repository : this.repositories) {
                this.cbxRepository.add(repository.getName());
            }

            // enable control if necessary
            if (!this.cbxRepository.getEnabled()) {
                this.cbxRepository.setEnabled(true);
            }
        }

        // must reload workspaces
        refreshWorkspaces();
    }

    /**
     * Refreshes the server-related fields and controls based on the server registry. This in turn causes the repositories and
     * workspaces to also to be refreshed.
     */
    void refreshServers() {
        this.server = null;
        this.servers = new ArrayList<Server>(getServerManager().getServers());

        // clear server combo
        this.cbxServer.removeAll();

        if (this.servers.size() == 0) {
            // disable control if necessary
            if (this.cbxServer.getEnabled()) {
                this.cbxServer.setEnabled(false);
            }
        } else if (this.servers.size() == 1) {
            this.server = this.servers.get(0);
            this.cbxServer.add(this.server.getName());
            this.cbxServer.select(0);

            // enable control if necessary
            if (!this.cbxServer.getEnabled()) {
                this.cbxServer.setEnabled(true);
            }
        } else {
            // add an item for each server
            for (Server server : this.servers) {
                this.cbxServer.add(server.getName());
            }

            // enable control if necessary
            if (!this.cbxServer.getEnabled()) {
                this.cbxServer.setEnabled(true);
            }
        }

        // must reload repositories
        refreshRepositories();
    }

    /**
     * Refreshes the workspace-related fields and controls based on the server registry.
     */
    private void refreshWorkspaces() {
        this.workspace = null;

        if (this.repository == null) {
            this.workspaces = Collections.emptyList();
        } else {
            try {
                this.workspaces = new ArrayList<Workspace>(getServerManager().getWorkspaces(this.repository));
            } catch (Exception e) {
                this.workspaces = Collections.emptyList();
                String msg = RestClientI18n.serverManagerGetWorkspacesExceptionMsg.text();
                Activator.getDefault().log(new Status(Severity.ERROR, msg, e));

                if (getControl().isVisible()) {
                    MessageDialog.openError(getShell(), RestClientI18n.errorDialogTitle.text(), msg);
                }
            }
        }

        // clear items
        this.cbxWorkspace.removeAll();

        // reload
        if (this.workspaces.isEmpty()) {
            // disable control if necessary
            if (this.cbxWorkspace.getEnabled()) {
                this.cbxWorkspace.setEnabled(false);
            }
        } else if (this.workspaces.size() == 1) {
            this.workspace = this.workspaces.get(0);
            this.cbxWorkspace.add(this.workspace.getName());
            this.cbxWorkspace.select(0);

            // enable control if necessary
            if (!this.cbxWorkspace.getEnabled()) {
                this.cbxWorkspace.setEnabled(true);
            }
        } else {
            // add an item for each workspace
            for (Workspace workspace : this.workspaces) {
                this.cbxWorkspace.add(workspace.getName());
            }

            // enable control if necessary
            if (!this.cbxWorkspace.getEnabled()) {
                this.cbxWorkspace.setEnabled(true);
            }
        }
    }

    /**
     * Saves the current recurse value to the wizard <code>IDialogSettings</code>.
     */
    protected void saveRecurseSetting() {
        getDialogSettings().put(RECURSE_KEY, this.recurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IServerRegistryListener#serverRegistryChanged(org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent)
     */
    @Override
    public Exception[] serverRegistryChanged( ServerRegistryEvent event ) {
        // should only be a new server event
        if (event.isNew()) {
            refreshServers();
            updateState();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    @Override
    public void setVisible( boolean visible ) {
        super.setVisible(visible);

        if (visible) {
            // set initial status
            validate();

            // update OK/Finish button enablement
            setPageComplete(!this.status.isError());

            // set initial message
            if (this.status.isOk()) {
                String msg = ((this.type == Type.PUBLISH) ? RestClientI18n.publishPagePublishOkStatusMsg.text()
                                                         : RestClientI18n.publishPageUnpublishOkStatusMsg.text());
                setMessage(msg, IMessageProvider.NONE);
            } else {
                setMessage(this.status.getMessage(), IMessageProvider.ERROR);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.WizardPage#setWizard(org.eclipse.jface.wizard.IWizard)
     */
    @Override
    public void setWizard( IWizard newWizard ) {
        super.setWizard(newWizard);

        // need to make sure the wizard has been set on the page since the recurse value is saved in the wizard dialog settings
        this.recurse = isRecursing();

        try {
            this.files = processResources(this.resources, this.recurse, this.filter);
        } catch (CoreException e) {
            Activator.getDefault().log(new Status(Severity.ERROR, RestClientI18n.publishPageRecurseProcessingErrorMsg.text(), e));
        }
    }

    /**
     * Updates message, message icon, and OK button enablement based on validation results
     */
    void updateState() {
        // get the current state
        validate();

        // update OK/Finish button enablement
        setPageComplete(!this.status.isError());

        // update page message
        if (this.status.isError()) {
            setMessage(this.status.getMessage(), IMessageProvider.ERROR);
        } else {
            if (this.status.isWarning()) {
                setMessage(this.status.getMessage(), IMessageProvider.WARNING);
            } else if (this.status.isInfo()) {
                setMessage(this.status.getMessage(), IMessageProvider.INFORMATION);
            } else {
                setMessage(this.status.getMessage(), IMessageProvider.NONE);
            }
        }
    }

    /**
     * Validates all inputs and sets the validation status.
     */
    private void validate() {
        String msg = null;
        Severity severity = Severity.ERROR;

        if ((this.resources == null) || this.resources.isEmpty() || this.files.isEmpty()) {
            msg = ((type == Type.PUBLISH) ? RestClientI18n.publishPageNoResourcesToPublishStatusMsg.text()
                                         : RestClientI18n.publishPageNoResourcesToUnpublishStatusMsg.text());
        } else if (this.server == null) {
            int count = this.cbxServer.getItemCount();
            msg = ((count == 0) ? RestClientI18n.publishPageNoAvailableServersStatusMsg.text()
                               : RestClientI18n.publishPageMissingServerStatusMsg.text());
        } else if (this.repository == null) {
            int count = this.cbxRepository.getItemCount();
            msg = ((count == 0) ? RestClientI18n.publishPageNoAvailableRepositoriesStatusMsg.text()
                               : RestClientI18n.publishPageMissingRepositoryStatusMsg.text());
        } else if (this.workspace == null) {
            int count = this.cbxWorkspace.getItemCount();
            msg = ((count == 0) ? RestClientI18n.publishPageNoAvailableWorkspacesStatusMsg.text()
                               : RestClientI18n.publishPageMissingWorkspaceStatusMsg.text());
        } else {
            severity = Severity.OK;
            msg = ((type == Type.PUBLISH) ? RestClientI18n.publishPagePublishOkStatusMsg.text()
                                         : RestClientI18n.publishPageUnpublishOkStatusMsg.text());
        }

        this.status = new Status(severity, msg, null);
    }

}
