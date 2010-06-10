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
package org.modeshape.maven.spi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Calendar;
import java.util.Properties;
import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.Logger;
import org.modeshape.maven.ArtifactType;
import org.modeshape.maven.MavenI18n;
import org.modeshape.maven.MavenId;
import org.modeshape.maven.MavenRepositoryException;
import org.modeshape.maven.MavenUrl;
import org.modeshape.maven.SignatureType;

/**
 * Base class for providers that work against a JCR repository. This class implements all functionality except for creating the
 * {@link Repository repository} instance, and it relies upon some other component or subclass to
 * {@link #setRepository(Repository) set the repository instance}. Typically, this is done by a subclass in it's
 * {@link #configure(Properties)} method:
 * 
 * <pre>
 * public class MyCustomJcrMavenUrlProvider extends JcrMavenUrlProvider {
 *     &#064;Override
 *     public void configure(Properties properties) {
 *          super.configure(properties);
 *          properties = super.getProperties();     // always non-null
 *          Repository repo = ...                   // Construct and configure
 *          super.setRepository(repo);
 *      }
 * }
 * 
 * 
 * 
 * </pre>
 */
public class JcrMavenUrlProvider extends AbstractMavenUrlProvider {

    public static final String USERNAME = "dna.maven.urlprovider.username";
    public static final String PASSWORD = "dna.maven.urlprovider.password";
    public static final String WORKSPACE_NAME = "dna.maven.urlprovider.repository.workspace";
    public static final String REPOSITORY_PATH = "dna.maven.urlprovider.repository.path";

    public static final String DEFAULT_PATH_TO_TOP_OF_MAVEN_REPOSITORY = "/dnaMavenRepository";
    public static final String DEFAULT_CREATE_REPOSITORY_PATH = Boolean.TRUE.toString();

    public static final String CONTENT_NODE_NAME = "jcr:content";
    public static final String CONTENT_PROPERTY_NAME = "jcr:data";

    private final URLStreamHandler urlStreamHandler = new JcrUrlStreamHandler();
    private final TextEncoder urlEncoder;
    private final TextDecoder urlDecoder;
    private Repository repository;
    private String workspaceName;
    private Credentials credentials;
    private String pathToTopOfRepository = DEFAULT_PATH_TO_TOP_OF_MAVEN_REPOSITORY;
    private static final Logger LOGGER = Logger.getLogger(JcrMavenUrlProvider.class);

