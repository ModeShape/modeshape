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
package org.jboss.example.dna.sequencer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.security.auth.login.LoginException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.repository.sequencer.SequencingService;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.sequencer.image.ImageMetadataSequencer;
import org.jboss.dna.sequencer.java.JavaMetadataSequencer;
import org.jboss.dna.sequencer.mp3.Mp3MetadataSequencer;

/**
 * @author Randall Hauch
 */
public class SequencingClient {

    public static final String DEFAULT_REPOSITORY_NAME = "repo";
    public static final String DEFAULT_WORKSPACE_NAME = "default";
    public static final String DEFAULT_USERNAME = "jsmith";
    public static final char[] DEFAULT_PASSWORD = "secret".toCharArray();

    public static void main( String[] args ) {
        // Set up an execution context in which we'll run, and authenticate using JAAS ...
        ExecutionContext context = new ExecutionContext();
        String jaasAppContext = "myAppContext";
        String username = "jsmith";
        char[] password = "secrete".toCharArray();
        try {
            context.with(jaasAppContext, username, password);
        } catch (LoginException err) {
            System.err.println("Error authenticating \"" + username + "\". Check username and password and try again.");
        }

        // Configure the DNA JCR engine ...
        String repositoryId = "content";
        String workspaceName = "default";
        JcrConfiguration config = new JcrConfiguration(context);
        config.withConfigurationSource().usingClass(InMemoryRepositorySource.class).usingWorkspace("default").under("/");
        // Set up an in-memory repository where the uploaded and sequenced content will be stored ...
        config.addSource(repositoryId)
              .usingClass(InMemoryRepositorySource.class)
              .withNodeTypes(ImageMetadataSequencer.class.getResource("org/jboss/dna/sequencer/image/images.cnd"))
              .withNodeTypes(Mp3MetadataSequencer.class.getResource("org/jboss/dna/sequencer/mp3/mp3.cnd"))
              .withNodeTypes(JavaMetadataSequencer.class.getResource("org/jboss/dna/sequencer/java/javaSource.cnd"))
              .named("Content Repository")
              .describedAs("The repository for our content")
              .with("defaultWorkspaceName")
              .setTo(workspaceName);
        // Set up the image sequencer ...
        config.addSequencer("images")
              .usingClass("org.jboss.dna.sequencer.image.ImageMetadataSequencer")
              .loadedFromClasspath()
              .describedAs("Sequences image files to extract the characteristics of the image")
              .named("Image Sequencer")
              .sequencingFrom("//(*.(jpg|jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd)[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/images/$1");
        // Set up the MP3 sequencer ...
        config.addSequencer("mp3s")
              .usingClass(Mp3MetadataSequencer.class)
              .named("MP3 Sequencer")
              .describedAs("Sequences mp3 files to extract the id3 tags of the audio file")
              .sequencingFrom("//(*.mp3[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/mp3s/$1");
        // Set up the Java source file sequencer ...
        config.addSequencer("javaSource")
              .usingClass(JavaMetadataSequencer.class)
              .named("Java Sequencer")
              .describedAs("Sequences mp3 files to extract the id3 tags of the audio file")
              .sequencingFrom("//(*.mp3[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/mp3s/$1");

        // Now start the client and tell it which repository and workspace to use ...
        SequencingClient client = new SequencingClient(config, repositoryId, workspaceName);
        client.setUserInterface(new ConsoleInput(client));
    }

    private final String repositoryName;
    private final String workspaceName;
    private final JcrConfiguration configuration;
    private JcrEngine engine;
    private UserInterface userInterface;
    private Repository repository;

