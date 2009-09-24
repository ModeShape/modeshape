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

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_WIZARD_BANNER_IMAGE;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>ServerWizard</code> is the wizard used to create and edit servers.
 */
public final class ServerWizard extends Wizard {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * Non-<code>null</code> if the wizard is editing an existing server.
     */
    private Server existingServer;

    /**
     * The wizard page containing all the controls that allow editing of server properties.
     */
    private final ServerPage page;

    /**
     * The manager in charge of the server registry.
     */
    private final ServerManager serverManager;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a wizard that creates a new server.
     * 
     * @param serverManager the server manager in charge of the server registry (never <code>null</code>)
     */
    public ServerWizard( ServerManager serverManager ) {
        this.page = new ServerPage();
        this.serverManager = serverManager;

        setDefaultPageImageDescriptor(Activator.getDefault().getImageDescriptor(DNA_WIZARD_BANNER_IMAGE));
        setWindowTitle(RestClientI18n.serverWizardNewServerTitle.text());
    }

    /**
     * Constructs a wizard that edits an existing server.
     * 
     * @param serverManager the server manager in charge of the server registry (never <code>null</code>)
     * @param server the server whose properties are being edited (never <code>null</code>)
     */
    public ServerWizard( ServerManager serverManager,
                         Server server ) {
        this.page = new ServerPage(server);
        this.serverManager = serverManager;
        this.existingServer = server;
        setWindowTitle(RestClientI18n.serverWizardEditServerTitle.text());
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        addPage(this.page);
    }

    /**
     * @return the server manager (never <code>null</code>)
     */
    protected ServerManager getServerManager() {
        return this.serverManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        Status status = Status.OK_STATUS;
        Server server = this.page.getServer();

        if (this.existingServer == null) {
            status = this.serverManager.addServer(server);

            if (status.isError()) {
                MessageDialog.openError(getShell(),
                                        RestClientI18n.errorDialogTitle.text(),
                                        RestClientI18n.serverWizardEditServerErrorMsg.text());
            }
        } else if (!this.existingServer.equals(server)) {
            status = this.serverManager.updateServer(this.existingServer, server);

            if (status.isError()) {
                MessageDialog.openError(getShell(),
                                        RestClientI18n.errorDialogTitle.text(),
                                        RestClientI18n.serverWizardNewServerErrorMsg.text());
            }
        }

        // log if necessary
        if (!status.isOk()) {
            Activator.getDefault().log(status);
        }

        return !status.isError();
    }

}
