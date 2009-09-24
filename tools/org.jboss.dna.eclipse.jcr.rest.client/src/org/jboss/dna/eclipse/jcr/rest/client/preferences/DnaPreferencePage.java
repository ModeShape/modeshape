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
package org.jboss.dna.eclipse.jcr.rest.client.preferences;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_IMAGE_16x;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.PREFERENCE_PAGE_HELP_CONTEXT;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.dnaPreferencePageDescription;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.dnaPreferencePageMessage;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.dnaPreferencePageTitle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;

/**
 * The <code>DnaPreferencePage</code> is the UI for managing all DNA-related preferences.
 */
public final class DnaPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The editor used to manage the list of filtered file extensions.
     */
    private FilteredFileExtensionEditor extensionsEditor;

    /**
     * The editor used to manage the list of filtered folder names.
     */
    private FilteredFoldersEditor foldersEditor;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents( Composite parent ) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(2, false));
        panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // create the filtered extensions editor
        this.extensionsEditor = new FilteredFileExtensionEditor(panel);
        this.extensionsEditor.setPreferenceStore(getPreferenceStore());
        this.extensionsEditor.getListControl(panel).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // populate the extensions editor
        this.extensionsEditor.load();

        // create the filtered folders editor
        this.foldersEditor = new FilteredFoldersEditor(panel);
        this.foldersEditor.setPreferenceStore(getPreferenceStore());
        this.foldersEditor.getListControl(panel).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // populate the folders editor
        this.foldersEditor.load();

        // register with the help system
        IWorkbenchHelpSystem helpSystem = Activator.getDefault().getWorkbench().getHelpSystem();
        helpSystem.setHelp(panel, PREFERENCE_PAGE_HELP_CONTEXT);

        return panel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getDescription()
     */
    @Override
    public String getDescription() {
        return dnaPreferencePageDescription.text();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getImage()
     */
    @Override
    public Image getImage() {
        return Activator.getDefault().getImage(DNA_IMAGE_16x);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getMessage()
     */
    @Override
    public String getMessage() {
        return dnaPreferencePageMessage.text();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.PreferencePage#getPreferenceStore()
     */
    @Override
    public IPreferenceStore getPreferenceStore() {
        return PrefUtils.getPreferenceStore();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.DialogPage#getTitle()
     */
    @Override
    public String getTitle() {
        return dnaPreferencePageTitle.text();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init( IWorkbench workbench ) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        this.extensionsEditor.loadDefault();
        this.foldersEditor.loadDefault();
        super.performDefaults();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        this.extensionsEditor.store();
        this.foldersEditor.store();
        return super.performOk();
    }

}
