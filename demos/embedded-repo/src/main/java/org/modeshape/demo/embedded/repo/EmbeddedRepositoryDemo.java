package org.modeshape.demo.embedded.repo;

import java.net.URL;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

public class EmbeddedRepositoryDemo {

    public static void main( String[] argv ) {

        // Create and start the engine ...
        ModeShapeEngine engine = new ModeShapeEngine();
        engine.start();

        // Load the configuration for a repository via the classloader (can also use path to a file)...
        Repository repository = null;
        String repositoryName = null;
        try {
            URL url = EmbeddedRepositoryDemo.class.getClassLoader().getResource("my-repository-config.json");
            RepositoryConfiguration config = RepositoryConfiguration.read(url);

            // We could change the name of the repository programmatically ...
            // config = config.withName("Some Other Repository");

            // Verify the configuration for the repository ...
            Problems problems = config.validate();
            if (problems.hasErrors()) {
                System.err.println("Problems starting the engine.");
                System.err.println(problems);
                System.exit(-1);
            }

            // Deploy the repository ...
            repository = engine.deploy(config);
            repositoryName = config.getName();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
            return;
        }

        Session session = null;
        try {
            // Get the repository
            repository = engine.getRepository(repositoryName);

            // Create a session ...
            session = repository.login("default");

            // Get the root node ...
            Node root = session.getRootNode();
            assert root != null;

            System.out.println("Found the root node in the \"" + session.getWorkspace().getName() + "\" workspace");
        } catch (RepositoryException e) {
            e.printStackTrace();
        } finally {
            if (session != null) session.logout();
            System.out.println("Shutting down engine ...");
            try {
                engine.shutdown().get();
                System.out.println("Success!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
