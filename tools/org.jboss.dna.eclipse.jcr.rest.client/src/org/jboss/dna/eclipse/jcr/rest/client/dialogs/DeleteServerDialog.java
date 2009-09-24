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
package org.jboss.dna.eclipse.jcr.rest.client.dialogs;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_IMAGE_16x;
import java.util.Collection;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.domain.IDnaObject;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>DeleteServerDialog</code> class provides a UI for deleting a {@link Server server}.
 */
public final class DeleteServerDialog extends MessageDialog {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * Collection of servers which will be deleted.
     */
    private final Collection<Server> serversBeingDeleted;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param parentShell the dialog parent
     * @param serversBeingDeleted the servers being deleted (may not be <code>null</code>)
     */
    public DeleteServerDialog( Shell parentShell,
                               Collection<Server> serversBeingDeleted ) {
        super(parentShell, RestClientI18n.deleteServerDialogTitle.text(), Activator.getDefault().getImage(DNA_IMAGE_16x), null,
              MessageDialog.QUESTION, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
        
        CheckArg.isNotNull(serversBeingDeleted, "serversBeingDeleted"); //$NON-NLS-1$
        this.serversBeingDeleted = serversBeingDeleted;

        // make sure dialog is resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.MessageDialog#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell( Shell shell ) {
        super.configureShell(shell);

        // now set message
        String msg;

        if (this.serversBeingDeleted.size() == 1) {
            Server server = this.serversBeingDeleted.iterator().next();
            msg = RestClientI18n.deleteServerDialogOneServerMsg.text(server.getName(), server.getUser());
        } else {
            msg = RestClientI18n.deleteServerDialogMultipleServersMsg.text(this.serversBeingDeleted.size());
        }

        this.message = msg;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createCustomArea( Composite parent ) {
        if (this.serversBeingDeleted.size() != 1) {
            List serverList = new List(parent, SWT.NONE);
            serverList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, true);
            gd.horizontalIndent = 40;
            serverList.setLayoutData(gd);

            for (IDnaObject server : this.serversBeingDeleted) {
                serverList.add(server.getName());
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
     */
    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        Utils.centerAndSizeShellRelativeToDisplay(getShell(), 75, 75);
    }

}
