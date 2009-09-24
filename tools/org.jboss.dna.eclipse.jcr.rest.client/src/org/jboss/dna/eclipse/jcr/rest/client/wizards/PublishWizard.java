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
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.jobs.PublishJob;
import org.jboss.dna.eclipse.jcr.rest.client.jobs.PublishJob.Type;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>PublishWizard</code> is the wizard that published and unpublishes resources.
 */
public final class PublishWizard extends Wizard {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The wizard page containing all the controls that allow publishing/unpublishing of resources.
     */
    private final PublishPage page;

    /**
     * The manager in charge of the server registry.
     */
    private final ServerManager serverManager;

    /**
     * Indicates if the wizard will perform a publishing or unpublishing operation.
     */
    private final Type type;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param type the publishing or unpublishing indicator (never <code>null</code>)
     * @param resources the resources being published or unpublished (never <code>null</code>)
     * @param serverManager the server manager in charge of the server registry (never <code>null</code>)
     * @throws CoreException if there is a problem processing the resources
     */
    public PublishWizard( Type type,
                          List<IResource> resources,
                          ServerManager serverManager ) throws CoreException {
        CheckArg.isNotNull(type, "type"); //$NON-NLS-1$
        CheckArg.isNotNull(resources, "resources"); //$NON-NLS-1$
        CheckArg.isNotNull(serverManager, "serverManager"); //$NON-NLS-1$

        this.type = type;
        this.page = new PublishPage(type, resources);
        this.serverManager = serverManager;

        setWindowTitle((type == Type.PUBLISH) ? RestClientI18n.publishWizardPublishTitle.text()
                                             : RestClientI18n.publishWizardUnpublishTitle.text());
        setDefaultPageImageDescriptor(Activator.getDefault().getImageDescriptor(DNA_WIZARD_BANNER_IMAGE));
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
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#getDialogSettings()
     */
    @Override
    public IDialogSettings getDialogSettings() {
        IDialogSettings settings = super.getDialogSettings();

        if (settings == null) {
            IDialogSettings temp = Activator.getDefault().getDialogSettings();
            settings = temp.getSection(getClass().getSimpleName());

            if (settings == null) {
                settings = temp.addNewSection(getClass().getSimpleName());
            }

            setDialogSettings(settings);
        }

        return super.getDialogSettings();
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
        Workspace workspace = this.page.getWorkspace();
        List<IFile> files = this.page.getFiles();
        PublishJob job = new PublishJob(this.type, files, workspace);
        job.schedule();

        return true;
    }

}
