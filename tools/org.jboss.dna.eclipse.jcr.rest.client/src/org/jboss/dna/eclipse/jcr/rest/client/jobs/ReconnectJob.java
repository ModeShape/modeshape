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
package org.jboss.dna.eclipse.jcr.rest.client.jobs;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.DNA_PUBLISHING_JOB_FAMILY;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.PLUGIN_ID;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.reconnectJobTaskName;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>ReconnectJob</code> attempts to reconnect to the selected {@link Server server(s)}.
 */
public final class ReconnectJob extends Job {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The server being reconnected to.
     */
    private final Server server;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server being connected to (never <code>null</code>)
     */
    public ReconnectJob( Server server ) {
        super(reconnectJobTaskName.text(server.getShortDescription()));
        this.server = server;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
     */
    @Override
    public boolean belongsTo( Object family ) {
        return DNA_PUBLISHING_JOB_FAMILY.equals(family);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run( IProgressMonitor monitor ) {
        IStatus result = null;
        ServerManager serverManager = Activator.getDefault().getServerManager();

        try {
            String taskName = reconnectJobTaskName.text(this.server.getShortDescription());
            monitor.beginTask(taskName, 1);
            monitor.setTaskName(taskName);
            Status status = serverManager.ping(this.server);
            result = Utils.convert(status);
        } catch (Exception e) {
            String msg = null;

            if (e instanceof InterruptedException) {
                msg = e.getLocalizedMessage();
            } else {
                msg = RestClientI18n.publishJobUnexpectedErrorMsg.text();
            }

            result = new org.eclipse.core.runtime.Status(IStatus.ERROR, PLUGIN_ID, msg, e);
        } finally {
            monitor.done();
            done(result);
        }

        return result;
    }

}
