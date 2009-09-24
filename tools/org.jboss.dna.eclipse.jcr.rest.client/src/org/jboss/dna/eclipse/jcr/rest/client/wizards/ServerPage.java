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

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.SERVER_DIALOG_HELP_CONTEXT;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.validation.ServerValidator;

/**
 * The <code>ServerPage</code> is used to create or modify a server.
 */
public final class ServerPage extends WizardPage {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The button used to test the connection to the server. Should only be enabled when server properties are valid.
     */
    private Button btnTestConnection;

    /**
     * The user password needed to login to the server.
     */
    private String password;

    /**
     * Indicates if the password should be persisted.
     */
    private boolean savePassword;

    /**
     * The server being editor or <code>null</code> if creating a new server.
     */
    private Server server;

    /**
     * The current validation status.
     */
    private Status status;

    /**
     * The server URL.
     */
    private String url;

    /**
     * The user needed to login to the server.
     */
    private String user;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a wizard page that will create a new server.
     */
    public ServerPage() {
        super(ServerPage.class.getSimpleName());
        setTitle(RestClientI18n.serverPageTitle.text());
        setPageComplete(false);
    }

    /**
     * Constructs a wizard page that edits the specified server's properties.
     * 
     * @param server the server being edited
     */
    public ServerPage( Server server ) {
        super(ServerPage.class.getSimpleName());
        setTitle(RestClientI18n.serverPageTitle.text());

        this.server = server;
        this.url = server.getUrl();
        this.user = server.getUser();
        this.password = server.getPassword();
        this.savePassword = server.isPasswordBeingPersisted();
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    private void constructAuthenticationPanel( Composite parent ) {
        Group pnl = new Group(parent, SWT.NONE);
        pnl.setText(RestClientI18n.serverPageAuthenticationGroupTitle.text());
        pnl.setLayout(new GridLayout(2, false));
        pnl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        { // user row
            Label lblUser = new Label(pnl, SWT.LEFT);
            lblUser.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            lblUser.setText(RestClientI18n.serverPageUserLabel.text());

            Text txtUser = new Text(pnl, SWT.BORDER);
            txtUser.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            txtUser.setToolTipText(RestClientI18n.serverPageUserToolTip.text());

            // set initial value
            if (this.user != null) {
                txtUser.setText(this.user);
            }

            txtUser.addModifyListener(new ModifyListener() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
                 */
                @Override
                public void modifyText( ModifyEvent e ) {
                    handleUserModified(((Text)e.widget).getText());
                }
            });
        }

        { // password row
            Label lblPassword = new Label(pnl, SWT.LEFT);
            lblPassword.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            lblPassword.setText(RestClientI18n.serverPagePasswordLabel.text());

            Text txtPassword = new Text(pnl, SWT.BORDER);
            txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            txtPassword.setToolTipText(RestClientI18n.serverPagePasswordToolTip.text());
            txtPassword.setEchoChar('*');

            // set initial value before hooking up listener
            if (this.password != null) {
                txtPassword.setText(this.password);
            }

            // listener for when value changes
            txtPassword.addModifyListener(new ModifyListener() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
                 */
                @Override
                public void modifyText( ModifyEvent e ) {
                    handlePasswordModified(((Text)e.widget).getText());
                }
            });
        }

