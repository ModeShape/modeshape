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

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_IMAGE_16x;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.eclipse.jcr.rest.client.jobs.PublishJob.Type;
import org.jboss.dna.eclipse.jcr.rest.client.wizards.PublishWizard;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;

/**
 * The <code>BasePublishingAction</code> is a base class for all publishing actions.
 */
public abstract class BasePublishingAction implements IObjectActionDelegate {

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

    /**
     * Indicates if this is a publishing or unpublishing action.
     */
    private final Type type;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param type indicates the type of action
     */
    public BasePublishingAction( Type type ) {
        this.type = type;
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
    @SuppressWarnings( "unchecked" )
    public void run( IAction action ) {
        assert (this.selection != null);
        assert (!this.selection.isEmpty());

        List<IResource> resources;

        if (this.selection.size() == 1) {
            resources = Collections.singletonList((IResource)this.selection.getFirstElement());
        } else {
            resources = this.selection.toList();
        }

        // run wizard
        try {
            WizardDialog dialog = new WizardDialog(shell, new PublishWizard(this.type, resources,
                                                                            Activator.getDefault().getServerManager())) {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
                 */
                @Override
                protected void initializeBounds() {
                    super.initializeBounds();
                    getShell().setImage(Activator.getDefault().getImage(DNA_IMAGE_16x));
                    Utils.centerAndSizeShellRelativeToDisplay(getShell(), 75, 75);
                }
            };

            dialog.open();
        } catch (CoreException e) {
            String msg = null;

            if (this.type == Type.PUBLISH) {
                msg = RestClientI18n.basePublishingActionPublishingWizardErrorMsg.text();
            } else {
                msg = RestClientI18n.basePublishingActionUnpublishingWizardErrorMsg.text();
            }

            Activator.getDefault().log(new Status(Severity.ERROR, msg, e));
            MessageDialog.openError(this.shell, RestClientI18n.errorDialogTitle.text(), msg);
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
