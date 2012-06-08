package org.modeshape.test.integration.infinispan;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.concurrent.TimeUnit;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

/*
 * This test is currently ignored. See MODE-764 for details.
 */
@Ignore
public class InfinispanClusterTest {

    private JcrEngine engine1;
    private JcrEngine engine2;

    @Before
    public void beforeEach() throws Exception {
        engine1 = new JcrConfiguration().loadFrom("./src/test/resources/infinispan/configRepository.xml").build();
        engine2 = new JcrConfiguration().loadFrom("./src/test/resources/infinispan/configRepository.xml").build();

        engine1.start();
        if (engine1.getProblems().hasProblems()) {
            for (Problem problem : engine1.getProblems()) {
                System.err.println(problem.getMessageString());
            }
        }

        engine2.start();
        if (engine2.getProblems().hasProblems()) {
            for (Problem problem : engine2.getProblems()) {
                System.err.println(problem.getMessageString());
            }
        }
    }

    @After
    public void afterEach() throws Exception {
        if (engine1 != null) {
            engine1.shutdown();
            engine1.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (engine2 != null) {
            engine2.shutdown();
            engine2.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldShareContent() throws Exception {
        assertThat(engine1, is(notNullValue()));
        assertThat(engine2, is(notNullValue()));

        Repository repo1 = engine1.getRepository("Test Repository Source");
        Repository repo2 = engine2.getRepository("Test Repository Source");

        Session session1 = repo1.login();
        Session session2 = repo2.login();

        Node fooNode = session1.getRootNode().addNode("foo");
        fooNode.addMixin("mix:referenceable");

        checkNoNodeAt(session2, "/foo");

        session1.save();

        session2.refresh(false);
        Item foo2 = session2.getItem("/foo");
        foo2.remove();
        session2.save();

        session1.refresh(false);
        checkNoNodeAt(session1, "/foo");

    }

    private void checkNoNodeAt( Session session,
                                String path ) throws Exception {
        try {
            session.getItem(path);
            fail("No node should exist at path: " + path);
        } catch (PathNotFoundException expected) {

        }
    }
}
