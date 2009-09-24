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
package org.jboss.dna.eclipse.jcr.rest.client.actions;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DELETE_SERVER_IMAGE;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.DnaResourceHelper;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.dialogs.PublishedLocationsDialog;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>PublishAction</code> controls the publishing of one or more {@link org.eclipse.core.resources.IResource}s to a DNA
 * repository.
 */
public final class ShowPublishedLocationsAction extends Action implements IObjectActionDelegate {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The current workspace selection.
     */
    private IStructuredSelection selection;

    /**
     * The active part's Shell.
     */
    private Shell shell;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    public ShowPublishedLocationsAction() {
        super(RestClientI18n.deleteServerActionText.text(), Activator.getDefault().getImageDescriptor(DELETE_SERVER_IMAGE));
        setToolTipText(RestClientI18n.deleteServerActionToolTip.text());
        setEnabled(false);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run( IAction action ) {
        assert ((this.selection != null) && (this.selection.size() == 1));
        assert (this.selection.getFirstElement() instanceof IFile);

        // open dialog
        ServerManager serverManager = Activator.getDefault().getServerManager();
        DnaResourceHelper resourceHelper = new DnaResourceHelper(serverManager);

        try {
            Set<Workspace> workspaces = resourceHelper.getPublishedOnWorkspaces((IFile)this.selection.getFirstElement());
            new PublishedLocationsDialog(this.shell, serverManager, (IFile)this.selection.getFirstElement(), workspaces).open();
        } catch (Exception e) {
            Activator.getDefault().log(new Status(Severity.ERROR, RestClientI18n.showPublishedLocationsErrorMsg.text(), e));
            MessageDialog.openError(this.shell,
                                    RestClientI18n.errorDialogTitle.text(),
                                    RestClientI18n.showPublishedLocationsErrorMsg.text());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged( IAction action,
                                  ISelection selection ) {
        if (selection instanceof IStructuredSelection) {
            this.selection = (IStructuredSelection)selection;
        } else {
            this.selection = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void setActivePart( IAction action,
                               IWorkbenchPart targetPart ) {
        this.shell = targetPart.getSite().getShell();
    }

}
