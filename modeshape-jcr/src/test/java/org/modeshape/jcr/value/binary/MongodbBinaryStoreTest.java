/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.binary;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        mongodExecutable = runtime.prepare(new MongodConfig(Version.Main.V2_0, freeServerPort,
                                                                             Network.localhostIsIPv6()));
        mongodProcess = mongodExecutable.start();

        binaryStore = new MongodbBinaryStore(Network.getLocalHost().getHostAddress(), freeServerPort, "test-" + UUID.randomUUID());
        binaryStore.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        binaryStore.shutdown();
        mongodExecutable.stop();
        mongodProcess.stop();
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return binaryStore;
    }
}
