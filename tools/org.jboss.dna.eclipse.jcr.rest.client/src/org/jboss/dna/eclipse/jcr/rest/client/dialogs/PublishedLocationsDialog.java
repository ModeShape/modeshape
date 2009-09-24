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

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.BLANK_IMAGE;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_IMAGE_16x;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.REPOSITORY_IMAGE;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.SERVER_IMAGE;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.WORKSPACE_IMAGE;
import java.io.File;
import java.util.Collection;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>PublishedLocationsDialog</code> class provides a UI for viewing a list of {@link Server servers} a selected file has
 * been published to.
 */
public final class PublishedLocationsDialog extends MessageDialog {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The column index of the server URL.
     */
    private static final int SERVER_URL_COL = 0;

    /**
     * The column index of the server user.
     */
    private static final int USER_COL = 1;

    /**
     * The column index of the repository name.
     */
    private static final int REPOSITORY_COL = 2;

    /**
     * The column index of the workspace name.
     */
    private static final int WORKSPACE_COL = 3;

    /**
     * The column index of the URL where the file was published.
     */
    private static final int FILE_URL_COL = 4;

    /**
     * The column indexes of all columns.
     */
    private static final int[] COLUMNS = {SERVER_URL_COL, USER_COL, REPOSITORY_COL, WORKSPACE_COL, FILE_URL_COL};

    /**
     * The column headers.
     */
    private static final String[] HEADERS = {RestClientI18n.publishedLocationsDialogServerUrlColumnHeader.text(),
        RestClientI18n.publishedLocationsDialogUserColumnHeader.text(),
        RestClientI18n.publishedLocationsDialogRepositoryColumnHeader.text(),
        RestClientI18n.publishedLocationsDialogWorkspaceColumnHeader.text(),
        RestClientI18n.publishedLocationsDialogFileUrlColumnHeader.text(),};

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The button that copies the file URL to the clipboard.
     */
    private Button btnCopy;

    /**
     * The file whose workspaces it was published to is being displayed by this dialog.
     */
    private final IFile file;

    /**
     * The server manager who can obtain the URL for the file at each of the workspaces.
     */
    private final ServerManager serverManager;

    /**
     * The viewer of the table holding the published locations.
     */
    private TableViewer viewer;

    /**
     * Collection of workspaces the selected file has been published to.
     */
    private final Collection<Workspace> workspaces;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param parentShell the dialog parent
     * @param serverManager the server manager that this dialog will get URLs from (never <code>null</code>)
     * @param file the file whose workspaces it has been published on is being requested (never <code>null</code>)
     * @param workspaces the workspaces (never <code>null</code>)
     */
    public PublishedLocationsDialog( Shell parentShell,
                                     ServerManager serverManager,
                                     IFile file,
                                     Collection<Workspace> workspaces ) {
        super(parentShell, RestClientI18n.publishedLocationsDialogTitle.text(), Activator.getDefault().getImage(DNA_IMAGE_16x),
              RestClientI18n.publishedLocationsDialogMsg.text(file.getFullPath()), MessageDialog.INFORMATION,
              new String[] {IDialogConstants.OK_LABEL}, 0);

        CheckArg.isNotNull(serverManager, "serverManager"); //$NON-NLS-1$
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$
        CheckArg.isNotNull(workspaces, "workspaces"); //$NON-NLS-1$

        this.file = file;
        this.serverManager = serverManager;
        this.workspaces = workspaces;

        // make sure dialog is resizable
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createCustomArea( Composite parent ) {
        // layout consists of a panel that contains a table and a button
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(2, false));
        panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        //
        // construct table
        //

        this.viewer = new TableViewer(panel, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        LocationsTableProvider provider = new LocationsTableProvider();
        this.viewer.setLabelProvider(provider);
        this.viewer.setContentProvider(provider);
        this.viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
             */
            @Override
            public void selectionChanged( SelectionChangedEvent e ) {
                handleTableSelection();
            }
        });

        // configure table
        Table table = this.viewer.getTable();
        table.setLayout(new TableLayout());
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        // create columns
        for (int numCols = COLUMNS.length, i = 0; i < numCols; ++i) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(HEADERS[i]);

            // set image
            Image image = null;

