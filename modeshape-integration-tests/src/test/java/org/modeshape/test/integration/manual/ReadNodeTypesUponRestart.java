package org.modeshape.test.integration.manual;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.eclipse.core.runtime.AssertionFailedException;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;
import org.modeshape.test.ModeShapeUnitTest;
import org.modeshape.test.integration.performance.GuvnorEmulator;
import org.xml.sax.SAXException;

/**
 * This manual test verifies that node types can be registered in an engine and can be read in successfully upon subsequent
 * restarts. The procedure for running this test is as follows:
 * <ol>
 * <li>Change the value of the "FIRST_TIME" constant to "true" and recompile.</li>
 * <li>Run this application once.</li>
 * <li>Change the value of the "FIRST_TIME" constant to "false" and recompile.</li>
 * <li>Run this application any number of times.</li>
 */
public class ReadNodeTypesUponRestart extends ModeShapeUnitTest {

    private static final boolean FIRST_TIME = false;
    private static final String CONFIG_FILE_PATH = "src/test/resources/config/read-node-types-upon-restart-config.xml";
    private static final String NODE_TYPE_NAME = "StandardArticle";

    private static JcrEngine engine;

    public static void main( String[] args ) {
        if (FIRST_TIME) {
            FileUtil.delete("target/jcr-test-db");
        }

        try {
            startEngine(true);

            try {
                Session session = getSession();
                Workspace workspace = session.getWorkspace();
                getOrCreateNamespace(workspace, "jboss", "http://www.jboss.org");
                getOrCreateNodeType(workspace);
                // getNodeType(workspace);
                // session.save();

                GuvnorEmulator guvnor = new GuvnorEmulator(getSession().getRepository(), 150, true);
                guvnor.importGuvnorNodeTypes(!FIRST_TIME);

                if (FIRST_TIME) {
                    System.out.print("Importing Guvnor content ... ");
                    JcrTools tools = new JcrTools();
                    int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
                    tools.importContent(session, "io/drools/mortgage-sample-repository.xml", importBehavior);
                } else {
                    System.out.print("Verifying Guvnor content ... ");
                    guvnor.verifyContent();
                }
                System.out.println("completed");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.print("exception" + e);
            }

            try {
                Session session = getSession();
                Workspace workspace = session.getWorkspace();
                getNamespace(workspace, "jboss", "http://www.jboss.org");
                getNodeType(workspace);
                // session.save();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                System.out.print("exception" + e);
            }

        } finally {
            shutdownEngine();
        }

        try {
            startEngine(false);

            try {
                Session session = getSession();
                Workspace workspace = session.getWorkspace();
                getNamespace(workspace, "jboss", "http://www.jboss.org");
                getNodeType(workspace);
                // session.save();

                GuvnorEmulator guvnor = new GuvnorEmulator(getSession().getRepository(), 150, true);
                guvnor.importGuvnorNodeTypes(true);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.print("exception" + e);
            }

        } finally {
            shutdownEngine();
        }
    }

