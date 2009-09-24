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
package org.jboss.dna.eclipse.jcr.rest.client.views;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.PUBLISHED_OVERLAY_IMAGE;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.DnaResourceHelper;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.IDnaObject;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>DnaContentProvider</code> is a content and label provider for DNA repositories.
 */
public final class DnaContentProvider extends ColumnLabelProvider implements ILightweightLabelDecorator, ITreeContentProvider {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The decorator ID.
     */
    private static final String ID = "org.jboss.dna.eclipse.jcr.rest.client.dnaDecorator"; //$NON-NLS-1$

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @return the DNA decorator
     */
    public static DnaContentProvider getDecorator() {
        IDecoratorManager decoratorMgr = Activator.getDefault().getWorkbench().getDecoratorManager();

        if (decoratorMgr.getEnabled(ID)) {
            return (DnaContentProvider)decoratorMgr.getBaseLabelProvider(ID);
        }

        return null;
    }

    // ===========================================dnaDecorator================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The server manager where the server registry is managed.
     */
    private ServerManager serverManager;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

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
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
     */
    @Override
    public void decorate( final Object element,
                          IDecoration decoration ) {
        ImageDescriptor overlay = null;
        final Display display = Display.getDefault();

        if (display.isDisposed()) {
            return;
        }

        // must be an IDnaObject
        if (getServerManager() != null) {
            if (element instanceof Server) {
                Status status = getServerManager().ping((Server)element);
                overlay = Utils.getOverlayImage(status);
            } else if ((element instanceof IFile) && new DnaResourceHelper(getServerManager()).isPublished((IFile)element)) {
                overlay = Activator.getDefault().getImageDescriptor(PUBLISHED_OVERLAY_IMAGE);
            }

            if (overlay != null) {
                decoration.addOverlay(overlay);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren( Object parentElement ) {
        assert (parentElement instanceof IDnaObject);

        if (getServerManager() != null) {
            try {
                if ((parentElement instanceof Server) && getServerManager().ping((Server)parentElement).isOk()) {
                    return getServerManager().getRepositories((Server)parentElement).toArray();
                }
            } catch (Exception e) {
                String msg = RestClientI18n.serverManagerGetRepositoriesExceptionMsg.text(((Server)parentElement).getShortDescription());
                Activator.getDefault().log(new Status(Severity.ERROR, msg, e));
            }

            try {
                if (parentElement instanceof Repository) {
                    return getServerManager().getWorkspaces((Repository)parentElement).toArray();
                }
            } catch (Exception e) {
                String msg = RestClientI18n.serverManagerGetWorkspacesExceptionMsg.text();
                Activator.getDefault().log(new Status(Severity.ERROR, msg, e));
            }
        }

        return new Object[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements( Object inputElement ) {
        return ((getServerManager() == null) ? new Object[0] : getServerManager().getServers().toArray());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage( Object element ) {
        return Activator.getDefault().getImage(element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    @Override
    public Object getParent( Object element ) {
        assert (element instanceof IDnaObject);

        if (element instanceof Workspace) {
            return ((Workspace)element).getRepository();
        }

        if (element instanceof Repository) {
            return ((Repository)element).getServer();
        }

        // server
        return null;
    }

    /**
     * @return the server manager or <code>null</code>
     */
    private ServerManager getServerManager() {
        if (this.serverManager == null) {
            this.serverManager = Activator.getDefault().getServerManager();
        }

        return this.serverManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText( Object element ) {
        assert (element instanceof IDnaObject);
        return ((IDnaObject)element).getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipImage(java.lang.Object)
     */
    @Override
    public Image getToolTipImage( Object object ) {
        return getImage(object);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
     */
    @Override
    public String getToolTipText( Object element ) {
        if (element instanceof IDnaObject) {
            return ((IDnaObject)element).getShortDescription();
        }

        return super.getToolTipText(element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
     */
    @Override
    public int getToolTipTimeDisplayed( Object object ) {
        return 3000;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren( Object element ) {
        return getChildren(element).length > 0;
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
        // this.viewer = (StructuredViewer)viewer;
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

    public void refresh( final Object element ) {
        final Display display = Display.getDefault();

        if (display.isDisposed()) {
            return;
        }

        display.asyncExec(new Runnable() {
            /**
             * {@inheritDoc}
             * 
             * @see java.lang.Runnable#run()
             */
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void run() {
                fireLabelProviderChanged(new LabelProviderChangedEvent(DnaContentProvider.this, element));
            }
        });
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
