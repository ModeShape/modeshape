package org.modeshape.jcr;

import static org.junit.Assert.assertTrue;
import static org.modeshape.common.logging.Logger.getLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.value.basic.JodaDateTime;

public class ConnectorChangesTest extends SingleUseAbstractTest {

	private ChangeBus changeBus;
	private TestListener listener;

	private Logger logger = getLogger(getClass());

	@Before
	public void beforeEach() throws Exception {
		startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-with-persistence.json"));
		logger.debug("Started repository...");
		changeBus = repository().changeBus();
	}

	@After
	public void afterEach() throws Exception {
		stopRepository();
		logger.debug("Stopped repository.");
	}

	@Test
	public void testChangesEmittedWhenNodeCreated() throws Exception {
		/* set the expected event numbres for the countdown latch and this test */
		listener = new TestListener(1);
		changeBus.register(listener);

		logger.debug("Executing testChangesEmittedWhenNodeCreated()...");

		/* hmm I guess the deletion is there to remove previously generated state, so I'll leave it in */
		FileUtil.delete("target/federation_persistent_repository");
		/* but for the tests to run the following directory must exists it seems */
		new File("target/federation_persistent_repository/store/persistentRepository").mkdirs();

		final Session session = session();
		final Node root = session.getRootNode();
		logger.debug("Root node is: ");
		new JcrTools(true).printSubgraph(root, 2);
		final Node federation = session.getNode("/federation");
		federation.addNode("testNode");
		session.save();

		/* since the event needs some time to propagate TestListener's CountdownLatch*/
		/* is used to determine the duration to wait for the event */
		listener.await();
		
		assertTrue("Didn't receive changes after node creation!",
				listener.receivedChangeSet.size() > 0);
		logger.debug("Executed testChangesEmittedWhenNodeCreated().");

	}

	protected static class TestListener implements ChangeSetListener {
		private final List<RecordingChanges> receivedChangeSet;
		private CountDownLatch latch;

		public TestListener() {
			this(0);
		}

		protected TestListener(int expectedNumberOfChangeSet) {
			latch = new CountDownLatch(expectedNumberOfChangeSet);
			receivedChangeSet = new ArrayList<RecordingChanges>();
		}

		public void expectChangeSet(int expectedNumberOfChangeSet) {
			latch = new CountDownLatch(expectedNumberOfChangeSet);
			receivedChangeSet.clear();
		}

		@Override
		public void notify(ChangeSet changeSet) {
			if (!(changeSet instanceof RecordingChanges)) {
				throw new IllegalArgumentException(
						"Invalid type of change set received");
			}
			receivedChangeSet.add((RecordingChanges) changeSet);
			latch.countDown();
		}

		public void await() throws InterruptedException {
			latch.await(1000, TimeUnit.MILLISECONDS);
		}

		public List<RecordingChanges> getObservedChangeSet() {
			return receivedChangeSet;
		}
	}
}
