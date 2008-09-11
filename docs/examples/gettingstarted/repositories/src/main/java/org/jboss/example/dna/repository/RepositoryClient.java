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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.PasswordCallback;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrRepository;
import org.jboss.dna.repository.RepositoryImporter;
import org.jboss.dna.repository.RepositoryService;
import org.jboss.dna.repository.RepositorySourceManager;
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
            if (arg.startsWith("--user=") && arg.length() > 7) client.setUsername(arg.substring(7).trim());
        }
        client.setUserInterface(new ConsoleInput(client, args));
    }

    public enum Api {
        JCR,
        DNA;
    }

    private ClassLoaderFactory classLoaderFactory;
    private RepositorySourceManager sources;
    private ExecutionContextFactory contextFactory;
    private RepositoryService repositoryService;
    private Api api = Api.DNA;
    private String username = null;
    private char[] password = null;
    private UserInterface userInterface;

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
     * Set the username that this client should use.
     * 
     * @param username the new username, or null if no username should be used.
     */
    public void setUsername( String username ) {
        this.username = username;
        this.password = null;
    }

    /**
     * Clear any cached password.
     */
    public void clearPassword() {
        password = null;
    }

    /**
     * Method used internally to obtain the password, which is obtained from the user (only once per process) if needed
     * 
     * @return the user's password, or null if no username is known
     */
    private char[] getPassword() {
        if (username == null) return null;
        if (password == null) {
            PasswordCallback callback = new PasswordCallback("Password:", false);
            password = callback.getPassword();
        }
        return password;
    }

    /**
     * Start up the repositories. This method creates the necessary components and services, and initializes the in-memory
     * repositories.
     * 
     * @throws IOException if there is a problem initializing the repositories from the files.
     */
    public void startRepositories() throws IOException {
        if (repositoryService != null) return; // already started

        // Create the class loader factory, which for this example will simply use this class' class loader...
        classLoaderFactory = new StandardClassLoaderFactory();

        // Create the factory for execution contexts.
        contextFactory = new BasicExecutionContextFactory();

        // Create the execution context that we'll use for the services. If we'd want to use JAAS, we'd create the context
        // by supply LoginContext, AccessControlContext, or even Subject with CallbackHandlers. But no JAAS in this example.
        ExecutionContext context = contextFactory.create();

        // Create the manager for the RepositorySource instances ...
        sources = new RepositorySourceManager();

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
    public Collection<String> getNamesOfRepositories() {
        return sources.getSourceNames();
    }

    /**
     * Shut down the components and services and blocking until all resources have been released.
     * 
     * @throws InterruptedException if the thread was interrupted before completing the shutdown.
     */
    public void shutdown() throws InterruptedException {
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
        switch (api) {
            case JCR: {
                JcrRepository jcrRepository = new JcrRepository(contextFactory, sources);
                Session session = null;
                if (username != null) {
                    Credentials credentials = new SimpleCredentials(username, getPassword());
                    session = jcrRepository.login(credentials, sourceName);
                } else {
                    session = jcrRepository.login(sourceName);
                }
                try {
                    // Get the node by path ...
                    Node root = session.getRootNode();
                    Node node = root.getNode(pathToNode);

                    // Now populate the properties and children ...
                    if (properties != null) {
                        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                            javax.jcr.Property property = iter.nextProperty();
                            properties.put(property.getName(), property.getValues());
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
                ExecutionContext context = null;
                if (username != null) {
                    context = contextFactory.create(JAAS_LOGIN_CONTEXT_NAME);
                } else {
                    context = contextFactory.create();
                }
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
}
