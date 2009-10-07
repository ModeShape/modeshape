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
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.DnaResourceHelper;
import org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n;
import org.jboss.dna.eclipse.jcr.rest.client.views.DnaContentProvider;
import org.jboss.dna.eclipse.jcr.rest.client.views.DnaMessageConsole;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>PublishJob</code> publishes or unpublishes one or more files using the {@link ServerManager}.
 */
public final class PublishJob extends Job {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The job type.
     */
    public enum Type {
        /**
         * Indicates a publish job.
         */
        PUBLISH,

        /**
         * Indicates an unpublish job.
         */
        UNPUBLISH
    }

    /**
     * A unique job identifier given to each publishing/unpublishing job.
     */
    private static final AtomicInteger JOB_ID = new AtomicInteger();

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param type the job type (never <code>null</code>)
     * @param jobId the job identifier
     * @return the job name
     */
    private static String getJobName( Type type,
                                      int jobId ) {
        CheckArg.isNotNull(type, "type"); //$NON-NLS-1$

        if (Type.PUBLISH == type) {
            return RestClientI18n.publishJobPublishName.text(jobId);
        }

        // unpublish
        return RestClientI18n.publishJobUnpublishName.text(jobId);
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The files being published or unpublished.
     */
    private final List<IFile> files;

    /**
     * The unique job identifier.
     */
    private final int jobId;

    /**
     * The job type.
     */
    private final Type type;

    /**
     * The workspace to use when publishing or unpublishing.
     */
    private final Workspace workspace;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param type the job type (never <code>null</code>)
     * @param files the files being published or unpublished (never <code>null</code>)
     * @param workspace the workspace to use when publishing or unpublishing (never <code>null</code>)
     */
    public PublishJob( Type type,
                       List<IFile> files,
                       Workspace workspace ) {
        super(getJobName(type, JOB_ID.incrementAndGet()));

        CheckArg.isNotNull(files, "files"); //$NON-NLS-1$

        this.type = type;
        this.files = files;
        this.workspace = workspace;
        this.jobId = JOB_ID.get();

        setUser(true); // allow user to run in background
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
     * @return the server manager
     */
    private ServerManager getServerManager() {
        return Activator.getDefault().getServerManager();
    }

    /**
     * @return <code>true</code> if a publishing job
     */
    private boolean isPublishing() {
        return (this.type == Type.PUBLISH);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run( IProgressMonitor monitor ) {
        assert (this.workspace != null);
        long startTime = System.currentTimeMillis();
        boolean canceled = false;
        int numProcessed = 0;

        try {
            int fileCount = this.files.size();
            String name = (isPublishing() ? RestClientI18n.publishJobPublishTaskName.text(this.jobId)
                                         : RestClientI18n.publishJobUnpublishTaskName.text(this.jobId));
            monitor.beginTask(name, fileCount);
            monitor.setTaskName(name);

            String serverUrl = this.workspace.getServer().getUrl();
            String repositoryName = this.workspace.getRepository().getName();
            String workspaceName = this.workspace.getName();

            // write initial message to console
            if (isPublishing()) {
                DnaMessageConsole.writeln(RestClientI18n.publishJobPublish.text(this.jobId,
                                                                                serverUrl,
                                                                                repositoryName,
                                                                                workspaceName,
                                                                                fileCount));
            } else {
                DnaMessageConsole.writeln(RestClientI18n.publishJobUnpublish.text(this.jobId,
                                                                                  serverUrl,
                                                                                  repositoryName,
                                                                                  workspaceName,
                                                                                  fileCount));
            }

            DnaResourceHelper resourceHelper = new DnaResourceHelper(getServerManager());

            // process the files
            for (IFile eclipseFile : this.files) {
                if (monitor.isCanceled()) {
                    canceled = true;
                    throw new InterruptedException(RestClientI18n.publishJobCanceled.text(jobId));
                }

                File file = eclipseFile.getLocation().toFile();
                String path = eclipseFile.getParent().getFullPath().toString();
                Status status = null;

                if (isPublishing()) {
                    status = getServerManager().publish(this.workspace, path, file);

                    // set persistent property on resource indicating it has been published
                    if (!status.isError()) {
                        resourceHelper.addPublishedProperty(eclipseFile, workspace);
                    }
                } else {
                    status = getServerManager().unpublish(this.workspace, path, file);

                    // clear persistent property on resource indicating it has been unpublished
                    if (!status.isError()) {
                        resourceHelper.removePublishedProperty(eclipseFile, workspace);
                    }
                }

                ++numProcessed;
                monitor.worked(1);

                // let decorator know publishing state has changed on this file
                DnaContentProvider decorator = DnaContentProvider.getDecorator();

                if (decorator != null) {
                    decorator.refresh(eclipseFile);
                }

                // write outcome message to console
                if (isPublishing() && status.isOk()) {
                    URL url = getServerManager().getUrl(file, path, this.workspace);
                    writeToConsole(eclipseFile, url, status);
                } else {
                    writeToConsole(eclipseFile, null, status);
                }
            }

            return org.eclipse.core.runtime.Status.OK_STATUS;
        } catch (Exception e) {
            String msg = null;

            if (e instanceof InterruptedException) {
                msg = e.getLocalizedMessage();
            } else {
                msg = RestClientI18n.publishJobUnexpectedErrorMsg.text();
            }

            return new org.eclipse.core.runtime.Status(IStatus.INFO, PLUGIN_ID, msg, e);
        } finally {
            monitor.done();

            // add operation completed message
            String duration;
            long milliseconds = (System.currentTimeMillis() - startTime);
            long hours = milliseconds / (1000 * 60 * 60);
            long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = ((milliseconds % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

            if (hours > 0) {
                duration = RestClientI18n.publishJobDurationMsg.text(hours, minutes, seconds);
            } else if (minutes > 0) {
                duration = RestClientI18n.publishJobDurationNoHoursMsg.text(minutes, seconds);
            } else if (seconds > 0) {
                duration = RestClientI18n.publishJobDurationNoHoursNoMinutesMsg.text(seconds);
            } else {
                duration = RestClientI18n.publishJobDurationShortMsg.text();
            }

            if (canceled) {
                if (isPublishing()) {
                    DnaMessageConsole.writeln(RestClientI18n.publishJobPublishCanceledMsg.text(this.jobId,
                                                                                               numProcessed,
                                                                                               this.files.size(),
                                                                                               duration));
                } else {
                    DnaMessageConsole.writeln(RestClientI18n.publishJobUnpublishCanceledMsg.text(this.jobId,
                                                                                                 numProcessed,
                                                                                                 this.files.size(),
                                                                                                 duration));
                }
            } else {
                if (isPublishing()) {
                    DnaMessageConsole.writeln(RestClientI18n.publishJobPublishFinishedMsg.text(this.jobId, duration));
                } else {
                    DnaMessageConsole.writeln(RestClientI18n.publishJobUnpublishFinishedMsg.text(this.jobId, duration));
                }
            }
        }
    }

    /**
     * Create a hyperlink in console.
     * 
     * @param file the file involved in the publishing operation
     * @param url the destination file URL or <code>null</code>
     * @param status the status of the publishing operation
     */
    private void writeToConsole( final IFile file,
                                 URL url,
                                 Status status ) {
        String message = null;

        if (status.isOk()) {
            if (isPublishing()) {
                message = RestClientI18n.publishJobPublishFile.text(this.jobId, file.getFullPath(), url.toString());
            } else {
                message = RestClientI18n.publishJobUnpublishFile.text(this.jobId, file.getFullPath());
            }
        } else if (status.isError()) {
            if (isPublishing()) {
                message = RestClientI18n.publishJobPublishFileFailed.text(this.jobId, file.getFullPath());
            } else {
                message = RestClientI18n.publishJobUnpublishFileFailed.text(this.jobId, file.getFullPath());
            }

            // log
            Activator.getDefault().log(status);
        } else if (status.isWarning()) {
            if (isPublishing()) {
                message = RestClientI18n.publishJobPublishFileWarning.text(this.jobId, file.getFullPath());
            } else {
                message = RestClientI18n.publishJobUnpublishFileWarning.text(this.jobId, file.getFullPath());
            }

            // log
            Activator.getDefault().log(status);
        } else {
            if (isPublishing()) {
                message = RestClientI18n.publishJobPublishFileInfo.text(this.jobId, file.getFullPath());
            } else {
                message = RestClientI18n.publishJobUnpublishFileInfo.text(this.jobId, file.getFullPath());
            }

            // log
            Activator.getDefault().log(status);
        }

        // write to console creating a hyperlink
        DnaMessageConsole.writeln(message, file);
    }

}
