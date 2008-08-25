package org.jboss.dna.sequencer.java;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.net.URL;
import org.jboss.dna.common.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class JavaSequencingClientTest {

    private URL javaSourceUrl;

    private JavaSequencingClient client;

    @Before
    public void beforeEach() throws Exception {
        
        // Get the URL of source (MySource.java), that have to be sequencing
        this.javaSourceUrl = FileUtil.convertFileToURL("workspace/project1/src/org/acme/MySource.java");
        
        // create the sequencing client
        client = new JavaSequencingClient();
        
        // the repository
        client.setWorkingDirectory("target/repositoryData");
        
        // for our content repository, we need a configuration file
        client.setJackrabbitConfigPath("src/main/resources/jackrabbitConfig.xml");
        
        // after all tests we have to deleted the local content repository
        FileUtil.delete("target/repositoryData");
    }

    @After
    public void afterEach() throws Exception {
        client.shutdownDnaServices();
        client.shutdownRepository();
        FileUtil.delete("target/repositoryData");
    }

    @Test
    public void shouldFindCompilationUnitSource() {
        assertThat(this.javaSourceUrl, is(notNullValue()));
    }

    @Test
    public void shouldStartupAndShutdownRepository() throws Exception {
        client.startRepository();
        client.shutdownRepository();

    }

    @Test
    public void shouldStartupAndShutdownRepositoryAndSequencingService() throws Exception {
        client.startRepository();
        client.startDnaServices();
        client.shutdownDnaServices();
        client.shutdownRepository();
    }

    @Test
    public void shouldUploadAndSequenceJavaSourceFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.javaSourceUrl, "/a/b/MySource.java", 1));
        client.startRepository();
        client.startDnaServices();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(1000);
        client.shutdownDnaServices();

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        // TODO
        client.search();
        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(1l));
    }

}
