/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.connector.svn;

import java.util.Collection;
import java.util.Collections;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 */
public class SvnRepositoryUtil {

    /**
     * @param url
     * @param sourceName
     * @return SVNURL
     */
    public static SVNURL createSVNURL( String url,
                                       String sourceName ) {

        SVNURL theUrl;
        try {
            theUrl = SVNURL.parseURIDecoded(url);
        } catch (SVNException e) {
            // protocol not supported by this connector
            throw new RepositorySourceException(sourceName,
                                                "Protocol is not supported by this connector or there is problem in the svn url");
        }
        return theUrl;
    }

    public static void setNewSVNRepositoryLocation( SVNRepository oldRespository,
                                                    String url,
                                                    boolean forceReconnect,
                                                    String sourceName ) {
        try {
            oldRespository.setLocation(createSVNURL(url, sourceName), forceReconnect);
        } catch (SVNException e) {
            throw new RepositorySourceException(sourceName, "the old url and a new one has got different protocols");
        }
    }

    /**
     * @param repository
     * @param path
     * @param revisionNumber
     * @param sourceName
     * @return SVNNodeKind
     */
    public static SVNNodeKind checkThePath( SVNRepository repository,
                                            String path,
                                            long revisionNumber,
                                            String sourceName ) {
        SVNNodeKind kind;
        try {
            kind = repository.checkPath(path, revisionNumber);

        } catch (SVNException e) {
            return null;
        }
        return kind;
    }

    /**
     * Create a {@link SVNRepository} from a http protocol.
     * 
     * @param url - the url of the repository.
     * @param username - username credential.
     * @param password - password credential
     * @return {@link SVNRepository}.
     */
    public static SVNRepository createRepository( String url,
                                                  String username,
                                                  String password ) {
        // for DAV (over http and https)
        DAVRepositoryFactory.setup();
        // For File
        FSRepositoryFactory.setup();
        // for SVN (over svn and svn+ssh)
        SVNRepositoryFactoryImpl.setup();

        // The factory knows how to create a DAVRepository
        SVNRepository repository;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));

            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(authManager);
        } catch (SVNException e) {
            throw new InvalidWorkspaceException(SvnRepositoryConnectorI18n.workspaceDoesNotExist.text(e.getMessage()));
        }
        return repository;
    }

    /**
     * Util to get the last segment from a path.
     * 
     * @param repository
     * @return last segment.
     */
    public static String getRepositoryWorspaceName( SVNRepository repository ) {
        String[] segments = repository.getLocation().getPath().split("/");
        return segments[segments.length - 1];
    }

    private SvnRepositoryUtil() {
        // prvent construction
    }

    /**
     * Check if the repository path exist.
     * 
     * @param repos
     * @return true if repository exist and false otherwise.
     */
    public static boolean exist( SVNRepository repos ) {
        try {
            SVNNodeKind kind = repos.checkPath("", -1);
            if (kind == SVNNodeKind.NONE) {
                return false;
            }
            return true;

        } catch (SVNException e) {
            return false;
        }
    }

    /**
     * Check if repository path is a directory.
     * 
     * @param repos
     * @param path
     * @return true if repository path is a directory and false otherwise.
     */
    public static boolean isDirectory( SVNRepository repos,
                                       String path ) {
        try {
            SVNNodeKind kind = repos.checkPath(path, -1);
            if (kind == SVNNodeKind.DIR) {
                return true;
            }
        } catch (SVNException e) {
            return false;
        }
        return false;
    }

    /**
     * @param repos
     * @param path
     * @return a collect of entry from directory path; never null
     */
    @SuppressWarnings( "unchecked" )
    public static Collection<SVNDirEntry> getDir( SVNRepository repos,
                                                  String path ) {
        try {
            return repos.getDir(path, -1, null, (Collection<SVNDirEntry>)null);
        } catch (SVNException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Check if the path is a file.
     * 
     * @param repos
     * @param path
     * @return true if the path is a file and false otherwise.
     */
    public static boolean isFile( SVNRepository repos,
                                  String path ) {
        try {
            SVNNodeKind kind = repos.checkPath(path, -1);
            if (kind == SVNNodeKind.FILE) {
                return true;
            }
        } catch (SVNException e) {
            return false;
        }
        return false;
    }

    public static boolean exists( SVNRepository repository,
                                  String path ) throws SVNException{
        try {
            if (repository.checkPath(path, -1) == SVNNodeKind.NONE) {
                return false;
            } else if (repository.checkPath(path, -1) == SVNNodeKind.UNKNOWN) {
                return false;
            }
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "unknow error during delete action: {0)",
                                                         e.getMessage());
            throw new SVNException(err);
        }
        return true;
    }
}
