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
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.EDIT_SERVER_IMAGE;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.wizards.ServerWizard;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>EditServerAction</code> runs a UI that allows {@link Server server} properties to be changed.
 */
public final class EditServerAction extends BaseSelectionListenerAction {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The selected server being edited.
     */
    private Server serverBeingEdited;

    /**
     * The server manager used to create and edit servers.
     */
    private final ServerManager serverManager;

    /**
     * The shell used to display the dialog that edits and creates servers.
     */
    private final Shell shell;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param shell the parent shell used to display the dialog
     * @param serverManager the server manager to use when creating and editing servers
     */
    public EditServerAction( Shell shell,
                             ServerManager serverManager ) {
        super(RestClientI18n.editServerActionText.text());
        setToolTipText(RestClientI18n.editServerActionToolTip.text());
        setImageDescriptor(Activator.getDefault().getImageDescriptor(EDIT_SERVER_IMAGE));
        setEnabled(false);

        this.shell = shell;
        this.serverManager = serverManager;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        ServerWizard wizard = new ServerWizard(this.serverManager, this.serverBeingEdited);
        WizardDialog dialog = new WizardDialog(this.shell, wizard) {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.wizard.WizardDialog#configureShell(org.eclipse.swt.widgets.Shell)
             */
            @Override
            protected void configureShell( Shell newShell ) {
                super.configureShell(newShell);
                newShell.setImage(Activator.getDefault().getImage(DNA_IMAGE_16x));
            }
        };

        dialog.open();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    protected boolean updateSelection( IStructuredSelection selection ) {
        // disable if empty selection or multiple objects selected
        if (selection.isEmpty() || (selection.size() > 1)) {
            this.serverBeingEdited = null;
            return false;
        }

        Object obj = selection.getFirstElement();

        // enable if server is selected
        if (obj instanceof Server) {
            this.serverBeingEdited = (Server)obj;
            return true;
        }

        // disable if non-server is selected
        this.serverBeingEdited = null;
        return false;
    }

}
