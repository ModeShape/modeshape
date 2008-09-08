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
package org.jboss.dna.sequencer.java;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
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
import javax.jcr.ValueFormatException;
import javax.jcr.observation.Event;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencers.SequencerConfig;
import org.jboss.dna.repository.sequencers.SequencingService;
import org.jboss.dna.repository.util.BasicJcrExecutionContext;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.repository.util.SimpleSessionFactory;

/**
 * @author serge pagop
 */
public class JavaSequencingClient {

    public static final String DEFAULT_JACKRABBIT_CONFIG_PATH = "jackrabbitConfig.xml";
    public static final String DEFAULT_WORKING_DIRECTORY = "repositoryData";
    public static final String DEFAULT_REPOSITORY_NAME = "repo";
    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String DEFAULT_USERNAME = "jsmith";
    public static final char[] DEFAULT_PASSWORD = "secret".toCharArray();

    public static void main( String[] args ) {
        JavaSequencingClient client = new JavaSequencingClient();
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
    private JcrExecutionContext executionContext;

    public JavaSequencingClient() {
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

    protected void setRepositoryInformation( String repositoryName,
                                             String workspaceName,
                                             String username,
                                             char[] password ) {
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
     * 
     * @param userInterface
     */
    public void setUserInterface( UserInterface userInterface ) {
        this.userInterface = userInterface;
    }

    /**
     * Start up the JCR repository. This method only operates using the JCR API and Jackrabbit-specific API.
     * 
     * @throws Exception
     */
    public void startRepository() throws Exception {
        if (this.repository == null) {
            try {

                // Load the Jackrabbit configuration ...
                File configFile = new File(this.jackrabbitConfigPath);
                if (!configFile.exists()) {
                    throw new SystemFailureException("The Jackrabbit configuration file cannot be found at "
                                                     + configFile.getAbsoluteFile());
                }
                if (!configFile.canRead()) {
                    throw new SystemFailureException("Unable to read the Jackrabbit configuration file at "
                                                     + configFile.getAbsoluteFile());
                }
                String pathToConfig = configFile.getAbsolutePath();

                // Find the directory where the Jackrabbit repository data will be stored ...
                File workingDirectory = new File(this.workingDirectory);
                if (workingDirectory.exists()) {
                    if (!workingDirectory.isDirectory()) {
                        throw new SystemFailureException("Unable to create working directory at "
                                                         + workingDirectory.getAbsolutePath());
                    }
                }
                String workingDirectoryPath = workingDirectory.getAbsolutePath();

                // Get the Jackrabbit custom node definition (CND) file ...
                URL cndFile = Thread.currentThread().getContextClassLoader().getResource("java-source-artifact.cnd");

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
     * 
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
     * 
     * @throws Exception
     */
    public void startDnaServices() throws Exception {
        if (this.repository == null) {
            this.startRepository();
        }
        if (this.sequencingService == null) {

            // Create an execution context for the sequencing service. This execution context provides an environment
            // for the DNA services which knows about the JCR repositories, workspaces, and credentials used to
            // establish sessions to these workspaces. This example uses the BasicJcrExecutionContext, but there is
            // implementation for use with JCR repositories registered in JNDI.
            final String repositoryWorkspaceName = this.repositoryName + "/" + this.workspaceName;
            SimpleSessionFactory sessionFactory = new SimpleSessionFactory();
            sessionFactory.registerRepository(this.repositoryName, this.repository);
            if (this.username != null) {
                Credentials credentials = new SimpleCredentials(this.username, this.password);
                sessionFactory.registerCredentials(repositoryWorkspaceName, credentials);
            }
            this.executionContext = new BasicJcrExecutionContext(sessionFactory, repositoryWorkspaceName);

            // Create the sequencing service, passing in the execution context ...
            this.sequencingService = new SequencingService();
            this.sequencingService.setExecutionContext(executionContext);

            // Configure the sequencers. In this example, we only one sequencers that processes java source.
            // So create a configurations. Note that the sequencing service expects the class to be on the thread's current
            // context
            // class loader, or if that's null the class loader that loaded the SequencingService class.
            //
            // Part of the configuration includes telling DNA which JCR paths should be processed by the sequencer.
            // These path expressions tell the service that this sequencer should be invoked on the "jcr:data" property
            // on the "jcr:content" child node of any node uploaded to the repository whose name ends with one of the
            // supported extensions, and the sequencer should place the generated output meta data in a node with the same name as
            // the file but immediately below the "/compilationUnits" node. Path expressions can be fairly complex, and can even
            // specify that the generated information be placed in a different repository.
            // 
            // Sequencer configurations can be added before or after the service is started, but here we do it before the service
            // is running.
            String name = "Java Sequencer";
            String desc = "Sequences java files to extract the characteristics of the java sources";
            String classname = "org.jboss.dna.sequencer.java.JavaMetadataSequencer";
            String[] classpath = null; // Use the current class path
            String[] pathExpressions = {"//(*.java[*])/jcr:content[@jcr:data] => /compilationUnits/$1"};
            SequencerConfig javaSequencerConfig = new SequencerConfig(name, desc, classname, classpath, pathExpressions);
            this.sequencingService.addSequencer(javaSequencerConfig);

            // Use the DNA observation service to listen to the JCR repository (or multiple ones), and
            // then register the sequencing service as a listener to this observation service...
            this.observationService = new ObservationService(this.executionContext.getSessionFactory());
            this.observationService.getAdministrator().start();
            this.observationService.addListener(this.sequencingService);
            this.observationService.monitor(repositoryWorkspaceName, Event.NODE_ADDED | Event.PROPERTY_ADDED
                                                                     | Event.PROPERTY_CHANGED);
        }
        // Start up the sequencing service ...
        this.sequencingService.getAdministrator().start();
    }

    /**
     * Shut down the DNA services.
     * 
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
     * 
     * @return the statistics; never null
     */
    public SequencingService.Statistics getStatistics() {
        return this.sequencingService.getStatistics();
    }

    /**
     * Prompt the user interface for the file to upload into the JCR repository, then upload it using the JCR API.
     * 
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
     * Perform a search of the repository for all java compilation units automatically created by the java sequencer.
     * 
     * @throws Exception
     */
    public void search() throws Exception {
        // Use JCR to search the repository for image meta data ...
        Map<String, List<Properties>> tree = new TreeMap<String, List<Properties>>();
        List<JavaInfo> javaInfos = new ArrayList<JavaInfo>();
        Session session = createSession();
        try {
            // Find the compilation unit node ...
            Node root = session.getRootNode();
            JavaInfo javaInfo;
            List<Properties> javaElements;
            if (root.hasNode("compilationUnits")) {
                Node javaSourcesNode = root.getNode("compilationUnits");
                for (NodeIterator i = javaSourcesNode.getNodes(); i.hasNext();) {

                    Node javaSourceNode = i.nextNode();

                    if (javaSourceNode.hasNodes()) {
                        Node javaCompilationUnit = javaSourceNode.getNodes().nextNode();
                        // package informations

                        javaElements = new ArrayList<Properties>();
                        try {
                            Node javaPackageDeclarationNode = javaCompilationUnit.getNode("java:package/java:packageDeclaration");
                            javaElements.add(extractInfo(javaPackageDeclarationNode));
                            tree.put("Class package", javaElements);
                        } catch (PathNotFoundException e) {
                            // do nothing
                        }

                        // import informations
                        javaElements = new ArrayList<Properties>();
                        try {
                            for (NodeIterator singleImportIterator = javaCompilationUnit.getNode("java:import/java:importDeclaration/java:singleImport").getNodes(); singleImportIterator.hasNext();) {
                                Node javasingleTypeImportDeclarationNode = singleImportIterator.nextNode();
                                javaElements.add(extractInfo(javasingleTypeImportDeclarationNode));
                            }
                            tree.put("Class single Imports", javaElements);
                        } catch (PathNotFoundException e) {
                            // do nothing
                        }

                        javaElements = new ArrayList<Properties>();
                        try {
                            for (NodeIterator javaImportOnDemandIterator = javaCompilationUnit.getNode("java:import/java:importDeclaration/java:importOnDemand").getNodes(); javaImportOnDemandIterator.hasNext();) {
                                Node javaImportOnDemandtDeclarationNode = javaImportOnDemandIterator.nextNode();
                                javaElements.add(extractInfo(javaImportOnDemandtDeclarationNode));
                            }
                            tree.put("Class on demand imports", javaElements);

                        } catch (PathNotFoundException e) {
                            // do nothing
                        }
                        // class head informations
                        javaElements = new ArrayList<Properties>();
                        Node javaNormalDeclarationClassNode = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration");
                        javaElements.add(extractInfo(javaNormalDeclarationClassNode));
                        tree.put("Class head information", javaElements);

                        // field member informations
                        javaElements = new ArrayList<Properties>();
                        for (NodeIterator javaFieldTypeIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType").getNodes(); javaFieldTypeIterator.hasNext();) {
                            Node rootFieldTypeNode = javaFieldTypeIterator.nextNode();
                            if (rootFieldTypeNode.hasNode("java:primitiveType")) {
                                Node javaPrimitiveTypeNode = rootFieldTypeNode.getNode("java:primitiveType");
                                javaElements.add(extractInfo(javaPrimitiveTypeNode));
                                // more informations
                            }

                            if (rootFieldTypeNode.hasNode("java:simpleType")) {
                                Node javaSimpleTypeNode = rootFieldTypeNode.getNode("java:simpleType");
                                javaElements.add(extractInfo(javaSimpleTypeNode));
                            }
                            if (rootFieldTypeNode.hasNode("java:parameterizedType")) {
                                Node javaParameterizedType = rootFieldTypeNode.getNode("java:parameterizedType");
                                javaElements.add(extractInfo(javaParameterizedType));
                            }
                            if(rootFieldTypeNode.hasNode("java:arrayType")) {
                                Node javaArrayType = rootFieldTypeNode.getNode("java:arrayType[2]");
                                javaElements.add(extractInfo(javaArrayType));
                            }
                        }
                        tree.put("Class field members", javaElements);

                        // constructor informations
                        javaElements = new ArrayList<Properties>();
                        for (NodeIterator javaConstructorIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor").getNodes(); javaConstructorIterator.hasNext();) {
                            Node javaConstructor = javaConstructorIterator.nextNode();
                            javaElements.add(extractInfo(javaConstructor));
                        }
                        tree.put("Class constructors", javaElements);
                        
                        // method informations
                        javaElements = new ArrayList<Properties>();
                        for (NodeIterator javaMethodIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:method").getNodes(); javaMethodIterator.hasNext();) {
                            Node javaMethod = javaMethodIterator.nextNode();
                            javaElements.add(extractInfo(javaMethod));
                        }
                        tree.put("Class member functions", javaElements);
                        
                        javaInfo = new JavaInfo(javaCompilationUnit.getPath(), javaCompilationUnit.getName(), "java source", tree);
                        javaInfos.add(javaInfo);
                    }
                }
            }

        } finally {
            session.logout();
        }

        // Display the search results ...
        this.userInterface.displaySearchResults(javaInfos);
    }

    /**
     * Extract informations from a specific node.
     * 
     * @param node - node, that contains informations.
     * @return a properties of keys/values.
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private Properties extractInfo( Node node ) throws ValueFormatException, IllegalStateException, RepositoryException {
        if (node.hasProperties()) {
            Properties properties = new Properties();
            for (PropertyIterator propertyIter = node.getProperties(); propertyIter.hasNext();) {
                Property property = propertyIter.nextProperty();
                String name = property.getName();
                String stringValue = property.getValue().getString();
                properties.put(name, stringValue);
            }
            return properties;
        }
        return null;
    }

    /**
     * Utility method to create a new JCR session from the execution context's {@link SessionFactory}.
     * 
     * @return the session
     * @throws RepositoryException
     */
    protected Session createSession() throws RepositoryException {
        return this.executionContext.getSessionFactory().createSession(this.repositoryName + "/" + this.workspaceName);
    }

    protected String getMimeType( URL file ) {
        String filename = file.getPath().toLowerCase();
        if (filename.endsWith(".java")) return "text/x-java-source";
        throw new SystemFailureException("Unknown mime type for " + file);
    }

}
