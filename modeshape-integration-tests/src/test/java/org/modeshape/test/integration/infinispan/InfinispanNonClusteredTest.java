package org.modeshape.test.integration.infinispan;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.test.ModeShapeSingleUseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Integration test which uses an Infinispan source, non-clustered.
 *
 * @author Horia Chiorean
 */
@Ignore("Ignored because it takes way too long. If needed in the context of a bug, activate it")
public class InfinispanNonClusteredTest extends ModeShapeSingleUseTest  {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanNonClusteredTest.class);

    /**
     * number of files to create in the repo
     */
    private static final int FILES_COUNT = 50;

    /**
     * the maximum number of threads to start
     */
    private static final int MAX_THREADS = 150;

    /**
     * the increment in which threads will be started
     */
    private static final int THREADS_INCREMENT = 10;

    /**
     * number of times each thread will repeat an operation
     */
    private static final int REPEAT_COUNT_PER_THREAD = 5;

    /**
     * the time a thread waits after one operation is performed (milliseconds)
     */
    private static final long WAIT_AFTER_OPERATION_MILLIS = 10;

    private static final Random RANDOM = new Random();
    
    private ExecutorService executorService;

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        executorService.shutdownNow();
    }

    @FixFor("MODE-1403")
    @Test
    public void shouldPerformMultiThreadedReadCopy() throws Exception {
        addFilesToRepository(FILES_COUNT);

        int iterationsNr = MAX_THREADS / THREADS_INCREMENT;
        int threadsPerBatch = 1;

        CopyRandomFileOperation copyRandomFileOperation = new CopyRandomFileOperation(REPEAT_COUNT_PER_THREAD);

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        for (int i = 0; i < iterationsNr; i++) {
            List<CopyRandomFileOperation> jobs = jobsList(threadsPerBatch, copyRandomFileOperation);
            List<Future<Void>> futures = executorService.invokeAll(jobs);

            for (Future<?> future : futures) {
                future.get();
            }
            threadsPerBatch += THREADS_INCREMENT;
        }
        
        stopwatch.stop();
        LOGGER.debug("Total duration: {}", stopwatch.getTotalDuration().getDurationInSeconds());
    }

    private <T extends Callable<?>> List<T> jobsList(int elementsCount, T job) {
        List<T> result = new ArrayList<T>(elementsCount);
        for (int i = 0; i < elementsCount; i++) {
            result.add(job);
        }
        return result;
    }
    
    private void addFilesToRepository(int count) throws RepositoryException {
        Session session = sessionTo(null, null);
        for (int i = 0; i < count; i++) {
            addFileWithContent(session, "file_" + i, null);                        
        }
        session.save();
        session.logout();
    }

    private void addFileWithContent( Session session,
                                     String filepath,
                                     InputStream contentStream ) throws RepositoryException {
        Node fileContent = session.getRootNode().addNode(filepath).addNode("jcr:content", NodeType.NT_RESOURCE);
        if (contentStream == null) {
           contentStream = new ByteArrayInputStream("test_content".getBytes());            
        }
        fileContent.setProperty("jcr:data", session.getValueFactory().createBinary(contentStream));
    }

    private class CopyRandomFileOperation implements Callable<Void> {
        private int repeatCount;

        private CopyRandomFileOperation( int repeatCount ) {
            this.repeatCount = repeatCount;
        }

        @Override
        public Void call() throws Exception {
            String threadName = Thread.currentThread().getName();

            Session session = sessionTo(null, null);
            for (int i = 0; i < repeatCount; i++) {
                LOGGER.debug("{} iteration: {}", threadName, i);
                int fileIndex = RANDOM.nextInt(FILES_COUNT);
                Node fileContent = session.getNode("/file_" + fileIndex + "/jcr:content");
                Binary contentStream = fileContent.getProperty("jcr:data").getBinary();
                assertNotNull(contentStream);
                
                addFileWithContent(session, "file_" + i + "_copy", contentStream.getStream());
                session.save();

                Thread.sleep(WAIT_AFTER_OPERATION_MILLIS);
            }
            session.logout();
            return null;
        }
    }

    @Override
    protected String getPathToDefaultConfiguration() {
        return "infinispan/configRepositoryNonClustered.xml";
    }
}
