package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.MockRepositoryContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.basic.FileInputStreamBinary;

/**
 * Test of large file handling performance. This can generally be @Ignored and used by developers to verify that the FS connector
 * isn't adding significant overhead.
 * 
 */
public class LargeFileTest {

    private static final long VERY_LARGE_FILE_SIZE = 1L * 1000 * 1000 * 1000;
    private static final String VERY_LARGE_FILE_PATH = "./target/vlf.bin";
    private static File veryLargeFile;
    
    private ExecutionContext context = new ExecutionContext();

    @BeforeClass
    public static void beforeAny() throws Exception {
        // Create the very large file if it doesn't exist
        veryLargeFile = new File(VERY_LARGE_FILE_PATH);
        if (veryLargeFile.exists() && veryLargeFile.length() != VERY_LARGE_FILE_SIZE) {
            veryLargeFile.delete();
        }

        if (!veryLargeFile.exists()) {
            byte[] buff = new byte[8192];
            long bytesRemaining = VERY_LARGE_FILE_SIZE;

            // Write out a file containing all '0's (from the uninitialized buffer) of length > VERY_LARGE_FILE_SIZE
            FileOutputStream fos = new FileOutputStream(veryLargeFile);
            while (bytesRemaining > 0) {
                fos.write(buff);
                bytesRemaining -= buff.length;
            }
            fos.close();
        }
    }
    
    @AfterClass
    public static void afterAll() throws Exception {
        veryLargeFile.delete();
    }

    @Ignore
    @Test
    public void shouldWriteLargeFileInTimelyManner() throws Exception {
        File vlfRoot = new File("./target/vlfRoot");
        vlfRoot.mkdir();

        FileSystemSource source = new FileSystemSource();
        source.setName("FS Source");
        source.setWorkspaceRootPath("./target/vlfRoot");
        source.setUpdatesAllowed(true);
        source.initialize(new MockRepositoryContext(context));

        Binary binary = new FileInputStreamBinary(new FileInputStream(veryLargeFile));

        Graph graph = Graph.create(source, context);

        graph.create("/vlfTest").with("jcr:primaryType", "nt:file").and();

        Stopwatch sw = new Stopwatch();
        sw.start();

        graph.create("/vlfTest/jcr:content").with("jcr:primaryType", "nt:resource").and("jcr:data", binary).and();

        sw.stop();
        System.out.println("Created file node in " + sw.getTotalDuration().getDurationInMilliseconds() + " ms");

        FileUtil.delete(vlfRoot);
    }
    
    @Ignore
    @Test
    public void shouldCopyLargeFileOutOfBand() throws Exception {
        Stopwatch sw = new Stopwatch();
        sw.start();
        
        File newFile = new File(veryLargeFile.getAbsolutePath() + ".new");

        FileUtil.copy(veryLargeFile, newFile);

        sw.stop();
        
        newFile.delete();
        System.out.println("Copied file in " + sw.getTotalDuration().getDurationInMilliseconds() + " ms");
    }
}
