/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.Problems;

public class RepositoryBackupTest extends MultiUseAbstractTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        startRepository();
        // Initialize the repository data
        initializeContent(session);
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        stopRepository();
    }

    protected static void initializeContent( JcrSession session ) throws Exception {
        int breadth = 10;
        int depth = 3;
        int properties = 7;
        boolean print = true;

        createSubgraph(session, "/", depth, breadth, properties, false, new Stopwatch(), print ? System.out : null, null);
        // session.save();
    }

    private File testDirectory;

    @Before
    public void setUp() throws Exception {
        testDirectory = new File("target/backupArea/backupTests");
        FileUtil.delete(testDirectory);
    }

    @After
    public void tearDown() throws Exception {
        try {
            // We want to be able to see the result of each test in 'target' directory, so don't do this ...
            // FileUtil.delete(testDirectory);
        } finally {
            testDirectory = null;
        }
    }

    @Test
    public void shouldPerformOneBackup() throws Exception {
        Stopwatch sw = new Stopwatch();
        sw.start();
        Problems problems = session().getWorkspace().getRepositoryManager().backupRepository(testDirectory);
        sw.stop();
        assertThat(problems.hasProblems(), is(false));
        System.out.println("Time to perform backup: " + sw.getMaximumDuration());
    }

    @Test
    public void shouldPerformMultipleBackups() throws Exception {
        for (int i = 0; i != 3; ++i) {
            File file = new File(testDirectory, "test" + i);
            Stopwatch sw = new Stopwatch();
            sw.start();
            Problems problems = session().getWorkspace().getRepositoryManager().backupRepository(file);
            sw.stop();
            assertThat(problems.hasProblems(), is(false));
            System.out.println("Time to perform backup: " + sw.getMaximumDuration());
        }
    }

    @Ignore
    @Test
    public void shouldPerformOneBackupWhileChangesAreMade() throws Exception {
        JcrSession session = session();
        session.getRootNode().addNode("extras");

        final File testDirectory = this.testDirectory;
        final Stopwatch sw = new Stopwatch();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Problems> problems = new AtomicReference<Problems>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sw.start();
                try {
                    Problems backupProblems = session().getWorkspace().getRepositoryManager().backupRepository(testDirectory);
                    problems.set(backupProblems);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
                sw.stop();
                latch.countDown();
            }
        }).start();
        createSubgraph(session, "/extras", 1, 2, 2, false, new Stopwatch(), print ? System.out : null, null);
        latch.await(10, TimeUnit.SECONDS);
        assertThat(problems.get().hasProblems(), is(false));
        System.out.println("Time to perform backup: " + sw.getTotalDuration());
    }

}
