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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
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
import org.jboss.dna.web.jcr.rest.client.IServerRegistryListener;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.IDnaObject;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>DnaContentProvider</code> is a content and label provider for DNA repositories. This class <strong>MUST</strong> be
 * registered, and then unregistered, to receive server registry events.
 */
public final class DnaContentProvider extends ColumnLabelProvider
    implements ILightweightLabelDecorator, IServerRegistryListener, ITreeContentProvider {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The decorator ID.
     */
    private static final String ID = "org.jboss.dna.eclipse.jcr.rest.client.dnaDecorator"; //$NON-NLS-1$

    /**
     * If a server connection cannot be established, wait this amount of time before trying again.
     */
    private static final long RETRY_DURATION = 2000;

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

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * Servers that a connection can't be established. Value is the last time a establishing a connection was tried.
     */
    @GuardedBy( "offlineServersLock" )
    private final Map<Server, Long> offlineServerMap = new HashMap<Server, Long>();

    /**
     * Lock used for when accessing the offline server map. The map will be accessed in different threads as the decorator runs in
     * its own thread (not the UI thread).
     */
    private final ReadWriteLock offlineServersLock = new ReentrantReadWriteLock();

    /**
     * The server manager where the server registry is managed.
     */
    private ServerManager serverManager;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @param server the server that is offline
     */
    private void addOfflineServer( Server server ) {
        try {
            this.offlineServersLock.writeLock().lock();
            this.offlineServerMap.put(server, System.currentTimeMillis());
        } finally {
            this.offlineServersLock.writeLock().unlock();
        }
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

        if (getServerManager() != null) {
            if (element instanceof Server) {
                Server server = (Server)element;

                if (isOkToConnect(server)) {
                    Status status = getServerManager().ping(server);
                    overlay = Utils.getOverlayImage(status);

                    if (status.isError()) {
                        addOfflineServer(server);
                    }
                }
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
            if (parentElement instanceof Server) {
                Server server = (Server)parentElement;

                if (isOkToConnect(server)) {
                    try {
                        return getServerManager().getRepositories(server).toArray();
                    } catch (Exception e) {
                        addOfflineServer(server);
                        String msg = RestClientI18n.serverManagerGetRepositoriesExceptionMsg.text(server.getShortDescription());
                        Activator.getDefault().log(new Status(Severity.ERROR, msg, e));
                    }
                }
            } else if (parentElement instanceof Repository) {
                Repository repository = (Repository)parentElement;

                if (isOkToConnect(repository.getServer())) {
                    try {
                        return getServerManager().getWorkspaces(repository).toArray();
                    } catch (Exception e) {
                        addOfflineServer(repository.getServer());
                        String msg = RestClientI18n.serverManagerGetWorkspacesExceptionMsg.text(repository.getShortDescription());
                        Activator.getDefault().log(new Status(Severity.ERROR, msg, e));
                    }
                }
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
        // nothing to do
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
     * Determines if a try to connect to a server should be done based on the last time a try was done and failed.
     * 
     * @param server the server being checked
     * @return <code>true</code> if it is OK to try and connect
     */
    private boolean isOkToConnect( Server server ) {
        boolean check = false; // check map for time

        try {
            this.offlineServersLock.readLock().lock();
            check = this.offlineServerMap.containsKey(server);
        } finally {
            this.offlineServersLock.readLock().unlock();
        }

        if (check) {
            try {
                this.offlineServersLock.writeLock().lock();

                if (this.offlineServerMap.containsKey(server)) {
                    long checkTime = this.offlineServerMap.get(server);

                    // OK to try and connect if last failed attempt was too long ago
                    if ((System.currentTimeMillis() - checkTime) > RETRY_DURATION) {
                        this.offlineServerMap.remove(server);
                        return true;
                    }

                    // don't try and connect because we just tried and failed
                    return false;
                }
            } finally {
                this.offlineServersLock.writeLock().unlock();
            }
        }

        // OK to try and connect
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IServerRegistryListener#serverRegistryChanged(org.jboss.dna.web.jcr.rest.client.ServerRegistryEvent)
     */
    @Override
    public Exception[] serverRegistryChanged( ServerRegistryEvent event ) {
        Exception[] errors = null;

        // only care about servers being removed or updated
        if (event.isRemove() || event.isUpdate()) {
            try {
                this.offlineServersLock.writeLock().lock();
                this.offlineServerMap.remove(event.getServer());
            } catch (Exception e) {
                errors = new Exception[] {e};
            } finally {
                this.offlineServersLock.writeLock().unlock();
            }
        }

        return errors;
    }

}
