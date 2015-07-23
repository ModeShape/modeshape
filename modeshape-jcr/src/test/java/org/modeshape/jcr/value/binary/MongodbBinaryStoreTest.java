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
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * Setup mongodb env and uncomment tests to run.
 * 
 * @author kulikov
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class MongodbBinaryStoreTest extends AbstractBinaryStoreTest {
    private static final Logger LOGGER = Logger.getLogger("MongoDBOutput");

    private static MongodProcess mongodProcess;
    private static MongodExecutable mongodExecutable;

    private static MongodbBinaryStore binaryStore;

    static {
        try {
            LOGGER.addHandler(new FileHandler("target/mongoDB_output.txt", false));
            LOGGER.setLevel(Level.SEVERE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        RuntimeConfig config = RuntimeConfig.getInstance(LOGGER);
        MongodStarter runtime = MongodStarter.getInstance(config);
        int freeServerPort = Network.getFreeServerPort();
        mongodExecutable = runtime.prepare(new MongodConfig(Version.Main.V2_3, freeServerPort,
                                                                             Network.localhostIsIPv6()));
        mongodProcess = mongodExecutable.start();

        binaryStore = new MongodbBinaryStore("localhost", freeServerPort, "test-" + UUID.randomUUID());
        binaryStore.setMimeTypeDetector(DEFAULT_DETECTOR);
        binaryStore.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            binaryStore.shutdown();
            mongodExecutable.stop();
            mongodProcess.stop();
        } catch (Throwable t) {
            //ignore
        }
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return binaryStore;
    }
}
