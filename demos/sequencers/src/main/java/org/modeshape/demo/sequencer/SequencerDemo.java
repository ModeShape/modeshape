package org.modeshape.demo.sequencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrTools;

public class SequencerDemo {

    protected static boolean print = true;

    public static void main( String[] argv ) {

        // Create and start the engine ...
        JcrEngine engine = new JcrEngine();
        engine.start();

        // Load the configuration for a repository via the classloader (can also use path to a file)...
        Repository repository = null;
        String repositoryName = null;
        try {
            URL url = SequencerDemo.class.getClassLoader().getResource("my-repository.json");
            RepositoryConfiguration config = RepositoryConfiguration.read(url);

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
        JcrTools tools = new JcrTools();
        try {
            // Get the repository
            repository = engine.getRepository(repositoryName);

            // Create a session ...
            session = repository.login("default");

            // Create the '/files' node that is an 'nt:folder' ...
            Node root = session.getRootNode();
            Node filesNode = root.addNode("files", "nt:folder");
            assert filesNode != null;

            // Update a couple of files ...
            tools.uploadFile(session, "/files/caution.png", getFile("caution.png"));
            tools.uploadFile(session, "/files/sample1.mp3", getFile("sample1.mp3"));
            tools.uploadFile(session, "/files/fixedWidthFile.txt", getFile("fixedWidthFile.txt"));
            tools.uploadFile(session, "/files/JcrRepository.class", getFile("JcrRepository.clazz"));

            // Save the session ...
            session.save();

            // Now look for the output. Note that sequencing may take a bit, so we'll cheat by just trying
            // to find the node until we can find it, waiting a maximum amount of time before failing.
            // The proper way is to either use events or not to expect the sequencers have finished.
            Node png = findNodeAndWait(session, "/images/caution.png", 10, TimeUnit.SECONDS);
            if (print) tools.printSubgraph(png);

            Node sampleMp3 = findNodeAndWait(session, "/audio/sample1.mp3", 10, TimeUnit.SECONDS);
            if (print) tools.printSubgraph(sampleMp3);

            Node javaClass = findNodeAndWait(session, "/java/JcrRepository.class", 10, TimeUnit.SECONDS);
            if (print) tools.printSubgraph(javaClass);

            Node textFile = findNodeAndWait(session, "/text/fixedWidthFile.txt", 10, TimeUnit.SECONDS);
            if (print) tools.printSubgraph(textFile);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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

    public static InputStream getFile( String path ) {
        // First try to read from the file system ...
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                // continue
            }
        }
        // If not found, try to read from the classpath ...
        return SequencerDemo.class.getClassLoader().getResourceAsStream(path);
    }

    public static Node findNodeAndWait( Session session,
                                        String path,
                                        long maxWaitTime,
                                        TimeUnit unit ) throws RepositoryException, InterruptedException {
        long start = System.currentTimeMillis();
        long maxWaitInMillis = TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);

        do {
            try {
                // This method either returns a non-null Node reference, or throws an exception ...
                return session.getNode(path);
            } catch (PathNotFoundException e) {
                // The node wasn't there yet, so try again ...
            }
            Thread.sleep(10L);
        } while ((System.currentTimeMillis() - start) <= maxWaitInMillis);
        throw new PathNotFoundException("Failed to find node '" + path + "' even after waiting " + maxWaitTime + " " + unit);
    }
}
