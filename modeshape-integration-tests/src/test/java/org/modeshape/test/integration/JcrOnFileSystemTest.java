package org.modeshape.test.integration;

import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.util.FileUtil;
import org.modeshape.connector.filesystem.FileSystemSource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;

public class JcrOnFileSystemTest {

    private static final String SOURCE_NAME = "Source";
    private static final String REPO_NAME = "Repository";
    private static final String STORAGE_PATH = "./target/scratch";
    private static final String EXCLUSION_PATTERN = ".*mode";

    private JcrEngine engine;
    private Session session;
    private ValueFactory vf;
    private File scratchSpace;

    @Before
    public void beforeEach() throws Exception {
        scratchSpace = new File(STORAGE_PATH);

        if (!scratchSpace.exists()) {
            scratchSpace.mkdir();
        }

        JcrConfiguration config = new JcrConfiguration();

        config.repositorySource(SOURCE_NAME).usingClass(FileSystemSource.class).setProperty("exclusionPattern", EXCLUSION_PATTERN).setProperty("workspaceRootPath",
                                                                                                                                        STORAGE_PATH).setProperty("defaultWorkspaceName",
                                                                                                                                                             "default").setProperty("updatesAllowed",
                                                                                                                                                                                    true);
        config.repository(REPO_NAME).setSource(SOURCE_NAME).setOption(JcrRepository.Option.ANONYMOUS_USER_ROLES, "readwrite");
        
        engine = config.build();
        engine.start();

        if (engine.getProblems().hasProblems()) {
            for (Problem problem : engine.getProblems()) {
                System.err.println(problem.getMessageString());
            }
        }

        JcrRepository repo = engine.getRepository(REPO_NAME);
        session = repo.login("default");
        vf = session.getValueFactory();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) session.logout();
        engine.shutdownAndAwaitTermination(3, TimeUnit.SECONDS);
        
        FileUtil.delete(scratchSpace);
    }

    @FixFor( "MODE-986" )
    @Test( expected = RepositoryException.class )
    public void shouldNotBeAbleToCreateFileWithFilteredName() throws Exception {
        Node root = session.getRootNode();
        Node file = root.addNode("createfile.mode", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));

        session.save();

    }

    @FixFor( "MODE-986" )
    @Test( expected = RepositoryException.class )
    public void shouldNotBeAbleToRenameToFileWithFilteredName() throws Exception {
        Node root = session.getRootNode();
        Node file = root.addNode("moveSource.txt", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));
        session.save();
        
        session.move("/moveSource.txt", "/createfile.mode");
        session.save();
    }

    @FixFor( "MODE-986" )
    @Test( expected = RepositoryException.class )
    public void shouldNotBeAbleToCopyToFileWithFilteredName() throws Exception {
        Node root = session.getRootNode();
        Node file = root.addNode("copySource.txt", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:data", vf.createBinary(new ByteArrayInputStream("Write 1".getBytes())));
        session.save();

        session.getWorkspace().copy("/copySource.txt", "/createfile.mode");
        session.save();

    }

    @FixFor( "MODE-1010" )
    @Test
    public void shouldAllowFileRenameThroughJcr() throws Exception {
        Node root = session.getRootNode();
        root.addNode("oldname", "nt:folder");
        session.save();

        File oldFile = new File(STORAGE_PATH + File.separator + "default" + File.separator + "oldname");
        assertTrue(oldFile.exists());

        session.move("/oldname", "/newname1");
        session.save();

        File newFile = new File(STORAGE_PATH + File.separator + "default" + File.separator + "newname1");
        assertTrue(newFile.exists());

    }
}
