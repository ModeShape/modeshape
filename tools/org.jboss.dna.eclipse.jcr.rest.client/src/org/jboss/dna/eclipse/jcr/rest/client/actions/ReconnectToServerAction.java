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

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.REFRESH_IMAGE;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.jobs.ReconnectJob;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>ReconnectToServerAction</code> tries to reconnect to a selected server.
 */
public final class ReconnectToServerAction extends BaseSelectionListenerAction {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The server view tree viewer.
     */
    private final TreeViewer viewer;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param viewer the server view tree viewer
     */
    public ReconnectToServerAction( TreeViewer viewer ) {
        super(RestClientI18n.serverReconnectActionText.text());
        setToolTipText(RestClientI18n.serverReconnectActionToolTip.text());
        setImageDescriptor(Activator.getDefault().getImageDescriptor(REFRESH_IMAGE));
        setEnabled(false);

        this.viewer = viewer;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the view's tree viewer
     */
    StructuredViewer getViewer() {
        return this.viewer;
    }

    /**
     * @param server the server being connected to
     */
    void refresh( final Server server ) {
        final Display display = this.viewer.getControl().getDisplay();

        if (!display.isDisposed()) {
            // make sure we are in the UI thread
            display.asyncExec(new Runnable() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                    getViewer().refresh(server);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        final Server server = (Server)getStructuredSelection().getFirstElement();
        final ReconnectJob job = new ReconnectJob(server);

        // add listener so we can refresh tree
        job.addJobChangeListener(new JobChangeAdapter() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
             */
            @Override
            public void done( IJobChangeEvent event ) {
                refresh(server);
                job.removeJobChangeListener(this);
            }
        });

        // run job in own thread not in the UI thread
        Thread t = new Thread();
        t.run();
        job.setThread(t);
        job.schedule();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    protected boolean updateSelection( IStructuredSelection selection ) {
        return ((selection.size() == 1) && (selection.getFirstElement() instanceof Server));
    }

}
