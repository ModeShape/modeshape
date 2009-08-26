package org.jboss.dna.connector.svn;

import java.util.Collection;
import java.util.Collections;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.tmatesoft.svn.core.SVNDirEntry;
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
 * @author Serge Pagop
 */
public class SVNRepositoryUtil {

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
            throw new RepositorySourceException(sourceName, e.getMessage());
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
            throw new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(e.getMessage()));
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

    private SVNRepositoryUtil() {
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
                                  String path ) {
        try {
            if (repository.checkPath(path, -1) == SVNNodeKind.NONE) {
                return false;
            } else if (repository.checkPath(path, -1) == SVNNodeKind.UNKNOWN) {
                return false;
            }
        } catch (SVNException e) {
            return false;
        }
        return true;
    }
}
