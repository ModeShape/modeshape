package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.jcr.Session;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ClientLoad.Client;
import org.modeshape.jcr.ClientLoad.ClientResultProcessor;
import org.modeshape.jcr.ModeShapeEngine.State;
import org.modeshape.jcr.RepositoryConfiguration.Default;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class ModeShapeEngineTest extends AbstractTransactionalTest {

    private RepositoryConfiguration config;
    private ModeShapeEngine engine;

    @Before
    public void beforeEach() throws Exception {
        config = RepositoryConfiguration.read("{ \"name\":\"my-repo\" }");
        engine = new ModeShapeEngine();
    }

    @After
    public void afterEach() throws Exception {
        try {
            engine.shutdown();
        } finally {
            engine = null;
            config = null;
        }
    }

    @Test
    public void shouldStart() {
        engine.start();
        assertThat(engine.getState(), is(State.RUNNING));
    }

    @Test
    public void shouldAllowStartToBeCalledMultipleTimes() {
        for (int i = 0; i != 10; ++i) {
            engine.start();
            assertThat(engine.getState(), is(State.RUNNING));
        }
    }

    @Test
    public void shouldAllowShutdownToBeCalledEvenIfNotRunning() {
        assertThat(engine.getState(), is(State.NOT_RUNNING));
        for (int i = 0; i != 10; ++i) {
            engine.shutdown();
            assertThat(engine.getState(), is(State.NOT_RUNNING));
        }
    }

    @Test
    public void shouldStartThenStopThenRestart() throws Exception {
        assertThat(engine.getState(), is(State.NOT_RUNNING));
        engine.start();
        assertThat(engine.getState(), is(State.RUNNING));
        for (int i = 0; i != 5; ++i) {
            engine.shutdown().get(3L, TimeUnit.SECONDS);
            assertThat(engine.getState(), is(State.NOT_RUNNING));
        }
        engine.start();
        assertThat(engine.getState(), is(State.RUNNING));
    }

    @Test
    public void shouldDeployRepositoryConfiguration() throws Exception {
        engine.start();
        JcrRepository repository = engine.deploy(config);
        assertThat(repository, is(notNullValue()));
        assertThat(repository, is(notNullValue()));
    }

    @Test( expected = ConfigurationException.class )
    public void shouldFailToDeployRepositoryConfigurationWithoutName() throws Throwable {
        config = new RepositoryConfiguration(); // without a name!
        assertThat(config.validate().hasErrors(), is(true));
        engine.start();
        engine.deploy(config);
    }

    @Test
    public void shouldNotAutomaticallyStartDeployedRepositories() throws Exception {
        engine.start();
        JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        engine.startRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));
        engine.shutdownRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
    }

    @Test
    public void shouldAutomaticallyStartRepositoryUponLogin() throws Exception {
        engine.start();
        JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        assertThat(repository.getState(), is(State.NOT_RUNNING));
        for (int i = 0; i != 4; ++i) {
            javax.jcr.Session session = repository.login();
            assertThat(repository.getState(), is(State.RUNNING));
            session.logout();
        }
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));
        engine.shutdownRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
    }

    @Test
    public void shouldAllowConcurrentLoginWhileRequiringAutoStartOfRepository() throws Exception {
        engine.start();
        final JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        assertThat(repository.getState(), is(State.NOT_RUNNING));

        List<Client<Session>> results = ClientLoad.runSimultaneously(10, new Callable<Session>() {
            @Override
            public Session call() throws Exception {
                return repository.login();
            }
        });

        // NOTE THAT MUCH OF THE TIME DELAY SEEN WITHIN THE CLIENT TIMES IS DUE TO THE EXECUTOR SERVICE AND THREAD SWITCHING.
        // We're only checking times here to make sure that they don't quit after failing to reach the barrier.

        final boolean print = false;

        ClientLoad.forEachResult(results, new ClientResultProcessor<Session>() {
            @Override
            public void process( Client<Session> clientResult ) throws Exception {
                assertThat(clientResult.isSuccess(), is(true));
                assertThat(clientResult.getTime(TimeUnit.SECONDS) < 20, is(true));
                clientResult.getResult().logout();
                if (print) System.out.println("Client result: " + clientResult.getTime(TimeUnit.MILLISECONDS) + " ms");
            }
        });

        long start = System.nanoTime();
        Session session = repository.login();
        if (print) System.out.println("Session result: "
                                      + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " ms");
        session.logout();

        // Make sure the repository is running ...
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));

        // Shutdown repository ...
        engine.shutdownRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
    }

    @Test
    public void shouldAllowConcurrentLoginOfAlreadyStartedRepository() throws Exception {
        engine.start();
        final JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        assertThat(repository.getState(), is(State.NOT_RUNNING));
        engine.startRepository(name).get(); // blocks
        assertThat(repository.getState(), is(State.RUNNING));

        // NOTE THAT MUCH OF THE TIME DELAY SEEN WITHIN THE CLIENT TIMES IS DUE TO THE EXECUTOR SERVICE AND THREAD SWITCHING.
        // We're only checking times here to make sure that they don't quit after failing to reach the barrier.
        // Plus, note that creating sessions within this thread happens very fast (usually < 1ms), whether its
        // the initial priming session, or the one created before all the client's sessions are terminated,
        // or the one created after all the others were closed.

        final boolean print = false;

        // Prime the sessions ...
        long start = System.nanoTime();
        Session session = repository.login();
        if (print) System.out.println("Initial session: "
                                      + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " ms");
        session.logout();

        // Now create bunch of sessions simultaneously ...
        List<Client<Session>> results = ClientLoad.run(20, new Callable<Session>() {
            @Override
            public Session call() throws Exception {
                return repository.login();
            }
        });

        // Create another session (before all the clients' sessions have been closed) ...
        start = System.nanoTime();
        session = repository.login();
        if (print) System.out.println("Before close: "
                                      + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " ms");
        session.logout();

        ClientLoad.forEachResult(results, new ClientResultProcessor<Session>() {
            @Override
            public void process( Client<Session> clientResult ) throws Exception {
                assertThat(clientResult.isSuccess(), is(true));
                assertThat(clientResult.getTime(TimeUnit.SECONDS) < 20, is(true));
                clientResult.getResult().logout();
                if (print) System.out.println("Client result: " + clientResult.getTime(TimeUnit.MICROSECONDS) + " ms");
            }
        });

        // Create another session (after all the clients' sessions have been closed) ...
        start = System.nanoTime();
        session = repository.login();
        if (print) System.out.println("After close: "
                                      + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + " ms");
        session.logout();

        // Make sure the repository is running ...
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));

        // Shutdown repository ...
        engine.shutdownRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
    }

    @Test
    public void shouldAllowUpdatingRepositoryConfigurationWhileNotRunning() throws Exception {
        engine.start();
        JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        assertThat(config.getBinaryStorage().getMinimumBinarySizeInBytes(), is(Default.MINIMUM_BINARY_SIZE_IN_BYTES));

        // Change the configuration ...
        long newLargeValueSizeInBytes = Default.MINIMUM_BINARY_SIZE_IN_BYTES * 2;
        Editor editor = repository.getConfiguration().edit();
        EditableDocument binaryStorage = editor.getOrCreateDocument(FieldName.STORAGE)
                                               .getOrCreateDocument(FieldName.BINARY_STORAGE);
        binaryStorage.setNumber(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, newLargeValueSizeInBytes);
        Changes changes = editor.getChanges();

        // Apply the changes to the deployed repository ...
        engine.update(name, changes).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));

        RepositoryConfiguration newConfig = engine.getRepository(name).getConfiguration();
        assertThat(newConfig.getBinaryStorage().getMinimumBinarySizeInBytes(), is(newLargeValueSizeInBytes));
    }

    @Test
    public void shouldAllowUpdatingRepositoryConfigurationWhileRunning() throws Exception {
        engine.start();
        JcrRepository repository = engine.deploy(config);
        String name = repository.getName();
        assertThat(engine.getRepositoryState(name), is(State.NOT_RUNNING));
        engine.startRepository(name).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));
        long defaultLargeValueSize = Default.MINIMUM_BINARY_SIZE_IN_BYTES;
        assertThat(config.getBinaryStorage().getMinimumBinarySizeInBytes(), is(defaultLargeValueSize));
        assertThat(repository.repositoryCache().largeValueSizeInBytes(), is(defaultLargeValueSize));

        // Change the configuration. We'll do something simple, like changing the large value size ...
        long newLargeValueSizeInBytes = defaultLargeValueSize * 2L;
        Editor editor = repository.getConfiguration().edit();
        EditableDocument binaryStorage = editor.getOrCreateDocument(FieldName.STORAGE)
                                               .getOrCreateDocument(FieldName.BINARY_STORAGE);
        binaryStorage.setNumber(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, newLargeValueSizeInBytes);
        Changes changes = editor.getChanges();

        // Apply the changes to the deployed repository ...
        engine.update(name, changes).get(); // blocks
        assertThat(engine.getRepositoryState(name), is(State.RUNNING));

        // Verify the running repository and its configuraiton are using the new value ...
        RepositoryConfiguration newConfig = engine.getRepository(name).getConfiguration();
        assertThat(newConfig.getBinaryStorage().getMinimumBinarySizeInBytes(), is(newLargeValueSizeInBytes));
        assertThat(repository.repositoryCache().largeValueSizeInBytes(), is(newLargeValueSizeInBytes));
    }

    @Test
    public void shouldAllowUpdatingSequencerInformationWhenRunning() throws Exception {
        URL configUrl = getClass().getClassLoader().getResource("config/repo-config.json");
        engine.start();
        config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = engine.deploy(config);

        // Obtain an editor ...
        Editor editor = repository.getConfiguration().edit();
        EditableDocument sequencing = editor.getDocument(FieldName.SEQUENCING);
        EditableDocument sequencers = sequencing.getDocument(FieldName.SEQUENCERS);
        EditableDocument sequencerA = sequencers.getDocument("CND sequencer");

        // Verify the existing value ...
        List<?> exprs = sequencerA.getArray(FieldName.PATH_EXPRESSIONS);
        assertThat(exprs.size(), is(1));
        assertThat((String)exprs.get(0), is("default://(*.cnd)/jcr:content[@jcr:data]"));

        // Set the new value ...
        sequencerA.setArray(FieldName.PATH_EXPRESSIONS, "//*.ddl", "//*.xml");

        // And apply the changes to the repository's configuration ...
        Changes changes = editor.getChanges();
        engine.update(config.getName(), changes).get(); // don't forget to wait!

        // Verify the configuration was changed successfully ...
        RepositoryConfiguration config2 = engine.getRepositoryConfiguration(config.getName());
        Document sequencerA2 = (Document)config2.getDocument()
                                                .getDocument(FieldName.SEQUENCING)
                                                .getDocument(FieldName.SEQUENCERS)
                                                .get("CND sequencer");
        List<?> exprs2 = sequencerA2.getArray(FieldName.PATH_EXPRESSIONS);
        assertThat(exprs2.size(), is(2));
        assertThat((String)exprs2.get(0), is("//*.ddl"));
        assertThat((String)exprs2.get(1), is("//*.xml"));
    }
}
