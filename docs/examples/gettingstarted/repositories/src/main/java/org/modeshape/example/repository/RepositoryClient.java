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
package org.modeshape.example.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.Immutable;
import org.jboss.security.config.IDTrustConfiguration;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JaasSecurityContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.xml.sax.SAXException;

/**
 * The repository client, with the main application.
 */
public class RepositoryClient {

    public static final String INMEMORY_REPOSITORY_SOURCE_CLASSNAME = "org.modeshape.connector.inmemory.InMemoryRepositorySource";
    public static final String JAAS_LOGIN_CONTEXT_NAME = "modeshape-jcr";

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // Set up the JAAS provider (IDTrust) and a policy file (which defines the "modeshape-jcr" login config name)
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config("security/jaas.conf.xml");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Now configure the repository client component ...
        RepositoryClient client = new RepositoryClient();
        for (String arg : args) {
            arg = arg.trim();
            if (arg.equals("--api=jcr")) client.setApi(Api.JCR);
            if (arg.equals("--api=dna")) client.setApi(Api.ModeShape);
            if (arg.equals("--jaas")) client.setJaasContextName(JAAS_LOGIN_CONTEXT_NAME);
            if (arg.startsWith("--jaas=") && arg.length() > 7) client.setJaasContextName(arg.substring(7).trim());
        }