    public SequencingClient( JcrConfiguration config,
                             String repositoryName,
                             String workspaceName ) {
        this.configuration = config;
        this.repositoryName = repositoryName != null ? repositoryName : DEFAULT_REPOSITORY_NAME;
        this.workspaceName = workspaceName != null ? workspaceName : DEFAULT_WORKSPACE_NAME;
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
                // Start the DNA engine ...
                this.engine = this.configuration.build();
                this.engine.start();

                // Now get the repository instance ...
                this.repository = this.engine.getRepository(repositoryName);

            } catch (Exception e) {
                this.repository = null;
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
                this.engine.shutdown();
                this.engine.awaitTermination(4, TimeUnit.SECONDS);
            } finally {
                this.repository = null;
            }
        }
    }

    /**
     * Get the sequencing statistics.
     * 
     * @return the statistics; never null
     */
    public SequencingService.Statistics getStatistics() {
        return this.engine.getSequencingService().getStatistics();
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

        if (mimeType == null) {
            System.err.println("Could not determine mime type for file.  Cancelling upload.");
            return;
        }

        // Now use the JCR API to upload the file ...
        Session session = createSession();
        JcrTools tools = new JcrTools();
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
     * 
     * @throws Exception
     */
    public void search() throws Exception {
        // Use JCR to search the repository for image metadata ...
        List<ContentInfo> infos = new ArrayList<ContentInfo>();
        Session session = createSession();
        try {
            // Find the node ...
            Node root = session.getRootNode();

            if (root.hasNode("images") || root.hasNode("mp3s")) {
                Node mediasNode;
                if (root.hasNode("images")) {
                    mediasNode = root.getNode("images");

                    for (NodeIterator iter = mediasNode.getNodes(); iter.hasNext();) {
                        Node mediaNode = iter.nextNode();
                        if (mediaNode.hasNode("image:metadata")) {
                            infos.add(extractMediaInfo("image:metadata", "image", mediaNode));
                        }
                    }
                }
                if (root.hasNode("mp3s")) {
                    mediasNode = root.getNode("mp3s");

                    for (NodeIterator iter = mediasNode.getNodes(); iter.hasNext();) {
                        Node mediaNode = iter.nextNode();
                        if (mediaNode.hasNode("mp3:metadata")) {
                            infos.add(extractMediaInfo("mp3:metadata", "mp3", mediaNode));
                        }
                    }
                }

            }
            if (root.hasNode("java")) {
                Map<String, List<Properties>> tree = new TreeMap<String, List<Properties>>();
                // Find the compilation unit node ...
                List<Properties> javaElements;
                if (root.hasNode("java")) {
                    Node javaSourcesNode = root.getNode("java");
                    for (NodeIterator i = javaSourcesNode.getNodes(); i.hasNext();) {

                        Node javaSourceNode = i.nextNode();

                        if (javaSourceNode.hasNodes()) {
                            Node javaCompilationUnit = javaSourceNode.getNodes().nextNode();
                            // package informations

                            javaElements = new ArrayList<Properties>();
                            try {
                                Node javaPackageDeclarationNode = javaCompilationUnit.getNode("java:package/java:packageDeclaration");
                                javaElements.add(extractJavaInfo(javaPackageDeclarationNode));
                                tree.put("Class package", javaElements);
                            } catch (PathNotFoundException e) {
                                // do nothing
                            }

                            // import informations
                            javaElements = new ArrayList<Properties>();
                            try {
                                for (NodeIterator singleImportIterator = javaCompilationUnit.getNode("java:import/java:importDeclaration/java:singleImport")
                                                                                            .getNodes(); singleImportIterator.hasNext();) {
                                    Node javasingleTypeImportDeclarationNode = singleImportIterator.nextNode();
                                    javaElements.add(extractJavaInfo(javasingleTypeImportDeclarationNode));
                                }
                                tree.put("Class single Imports", javaElements);
                            } catch (PathNotFoundException e) {
                                // do nothing
                            }

                            javaElements = new ArrayList<Properties>();
                            try {
                                for (NodeIterator javaImportOnDemandIterator = javaCompilationUnit.getNode("java:import/java:importDeclaration/java:importOnDemand")
                                                                                                  .getNodes(); javaImportOnDemandIterator.hasNext();) {
                                    Node javaImportOnDemandtDeclarationNode = javaImportOnDemandIterator.nextNode();
                                    javaElements.add(extractJavaInfo(javaImportOnDemandtDeclarationNode));
                                }
                                tree.put("Class on demand imports", javaElements);

                            } catch (PathNotFoundException e) {
                                // do nothing
                            }
                            // class head informations
                            javaElements = new ArrayList<Properties>();
                            Node javaNormalDeclarationClassNode = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration");
                            javaElements.add(extractJavaInfo(javaNormalDeclarationClassNode));
                            tree.put("Class head information", javaElements);

                            // field member informations
                            javaElements = new ArrayList<Properties>();
                            for (NodeIterator javaFieldTypeIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType")
                                                                                         .getNodes(); javaFieldTypeIterator.hasNext();) {
                                Node rootFieldTypeNode = javaFieldTypeIterator.nextNode();
                                if (rootFieldTypeNode.hasNode("java:primitiveType")) {
                                    Node javaPrimitiveTypeNode = rootFieldTypeNode.getNode("java:primitiveType");
                                    javaElements.add(extractJavaInfo(javaPrimitiveTypeNode));
                                    // more informations
                                }

                                if (rootFieldTypeNode.hasNode("java:simpleType")) {
                                    Node javaSimpleTypeNode = rootFieldTypeNode.getNode("java:simpleType");
                                    javaElements.add(extractJavaInfo(javaSimpleTypeNode));
                                }
                                if (rootFieldTypeNode.hasNode("java:parameterizedType")) {
                                    Node javaParameterizedType = rootFieldTypeNode.getNode("java:parameterizedType");
                                    javaElements.add(extractJavaInfo(javaParameterizedType));
                                }
                                if (rootFieldTypeNode.hasNode("java:arrayType")) {
                                    Node javaArrayType = rootFieldTypeNode.getNode("java:arrayType[2]");
                                    javaElements.add(extractJavaInfo(javaArrayType));
                                }
                            }
                            tree.put("Class field members", javaElements);

                            // constructor informations
                            javaElements = new ArrayList<Properties>();
                            for (NodeIterator javaConstructorIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor")
                                                                                           .getNodes(); javaConstructorIterator.hasNext();) {
                                Node javaConstructor = javaConstructorIterator.nextNode();
                                javaElements.add(extractJavaInfo(javaConstructor));
                            }
                            tree.put("Class constructors", javaElements);

                            // method informations
                            javaElements = new ArrayList<Properties>();
                            for (NodeIterator javaMethodIterator = javaCompilationUnit.getNode("java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:method")
                                                                                      .getNodes(); javaMethodIterator.hasNext();) {
                                Node javaMethod = javaMethodIterator.nextNode();
                                javaElements.add(extractJavaInfo(javaMethod));
                            }
                            tree.put("Class member functions", javaElements);

                            JavaInfo javaInfo = new JavaInfo(javaCompilationUnit.getPath(), javaCompilationUnit.getName(),
                                                             "java source", tree);
                            infos.add(javaInfo);
                        }
                    }
                }

            }
        } finally {
            session.logout();
        }

        // Display the search results ...
        this.userInterface.displaySearchResults(infos);
    }

    private MediaInfo extractMediaInfo( String metadataNodeName,
                                        String mediaType,
                                        Node mediaNode ) throws RepositoryException, PathNotFoundException, ValueFormatException {
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
     * Extract informations from a specific node.
     * 
     * @param node - node, that contains informations.
     * @return a properties of keys/values.
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private Properties extractJavaInfo( Node node ) throws ValueFormatException, IllegalStateException, RepositoryException {
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
        return this.repository.login(workspaceName);
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
        if (filename.endsWith(".java")) return "text/x-java-source";
        return null;
    }

}
