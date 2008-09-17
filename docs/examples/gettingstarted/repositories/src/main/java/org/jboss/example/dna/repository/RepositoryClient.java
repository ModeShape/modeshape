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
package org.jboss.example.dna.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrRepository;
import org.jboss.dna.repository.RepositoryImporter;
import org.jboss.dna.repository.RepositoryLibrary;
import org.jboss.dna.repository.RepositoryService;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.ExecutionContextFactory;
import org.jboss.dna.spi.connector.BasicExecutionContextFactory;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;

/**
 * @author Randall Hauch
 */
public class RepositoryClient {

    public static final String INMEMORY_REPOSITORY_SOURCE_CLASSNAME = "org.jboss.dna.connector.inmemory.InMemoryRepositorySource";
    public static final String JAAS_LOGIN_CONTEXT_NAME = "dna-repository-example";

    /**
     * @param args
     */
    public static void main( String[] args ) {
        RepositoryClient client = new RepositoryClient();
        for (String arg : args) {
            arg = arg.trim();
            if (arg.equals("--api=jcr")) client.setApi(Api.JCR);
            if (arg.equals("--api=dna")) client.setApi(Api.DNA);
            if (arg.equals("--jaas")) client.setJaasContextName(JAAS_LOGIN_CONTEXT_NAME);
            if (arg.startsWith("--jaas=") && arg.length() > 7) client.setJaasContextName(arg.substring(7).trim());
        }
        client.setUserInterface(new ConsoleInput(client, args));
    }

    public enum Api {
        JCR,
        DNA;
    }

    private ClassLoaderFactory classLoaderFactory;
    private RepositoryLibrary sources;
    private ExecutionContextFactory contextFactory;
    private RepositoryService repositoryService;
    private Api api = Api.JCR;
    private String jaasContextName;
    private UserInterface userInterface;
    private LoginContext loginContext;
    private ExecutionContext context;

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
        this.api = api != null ? api : Api.DNA;
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
     * Start up the repositories. This method creates the necessary components and services, and initializes the in-memory
     * repositories.
     * 
     * @throws IOException if there is a problem initializing the repositories from the files.
     * @throws NamingException if there is a problem registering or looking up objects in JNDI
     */
    public void startRepositories() throws IOException, NamingException {
        if (repositoryService != null) return; // already started

        // Create the class loader factory, which for this example will simply use this class' class loader...
        classLoaderFactory = new StandardClassLoaderFactory();

        // Create the factory for execution contexts.
        contextFactory = new BasicExecutionContextFactory();

        // Create the execution context that we'll use for the services. If we'd want to use JAAS, we'd create the context
        // by supply LoginContext, AccessControlContext, or even Subject with CallbackHandlers. But no JAAS in this example.
        context = contextFactory.create();

        // Create the library for the RepositorySource instances ...
        sources = new RepositoryLibrary(contextFactory);

        // Load into the source manager the repository source for the configuration repository ...
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("Configuration");
        sources.addSource(configSource);

        // For this example, we're using a couple of in-memory repositories (including one for the configuration repository).
        // Normally, these would exist already and would simply be accessed. But in this example, we're going to
        // populate these repositories here by importing from files. First do the configuration repository ...
        String location = this.userInterface.getLocationOfRepositoryFiles();
        RepositoryImporter importer = new RepositoryImporter(sources, context);
        importer.importXml(location + "/configRepository.xml").into(configSource.getName());

        // Now instantiate the Repository Service ...
        repositoryService = new RepositoryService(sources, configSource.getName(), context, classLoaderFactory);
        repositoryService.getAdministrator().start();

        // Now import the conten for two of the other in-memory repositories ...
        importer.importXml(location + "/cars.xml").into("Cars");
        importer.importXml(location + "/aircraft.xml").into("Aircraft");
    }

    /**
     * Get the names of the repositories.
     * 
     * @return the repository names
     */
    public List<String> getNamesOfRepositories() {
        List<String> names = new ArrayList<String>(sources.getSourceNames());
        Collections.sort(names);
        return names;
    }

    /**
     * Shut down the components and services and blocking until all resources have been released.
     * 
     * @throws InterruptedException if the thread was interrupted before completing the shutdown.
     * @throws LoginException
     */
    public void shutdown() throws InterruptedException, LoginException {
        logout();
        if (repositoryService == null) return;
        try {
            // Shut down the various services ...
            repositoryService.getAdministrator().shutdown();

            // Shut down the manager of the RepositorySource instances, waiting until all connections are closed
            sources.getAdministrator().shutdown();
            sources.getAdministrator().awaitTermination(1, TimeUnit.SECONDS);
        } finally {
            repositoryService = null;
            sources = null;
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
                loginContext.login();
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
                JcrRepository jcrRepository = new JcrRepository(contextFactory, sources);
                Session session = null;
                if (loginContext != null) {
                    Credentials credentials = new JaasCredentials(loginContext);
                    session = jcrRepository.login(credentials, sourceName);
                } else {
                    session = jcrRepository.login(sourceName);
                }
                try {
                    // Make the path relative to the root by removing the leading slash(es) ...
                    pathToNode = pathToNode.replaceAll("^/+", "");
                    // Get the node by path ...
                    Node root = session.getRootNode();
                    Node node = root.getNode(pathToNode);

                    // Now populate the properties and children ...
                    if (properties != null) {
                        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                            javax.jcr.Property property = iter.nextProperty();
                            Object[] values = property.getDefinition().isMultiple() ? property.getValues() : new Object[] {property.getValue()};
                            properties.put(property.getName(), values);
                        }
                    }
                    if (children != null) {
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            javax.jcr.Node child = iter.nextNode();
                            children.add(child.getName());
                        }
                    }
                } catch (javax.jcr.PathNotFoundException e) {
                    return false;
                } finally {
                    if (session != null) session.logout();
                }
                break;
            }
            case DNA: {
                ExecutionContext context = loginContext != null ? contextFactory.create(loginContext) : contextFactory.create();
                PathFactory pathFactory = context.getValueFactories().getPathFactory();

                // Get the node submitting a graph command to a repository connection.
                // (Using commands is a little verbose, but we'll be introducing in the next release
                // an API that is much easier and concise.)
                Path path = pathToNode != null ? pathFactory.create(pathToNode) : pathFactory.createRootPath();
                BasicGetNodeCommand command = new BasicGetNodeCommand(path);
                RepositoryConnection connection = sources.createConnection(sourceName);
                try {
                    connection.execute(context, command);
                } finally {
                    if (connection != null) connection.close();
                }

                // Check whether there's been an error ...
                if (command.hasError()) {
                    if (command.getError() instanceof PathNotFoundException) return false;
                    throw command.getError();
                }

                // Now populate the properties and children ...
                if (properties != null) {
                    for (Property property : command.getProperties()) {
                        String name = property.getName().getString(context.getNamespaceRegistry());
                        properties.put(name, property.getValuesAsArray());
                    }
                }
                if (children != null) {
                    for (Path.Segment child : command.getChildren()) {
                        String name = child.getString(context.getNamespaceRegistry());
                        children.add(name);
                    }
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
    protected String buildPath( String current,
                                String input ) {
        if (current == null) current = "/";
        if (input == null || input.length() == 0) return current;
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
            ArgCheck.isNotNull(context, "context");
            this.context = context;
        }

        /**
         * JBoss DNA's JCR implementation will reflectively look for and call this method to get the JAAS LoginContext.
         * 
         * @return the current LoginContext
         */
        public LoginContext getLoginContext() {
            return context;
        }

    }
}