        { // save button row
            final Button btn = new Button(pnl, SWT.CHECK | SWT.LEFT);
            btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            ((GridData)btn.getLayoutData()).horizontalSpan = 2;
            btn.setText(RestClientI18n.serverPageSavePasswordButton.text());
            btn.setToolTipText(RestClientI18n.serverPageSavePasswordToolTip.text());

            // set initial value before hooking up listeners
            if (this.savePassword) {
                btn.setSelection(true);
            }

            // listener for when value changes
            btn.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    handleSavePasswordChanged(((Button)e.widget).getSelection());
                }
            });

            // update page message first time selected to get rid of initial message by forcing validation
            btn.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    updateInitialMessage();
                    btn.removeSelectionListener(this);
                }
            });
        }

        { // save password message row
            Label lblImage = new Label(pnl, SWT.NONE);
            lblImage.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
            lblImage.setImage(Display.getDefault().getSystemImage(SWT.ICON_INFORMATION));

            StyledText st = new StyledText(pnl, SWT.READ_ONLY | SWT.MULTI | SWT.NO_FOCUS | SWT.WRAP);
            st.setText(RestClientI18n.serverPageSavePasswordLabel.text());
            st.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            st.setCaret(null);
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, true);
            gd.grabExcessVerticalSpace = false;
            gd.horizontalIndent = 4;
            gd.verticalIndent = 8;
            gd.widthHint = 100;
            st.setLayoutData(gd);
        }
    }

    private void constructTestConnectionPanel( Composite parent ) {
        Composite pnl = new Composite(parent, SWT.NONE);
        pnl.setLayout(new GridLayout(2, false));
        pnl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lbl = new Label(pnl, SWT.LEFT);
        lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        lbl.setText(RestClientI18n.serverPageTestConnectionLabel.text());

        this.btnTestConnection = new Button(pnl, SWT.PUSH);
        this.btnTestConnection.setText(RestClientI18n.serverPageTestConnectionButton.text());
        this.btnTestConnection.setToolTipText(RestClientI18n.serverPageTestConnectionButtonToolTip.text());

        // add margins to the side of the text
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        Point minSize = this.btnTestConnection.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        gd.widthHint = Math.max(widthHint, minSize.x + 10);
        this.btnTestConnection.setLayoutData(gd);

        this.btnTestConnection.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected( SelectionEvent e ) {
                handleTestConnection();
            }
        });
    }

    private void constructUrlPanel( Composite parent ) {
        Composite pnl = new Composite(parent, SWT.NONE);
        pnl.setLayout(new GridLayout(2, false));
        pnl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblUrl = new Label(pnl, SWT.LEFT);
        lblUrl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        lblUrl.setText(RestClientI18n.serverPageUrlLabel.text());

        Text txtUrl = new Text(pnl, SWT.BORDER);
        txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        txtUrl.setToolTipText(RestClientI18n.serverPageUrlToolTip.text());

        // set initial value
        if (this.url != null) {
            txtUrl.setText(this.url);
        }

        txtUrl.addModifyListener(new ModifyListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
             */
            @Override
            public void modifyText( ModifyEvent e ) {
                handleUrlModified(((Text)e.widget).getText());
            }
        });
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
        constructUrlPanel(pnlMain);
        constructAuthenticationPanel(pnlMain);
        constructTestConnectionPanel(pnlMain);
        setControl(pnlMain);

        // register with the help system
        IWorkbenchHelpSystem helpSystem = Activator.getDefault().getWorkbench().getHelpSystem();
        helpSystem.setHelp(pnlMain, SERVER_DIALOG_HELP_CONTEXT);
    }

    /**
     * @return the server represented by the dialog inputs
     * @throws RuntimeException if called when all inputs are not valid
     * @see #isPageComplete()
     */
    public Server getServer() {
        if (!this.status.isError()) {
            return new Server(this.url, this.user, this.password, this.savePassword);
        }

        // should never be called if error status
        throw new RuntimeException(RestClientI18n.serverPageInvalidServerProperties.text());
    }

    /**
     * @return the server manager obtained from the wizard
     */
    ServerManager getServerManager() {
        return ((ServerWizard)getWizard()).getServerManager();
    }

    /**
     * Handler for when the password control value is modified
     * 
     * @param newPassword the new password value
     */
    void handlePasswordModified( String newPassword ) {
        this.password = newPassword;
        updateState();
    }

    /**
     * @param savePassword <code>true</code> if the password should be persisted on the local file system
     */
    void handleSavePasswordChanged( boolean savePassword ) {
        this.savePassword = savePassword;
    }

    /**
     * Tests the connection of the server specified by the properties entered on this page. Precondition is that server properties
     * are valid.
     */
    void handleTestConnection() {
        final Server server = getServer();
        final boolean[] success = new boolean[1];

        BusyIndicator.showWhile(null, new Runnable() {
            /**
             * {@inheritDoc}
             * 
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                success[0] = getServerManager().ping(server).isOk();
            }
        });

        if (success[0]) {
            MessageDialog.openInformation(getShell(),
                                          RestClientI18n.serverPageTestConnectionDialogTitle.text(),
                                          RestClientI18n.serverPageTestConnectionDialogSuccessMsg.text());
        } else {
            MessageDialog.openError(getShell(),
                                    RestClientI18n.serverPageTestConnectionDialogTitle.text(),
                                    RestClientI18n.serverPageTestConnectionDialogFailureMsg.text());
        }
    }

    /**
     * Handler for when the URL control value is modified
     * 
     * @param newUrl the new URL value
     */
    void handleUrlModified( String newUrl ) {
        this.url = newUrl;
        updateState();
    }

    /**
     * Handler for when the user control value is modified
     * 
     * @param newUser the new user value
     */
    void handleUserModified( String newUser ) {
        this.user = newUser;
        updateState();
    }

    /**
     * If the initial message is being displayed do a validation.
     */
    void updateInitialMessage() {
        if (RestClientI18n.serverPageOkStatusMsg.text().equals(getMessage())) {
            updateState();
        }
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
            // set initial state
            validate();

            // set initial message
            setMessage(RestClientI18n.serverPageOkStatusMsg.text());
        }
    }

    /**
     * Updates message, message icon, and OK button enablement based on validation results
     */
    private void updateState() {
        // get the current status
        validate();

        // update OK/Finish button and test button enablement
        setPageComplete(!this.status.isError());
        this.btnTestConnection.setEnabled(isPageComplete());

        // update message
        if (this.status.isError()) {
            setMessage(this.status.getMessage(), IMessageProvider.ERROR);
        } else {
            if (this.status.isWarning()) {
                setMessage(this.status.getMessage(), IMessageProvider.WARNING);
            } else if (this.status.isInfo()) {
                setMessage(this.status.getMessage(), IMessageProvider.INFORMATION);
            } else {
                setMessage(RestClientI18n.serverPageOkStatusMsg.text());
            }
        }
    }

    /**
     * Validates all inputs and sets the validation status.
     */
    private void validate() {
        if (this.server == null) {
            this.status = ServerValidator.isValid(this.url, this.user, this.password, this.savePassword, getServerManager());
        } else {
            // make sure all fields are valid without seeing if server exists in registry
            this.status = ServerValidator.isValid(this.url, this.user, this.password, this.savePassword);

            // if server is valid and is different from what we started with validate and check the registry
            if (this.status.isOk() && !this.server.hasSameKey(getServer())) {
                this.status = ServerValidator.isValid(this.url, this.user, this.password, this.savePassword, getServerManager());
            }
        }
    }

}