    /**
     * 
     */
    public JcrMavenUrlProvider() {
        UrlEncoder encoder = new UrlEncoder().setSlashEncoded(false);
        this.urlEncoder = encoder;
        this.urlDecoder = encoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure( Properties properties ) {
        super.configure(properties);
        properties = super.getProperties();
        String username = properties.getProperty(USERNAME);
        if (username != null) {
            String password = properties.getProperty(PASSWORD, "");
            this.setCredentials(new SimpleCredentials(username, password.toCharArray()));
        }
        this.setWorkspaceName(properties.getProperty(WORKSPACE_NAME, this.getWorkspaceName()));
        this.setPathToTopOfRepository(properties.getProperty(REPOSITORY_PATH, this.getPathToTopOfRepository()));
    }

    /**
     * @return credentials
     */
    public Credentials getCredentials() {
        return this.credentials;
    }

    /**
     * @param credentials Sets credentials to the specified value.
     */
    public void setCredentials( Credentials credentials ) {
        this.credentials = credentials;
    }

    /**
     * @return workspaceName
     */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * @param workspaceName Sets workspaceName to the specified value.
     */
    public void setWorkspaceName( String workspaceName ) {
        this.workspaceName = workspaceName;
    }

    /**
     * @return pathToTopOfRepository
     */
    public String getPathToTopOfRepository() {
        return this.pathToTopOfRepository;
    }

    /**
     * @param pathToTopOfRepository Sets pathToTopOfRepository to the specified value.
     */
    public void setPathToTopOfRepository( String pathToTopOfRepository ) {
        this.pathToTopOfRepository = pathToTopOfRepository != null ? pathToTopOfRepository.trim() : DEFAULT_PATH_TO_TOP_OF_MAVEN_REPOSITORY;
    }

    /**
     * Get the JCR repository used by this provider
     * 
     * @return the repository instance
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * @param repository Sets repository to the specified value.
     */
    public void setRepository( Repository repository ) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    public URL getUrl( MavenId mavenId,
                       ArtifactType artifactType,
                       SignatureType signatureType,
                       boolean createIfRequired ) throws MalformedURLException, MavenRepositoryException {
        final String path = getUrlPath(mavenId, artifactType, signatureType);
        MavenUrl mavenUrl = new MavenUrl();
        mavenUrl.setWorkspaceName(this.getWorkspaceName());
        mavenUrl.setPath(path);
        if (createIfRequired) {
            final boolean metadataFile = ArtifactType.METADATA == artifactType;
            final String relPath = mavenId.getRelativePath(!metadataFile);
            Session session = null;
            try {
                session = this.createSession();
                Node root = session.getRootNode();
                Node top = getOrCreatePath(root, this.getPathToTopOfRepository(), "nt:folder");
                session.save();

                // Create the "nt:unstructured" nodes for the folder structures ...
                Node current = getOrCreatePath(top, relPath, "nt:folder");

                // Now create the node that represents the artifact (w/ signature?) ...
                if (artifactType != null) {
                    String name = metadataFile ? "" : mavenId.getArtifactId() + "-" + mavenId.getVersion();
                    name = name + artifactType.getSuffix();
                    if (signatureType != null) {
                        name = name + signatureType.getSuffix();
                    }
                    if (current.hasNode(name)) {
                        current = current.getNode(name);
                    } else {
                        // Create the node and set all of the required properties ...
                        current = current.addNode(name, "nt:file");
                    }
                    if (!current.hasNode(CONTENT_NODE_NAME)) {
                        Node contentNode = current.addNode(CONTENT_NODE_NAME, "nt:resource");
                        contentNode.setProperty("jcr:mimeType", "text/plain");
                        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
                        Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream("".getBytes()));
                        contentNode.setProperty(CONTENT_PROPERTY_NAME, binary);
                    }
                }
                session.save();
                LOGGER.trace("Created Maven repository node for {0}", mavenUrl);
            } catch (LoginException err) {
                throw new MavenRepositoryException(
                                                   MavenI18n.unableToOpenSessiontoRepositoryWhenCreatingNode.text(mavenUrl,
                                                                                                                  err.getMessage()),
                                                   err);
            } catch (NoSuchWorkspaceException err) {
                throw new MavenRepositoryException(MavenI18n.unableToFindWorkspaceWhenCreatingNode.text(this.getWorkspaceName(),
                                                                                                        mavenUrl,
                                                                                                        err.getMessage()), err);
            } catch (PathNotFoundException err) {
                return null;
            } catch (RepositoryException err) {
                throw new MavenRepositoryException(MavenI18n.errorCreatingNode.text(mavenUrl, err.getMessage()), err);
            } finally {
                if (session != null) session.logout();
            }
        }
        return mavenUrl.getUrl(this.urlStreamHandler, this.urlEncoder);
    }

    protected Node getOrCreatePath( Node root,
                                    String relPath,
                                    String nodeType )
        throws PathNotFoundException, ItemExistsException, NoSuchNodeTypeException, LockException, VersionException,
        ConstraintViolationException, RepositoryException {
        // Create the "nt:unstructured" nodes for the folder structures ...
        Node current = root;
        boolean created = false;
        String[] pathComponents = relPath.replaceFirst("^/+", "").split("/");
        for (String pathComponent : pathComponents) {
            if (pathComponent.length() == 0) continue;
            if (current.hasNode(pathComponent)) {
                current = current.getNode(pathComponent);
            } else {
                current = current.addNode(pathComponent, "nt:folder");
                created = true;
            }
        }
        if (created) {
            LOGGER.debug("Created Maven repository folders {0}", current.getPath());
        }
        return current;
    }

    protected Node getContentNodeForMavenResource( Session session,
                                                   MavenUrl mavenUrl ) throws RepositoryException {
        final String mavenPath = mavenUrl.getPath().replaceFirst("^/+", "");
        final String mavenRootPath = this.getPathToTopOfRepository().replaceFirst("^/+", "");
        Node root = session.getRootNode();
        Node top = root.getNode(mavenRootPath);
        Node resourceNode = top.getNode(mavenPath);
        return resourceNode.getNode(CONTENT_NODE_NAME);
    }

    /**
     * Get the JRC path to the node in this repository and it's workspace that represents the artifact with the given type in the
     * supplied Maven project.
     * 
     * @param mavenId the ID of the Maven project; may not be null
     * @param artifactType the type of artifact; may be null
     * @param signatureType the type of signature; may be null if the signature file is not desired
     * @return the path
     */
    protected String getUrlPath( MavenId mavenId,
                                 ArtifactType artifactType,
                                 SignatureType signatureType ) {
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        if (artifactType == null) {
            sb.append(mavenId.getRelativePath());
            sb.append("/");
        } else if (ArtifactType.METADATA == artifactType) {
            sb.append(mavenId.getRelativePath(false));
            sb.append("/");
        } else {
            // Add the file in the version
            sb.append(mavenId.getRelativePath());
            sb.append("/");
            sb.append(mavenId.getArtifactId());
            sb.append("-");
            sb.append(mavenId.getVersion());
        }
        if (artifactType != null) {
            sb.append(artifactType.getSuffix());
        }
        if (signatureType != null) {
            sb.append(signatureType.getSuffix());
        }
        return sb.toString();
    }

