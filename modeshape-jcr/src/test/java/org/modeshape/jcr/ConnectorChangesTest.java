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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.cache.change.PropertyAdded;
import org.modeshape.jcr.cache.change.RecordingChanges;

public class ConnectorChangesTest extends SingleUseAbstractTest {

    private ChangeBus changeBus;
    private TestListener listener;

    @Before
    @Override
    public void beforeEach() throws Exception {
        FileUtil.delete("target/files");
        // Now start the repository ...
        startRepositoryWithConfiguration(resource("config/repo-config-federation-changes.json"));
        printMessage("Started repository...");
        changeBus = repository().changeBus();
    }

    @After
    @Override
    public void afterEach() throws Exception {
        stopRepository();
        printMessage("Stopped repository.");
        FileUtil.delete("target/files");
    }

    @Test
    public void testChangesEmittedWhenNodeCreatedAndRemoved() throws Exception {
        final Session session = session();
        final Node root = session.getRootNode();
        printMessage("Root node is: ");
        print(root, false);
        Node projection1 = session.getNode("/projection1");
        Node projection1Generate = session.getNode("/projection1/generate");
        assertThat(projection1, is(notNullValue()));
        assertThat(projection1Generate, is(notNullValue()));

        // Let the existing startup-related events go through before we start listening ...
        Thread.sleep(1000);

        // expect 2 change sets (1 for the node we're adding, and one for the auto-generated events) ...
        listener = new TestListener(2);
        changeBus.register(listener);

        // Now add a node under '/testRoot/projection1/generate' to cause some events to be created under
        // '/testRoot/projection1/generated-out' ...
        Node node = projection1Generate.addNode("testNode1", "nt:unstructured");
        session.save();

        // Wait until the listener gets some events ...
        listener.await();
        assertThat("Didn't receive changes after node creation!", listener.receivedChangeSet.size(), is(2));

        // print = true;
        printMessage("Received change sets: \n" + listener.receivedChangeSet);

        // The first change set should be from the auto-created (since with the MockConnectorWithChanges will fire
        // off the events *before* the 'session.save()' call above completes...
        RecordingChanges changes = listener.receivedChangeSet.get(0);
        Iterator<Change> iter = changes.iterator();
        assertNodeAdded(iter.next(), "/projection1/generated-out/testNode1");
        assertNodeAdded(iter.next(), "/projection1/generated-out/testNode1/child0");
        assertNodeAdded(iter.next(), "/projection1/generated-out/testNode1/child1");
        assertNodeAdded(iter.next(), "/projection1/generated-out/testNode1/child2");
        assertThat(iter.hasNext(), is(false));

        // The remaining change set is due to our manually-created node saved above ...
        changes = listener.receivedChangeSet.get(1);
        iter = changes.iterator();
        assertNodeAdded(iter.next(), "/projection1/generate/testNode1");
        assertPropertyAdded(iter.next(), "/projection1/generate/testNode1", "jcr:primaryType");
        assertThat(iter.hasNext(), is(false));
        changeBus.unregister(listener);

        // expect 2 change sets (1 for the node we're adding, and one for the auto-generated events) ...
        listener = new TestListener(2);
        changeBus.register(listener);

        // Remove the recently-added node ...
        session.refresh(false);
        node = session.getNode("/projection1/generate/testNode1");
        node.remove();
        session.save();

        // Wait until the listener gets some events ...
        listener.await();
        assertThat("Didn't receive changes after node removal!", listener.receivedChangeSet.size(), is(2));
        // print = true;
        printMessage("Received change sets: \n" + listener.receivedChangeSet);

        // The first change set should be from the automatic removal (since with the MockConnectorWithChanges will fire
        // off the events *before* the 'session.save()' call above completes...
        changes = listener.receivedChangeSet.get(0);
        iter = changes.iterator();
        assertNodeRemoved(iter.next(), "/projection1/generated-out/testNode1");
        assertThat(iter.hasNext(), is(false));

        // The remaining change set is due to our manually-created node saved above ...
        changes = listener.receivedChangeSet.get(1);
        iter = changes.iterator();
        assertNodeRemoved(iter.next(), "/projection1/generate/testNode1");
        assertThat(iter.hasNext(), is(false));
        changeBus.unregister(listener);
    }

    protected void assertNodeAdded( Change change,
                                    String path ) {
        assertThat(change, is(instanceOf(NodeAdded.class)));
        NodeAdded added = (NodeAdded)change;
        assertThat(added.getPath(), is(path(path)));
    }

    protected void assertNodeRemoved( Change change,
                                      String path ) {
        assertThat(change, is(instanceOf(NodeRemoved.class)));
        NodeRemoved removed = (NodeRemoved)change;
        assertThat(removed.getPath(), is(path(path)));
    }

    protected void assertPropertyAdded( Change change,
                                        String path,
                                        String propertyName ) {
        assertThat(change, is(instanceOf(PropertyAdded.class)));
        PropertyAdded added = (PropertyAdded)change;
        assertThat(added.getPath(), is(path(path)));
        assertThat(added.getProperty().getName(), is(name(propertyName)));
    }

    protected static class TestListener implements ChangeSetListener {
        protected final List<RecordingChanges> receivedChangeSet;
        private CountDownLatch latch;

        public TestListener() {
            this(0);
        }

        protected TestListener( int expectedNumberOfChangeSet ) {
            latch = new CountDownLatch(expectedNumberOfChangeSet);
            receivedChangeSet = new ArrayList<RecordingChanges>();
        }

        public void expectChangeSet( int expectedNumberOfChangeSet ) {
            latch = new CountDownLatch(expectedNumberOfChangeSet);
            receivedChangeSet.clear();
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (!(changeSet instanceof RecordingChanges)) {
                throw new IllegalArgumentException("Invalid type of change set received");
            }
            receivedChangeSet.add((RecordingChanges)changeSet);
            latch.countDown();
        }

        public void await() throws InterruptedException {
            latch.await(5, TimeUnit.SECONDS);
        }

        public List<RecordingChanges> getObservedChangeSet() {
            return receivedChangeSet;
        }
    }
}