            if ((i == SERVER_URL_COL) || (i == USER_COL)) {
                image = Activator.getDefault().getImage(SERVER_IMAGE);
            } else if (i == REPOSITORY_COL) {
                image = Activator.getDefault().getImage(REPOSITORY_IMAGE);
            } else if (i == WORKSPACE_COL) {
                image = Activator.getDefault().getImage(WORKSPACE_IMAGE);
            } else {
                image = Activator.getDefault().getImage(BLANK_IMAGE);
            }

            column.setImage(image);
        }

        // populate the table
        this.viewer.setInput(this);

        // size columns
        for (TableColumn column : table.getColumns()) {
            column.pack();
            column.setWidth(column.getWidth() + 10);
        }

        //
        // construct button
        //

        this.btnCopy = new Button(panel, SWT.PUSH);
        this.btnCopy.setText(RestClientI18n.publishedLocationsDialogCopyUrlButton.text());
        this.btnCopy.setToolTipText(RestClientI18n.publishedLocationsDialogCopyUrlButtonToolTip.text());
        this.btnCopy.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected( SelectionEvent e ) {
                handleCopyUrl();
            }
        });

        return panel;
    }

    /**
     * @return the file system file this dialog is showing the published locations of (never <code>null</code>)
     */
    private File getFile() {
        return this.file.getLocation().toFile();
    }

    /**
     * This path does not include the name of the file.
     * 
     * @return the path the file was published (never <code>null</code>)
     */
    private String getPath() {
        return this.file.getParent().getFullPath().toString();
    }

    /**
     * @param workspace the workspace where the file was published
     * @return the URL where the file was published
     */
    String getPublishedAtUrl( Workspace workspace ) {
        try {
            return this.serverManager.getUrl(getFile(), getPath(), workspace).toString();
        } catch (Exception e) {
            String message = RestClientI18n.publishedLocationsDialogErrorObtainingUrlMsg.text();
            Activator.getDefault().log(new Status(Severity.ERROR, message, e));
            return message;
        }

    }

    /**
     * @return the workspaces the file has been published to (never <code>null</code>)
     */
    Object[] getWorkspaces() {
        return this.workspaces.toArray();
    }

    /**
     * Handler for when the copy URL button is clicked.
     */
    void handleCopyUrl() {
        Workspace workspace = (Workspace)((IStructuredSelection)this.viewer.getSelection()).getFirstElement();
        String url = getPublishedAtUrl(workspace);
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        clipboard.setContents(new Object[] {url}, new Transfer[] {TextTransfer.getInstance()});
    }

    /**
     * Handler for when a table row is selected.
     */
    void handleTableSelection() {
        IStructuredSelection selection = (IStructuredSelection)this.viewer.getSelection();
        boolean enable = (selection.size() == 1);

        if (this.btnCopy.getEnabled() != enable) {
            this.btnCopy.setEnabled(enable);
        }
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

    // ===========================================================================================================================
    // Inner Class
    // ===========================================================================================================================

    /**
     * The <code>LocationsTableProvider</code> provides content, labels, and images for the table.
     */
    class LocationsTableProvider implements IStructuredContentProvider, ITableLabelProvider {
        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
         */
        @Override
        public void addListener( ILabelProviderListener listener ) {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
         */
        @Override
        public void dispose() {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
         */
        @Override
        public Image getColumnImage( Object element,
                                     int columnIndex ) {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText( Object element,
                                     int columnIndex ) {
            Workspace workspace = (Workspace)element;

            if (columnIndex == SERVER_URL_COL) {
                return workspace.getServer().getUrl();
            }

            if (columnIndex == USER_COL) {
                return workspace.getServer().getUser();
            }

            if (columnIndex == REPOSITORY_COL) {
                return workspace.getRepository().getName();
            }

            if (columnIndex == WORKSPACE_COL) {
                return workspace.getName();
            }

            if (columnIndex == FILE_URL_COL) {
                return getPublishedAtUrl(workspace);
            }

            // should never get here
            assert false;
            return ""; //$NON-NLS-1$
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements( Object inputElement ) {
            return getWorkspaces();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void inputChanged( Viewer viewer,
                                  Object oldInput,
                                  Object newInput ) {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
         */
        @Override
        public boolean isLabelProperty( Object element,
                                        String property ) {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
         */
        @Override
        public void removeListener( ILabelProviderListener listener ) {
            // nothing to do
        }
    }

}
