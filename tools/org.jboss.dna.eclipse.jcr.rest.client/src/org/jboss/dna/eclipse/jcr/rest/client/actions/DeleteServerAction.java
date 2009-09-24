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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.dialogs.DeleteServerDialog;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>DeleteServerAction</code> deletes one or more servers from the server registry.
 */
public final class DeleteServerAction extends BaseSelectionListenerAction {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The server manager used to delete servers.
     */
    private final ServerManager serverManager;

    /**
     * The servers being deleted (never <code>null</code>).
     */
    private final List<Server> serversToDelete;

    /**
     * The shell used to display the delete confirmation dialog.
     */
    private final Shell shell;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param shell the parent shell used to display the confirmation dialog
     * @param serverManager the server manager to use when deleting servers
     */
    public DeleteServerAction( Shell shell,
                               ServerManager serverManager ) {
        super(RestClientI18n.deleteServerActionText.text());
        setToolTipText(RestClientI18n.deleteServerActionToolTip.text());
        setImageDescriptor(Activator.getDefault().getImageDescriptor(DELETE_SERVER_IMAGE));
        setEnabled(false);

        this.serversToDelete = new ArrayList<Server>(5);
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
        Dialog dialog = new DeleteServerDialog(this.shell, this.serversToDelete);

        if (dialog.open() == Window.OK) {
            boolean errorsOccurred = false;

            for (Server server : this.serversToDelete) {
                Status status = this.serverManager.removeServer(server);

                if (!status.isOk()) {
                    Activator.getDefault().log(status);

                    if (status.isError()) {
                        errorsOccurred = true;
                    }
                }
            }

            if (errorsOccurred) {
                MessageDialog.openError(this.shell,
                                        RestClientI18n.errorDialogTitle.text(),
                                        RestClientI18n.deleteServerDialogErrorsOccurredMsg.text());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    protected boolean updateSelection( IStructuredSelection selection ) {
        // reset selected server collection
        this.serversToDelete.clear();

        // disable if empty selection
        if (selection.isEmpty()) {
            return false;
        }

        // disable if one non-server is found
        for (Object obj : selection.toArray()) {
            if (obj instanceof Server) {
                this.serversToDelete.add((Server)obj);
            } else {
                this.serversToDelete.clear();
                return false;
            }
        }

        // enable since all objects are servers
        return true;
    }

}
