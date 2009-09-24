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
package org.jboss.dna.eclipse.jcr.rest.client.views;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.COLLAPSE_ALL_IMAGE;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.SERVER_VIEW_HELP_CONTEXT;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.part.ViewPart;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.actions.DeleteServerAction;
import org.jboss.dna.eclipse.jcr.rest.client.actions.EditServerAction;
import org.jboss.dna.eclipse.jcr.rest.client.actions.NewServerAction;
import org.jboss.dna.eclipse.jcr.rest.client.actions.ReconnectToServerAction;
import org.jboss.dna.web.jcr.rest.client.IServerRegistryListener;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent;
import org.jboss.dna.web.jcr.rest.client.domain.IDnaObject;

/**
 * The <code>ServerView</code> shows all defined servers and their DNA repositories.
 */
public final class ServerView extends ViewPart implements IServerRegistryListener {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * Collapses all tree nodes.
     */
    private IAction collapseAllAction;

    /**
     * Deletes a server.
     */
    private BaseSelectionListenerAction deleteAction;

    /**
     * Edits a server's properties.
     */
    private BaseSelectionListenerAction editAction;

    /**
     * Creates a new server.
     */
    private Action newAction;

    /**
     * The viewer's content and label provider.
     */
    private DnaContentProvider provider;

    /**
     * Refreshes the server connections.
     */
    private ReconnectToServerAction reconnectAction;

    private TreeViewer viewer;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    private void constructActions() {
        // the collapse all action is always enabled
        this.collapseAllAction = new Action() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                getViewer().collapseAll();
            }
        };

        this.collapseAllAction.setToolTipText(RestClientI18n.collapseActionToolTip.text());
        this.collapseAllAction.setImageDescriptor(Activator.getDefault().getImageDescriptor(COLLAPSE_ALL_IMAGE));

        // the reconnect action tries to ping a selected server
        this.reconnectAction = new ReconnectToServerAction(this.viewer);
        this.viewer.addSelectionChangedListener(this.reconnectAction);

        // the shell used for dialogs that the actions display
        Shell shell = this.getSite().getShell();

        // the delete action will delete one or more servers
        this.deleteAction = new DeleteServerAction(shell, getServerManager());
        this.viewer.addSelectionChangedListener(this.deleteAction);

        // the edit action is only enabled when one server is selected
        this.editAction = new EditServerAction(shell, getServerManager());
        this.viewer.addSelectionChangedListener(this.editAction);

        // the new server action is always enabled
        this.newAction = new NewServerAction(shell, getServerManager());
    }

    private void constructContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.add(this.newAction);
        menuMgr.add(this.editAction);
        menuMgr.add(this.deleteAction);
        menuMgr.add(this.reconnectAction);

        Menu menu = menuMgr.createContextMenu(this.viewer.getTree());
        this.viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuMgr, this.viewer);
    }

    private void constructToolBar() {
        IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
        toolBar.add(this.newAction);
        toolBar.add(this.editAction);
        toolBar.add(this.deleteAction);
        toolBar.add(this.reconnectAction);
        toolBar.add(this.collapseAllAction);
    }

    /**
     * @param parent the viewer's parent
     */
    private void constructTreeViewer( Composite parent ) {
        this.provider = new DnaContentProvider();
        this.viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);

        this.viewer.setContentProvider(this.provider);
        ILabelDecorator decorator = Activator.getDefault().getWorkbench().getDecoratorManager().getLabelDecorator();
        this.viewer.setLabelProvider(new DecoratingLabelProvider(this.provider, decorator));
        ColumnViewerToolTipSupport.enableFor(this.viewer);

        this.viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
             */
            @Override
            public void selectionChanged( SelectionChangedEvent event ) {
                handleSelectionChanged(event);
            }
        });
        this.viewer.addDoubleClickListener(new IDoubleClickListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
             */
            @Override
            public void doubleClick( DoubleClickEvent arg0 ) {
                handleDoubleClick();
            }
        });

        // need to call this (doesn't matter what the param is) to bootstrap the provider.
        this.viewer.setInput(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl( Composite parent ) {
        constructTreeViewer(parent);
        constructActions();
        constructToolBar();
        constructContextMenu();

        setTitleToolTip(RestClientI18n.serverViewToolTip.text());

        // register to receive changes to the server registry
        getServerManager().addRegistryListener(this);

        // register with the help system
        IWorkbenchHelpSystem helpSystem = Activator.getDefault().getWorkbench().getHelpSystem();
        helpSystem.setHelp(parent, SERVER_VIEW_HELP_CONTEXT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        getServerManager().removeRegistryListener(this);
        super.dispose();
    }

    /**
     * @return the server manager being used by this view
     */
    private ServerManager getServerManager() {
        return Activator.getDefault().getServerManager();
    }

    /**
     * @return the tree viewer
     */
    TreeViewer getViewer() {
        return this.viewer;
    }

    /**
     * Opens a dialog to edit server properties.
     */
    void handleDoubleClick() {
        this.editAction.run();
    }

    /**
     * @param event the event being processed
     */
    void handleSelectionChanged( SelectionChangedEvent event ) {
        updateStatusLine((IStructuredSelection)event.getSelection());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IServerRegistryListener#serverRegistryChanged(org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent)
     */
    @Override
    public Exception[] serverRegistryChanged( ServerRegistryEvent event ) {
        if (event.isNew() || event.isUpdate()) {
            this.viewer.refresh();
        } else {
            this.viewer.remove(event.getServer());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        if (!this.viewer.getControl().isDisposed()) {
            this.viewer.getControl().setFocus();
        }
    }

    /**
     * @param selection the current viewer selection (never <code>null</code>)
     */
    private void updateStatusLine( IStructuredSelection selection ) {
        assert (selection.size() < 2);

        String msg = (selection.isEmpty() ? "" : ((IDnaObject)selection.getFirstElement()).getShortDescription()); //$NON-NLS-1$
        getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
    }

}
