/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.example.dna.sequencers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.observation.Event;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencers.SequencerConfig;
import org.jboss.dna.repository.sequencers.SequencingService;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.repository.util.SimpleExecutionContext;
import org.jboss.dna.repository.util.SimpleSessionFactory;

/**
 * @author Randall Hauch
 */
public class SequencingClient {

    public static final String DEFAULT_JACKRABBIT_CONFIG_PATH = "jackrabbitConfig.xml";
    public static final String DEFAULT_WORKING_DIRECTORY = "repositoryData";
    public static final String DEFAULT_REPOSITORY_NAME = "repo";
    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String DEFAULT_USERNAME = "jsmith";
    public static final char[] DEFAULT_PASSWORD = "secret".toCharArray();

    public static void main( String[] args ) throws Exception {
        SequencingClient client = new SequencingClient();
        client.setRepositoryInformation(DEFAULT_REPOSITORY_NAME, DEFAULT_WORKSPACE_NAME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
        client.setUserInterface(new ConsoleInput(client));
    }

    private String repositoryName;
    private String workspaceName;
    private String username;
    private char[] password;
    private String jackrabbitConfigPath;
    private String workingDirectory;
    private Session keepAliveSession;
    private Repository repository;
    private SequencingService sequencingService;
    private ObservationService observationService;
    private UserInterface userInterface;
    private ExecutionContext executionContext;

    public SequencingClient() {
        setJackrabbitConfigPath(DEFAULT_JACKRABBIT_CONFIG_PATH);
        setWorkingDirectory(DEFAULT_WORKING_DIRECTORY);
        setRepositoryInformation(DEFAULT_REPOSITORY_NAME, DEFAULT_WORKSPACE_NAME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    protected void setWorkingDirectory( String workingDirectoryPath ) {
        this.workingDirectory = workingDirectoryPath != null ? workingDirectoryPath : DEFAULT_WORKING_DIRECTORY;
    }

    protected void setJackrabbitConfigPath( String jackrabbitConfigPath ) {
        this.jackrabbitConfigPath = jackrabbitConfigPath != null ? jackrabbitConfigPath : DEFAULT_JACKRABBIT_CONFIG_PATH;
    }

    protected void setRepositoryInformation( String repositoryName, String workspaceName, String username, char[] password ) {
        if (this.repository != null) {
            throw new IllegalArgumentException("Unable to set repository information when repository is already running");
        }
        this.repositoryName = repositoryName != null ? repositoryName : DEFAULT_REPOSITORY_NAME;
        this.workspaceName = workspaceName != null ? workspaceName : DEFAULT_WORKSPACE_NAME;
        this.username = username;
        this.password = password;
    }

    /**
     * Set the user interface that this client should use.
     * @param userInterface
     */
    public void setUserInterface( UserInterface userInterface ) {
        this.userInterface = userInterface;
    }

    /**
     * Start up the JCR repository. This method only operates using the JCR API and Jackrabbit-specific API.
     * @throws Exception
     */
    public void startRepository() throws Exception {
        if (this.repository == null) {
            try {

                // Load the Jackrabbit configuration ...
                File configFile = new File(this.jackrabbitConfigPath);
                if (!configFile.exists()) {
                    throw new SystemFailureException("The Jackrabbit configuration file cannot be found at " + configFile.getAbsoluteFile());
                }
                if (!configFile.canRead()) {
                    throw new SystemFailureException("Unable to read the Jackrabbit configuration file at " + configFile.getAbsoluteFile());
                }
                String pathToConfig = configFile.getAbsolutePath();

                // Find the directory where the Jackrabbit repository data will be stored ...
                File workingDirectory = new File(this.workingDirectory);
                if (workingDirectory.exists()) {
                    if (!workingDirectory.isDirectory()) {
                        throw new SystemFailureException("Unable to create working directory at " + workingDirectory.getAbsolutePath());
                    }
                }
                String workingDirectoryPath = workingDirectory.getAbsolutePath();

                // Get the Jackrabbit custom node definition (CND) file ...
                URL cndFile = Thread.currentThread().getContextClassLoader().getResource("jackrabbitNodeTypes.cnd");

                // Create the Jackrabbit repository instance and establish a session to keep the repository alive ...
                this.repository = new TransientRepository(pathToConfig, workingDirectoryPath);
                if (this.username != null) {
                    Credentials credentials = new SimpleCredentials(this.username, this.password);
                    this.keepAliveSession = this.repository.login(credentials, this.workspaceName);
                } else {
                    this.keepAliveSession = this.repository.login();
                }

                try {
                    // Register the node types (only valid the first time) ...
                    JackrabbitNodeTypeManager mgr = (JackrabbitNodeTypeManager)this.keepAliveSession.getWorkspace().getNodeTypeManager();
                    mgr.registerNodeTypes(cndFile.openStream(), JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
                } catch (RepositoryException e) {
                    if (!e.getMessage().contains("already exists")) throw e;
                }

            } catch (Exception e) {
                this.repository = null;
                this.keepAliveSession = null;
                throw e;
            }
        }
    }

    /**
     * Shutdown the repository. This method only uses the JCR API.
     * @throws Exception
     */
    public void shutdownRepository() throws Exception {
        if (this.repository != null) {
            try {
                this.keepAliveSession.logout();
            } finally {
                this.repository = null;
                this.keepAliveSession = null;
            }
        }
    }

    /**
     * Start the DNA services.
     * @throws Exception
     */
    public void startDnaServices() throws Exception {
        if (this.repository == null) {
            this.startRepository();
        }
        if (this.sequencingService == null) {

            // Create an execution context for the sequencing service. This execution context provides an environment
            // for the DNA services which knows about the JCR repositories, workspaces, and credentials used to
            // establish sessions to these workspaces. This example uses the SimpleExecutionContext, but there is
            // implementation for use with JCR repositories registered in JNDI.
            SimpleSessionFactory sessionFactory = new SimpleSessionFactory();
            sessionFactory.registerRepository(this.repositoryName, this.repository);
            if (this.username != null) {
                Credentials credentials = new SimpleCredentials(this.username, this.password);
                sessionFactory.registerCredentials(this.repositoryName + "/" + this.workspaceName, credentials);
            }
            this.executionContext = new SimpleExecutionContext(sessionFactory);

            // Create the sequencing service, passing in the execution context ...
            this.sequencingService = new SequencingService();
            this.sequencingService.setExecutionContext(executionContext);

            // Configure the sequencers. In this example, we only two sequencers that processes image and mp3 files.
            // So create a configurations. Note that the sequencing service expects the class to be on the thread's current
            // context
            // classloader, or if that's null the classloader that loaded the SequencingService class.
            //
            // Part of the configuration includes telling DNA which JCR paths should be processed by the sequencer.
            // These path expressions tell the service that this sequencer should be invoked on the "jcr:data" property
            // on the "jcr:content" child node of any node uploaded to the repository whose name ends with one of the
            // supported extensions, and the sequencer should place the generated output metadata in a node with the same name as
            // the file but immediately below the "/images" node. Path expressions can be fairly complex, and can even
            // specify that the generated information be placed in a different repository.
            // 
            // Sequencer configurations can be added before or after the service is started, but here we do it before the service
            // is running.
            String name = "Image Sequencer";
            String desc = "Sequences image files to extract the characteristics of the image";
            String classname = "org.jboss.dna.sequencer.images.ImageMetadataSequencer";
            String[] classpath = null; // Use the current classpath
            String[] pathExpressions = {"//(*.(jpg|jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd))[*]/jcr:content[@jcr:data] => /images/$1"};
            SequencerConfig imageSequencerConfig = new SequencerConfig(name, desc, classname, classpath, pathExpressions);
            this.sequencingService.addSequencer(imageSequencerConfig);

            // Set up the MP3 sequencer ...
            name = "Mp3 Sequencer";
            desc = "Sequences mp3 files to extract the id3 tags of the audio file";
            classname = "org.jboss.dna.sequencer.mp3.Mp3MetadataSequencer";
            String[] mp3PathExpressions = {"//(*.mp3)[*]/jcr:content[@jcr:data] => /mp3s/$1"};
            SequencerConfig mp3SequencerConfig = new SequencerConfig(name, desc, classname, classpath, mp3PathExpressions);
            this.sequencingService.addSequencer(mp3SequencerConfig);

            // Use the DNA observation service to listen to the JCR repository (or multiple ones), and
            // then register the sequencing service as a listener to this observation service...
            this.observationService = new ObservationService(this.executionContext.getSessionFactory());
            this.observationService.getAdministrator().start();
            this.observationService.addListener(this.sequencingService);
            this.observationService.monitor(this.repositoryName + "/" + this.workspaceName, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED);
        }
        // Start up the sequencing service ...
        this.sequencingService.getAdministrator().start();
    }

    /**
     * Shut down the DNA services.
     * @throws Exception
     */
    public void shutdownDnaServices() throws Exception {
        if (this.sequencingService == null) return;

        // Shut down the service and wait until it's all shut down ...
        this.sequencingService.getAdministrator().shutdown();
        this.sequencingService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);

        // Shut down the observation service ...
        this.observationService.getAdministrator().shutdown();
        this.observationService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Get the sequencing statistics.
     * @return the statistics; never null
     */
    public SequencingService.Statistics getStatistics() {
        return this.sequencingService.getStatistics();
    }

    /**
     * Prompt the user interface for the file to upload into the JCR repository, then upload it using the JCR API.
     * @throws Exception
     */
    public void uploadFile() throws Exception {
        URL url = this.userInterface.getFileToUpload();
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        String nodePath = this.userInterface.getRepositoryPath("/a/b/" + filename);
        String mimeType = getMimeType(url);

        // Now use the JCR API to upload the file ...
        Session session = createSession();
        JcrTools tools = this.executionContext.getTools();
        try {
            // Create the node at the supplied path ...
            Node node = tools.findOrCreateNode(session, nodePath, "nt:folder", "nt:file");

            // Upload the file to that node ...
            Node contentNode = tools.findOrCreateChild(session, node, "jcr:content", "nt:resource");
            contentNode.setProperty("jcr:mimeType", mimeType);
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            contentNode.setProperty("jcr:data", url.openStream());

            // Save the session ...
            session.save();
        } finally {
            session.logout();
        }
    }

    /**
     * Perform a search of the repository for all image metadata automatically created by the image sequencer.
     * @throws Exception
     */
    public void search() throws Exception {
        // Use JCR to search the repository for image metadata ...
        List<MediaInfo> medias = new ArrayList<MediaInfo>();
        Session session = createSession();
        try {
            // Find the image node ...
            Node root = session.getRootNode();

            if (root.hasNode("images") || root.hasNode("mp3s")) {
                Node mediasNode;
                if (root.hasNode("images")) {
                    mediasNode = root.getNode("images");

                    for (NodeIterator iter = mediasNode.getNodes(); iter.hasNext();) {
                        Node mediaNode = iter.nextNode();
                        if (mediaNode.hasNode("image:metadata")) {
                            medias.add(extractMediaInfo("image:metadata", "image", mediaNode));
                        }
                    }
                }
                if (root.hasNode("mp3s")) {
                    mediasNode = root.getNode("mp3s");

                    for (NodeIterator iter = mediasNode.getNodes(); iter.hasNext();) {
                        Node mediaNode = iter.nextNode();
                        if (mediaNode.hasNode("mp3:metadata")) {
                            medias.add(extractMediaInfo("mp3:metadata", "mp3", mediaNode));
                        }
                    }
                }

            }
        } finally {
            session.logout();
        }

        // Display the search results ...
        this.userInterface.displaySearchResults(medias);
    }

    /**
     * @param metadataNodeName
     * @param mediaType
     * @param mediaNode media node
     * @throws RepositoryException
     * @throws PathNotFoundException
     * @throws ValueFormatException
     */
    private MediaInfo extractMediaInfo( String metadataNodeName, String mediaType, Node mediaNode ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        String nodePath = mediaNode.getPath();
        String nodeName = mediaNode.getName();
        mediaNode = mediaNode.getNode(metadataNodeName);

        // Create a Properties object containing the properties for this node; ignore any children ...
        Properties props = new Properties();
        for (PropertyIterator propertyIter = mediaNode.getProperties(); propertyIter.hasNext();) {
            Property property = propertyIter.nextProperty();
            String name = property.getName();
            String stringValue = null;
            if (property.getDefinition().isMultiple()) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (!first) {
                        sb.append(", ");
                        first = false;
                    }
                    sb.append(value.getString());
                }
                stringValue = sb.toString();
            } else {
                stringValue = property.getValue().getString();
            }
            props.put(name, stringValue);
        }
        // Create the image information object, and add it to the collection ...
        return new MediaInfo(nodePath, nodeName, mediaType, props);
    }

    /**
     * Utility method to create a new JCR session from the execution context's {@link SessionFactory}.
     * @return the session
     * @throws RepositoryException
     */
    protected Session createSession() throws RepositoryException {
        return this.executionContext.getSessionFactory().createSession(this.repositoryName + "/" + this.workspaceName);
    }

    protected String getMimeType( URL file ) {
        String filename = file.getPath().toLowerCase();
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".pict")) return "image/x-pict";
        if (filename.endsWith(".bmp")) return "image/bmp";
        if (filename.endsWith(".jpg")) return "image/jpeg";
        if (filename.endsWith(".jpe")) return "image/jpeg";
        if (filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".ras")) return "image/x-cmu-raster";
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        throw new SystemFailureException("Unknown mime type for " + file);
    }

}
