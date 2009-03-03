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
package org.jboss.dna.connector.svn;

import java.io.File;
import java.io.IOException;
import org.jboss.dna.common.util.FileUtil;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * @author Serge Pagop
 */
public class SVNConnectorTestUtil {

    public static void main( String[] args ) throws Exception {
        try {
            System.out.println("hello ......");
            String svnUrl = SVNConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");
            String username = "sp";
            String password = "";
            SVNRepository repos = createRepository(svnUrl, username, password);
            System.out.println("Repository Root: " + repos.getRepositoryRoot(true));
            System.out.println("Repository UUID: " + repos.getRepositoryUUID(true));
            System.out.println("hello ......");
        } catch (SVNException e) {
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

        // The factory knows how to create a DAVRepository
        SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
        repository.setAuthenticationManager(authManager);
        return repository;
    }
    
    public static String createURL(String src, String dst) throws IOException {
        // First we need to find the absolute path. Note that Maven always runs the tests from the project's directory,
        // so use new File to create an instance at the current location ...
        File mySrc = new File(src);
        File myDst = new File(dst);

        // make sure the destination is empty before we copy
        FileUtil.delete(myDst);
        FileUtil.copy(mySrc, myDst);

        // Now set the two path roots
        String url = myDst.getCanonicalFile().toURL().toString();
        return url.replaceFirst("file:/", "file://localhost/");
    }
    private SVNConnectorTestUtil() {
        // prevent constructor
    }

}