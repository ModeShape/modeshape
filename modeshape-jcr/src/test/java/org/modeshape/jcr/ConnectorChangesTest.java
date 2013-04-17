package org.modeshape.jcr;

import static org.junit.Assert.assertTrue;
import static org.modeshape.common.logging.Logger.getLogger;

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
		listener = new TestListener();
		changeBus.register(listener);
	}

	@After
	public void afterEach() throws Exception {
		stopRepository();
		logger.debug("Stopped repository.");
	}

	@Test
	public void testChangesEmittedWhenNodeCreated() throws Exception {
		logger.debug("Executing testChangesEmittedWhenNodeCreated()...");
		FileUtil.delete("target/federation_persistent_repository");
		final Session session = session();
		final Node root = session.getRootNode();
		logger.debug("Root node is: ");
		new JcrTools(true).printSubgraph(root, 2);
		final Node federation = session.getNode("/federation");
		federation.addNode("testNode");
		session.save();
		assertTrue("Didn't receive changes after node creation!",
				listener.receivedChangeSet.size() > 0);
		logger.debug("Executed testChangesEmittedWhenNodeCreated().");

	}

	protected static class TestListener implements ChangeSetListener {
		private final List<TestChangeSet> receivedChangeSet;
		private CountDownLatch latch;

		public TestListener() {
			this(0);
		}

		protected TestListener(int expectedNumberOfChangeSet) {
			latch = new CountDownLatch(expectedNumberOfChangeSet);
			receivedChangeSet = new ArrayList<TestChangeSet>();
		}

		public void expectChangeSet(int expectedNumberOfChangeSet) {
			latch = new CountDownLatch(expectedNumberOfChangeSet);
			receivedChangeSet.clear();
		}

		@Override
		public void notify(ChangeSet changeSet) {
			if (!(changeSet instanceof TestChangeSet)) {
				throw new IllegalArgumentException(
						"Invalid type of change set received");
			}
			receivedChangeSet.add((TestChangeSet) changeSet);
			latch.countDown();
		}

		public void await() throws InterruptedException {
			latch.await(250, TimeUnit.MILLISECONDS);
		}

		public List<TestChangeSet> getObservedChangeSet() {
			return receivedChangeSet;
		}
	}

	protected static class TestChangeSet implements ChangeSet {

		private static final long serialVersionUID = 1L;

		private final String workspaceName;
		private final DateTime dateTime;

		protected TestChangeSet(String workspaceName) {
			this.workspaceName = workspaceName;
			this.dateTime = new JodaDateTime(System.currentTimeMillis());
		}

		@Override
		public Set<NodeKey> changedNodes() {
			return Collections.emptySet();
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public String getUserId() {
			return null;
		}

		@Override
		public Map<String, String> getUserData() {
			return Collections.emptyMap();
		}

		@Override
		public DateTime getTimestamp() {
			return dateTime;
		}

		@Override
		public String getProcessKey() {
			return null;
		}

		@Override
		public String getRepositoryKey() {
			return null;
		}

		@Override
		public String getWorkspaceName() {
			return workspaceName;
		}

		@Override
		public Iterator<Change> iterator() {
			return Collections.<Change> emptySet().iterator();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			TestChangeSet changes = (TestChangeSet) o;

			if (!dateTime.equals(changes.dateTime))
				return false;
			if (!workspaceName.equals(changes.workspaceName))
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = workspaceName.hashCode();
			result = 31 * result + dateTime.hashCode();
			return result;
		}
	}
}