    private static void shutdownEngine() {
        if (engine == null) return; // not yet started

        // Shutdown the engine ...
        try {
            System.out.print("Shutting down ModeShape engine ... ");
            long nanos = System.nanoTime();
            engine.shutdownAndAwaitTermination(5, TimeUnit.SECONDS);
            nanos = System.nanoTime() - nanos;
            System.out.println("completed in " + TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS) + " millis");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            engine = null;
        }
    }

    @SuppressWarnings( "unused" )
    private static void startEngine( boolean firstTime ) {
        if (engine != null) return; // already started

        if (firstTime) {
            System.out.print("Starting ModeShape engine ... ");
        } else {
            System.out.println("");
            System.out.print("Restarting ModeShape engine ... ");
        }
        try {
            JcrConfiguration config = new JcrConfiguration().loadFrom(CONFIG_FILE_PATH);

            if (firstTime && FIRST_TIME) {
                config.repositorySource("Store").setProperty("mode:autoGenerateSchema", "create");
            } else {
                config.repositorySource("Store").setProperty("mode:autoGenerateSchema", "disable");
            }

            engine = config.build();
            long nanos = System.nanoTime();
            engine.start(true);
            nanos = System.nanoTime() - nanos;

            if (engine.getProblems().hasProblems()) {
                System.out.println("resulted in problems:");
                for (Problem problem : engine.getProblems()) {
                    System.err.println(problem.getMessageString());
                }
                throw new RuntimeException("Could not start due to problems");
            }
            System.out.println("completed in " + TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS) + " millis");
        } catch (IOException e) {
            System.out.print("exception" + e);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            System.out.print("exception" + e);
        }

        // Get a session to make sure everything is initialized ...
        Session session = null;
        try {
            System.out.print("Getting initial session ... ");
            long nanos = System.nanoTime();
            session = getSession();
            nanos = System.nanoTime() - nanos;
            System.out.println("completed in " + TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS) + " millis");
        } catch (RepositoryException e) {
            System.out.print("exception" + e);
        } finally {
            if (session != null) session.logout();
        }
    }

    private static Session getSession() throws RepositoryException {
        Repository repository = engine.getRepository("Repo");
        Session session = repository.login(); // Logs in with a guest session - works by default.
        return session;
    }

    private static void getNodeType( Workspace workspace ) {
        NodeTypeManager mgr;
        try {
            mgr = workspace.getNodeTypeManager();
            mgr.getAllNodeTypes();
            NodeType nodeType = mgr.getNodeType(NODE_TYPE_NAME);
            System.out.println("Found node type \"" + nodeType.getName() + "\"");

        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Example to create a node type Article
     * 
     * @param workspace
     */
    @SuppressWarnings( "unchecked" )
    private static void getOrCreateNodeType( Workspace workspace ) {
        try {
            NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();

            // Check if it's already there ...
            try {
                NodeType newNodeType = nodeTypeManager.getNodeType(NODE_TYPE_NAME);
                if (FIRST_TIME) {
                    System.out.println("Should not have found existing node type \"" + newNodeType.getName()
                                       + "\"; check instructions in JavaDoc and try again.");
                } else {
                    System.out.println("Found existing node type \"" + newNodeType.getName() + "\"");
                }
            } catch (NoSuchNodeTypeException e) {
                if (!FIRST_TIME) {
                    throw new AssertionFailedException("Should have found existing node type: " + e.getMessage());
                }

                // Declare a mixin node type named "searchable" (with no namespace)
                @SuppressWarnings( "unused" )
                NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
                nodeType.setName(NODE_TYPE_NAME);
                // nodeType.setMixin(true);

                // Add a property named "headline"
                PropertyDefinitionTemplate headline = nodeTypeManager.createPropertyDefinitionTemplate();
                headline.setName("headline");
                headline.setMandatory(true);
                headline.setRequiredType(PropertyType.STRING);
                nodeType.getPropertyDefinitionTemplates().add(headline);

                // Add a property named "teaser"
                PropertyDefinitionTemplate teaser = nodeTypeManager.createPropertyDefinitionTemplate();
                teaser.setName("teaser");
                teaser.setMandatory(true);
                teaser.setRequiredType(PropertyType.STRING);
                nodeType.getPropertyDefinitionTemplates().add(teaser);

                // Add a property named "body"
                PropertyDefinitionTemplate body = nodeTypeManager.createPropertyDefinitionTemplate();
                body.setName("body");
                body.setMandatory(true);
                body.setRequiredType(PropertyType.STRING);

                // Register the custom node type
                NodeType createdNodeType = nodeTypeManager.registerNodeType(nodeType, true);
                System.out.println("node type name:" + createdNodeType.getName());

                // Check that it's still there ...
                NodeType newNodeType = nodeTypeManager.getNodeType(NODE_TYPE_NAME);
                System.out.println("Created node type \"" + newNodeType.getName() + "\"");
            }

        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void getNamespace( Workspace workspace,
                                      String prefix,
                                      String uri ) {
        try {
            NamespaceRegistry registry = workspace.getNamespaceRegistry();
            String actualUri = registry.getURI(prefix);
            if (actualUri.equals(uri)) {
                System.out.println("Found existing namespace " + namespace(prefix, actualUri));
            } else {
                System.out.println("Found existing namespace " + namespace(prefix, actualUri)
                                   + " that didn't match expected URI: " + uri);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings( "unused" )
    private static void getOrCreateNamespace( Workspace workspace,
                                              String prefix,
                                              String uri ) {
        try {
            NamespaceRegistry registry = workspace.getNamespaceRegistry();

            // Check if it's already there ...
            try {
                String actualUri = registry.getURI(prefix);
                if (FIRST_TIME) {
                    System.out.println("Should not have found existing namespace " + namespace(prefix, uri)
                                       + " - check instructions in JavaDoc and try again.");
                } else {
                    if (actualUri.equals(uri)) {
                        System.out.println("Found existing namespace " + namespace(prefix, actualUri));
                    } else {
                        System.out.println("Found existing namespace " + namespace(prefix, actualUri)
                                           + " that didn't match expected URI: " + uri);
                    }
                }
            } catch (NamespaceException e) {
                if (!FIRST_TIME) {
                    throw new AssertionFailedException("Should have found existing namespace " + namespace(prefix, uri) + " : "
                                                       + e.getMessage());
                }

                registry.registerNamespace(prefix, uri);
                System.out.println("Registered namespace " + namespace(prefix, uri));
            }

        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private static String namespace( String prefix,
                                     String uri ) {
        return prefix + ":" + uri;
    }

}
