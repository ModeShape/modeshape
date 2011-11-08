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

import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.Logger;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Serge Pagop
 * @author Horia Chiorean
 */
public class SvnConnectorTestUtil {

    private static final Logger LOGGER = Logger.getLogger(SvnConnectorTestUtil.class);

    @SuppressWarnings( "unchecked" )
    public static void main( String[] args ) throws Exception {
        try {
            System.out.println("My repos. ......");
            String svnUrl = SvnConnectorTestUtil.createTestRepoURL();
            String username = "sp";
            String password = "";
            System.out.println(svnUrl);
            SVNRepository trunkWorkspace = createRepository(svnUrl + "/trunk", username, password);
            System.out.println("Repository location: " + trunkWorkspace.getLocation().toString());
            System.out.println("Repository Root: " + trunkWorkspace.getRepositoryRoot(true));
            System.out.println("Repository UUID: " + trunkWorkspace.getRepositoryUUID(true));
            /**
             * Returns the repository location to which this object is set. It may be the location that was used to create this
             * object (see {@link SVNRepositoryFactory#create(SVNURL)}), or the recent one the object was set to.
             */
            System.out.println("location: " + trunkWorkspace.getLocation().getPath());
            System.out.println("decoded location: " + trunkWorkspace.getLocation().toDecodedString());
            System.out.println("last seg: " + getRepositoryWorspaceName(trunkWorkspace));

            final Collection<SVNDirEntry> dirEntries = trunkWorkspace.getDir("", -1, null, (Collection<SVNDirEntry>)null);
            for (SVNDirEntry dirEntry : dirEntries) {
                System.out.println("name: " + dirEntry.getName());
            }

            // //
            // SVNNodeKind nodeKind = trunkWorkspace.checkPath( "/" , -1 );
            // if ( nodeKind == SVNNodeKind.NONE ) {
            // System.err.println( "There is no entry in the workspace "+ trunkWorkspace );
            // System.exit( 1 );
            // } else if ( nodeKind == SVNNodeKind.FILE ) {
            // System.err.println( "The entry at '" + trunkWorkspace + "' is a file while a directory was expected." );
            // System.exit( 1 );
            // } else {
            // listEntries(trunkWorkspace, "/root");
            // // long latestRevision = trunkWorkspace.getLatestRevision( );
            // // System.out.println( "workspace latest revision: " + latestRevision );
            //
            // //// SVNNodeKind kind = trunkWorkspace.checkPath("/", -1);
            // // if(kind == SVNNodeKind.NONE) {
            // // System.out.println("none");
            // // } else if(kind == SVNNodeKind.UNKNOWN) {
            // // System.out.println("unknown");
            // // } else if(kind == SVNNodeKind.FILE) {
            // // System.out.println("file");
            // // } else if(kind == SVNNodeKind.DIR) {
            // System.out.println("dir");
            // // listEntries(trunkWorkspace,"root");
            // }

        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a {@link SVNRepository} from a http protocol.
     * 
     * @param url - the url of the repository.
     * @param username - username credential.
     * @param password - password credential
     * @return {@link SVNRepository}.
     * @throws SVNException - when error situation.
     */
    public static SVNRepository createRepository( String url,
                                                  String username,
                                                  String password ) throws SVNException {
        // for DAV (over http and https)
        DAVRepositoryFactory.setup();
        // For File
        FSRepositoryFactory.setup();
        // for SVN (over svn and svn+ssh)
        SVNRepositoryFactoryImpl.setup();

        if (!url.endsWith("/")) url = url + "/";

        // The factory knows how to create a DAVRepository
        SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(false);
        options.setPropertyValue(SVNProperty.EOL_STYLE, SVNProperty.EOL_STYLE_NATIVE);
        repository.setAuthenticationManager(authManager);

        return repository;
    }

    public static String createTestRepoURL() throws IOException, SVNException, URISyntaxException {
        String srcRepoPath = "dummy_svn_repos";
        File srcRepoRepo = new File(SvnConnectorTestUtil.class.getClassLoader().getResource(srcRepoPath).toURI());
        File srcRepoParentDir = new File(srcRepoRepo.getParent());

        String dstRepoPath = "copyOf_" + srcRepoPath;
        File dstRepoDir = new File(srcRepoParentDir, dstRepoPath);

        // make sure the destination is empty before we copy
        if (dstRepoDir.exists() && !FileUtil.delete(dstRepoDir)) {
            LOGGER.debug("Cannot delete directory " + dstRepoDir);
            dstRepoDir = new File(srcRepoParentDir, dstRepoPath + "_" + System.currentTimeMillis());
        }
        dstRepoDir.deleteOnExit();
        FileUtil.copy(srcRepoRepo, dstRepoDir);

        // Now set the two path roots
        String url = dstRepoDir.getCanonicalFile().toURI().toURL().toExternalForm();

        url = url.replaceFirst("file:/", "file://localhost/");

        // Have to decode the URL ...
        SVNURL encodedUrl = SVNURL.parseURIEncoded(url);
        url = encodedUrl.toDecodedString();

        return url;
    }

    @SuppressWarnings( "unchecked" )
    public static void listEntries( SVNRepository workspace,
                                    String path ) throws SVNException {
        Collection<SVNDirEntry> entries = workspace.getDir(path, -1, null, (Collection<SVNDirEntry>)null);
        Iterator<SVNDirEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            SVNDirEntry entry = iterator.next();
            System.out.println("/" + (path.equals("") ? "" : path + "/") + entry.getName() + " ( author: '" + entry.getAuthor()
                               + "'; revision: " + entry.getRevision() + "; date: " + entry.getDate() + ")");
            if (entry.getKind() == SVNNodeKind.DIR) {
                listEntries(workspace, (path.equals("")) ? entry.getName() : path + "/" + entry.getName());
            }
        }
    }

    public static String getRepositoryWorspaceName( SVNRepository repository ) {
        String[] segments = repository.getLocation().getPath().split("/");
        return segments[segments.length - 1];
    }

    private SvnConnectorTestUtil() {
        // prevent constructor
    }

}
