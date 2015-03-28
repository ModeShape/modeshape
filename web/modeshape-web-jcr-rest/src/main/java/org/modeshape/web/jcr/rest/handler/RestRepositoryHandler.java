/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.web.jcr.rest.handler;

import java.io.File;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.BackupOptions;
import org.modeshape.jcr.api.Problem;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.jcr.api.RestoreOptions;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.JSONAble;
import org.modeshape.web.jcr.rest.model.RestException;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;

/**
 * An handler which returns POJO-based rest model instances.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestRepositoryHandler extends AbstractHandler {

    private static final String BACKUP_LOCATION_INIT_PARAM = "backupLocation";
    private static final String JBOSS_DOMAIN_DATA_DIR = "jboss.domain.data.dir";
    private static final String JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";
    private static final String USER_HOME = "user.home";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyyyy_HHmmss");

    /**
     * Returns the list of workspaces available to this user within the named repository.
     *
     * @param request the servlet request; may not be null
     * @param repositoryName the name of the repository; may not be null
     * @return the list of workspaces available to this user within the named repository, as a {@link RestWorkspaces} object
     *
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     */
    public RestWorkspaces getWorkspaces( HttpServletRequest request,
                                         String repositoryName ) throws RepositoryException {
        assert request != null;
        assert repositoryName != null;

        RestWorkspaces workspaces = new RestWorkspaces();
        Session session = getSession(request, repositoryName, null);
        for (String workspaceName : session.getWorkspace().getAccessibleWorkspaceNames()) {
            String repositoryUrl = RestHelper.urlFrom(request);
            workspaces.addWorkspace(workspaceName, repositoryUrl);
        }
        return workspaces;
    }

    /**
     * Performs a repository backup.
     */
    public Response backupRepository( ServletContext context, 
                                      HttpServletRequest request, 
                                      String repositoryName,
                                      BackupOptions options ) throws RepositoryException {
        final File backupLocation = resolveBackupLocation(context);

        Session session = getSession(request, repositoryName, null);
        String repositoryVersion = session.getRepository().getDescriptorValue(Repository.REP_VERSION_DESC).getString().replaceAll("\\.","");
        final String backupName = "modeshape_" + repositoryVersion + "_" + repositoryName + "_backup_" + DATE_FORMAT.format(new Date());
        final File backup = new File(backupLocation, backupName);
        if (!backup.mkdirs()) {
            throw new RuntimeException("Cannot create backup folder: " + backup);
        }
        logger.debug("Backing up repository '{0}' to '{1}', using '{2}'", repositoryName, backup, options);


        RepositoryManager repositoryManager = ((org.modeshape.jcr.api.Workspace)session.getWorkspace()).getRepositoryManager();
        repositoryManager.backupRepository(backup, options);
        final String backupURL;
        try {
            backupURL = backup.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            //should never happen
            throw new RuntimeException(e);
        }
        JSONAble responseContent = new JSONAble() {
            @Override
            public JSONObject toJSON() throws JSONException {
                JSONObject object = new JSONObject();
                object.put("name", backupName);
                object.put("url", backupURL);
                return object;
            }
        }; 
        return Response.status(Response.Status.CREATED).entity(responseContent).build();
    }

    /**
     * Restores a repository using an existing backup.
     */
    public Response restoreRepository( ServletContext context, 
                                       HttpServletRequest request, 
                                       String repositoryName,
                                       String backupName, 
                                       RestoreOptions options ) throws RepositoryException {
        if (StringUtil.isBlank(backupName)) {
            throw new IllegalArgumentException("The name of the backup cannot be null");
        }
        File backup = resolveBackup(context, backupName);
        logger.debug("Restoring repository '{0}' from backup '{1}' using '{2}'", repositoryName, backup, options);
        Session session = getSession(request, repositoryName, null);
        RepositoryManager repositoryManager = ((org.modeshape.jcr.api.Workspace)session.getWorkspace()).getRepositoryManager();
        final Problems problems = repositoryManager.restoreRepository(backup, options);
        if (!problems.hasProblems()) {
            return Response.ok().build();
        }
        List<JSONAble> response = new ArrayList<JSONAble>(problems.size());
        for (Problem problem : problems) {
            RestException exception = problem.getThrowable() != null ? 
                                      new RestException(problem.getMessage(), problem.getThrowable()) : 
                                      new RestException(problem.getMessage());
            response.add(exception);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
    }
    
    private File resolveBackup( ServletContext context, String backupName ) {
        // first look at the servlet init param
        String backupRoot = context.getInitParameter(BACKUP_LOCATION_INIT_PARAM);
        if (!StringUtil.isBlank(backupRoot)) {
            if (isValidDir(backupRoot + "/" + backupName)) {
                return new File(backupRoot + "/" + backupName);
            }

            // try resolving it as a system property
            backupRoot = System.getProperty(backupRoot);
            if (isValidDir(backupRoot + "/" + backupName)) {
                return new File(backupRoot + "/" + backupName);
            }
        }

        // jboss.domain.data.dir
        backupRoot = System.getProperty(JBOSS_DOMAIN_DATA_DIR);
        if (isValidDir(backupRoot + "/" + backupName)) {
            return new File(backupRoot + "/" + backupName);
        }

        // jboss.server.data.dir
        backupRoot = System.getProperty(JBOSS_SERVER_DATA_DIR);
        if (isValidDir(backupRoot + "/" + backupName)) {
            return new File(backupRoot + "/" + backupName);
        }

        // finally user.home
        backupRoot = System.getProperty(USER_HOME);
        if (isValidDir(backupRoot + "/" + backupName)) {
            return new File(backupRoot + "/" + backupName);
        }

        // none of the above are available, so fail
        throw new IllegalArgumentException(
                "Cannot locate backup '" + backupName + "' anywhere on the server in the following locations:" +
                BACKUP_LOCATION_INIT_PARAM + " context param, " + JBOSS_DOMAIN_DATA_DIR + ", " + JBOSS_SERVER_DATA_DIR + ", "
                + USER_HOME);

    }

    private File resolveBackupLocation( ServletContext context ) {
        // first look at the servlet init param 'backupLocation'
        String backupLocation = context.getInitParameter(BACKUP_LOCATION_INIT_PARAM);
        if (!StringUtil.isBlank(backupLocation)) {
            if (isValidDir(backupLocation)) {
                return new File(backupLocation);
            }

            // try resolving it as a system property
            backupLocation = System.getProperty(backupLocation);
            if (isValidDir(backupLocation)) {
                return new File(backupLocation);
            }
        }

        // jboss.domain.data.dir
        backupLocation = System.getProperty(JBOSS_DOMAIN_DATA_DIR);
        if (isValidDir(backupLocation)) {
            return new File(backupLocation);
        }

        // jboss.server.data.dir
        backupLocation = System.getProperty(JBOSS_SERVER_DATA_DIR);
        if (isValidDir(backupLocation)) {
            return new File(backupLocation);
        }

        // finally user.home
        backupLocation = System.getProperty(USER_HOME);
        if (isValidDir(backupLocation)) {
            return new File(backupLocation);
        }

        // none of the above are available, so fail
        throw new IllegalArgumentException(
                "None of the following locations are writable folders on the server: " +
                BACKUP_LOCATION_INIT_PARAM + " context param, " + JBOSS_DOMAIN_DATA_DIR + ", " + JBOSS_SERVER_DATA_DIR + ", "
                + USER_HOME);
    }

    private boolean isValidDir( String dir ) {
        if (StringUtil.isBlank(dir)) {
            return false;
        }
        File dirFile = new File(dir);
        return dirFile.exists() && dirFile.canWrite() && dirFile.isDirectory();
    }
}