        // And have it use a ConsoleInput user interface ...
        client.setUserInterface(new ConsoleInput(client, args));
    }

    public enum Api {
        JCR,
        ModeShape;
    }

    private Api api = Api.JCR;
    private String jaasContextName = JAAS_LOGIN_CONTEXT_NAME;
    private UserInterface userInterface;
    private LoginContext loginContext;
    private JcrEngine engine;

    /**
     * @param userInterface Sets userInterface to the specified value.
     */
    public void setUserInterface( UserInterface userInterface ) {
        this.userInterface = userInterface;
    }

    /**
     * Set the API that this client should use to interact with the repositories.
     * 
     * @param api The API that should be used
     */
    public void setApi( Api api ) {
        this.api = api != null ? api : Api.ModeShape;
    }

    /**
     * Set the JAAS context name that should be used. If null (which is the default), then no authentication will be used.
     * 
     * @param jaasContextName the JAAS context name, or null if no authentication should be performed
     */
    public void setJaasContextName( String jaasContextName ) {
        this.jaasContextName = jaasContextName;
    }

    /**
     * Start up the repositories. This method loads the configuration, then creates the engine and starts it.
     * 
     * @throws IOException if there is a problem initializing the repositories from the files.
     * @throws SAXException if there is a problem with the SAX Parser
     */
    public void startRepositories() throws IOException, SAXException {
        if (engine != null) return; // already started

        // Load the configuration from a file, as provided by the user interface ...
        JcrConfiguration configuration = new JcrConfiguration();
        configuration.loadFrom(userInterface.getRepositoryConfiguration());

        // Now create the JCR engine ...
        engine = configuration.build();
        engine.start();

        if (engine.getProblems().hasProblems()) {
            for (Problem problem : engine.getProblems()) {
                System.err.println(problem.getMessageString());
            }
            throw new RuntimeException("Could not start due to problems");
        }

        // For this example, we're using a couple of in-memory repositories (including one for the configuration repository).
        // Normally, these would exist already and would simply be accessed. But in this example, we're going to
        // populate these repositories here by importing from files. First do the configuration repository ...
        String location = this.userInterface.getLocationOfRepositoryFiles();

        // Now import the content for the two in-memory repository sources ...
        engine.getGraph("Cars").importXmlFrom(location + "/cars.xml").into("/");
        engine.getGraph("Aircraft").importXmlFrom(location + "/aircraft.xml").into("/");
    }

    /**
     * Get the names of the repositories.
     * 
     * @return the sorted but immutable list of repository names; never null
     */
    public List<String> getNamesOfRepositories() {
        List<String> names = new ArrayList<String>(engine.getRepositoryNames());
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    /**
     * Shut down the components and services and blocking until all resources have been released.
     * 
     * @throws InterruptedException if the thread was interrupted before completing the shutdown.
     * @throws LoginException
     */
    public void shutdown() throws InterruptedException, LoginException {
        logout();
        if (engine == null) return;
        try {
            // Tell the engine to shut down, and then wait up to 5 seconds for it to complete...
            engine.shutdown();
            engine.awaitTermination(5, TimeUnit.SECONDS);
        } finally {
            engine = null;
        }
    }

    /**
     * Get the current JAAS LoginContext (if there is one).
     * 
     * @return the current login context, or null if no JAAS authentication is to be used.
     * @throws LoginException if authentication was attempted but failed
     */
    protected LoginContext getLoginContext() throws LoginException {
        if (loginContext == null) {
            if (jaasContextName != null) {
                loginContext = new LoginContext(jaasContextName, this.userInterface.getCallbackHandler());
                loginContext.login(); // This authenticates the user
            }
        }
        return loginContext;
    }

    /**
     * Calling this will lose the context
     * 
     * @throws LoginException
     */
    public void logout() throws LoginException {
        if (loginContext != null) {
            try {
                loginContext.logout();
            } finally {
                loginContext = null;
            }
        }
    }

    /**
     * Get the information about a node, using the {@link #setApi(Api) API} method.
     * 
     * @param sourceName the name of the repository source
     * @param pathToNode the path to the node in the repository that is to be retrieved
     * @param properties the map into which the property values will be placed; may be null if the properties are not to be
     *        retrieved
     * @param children the collection into which the child names should be placed; may be null if the children are not to be
     *        retrieved
     * @return true if the node was found, or false if it was not
     * @throws Throwable
     */
    public boolean getNodeInfo( String sourceName,
                                String pathToNode,
                                Map<String, Object[]> properties,
                                List<String> children ) throws Throwable {
        LoginContext loginContext = getLoginContext(); // will ask user to authenticate if needed
        switch (api) {
            case JCR: {
                JcrRepository jcrRepository = engine.getRepository(sourceName);
                Session session = null;
                if (loginContext != null) {
                    // Could also use SimpleCredentials(username,password) too
                    Credentials credentials = new JaasCredentials(loginContext);
                    session = jcrRepository.login(credentials);
                } else {
                    session = jcrRepository.login();
                }
                try {
                    // Make the path relative to the root by removing the leading slash(es) ...
                    pathToNode = pathToNode.replaceAll("^/+", "");
                    // Get the node by path ...
                    Node root = session.getRootNode();
                    Node node = root;
                    if (pathToNode.length() != 0) {
                        if (!pathToNode.endsWith("]")) pathToNode = pathToNode + "[1]";
                        node = pathToNode.equals("") ? root : root.getNode(pathToNode);
                    }

                    // Now populate the properties and children ...
                    if (properties != null) {
                        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                            javax.jcr.Property property = iter.nextProperty();
                            Object[] values = null;
                            // Must call either 'getValue()' or 'getValues()' depending upon # of values
                            if (property.getDefinition().isMultiple()) {
                                Value[] jcrValues = property.getValues();
                                values = new String[jcrValues.length];
                                for (int i = 0; i < jcrValues.length; i++) {
                                    values[i] = jcrValues[i].getString();
                                }
                            } else {
                                values = new Object[] {property.getValue().getString()};
                            }
                            properties.put(property.getName(), values);
                        }
                    }
                    if (children != null) {
                        // Figure out which children need same-name sibling indexes ...
                        Set<String> sameNameSiblings = new HashSet<String>();
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            javax.jcr.Node child = iter.nextNode();
                            if (child.getIndex() > 1) sameNameSiblings.add(child.getName());
                        }
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            javax.jcr.Node child = iter.nextNode();
                            String name = child.getName();
                            if (sameNameSiblings.contains(name)) name = name + "[" + child.getIndex() + "]";
                            children.add(name);
                        }
                    }
                } catch (javax.jcr.ItemNotFoundException e) {
                    return false;
                } catch (javax.jcr.PathNotFoundException e) {
                    return false;
                } finally {
                    if (session != null) session.logout();
                }
                break;
            }
            case ModeShape: {
                try {
                    // Use the ModeShape Graph API to read the properties and children of the node ...
                    ExecutionContext context = this.engine.getExecutionContext();
                    if (loginContext != null) {
                        JaasSecurityContext security = new JaasSecurityContext(loginContext);
                        context = context.with(security);
                    }
                    Graph graph = engine.getGraph(context, sourceName);
                    org.modeshape.graph.Node node = graph.getNodeAt(pathToNode);

                    if (properties != null) {
                        // Now copy the properties into the map provided as a method parameter ...
                        for (Property property : node.getProperties()) {
                            String name = property.getName().getString(context.getNamespaceRegistry());
                            properties.put(name, property.getValuesAsArray());
                        }
                    }
                    if (children != null) {
                        // And copy the names of the children into the list provided as a method parameter ...
                        for (Location child : node.getChildren()) {
                            String name = child.getPath().getLastSegment().getString(context.getNamespaceRegistry());
                            children.add(name);
                        }
                    }
                } catch (PathNotFoundException e) {
                    return false;
                }
                break;
            }
        }
        return true;
    }

    /**
     * Utility to build a path given the current path and the input path as string, where the input path could be an absolute path
     * or relative to the current and where the input may use "." and "..".
     * 
     * @param current the current path
     * @param input the input path
     * @return the resulting full and normalized path
     */
    public String buildPath( String current,
                             String input ) {
        if (current == null) current = "/";
        if (input == null || input.length() == 0) return current;
        ExecutionContext context = this.engine.getExecutionContext();
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path inputPath = factory.create(input);
        if (inputPath.isAbsolute()) {
            return inputPath.getNormalizedPath().getString(context.getNamespaceRegistry(), NoOpEncoder.getInstance());
        }
        Path currentPath = factory.create(current);
        currentPath = factory.create(currentPath, inputPath);
        currentPath = currentPath.getNormalizedPath();
        return currentPath.getString(context.getNamespaceRegistry(), NoOpEncoder.getInstance());
    }

    /**
     * A class that represents JCR Credentials containing the JAAS LoginContext.
     * 
     * @author Randall Hauch
     */
    @Immutable
    protected static class JaasCredentials implements Credentials {
        private static final long serialVersionUID = 1L;
        private final LoginContext context;

        public JaasCredentials( LoginContext context ) {
            CheckArg.isNotNull(context, "context");
            this.context = context;
        }

        /**
         * ModeShape's JCR implementation will reflectively look for and call this method to get the JAAS LoginContext.
         * 
         * @return the current LoginContext
         */
        public LoginContext getLoginContext() {
            return context;
        }

    }
}