    protected TextEncoder getUrlEncoder() {
        return this.urlEncoder;
    }

    protected TextDecoder getUrlDecoder() {
        return this.urlDecoder;
    }

    protected Session createSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        if (this.workspaceName != null) {
            if (this.credentials != null) {
                return this.repository.login(this.credentials, this.workspaceName);
            }
            return this.repository.login(this.workspaceName);
        }
        if (this.credentials != null) {
            return this.repository.login(this.credentials);
        }
        return this.repository.login();
    }

    /**
     * Obtain an input stream to the existing content at the location given by the supplied {@link MavenUrl}. The Maven URL should
     * have a path that points to the node where the content is stored in the {@link #CONTENT_PROPERTY_NAME content property}.
     * 
     * @param mavenUrl the Maven URL to the content; may not be null
     * @return the input stream to the content, or null if there is no existing content
     * @throws IOException
     */
    protected InputStream getInputStream( MavenUrl mavenUrl ) throws IOException {
        Session session = null;
        try {
            // Create a new session, get the actual input stream to the underlying node, and return a wrapper to the actual
            // InputStream that, when closed, will close the session.
            session = this.createSession();
            // Find the node and it's property ...
            final Node contentNode = getContentNodeForMavenResource(session, mavenUrl);
            Property contentProperty = contentNode.getProperty(CONTENT_PROPERTY_NAME);
            InputStream result = contentProperty.getBinary().getStream();
            result = new MavenInputStream(session, result);
            return result;
        } catch (LoginException err) {
            throw new MavenRepositoryException(MavenI18n.unableToOpenSessiontoRepositoryWhenReadingNode.text(mavenUrl,
                                                                                                             err.getMessage()),
                                               err);
        } catch (NoSuchWorkspaceException err) {
            throw new MavenRepositoryException(MavenI18n.unableToFindWorkspaceWhenReadingNode.text(this.getWorkspaceName(),
                                                                                                   mavenUrl,
                                                                                                   err.getMessage()), err);
        } catch (PathNotFoundException err) {
            return null;
        } catch (RepositoryException err) {
            throw new MavenRepositoryException(MavenI18n.errorReadingNode.text(mavenUrl, err.getMessage()), err);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Obtain an output stream to the existing content at the location given by the supplied {@link MavenUrl}. The Maven URL
     * should have a path that points to the property or to the node where the content is stored in the
     * {@link #CONTENT_PROPERTY_NAME content property}.
     * 
     * @param mavenUrl the Maven URL to the content; may not be null
     * @return the input stream to the content, or null if there is no existing content
     * @throws IOException
     */
    protected OutputStream getOutputStream( MavenUrl mavenUrl ) throws IOException {
        try {
            // Create a temporary file to which the content will be written and then read from ...
            OutputStream result = null;
            try {
                File tempFile = File.createTempFile("dnamaven", null);
                result = new MavenOutputStream(mavenUrl, tempFile);
            } catch (IOException err) {
                throw new RepositoryException("Unable to obtain a temporary file for streaming content to " + mavenUrl, err);
            }
            return result;
        } catch (LoginException err) {
            throw new MavenRepositoryException(MavenI18n.unableToOpenSessiontoRepositoryWhenReadingNode.text(mavenUrl,
                                                                                                             err.getMessage()),
                                               err);
        } catch (NoSuchWorkspaceException err) {
            throw new MavenRepositoryException(MavenI18n.unableToFindWorkspaceWhenReadingNode.text(this.getWorkspaceName(),
                                                                                                   mavenUrl,
                                                                                                   err.getMessage()), err);
        } catch (RepositoryException err) {
            throw new MavenRepositoryException(MavenI18n.errorReadingNode.text(mavenUrl, err.getMessage()), err);
        }
    }

    public void setContent( MavenUrl mavenUrl,
                            InputStream content ) throws IOException {
        Session session = null;
        try {
            // Create a new session, find the actual node, create a temporary file to which the content will be written,
            // and return a wrapper to the actual Output that writes to the file and that, when closed, will set the
            // content on the node and close the session.
            session = this.createSession();
            // Find the node and it's property ...
            final Node contentNode = getContentNodeForMavenResource(session, mavenUrl);
            contentNode.setProperty(CONTENT_PROPERTY_NAME, session.getValueFactory().createBinary(content));
            session.save();
        } catch (LoginException err) {
            throw new IOException(MavenI18n.unableToOpenSessiontoRepositoryWhenWritingNode.text(mavenUrl, err.getMessage()));
        } catch (NoSuchWorkspaceException err) {
            throw new IOException(MavenI18n.unableToFindWorkspaceWhenWritingNode.text(this.getWorkspaceName(),
                                                                                      mavenUrl,
                                                                                      err.getMessage()));
        } catch (RepositoryException err) {
            throw new IOException(MavenI18n.errorWritingNode.text(mavenUrl, err.getMessage()));
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    protected class MavenInputStream extends InputStream {

        private final InputStream stream;
        private final Session session;

        protected MavenInputStream( final Session session,
                                    final InputStream stream ) {
            this.session = session;
            this.stream = stream;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            return stream.read();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            try {
                stream.close();
            } finally {
                this.session.logout();
            }
        }
    }

    protected class MavenOutputStream extends OutputStream {

        private OutputStream stream;
        private final File file;
        private final MavenUrl mavenUrl;
        private final Logger LOGGER = Logger.getLogger(MavenOutputStream.class);

        protected MavenOutputStream( final MavenUrl mavenUrl,
                                     final File file ) throws FileNotFoundException {
            this.mavenUrl = mavenUrl;
            this.file = file;
            this.stream = new BufferedOutputStream(new FileOutputStream(this.file));
            assert this.file != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( int b ) throws IOException {
            if (stream == null) throw new IOException(MavenI18n.unableToWriteToClosedStream.text());
            stream.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( byte[] b ) throws IOException {
            if (stream == null) throw new IOException(MavenI18n.unableToWriteToClosedStream.text());
            stream.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( byte[] b,
                           int off,
                           int len ) throws IOException {
            if (stream == null) throw new IOException(MavenI18n.unableToWriteToClosedStream.text());
            stream.write(b, off, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            // Close the output stream to the temporary file
            if (stream != null) {
                stream.close();
                InputStream inputStream = null;
                try {
                    // Create an input stream to the temporary file...
                    inputStream = new BufferedInputStream(new FileInputStream(file));
                    // Write the content to the node ...
                    setContent(this.mavenUrl, inputStream);

                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioe) {
                            LOGGER.error(ioe, MavenI18n.errorClosingTempFileStreamAfterWritingContent, mavenUrl, ioe.getMessage());
                        } finally {
                            try {
                                file.delete();
                            } catch (SecurityException se) {
                                Logger.getLogger(this.getClass()).error(se,
                                                                        MavenI18n.errorDeletingTempFileStreamAfterWritingContent,
                                                                        mavenUrl,
                                                                        se.getMessage());
                            } finally {
                                stream = null;
                            }
                        }
                    }
                }
                super.close();
            }
        }
    }

    /**
     * This {@link URLStreamHandler} specialization understands {@link URL URLs} that point to content in the JCR repository used
     * by this Maven repository.
     * 
     * @author Randall Hauch
     */
    protected class JcrUrlStreamHandler extends URLStreamHandler {

        protected JcrUrlStreamHandler() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected URLConnection openConnection( URL url ) {
            return new MavenUrlConnection(url);
        }
    }

    /**
     * A URLConnection with support for obtaining content from a node in a JCR repository.
     * <p>
     * Each JcrUrlConnection is used to make a single request to read or write the <code>jcr:content</code> property value on the
     * {@link javax.jcr.Node node} that corresponds to the given URL. The node must already exist.
     * </p>
     * 
     * @author Randall Hauch
     */
    protected class MavenUrlConnection extends URLConnection {

        private final MavenUrl mavenUrl;

        /**
         * @param url the URL that is to be processed
         */
        protected MavenUrlConnection( URL url ) {
            super(url);
            this.mavenUrl = MavenUrl.parse(url, JcrMavenUrlProvider.this.getUrlDecoder());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect() throws IOException {
            // If the URL is not a valid JCR URL, then throw a new IOException ...
            if (this.mavenUrl == null) {
                String msg = "Unable to connect to JCR repository because the URL is not valid for JCR: " + this.getURL();
                throw new IOException(msg);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() throws IOException {
            return JcrMavenUrlProvider.this.getInputStream(this.mavenUrl);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream() throws IOException {
            return JcrMavenUrlProvider.this.getOutputStream(this.mavenUrl);
        }
    }

}
