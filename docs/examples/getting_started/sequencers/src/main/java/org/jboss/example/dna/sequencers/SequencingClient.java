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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import net.jcip.annotations.NotThreadSafe;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencers.SequencerConfig;
import org.jboss.dna.repository.sequencers.SequencingService;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.SimpleExecutionContext;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class SequencingClient {

    public static final String DEFAULT_JACKRABBIT_CONFIG_PATH = "jackrabbitConfig.xml";
    public static final String DEFAULT_WORKING_DIRECTORY = "repositoryData";
    public static final String DEFAULT_REPOSITORY_NAME = "repo";
    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String DEFAULT_USERNAME = "jsmith";
    public static final char[] DEFAULT_PASSWORD = "secret".toCharArray();

    public static void main( String[] args ) throws Exception {
        SequencingClient client = new SequencingClient();
        client.setRepositoryInformation(DEFAULT_REPOSITORY_NAME, DEFAULT_WORKSPACE_NAME, "jsmith", "secret".toCharArray());
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

    public void setUserInterface( UserInterface userInterface ) {
        this.userInterface = userInterface;
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

    public void startDnaServices() throws Exception {
        if (this.repository == null) {
            this.startRepository();
        }
        if (this.sequencingService != null) {
            return;
        }
        // Create an execution context for the sequencing service.
        // The repository instances and workspace names are registered, and that the service will reference.
        // the repository ...
        SimpleExecutionContext executionContext = new SimpleExecutionContext();
        // Register the JCR repository ...
        executionContext.registerRepository(this.repositoryName, this.repository);
        if (this.username != null) {
            Credentials credentials = new SimpleCredentials(this.username, this.password);
            executionContext.registerCredentials(this.repositoryName + "/" + this.workspaceName, credentials);
        }
        this.executionContext = executionContext;

        // Create the sequencing service ...
        this.sequencingService = new SequencingService();
        this.sequencingService.setExecutionContext(executionContext);

        // Add the configuration for the image sequencer. This sequencer class should be on the thread's current context class
        // loader, or if that's null the classloader that loaded the SequencingService class.
        //
        // The path expressions tell the service that this sequencer should be invoked on the "jcr:data" property
        // on the "jcr:content" child node of any node uploaded to the repository whose name ends with one of the
        // supported extensions, and it should place the output metadata in a node with the same name as the file
        // but immediately below the "/images" node. Path expressions can be fairly complex, and can even
        // specify that the generated information be placed in a different repository.
        // 
        // Sequencers can be added before or after the service is started.
        String name = "Image Sequencer";
        String desc = "Sequences image files to extract the characteristics of the image";
        String classname = "org.jboss.dna.sequencer.images.ImageMetadataSequencer";
        String[] classpath = null; // Use the current classpath
        String[] pathExpressions = {"//(*.(jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd))[*]/jcr:content[@jcr:data] => /images/$1"};
        SequencerConfig imageSequencerConfig = new SequencerConfig(name, desc, classname, classpath, pathExpressions);
        this.sequencingService.addSequencer(imageSequencerConfig);

        // Start up the sequencing service ...
        this.sequencingService.getAdministrator().start();

        // Register the sequencing service as a listener using the observation service ...
        this.observationService = new ObservationService(this.executionContext.getSessionFactory());
        this.observationService.getAdministrator().start();
        this.observationService.addListener(this.sequencingService);
        this.observationService.monitor(this.repositoryName + "/" + this.workspaceName, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED);
    }

    public void shutdownDnaServices() throws Exception {
        if (this.sequencingService == null) return;

        try {
            // Shut down the service and wait until it's all shut down ...
            this.sequencingService.getAdministrator().shutdown();
            this.sequencingService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);

            // Shut down the observation service ...
            this.observationService.getAdministrator().shutdown();
            this.observationService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);

        } finally {
            this.sequencingService = null;
            this.observationService = null;
        }
    }

    public SequencingService.Statistics getStatistics() {
        return this.sequencingService.getStatistics();
    }

    public void uploadFile() throws Exception {
        File file = this.userInterface.getPathOfFileToUpload();
        String nodePath = this.userInterface.getRepositoryPath("/a/b/" + file.getName());
        String mimeType = getMimeType(file);
        uploadFile(new FileInputStream(file), nodePath, mimeType);
    }

    public void search() throws Exception {
        List<ImageInfo> images = getImages();
        // Display the search results ...
        this.userInterface.displaySearchResults(images);
    }

    protected List<ImageInfo> getImages() throws Exception {
        List<ImageInfo> images = new ArrayList<ImageInfo>();
        Session session = createSession();
        try {
            // Find the image node ...
            Node root = session.getRootNode();
            if (root.hasNode("images")) {
                Node imagesNode = root.getNode("images");

                // Iterate over each child ...
                for (NodeIterator iter = imagesNode.getNodes(); iter.hasNext();) {
                    Node imageNode = iter.nextNode();
                    String nodePath = imageNode.getPath();
                    String nodeName = imageNode.getName();
                    if (imageNode.hasNode("image:metadata")) {
                        imageNode = imageNode.getNode("image:metadata");

                        // Create a Properties object containing the properties for this node; ignore any children ...
                        Properties props = new Properties();
                        for (PropertyIterator propertyIter = imageNode.getProperties(); propertyIter.hasNext();) {
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
                        ImageInfo info = new ImageInfo(nodePath, nodeName, props);
                        images.add(info);
                    }
                }
            }
        } finally {
            session.logout();
        }
        return images;
    }

    protected Session createSession() throws RepositoryException {
        return this.executionContext.getSessionFactory().createSession(this.repositoryName + "/" + this.workspaceName);
    }

    protected boolean uploadFile( InputStream content, String nodePath, String mimeType ) throws Exception {
        Session session = createSession();
        JcrTools tools = this.executionContext.getTools();
        try {
            // Create the node at the supplied path ...
            Node node = tools.findOrCreateNode(session, nodePath, "nt:folder", "nt:file");

            // Upload the file to that node ...
            Node contentNode = tools.findOrCreateChild(session, node, "jcr:content", "nt:resource");
            contentNode.setProperty("jcr:mimeType", mimeType);
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            contentNode.setProperty("jcr:data", content);

            // Save the session ...
            session.save();
        } finally {
            session.logout();
        }
        return true;
    }

    protected String getMimeType( File file ) {
        String extension = file.getName().toLowerCase();
        if (extension.endsWith(".gif")) return "image/gif";
        if (extension.endsWith(".png")) return "image/png";
        if (extension.endsWith(".pict")) return "image/x-pict";
        if (extension.endsWith(".bmp")) return "image/bmp";
        if (extension.endsWith(".jpg")) return "image/jpeg";
        if (extension.endsWith(".jpe")) return "image/jpeg";
        if (extension.endsWith(".jpeg")) return "image/jpeg";
        if (extension.endsWith(".ras")) return "image/x-cmu-raster";
        throw new SystemFailureException("Unknown mime type for file " + file);
    }

}
